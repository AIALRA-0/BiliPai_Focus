package com.android.purebilibili.core.store.home

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.android.purebilibili.core.store.DEFAULT_TOP_TAB_ORDER
import com.android.purebilibili.core.store.DEFAULT_TOP_TAB_VISIBLE
import com.android.purebilibili.core.store.DEFAULT_TOP_TAB_LABEL_MODE
import com.android.purebilibili.core.store.FocusSettingsStore
import com.android.purebilibili.core.store.MAX_HOME_TOP_TABS
import com.android.purebilibili.core.store.HomeTopLayoutOrder
import com.android.purebilibili.core.store.HomeTopRightAction
import com.android.purebilibili.core.store.HomeTopTabSettings
import com.android.purebilibili.core.store.KEY_FOCUS_HOME_TOP_TABS_MIGRATION_VERSION
import com.android.purebilibili.core.store.KEY_HIDE_TOP_TABS
import com.android.purebilibili.core.store.KEY_HOME_TOP_LAYOUT_ORDER
import com.android.purebilibili.core.store.KEY_HOME_TOP_RIGHT_ACTION
import com.android.purebilibili.core.store.KEY_TOP_TAB_LABEL_MODE
import com.android.purebilibili.core.store.KEY_TOP_TAB_ORDER
import com.android.purebilibili.core.store.KEY_TOP_TAB_VISIBLE_TABS
import com.android.purebilibili.core.store.resolveFocusHomeTopTabMigration
import com.android.purebilibili.core.store.settingsDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

object HomeTopTabSettingsStore {
    private const val FOCUS_HOME_TOP_TABS_MIGRATION_VERSION = 1

    internal fun mapFromPreferences(preferences: Preferences): HomeTopTabSettings {
        val orderIds = (preferences[KEY_TOP_TAB_ORDER] ?: DEFAULT_TOP_TAB_ORDER).split(",").filter { it.isNotBlank() }
        val visibleIds = (preferences[KEY_TOP_TAB_VISIBLE_TABS] ?: DEFAULT_TOP_TAB_VISIBLE)
            .split(",").filter { it.isNotBlank() }.toSet()
        val cappedVisibleIds = if (visibleIds.size <= MAX_HOME_TOP_TABS) visibleIds else {
            orderIds.filter { it in visibleIds }.take(MAX_HOME_TOP_TABS).toSet()
        }
        return HomeTopTabSettings(
            orderIds = orderIds,
            visibleIds = cappedVisibleIds,
            hideTopTabs = preferences[KEY_HIDE_TOP_TABS] ?: false,
        )
    }

    fun observe(context: Context): Flow<HomeTopTabSettings> = afterLegacyMigration(context) {
        context.settingsDataStore.data.map(::mapFromPreferences).distinctUntilChanged()
    }
    fun getTopTabLabelMode(context: Context): Flow<Int> = context.settingsDataStore.data
        .map { preferences -> preferences[KEY_TOP_TAB_LABEL_MODE] ?: DEFAULT_TOP_TAB_LABEL_MODE }
    suspend fun setTopTabLabelMode(context: Context, value: Int) {
        context.settingsDataStore.edit { preferences -> preferences[KEY_TOP_TAB_LABEL_MODE] = value }
    }
    fun getHideTopTabs(context: Context): Flow<Boolean> = context.settingsDataStore.data
        .map { preferences -> preferences[KEY_HIDE_TOP_TABS] ?: false }.distinctUntilChanged()
    suspend fun setHideTopTabs(context: Context, hide: Boolean) {
        context.settingsDataStore.edit { preferences -> preferences[KEY_HIDE_TOP_TABS] = hide }
    }
    fun getHomeTopRightAction(context: Context): Flow<HomeTopRightAction> = context.settingsDataStore.data.map { preferences ->
        HomeTopRightAction.fromValue(preferences[KEY_HOME_TOP_RIGHT_ACTION] ?: HomeTopRightAction.SETTINGS.value)
    }
    suspend fun setHomeTopRightAction(context: Context, action: HomeTopRightAction) {
        context.settingsDataStore.edit { preferences -> preferences[KEY_HOME_TOP_RIGHT_ACTION] = action.value }
    }
    fun getHomeTopLayoutOrder(context: Context): Flow<HomeTopLayoutOrder> = context.settingsDataStore.data.map { preferences ->
        HomeTopLayoutOrder.fromValue(preferences[KEY_HOME_TOP_LAYOUT_ORDER] ?: HomeTopLayoutOrder.SEARCH_THEN_TABS.value)
    }
    suspend fun setHomeTopLayoutOrder(context: Context, order: HomeTopLayoutOrder) {
        context.settingsDataStore.edit { preferences -> preferences[KEY_HOME_TOP_LAYOUT_ORDER] = order.value }
    }
    fun getTopTabOrder(context: Context): Flow<List<String>> = afterLegacyMigration(context) {
        context.settingsDataStore.data.map { preferences ->
            (preferences[KEY_TOP_TAB_ORDER] ?: DEFAULT_TOP_TAB_ORDER).split(",").filter { it.isNotBlank() }
        }
    }
    suspend fun setTopTabOrder(context: Context, order: List<String>) {
        migrateLegacyFocusHomeTopTabs(context)
        context.settingsDataStore.edit { preferences -> preferences[KEY_TOP_TAB_ORDER] = order.joinToString(",") }
    }
    fun getTopTabVisibleTabs(context: Context): Flow<Set<String>> = afterLegacyMigration(context) {
        context.settingsDataStore.data.map { preferences -> mapFromPreferences(preferences).visibleIds }
    }
    suspend fun setTopTabVisibleTabs(context: Context, tabs: Set<String>) {
        migrateLegacyFocusHomeTopTabs(context)
        context.settingsDataStore.edit { preferences ->
            val order = (preferences[KEY_TOP_TAB_ORDER] ?: DEFAULT_TOP_TAB_ORDER)
                .split(",")
                .filter { it.isNotBlank() }
            val orderedSelection = (order.filter { it in tabs } + tabs).distinct()
            val cappedSelection = orderedSelection.take(MAX_HOME_TOP_TABS)
            preferences[KEY_TOP_TAB_VISIBLE_TABS] = cappedSelection.joinToString(",")
        }
    }

    private fun <T> afterLegacyMigration(
        context: Context,
        source: () -> Flow<T>,
    ): Flow<T> = flow {
        migrateLegacyFocusHomeTopTabs(context)
        emitAll(source())
    }

    private suspend fun migrateLegacyFocusHomeTopTabs(context: Context) {
        context.settingsDataStore.edit { preferences ->
            if ((preferences[KEY_FOCUS_HOME_TOP_TABS_MIGRATION_VERSION] ?: 0) >=
                FOCUS_HOME_TOP_TABS_MIGRATION_VERSION
            ) {
                return@edit
            }
            val legacyOrder = preferences[KEY_TOP_TAB_ORDER]
                ?.split(",")
                ?.filter { it.isNotBlank() }
            val legacyVisible = preferences[KEY_TOP_TAB_VISIBLE_TABS]
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?.toSet()
            val migration = resolveFocusHomeTopTabMigration(
                orderIds = legacyOrder,
                visibleIds = legacyVisible,
                focusSettings = FocusSettingsStore.mapLegacyHomeTabVisibilityFromPreferences(preferences),
            )
            preferences[KEY_TOP_TAB_ORDER] = migration.orderIds.joinToString(",")
            preferences[KEY_TOP_TAB_VISIBLE_TABS] = migration.visibleIds.joinToString(",")
            preferences[KEY_FOCUS_HOME_TOP_TABS_MIGRATION_VERSION] =
                FOCUS_HOME_TOP_TABS_MIGRATION_VERSION
        }
    }
}
