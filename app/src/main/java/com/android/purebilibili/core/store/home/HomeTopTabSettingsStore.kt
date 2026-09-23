package com.android.purebilibili.core.store.home

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.android.purebilibili.core.store.DEFAULT_TOP_TAB_ORDER
import com.android.purebilibili.core.store.DEFAULT_TOP_TAB_VISIBLE
import com.android.purebilibili.core.store.DEFAULT_TOP_TAB_LABEL_MODE
import com.android.purebilibili.core.store.MAX_HOME_TOP_TABS
import com.android.purebilibili.core.store.HomeTopLayoutOrder
import com.android.purebilibili.core.store.HomeTopRightAction
import com.android.purebilibili.core.store.HomeTopTabSettings
import com.android.purebilibili.core.store.KEY_HIDE_TOP_TABS
import com.android.purebilibili.core.store.KEY_HOME_TOP_LAYOUT_ORDER
import com.android.purebilibili.core.store.KEY_HOME_TOP_RIGHT_ACTION
import com.android.purebilibili.core.store.KEY_TOP_TAB_LABEL_MODE
import com.android.purebilibili.core.store.KEY_TOP_TAB_ORDER
import com.android.purebilibili.core.store.KEY_TOP_TAB_VISIBLE_TABS
import com.android.purebilibili.core.store.settingsDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

object HomeTopTabSettingsStore {
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

    fun observe(context: Context): Flow<HomeTopTabSettings> = context.settingsDataStore.data
        .map(::mapFromPreferences).distinctUntilChanged()
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
    fun getTopTabOrder(context: Context): Flow<List<String>> = context.settingsDataStore.data.map { preferences ->
        (preferences[KEY_TOP_TAB_ORDER] ?: DEFAULT_TOP_TAB_ORDER).split(",").filter { it.isNotBlank() }
    }
    suspend fun setTopTabOrder(context: Context, order: List<String>) {
        context.settingsDataStore.edit { preferences -> preferences[KEY_TOP_TAB_ORDER] = order.joinToString(",") }
    }
    fun getTopTabVisibleTabs(context: Context): Flow<Set<String>> = context.settingsDataStore.data.map { preferences ->
        (preferences[KEY_TOP_TAB_VISIBLE_TABS] ?: DEFAULT_TOP_TAB_VISIBLE).split(",").filter { it.isNotBlank() }.toSet()
    }
    suspend fun setTopTabVisibleTabs(context: Context, tabs: Set<String>) {
        context.settingsDataStore.edit { preferences -> preferences[KEY_TOP_TAB_VISIBLE_TABS] = tabs.joinToString(",") }
    }
}