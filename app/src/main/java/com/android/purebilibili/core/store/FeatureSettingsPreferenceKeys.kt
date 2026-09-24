package com.android.purebilibili.core.store

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

internal val KEY_SEARCH_HOT_SECTION_ENABLED = booleanPreferencesKey("search_hot_section_enabled")
internal val KEY_SEARCH_DISCOVER_SECTION_ENABLED = booleanPreferencesKey("search_discover_section_enabled")
internal val KEY_SEARCH_HISTORY_SECTION_ENABLED = booleanPreferencesKey("search_history_section_enabled")

internal val KEY_TOP_TAB_LABEL_MODE = intPreferencesKey("top_tab_label_mode")
internal val KEY_HIDE_TOP_TABS = booleanPreferencesKey("hide_top_tabs")
internal val KEY_HOME_TOP_RIGHT_ACTION = intPreferencesKey("home_top_right_action")
internal val KEY_TOP_TAB_ORDER = stringPreferencesKey("top_tab_order")
internal val KEY_TOP_TAB_VISIBLE_TABS = stringPreferencesKey("top_tab_visible_tabs")
internal val KEY_FOCUS_HOME_TOP_TABS_MIGRATION_VERSION = intPreferencesKey("focus_home_top_tabs_migration_version")
internal val KEY_HOME_TOP_LAYOUT_ORDER = intPreferencesKey("home_top_layout_order")

// Retained for the one-time migration from the old duplicate Focus visibility layer.
internal val KEY_FOCUS_HOME_RECOMMEND_TAB_VISIBLE = booleanPreferencesKey("focus_home_recommend_tab_visible")
internal val KEY_FOCUS_HOME_FOLLOW_TAB_VISIBLE = booleanPreferencesKey("focus_home_follow_tab_visible")
internal val KEY_FOCUS_HOME_POPULAR_TAB_VISIBLE = booleanPreferencesKey("focus_home_popular_tab_visible")
internal val KEY_FOCUS_HOME_LIVE_TAB_VISIBLE = booleanPreferencesKey("focus_home_live_tab_visible")
internal val KEY_FOCUS_HOME_ANIME_TAB_VISIBLE = booleanPreferencesKey("focus_home_anime_tab_visible")
internal val KEY_FOCUS_HOME_GAME_TAB_VISIBLE = booleanPreferencesKey("focus_home_game_tab_visible")
internal val KEY_FOCUS_HOME_KNOWLEDGE_TAB_VISIBLE = booleanPreferencesKey("focus_home_knowledge_tab_visible")
internal val KEY_FOCUS_HOME_TECH_TAB_VISIBLE = booleanPreferencesKey("focus_home_tech_tab_visible")
internal val KEY_FOCUS_HOME_PARTITION_BUTTON_VISIBLE = booleanPreferencesKey("focus_home_partition_button_visible")
internal val KEY_FOCUS_FOLLOW_GROUP_FILTERING_ENABLED = booleanPreferencesKey("focus_follow_group_filtering_enabled")
internal val KEY_FOCUS_VIDEO_RELATED_VIDEOS_SECTION_VISIBLE = booleanPreferencesKey("focus_video_related_videos_section_visible")
internal val KEY_FOCUS_HISTORY_CLEAR_ALL_ACTION_ENABLED = booleanPreferencesKey("focus_history_clear_all_action_enabled")

internal const val DEFAULT_TOP_TAB_ORDER = "FOLLOW,SUBSCRIPTIONS"
internal const val DEFAULT_TOP_TAB_VISIBLE = "FOLLOW,SUBSCRIPTIONS"
internal const val DEFAULT_TOP_TAB_LABEL_MODE = 2
internal const val MAX_HOME_TOP_TABS = 5
