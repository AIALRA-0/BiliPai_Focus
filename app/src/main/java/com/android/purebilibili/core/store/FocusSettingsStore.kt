package com.android.purebilibili.core.store

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

object FocusSettingsStore {
    internal fun mapFromPreferences(preferences: Preferences): FocusSettings = FocusSettings(
        showHomeRecommendTab = preferences[KEY_FOCUS_HOME_RECOMMEND_TAB_VISIBLE] ?: false,
        showHomeFollowTab = preferences[KEY_FOCUS_HOME_FOLLOW_TAB_VISIBLE] ?: true,
        showHomePopularTab = preferences[KEY_FOCUS_HOME_POPULAR_TAB_VISIBLE] ?: false,
        showHomeLiveTab = preferences[KEY_FOCUS_HOME_LIVE_TAB_VISIBLE] ?: false,
        showHomeAnimeTab = preferences[KEY_FOCUS_HOME_ANIME_TAB_VISIBLE] ?: true,
        showHomeGameTab = preferences[KEY_FOCUS_HOME_GAME_TAB_VISIBLE] ?: false,
        showHomeKnowledgeTab = preferences[KEY_FOCUS_HOME_KNOWLEDGE_TAB_VISIBLE] ?: true,
        showHomeTechTab = preferences[KEY_FOCUS_HOME_TECH_TAB_VISIBLE] ?: true,
        showHomePartitionButton = preferences[KEY_FOCUS_HOME_PARTITION_BUTTON_VISIBLE] ?: false,
        enableFollowGroupFiltering = preferences[KEY_FOCUS_FOLLOW_GROUP_FILTERING_ENABLED] ?: true,
        showVideoRelatedVideosSection = preferences[KEY_FOCUS_VIDEO_RELATED_VIDEOS_SECTION_VISIBLE] ?: false,
        showHistoryClearAllAction = preferences[KEY_FOCUS_HISTORY_CLEAR_ALL_ACTION_ENABLED] ?: true,
        showSearchHotSection = preferences[KEY_SEARCH_HOT_SECTION_ENABLED] ?: false,
        showSearchDiscoverSection = preferences[KEY_SEARCH_DISCOVER_SECTION_ENABLED] ?: false,
        showSearchHistorySection = preferences[KEY_SEARCH_HISTORY_SECTION_ENABLED] ?: false,
    )

    fun observe(context: Context): Flow<FocusSettings> = context.settingsDataStore.data
        .map(::mapFromPreferences)
        .distinctUntilChanged()

    fun getSearchHotSectionEnabled(context: Context): Flow<Boolean> = observeBoolean(context, KEY_SEARCH_HOT_SECTION_ENABLED, false)
    suspend fun setSearchHotSectionEnabled(context: Context, value: Boolean) = writeBoolean(context, KEY_SEARCH_HOT_SECTION_ENABLED, value)
    fun getHomeRecommendTabVisible(context: Context): Flow<Boolean> = observeBoolean(context, KEY_FOCUS_HOME_RECOMMEND_TAB_VISIBLE, false)
    suspend fun setHomeRecommendTabVisible(context: Context, value: Boolean) = writeBoolean(context, KEY_FOCUS_HOME_RECOMMEND_TAB_VISIBLE, value)
    fun getHomeFollowTabVisible(context: Context): Flow<Boolean> = observeBoolean(context, KEY_FOCUS_HOME_FOLLOW_TAB_VISIBLE, true)
    suspend fun setHomeFollowTabVisible(context: Context, value: Boolean) = writeBoolean(context, KEY_FOCUS_HOME_FOLLOW_TAB_VISIBLE, value)
    fun getHomePopularTabVisible(context: Context): Flow<Boolean> = observeBoolean(context, KEY_FOCUS_HOME_POPULAR_TAB_VISIBLE, false)
    suspend fun setHomePopularTabVisible(context: Context, value: Boolean) = writeBoolean(context, KEY_FOCUS_HOME_POPULAR_TAB_VISIBLE, value)
    fun getHomeLiveTabVisible(context: Context): Flow<Boolean> = observeBoolean(context, KEY_FOCUS_HOME_LIVE_TAB_VISIBLE, false)
    suspend fun setHomeLiveTabVisible(context: Context, value: Boolean) = writeBoolean(context, KEY_FOCUS_HOME_LIVE_TAB_VISIBLE, value)
    fun getHomeAnimeTabVisible(context: Context): Flow<Boolean> = observeBoolean(context, KEY_FOCUS_HOME_ANIME_TAB_VISIBLE, true)
    suspend fun setHomeAnimeTabVisible(context: Context, value: Boolean) = writeBoolean(context, KEY_FOCUS_HOME_ANIME_TAB_VISIBLE, value)
    fun getHomeGameTabVisible(context: Context): Flow<Boolean> = observeBoolean(context, KEY_FOCUS_HOME_GAME_TAB_VISIBLE, false)
    suspend fun setHomeGameTabVisible(context: Context, value: Boolean) = writeBoolean(context, KEY_FOCUS_HOME_GAME_TAB_VISIBLE, value)
    fun getHomeKnowledgeTabVisible(context: Context): Flow<Boolean> = observeBoolean(context, KEY_FOCUS_HOME_KNOWLEDGE_TAB_VISIBLE, true)
    suspend fun setHomeKnowledgeTabVisible(context: Context, value: Boolean) = writeBoolean(context, KEY_FOCUS_HOME_KNOWLEDGE_TAB_VISIBLE, value)
    fun getHomeTechTabVisible(context: Context): Flow<Boolean> = observeBoolean(context, KEY_FOCUS_HOME_TECH_TAB_VISIBLE, true)
    suspend fun setHomeTechTabVisible(context: Context, value: Boolean) = writeBoolean(context, KEY_FOCUS_HOME_TECH_TAB_VISIBLE, value)
    fun getHomePartitionButtonVisible(context: Context): Flow<Boolean> = observeBoolean(context, KEY_FOCUS_HOME_PARTITION_BUTTON_VISIBLE, false)
    suspend fun setHomePartitionButtonVisible(context: Context, value: Boolean) = writeBoolean(context, KEY_FOCUS_HOME_PARTITION_BUTTON_VISIBLE, value)
    fun getFollowGroupFilteringEnabled(context: Context): Flow<Boolean> = observeBoolean(context, KEY_FOCUS_FOLLOW_GROUP_FILTERING_ENABLED, true)
    suspend fun setFollowGroupFilteringEnabled(context: Context, value: Boolean) = writeBoolean(context, KEY_FOCUS_FOLLOW_GROUP_FILTERING_ENABLED, value)
    fun getVideoRelatedVideosSectionVisible(context: Context): Flow<Boolean> = observeBoolean(context, KEY_FOCUS_VIDEO_RELATED_VIDEOS_SECTION_VISIBLE, false)
    suspend fun setVideoRelatedVideosSectionVisible(context: Context, value: Boolean) = writeBoolean(context, KEY_FOCUS_VIDEO_RELATED_VIDEOS_SECTION_VISIBLE, value)
    fun getHistoryClearAllActionEnabled(context: Context): Flow<Boolean> = observeBoolean(context, KEY_FOCUS_HISTORY_CLEAR_ALL_ACTION_ENABLED, true)
    suspend fun setHistoryClearAllActionEnabled(context: Context, value: Boolean) = writeBoolean(context, KEY_FOCUS_HISTORY_CLEAR_ALL_ACTION_ENABLED, value)
    fun getSearchDiscoverSectionEnabled(context: Context): Flow<Boolean> = observeBoolean(context, KEY_SEARCH_DISCOVER_SECTION_ENABLED, false)
    suspend fun setSearchDiscoverSectionEnabled(context: Context, value: Boolean) = writeBoolean(context, KEY_SEARCH_DISCOVER_SECTION_ENABLED, value)
    fun getSearchHistorySectionEnabled(context: Context): Flow<Boolean> = observeBoolean(context, KEY_SEARCH_HISTORY_SECTION_ENABLED, false)
    suspend fun setSearchHistorySectionEnabled(context: Context, value: Boolean) = writeBoolean(context, KEY_SEARCH_HISTORY_SECTION_ENABLED, value)

    private fun observeBoolean(context: Context, key: Preferences.Key<Boolean>, defaultValue: Boolean): Flow<Boolean> =
        context.settingsDataStore.data.map { preferences -> preferences[key] ?: defaultValue }

    private suspend fun writeBoolean(context: Context, key: Preferences.Key<Boolean>, value: Boolean) {
        context.settingsDataStore.edit { preferences -> preferences[key] = value }
    }
}