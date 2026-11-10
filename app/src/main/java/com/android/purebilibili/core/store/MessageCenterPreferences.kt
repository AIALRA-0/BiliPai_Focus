package com.android.purebilibili.core.store

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 未读标记的展示形态；数值即存储值，新增选项只能追加不能重排。 */
enum class UnreadBadgeMode(val label: String) {
    HIDDEN("隐藏"),
    DOT("红点"),
    NUMBER("数字");

    companion object {
        fun fromStored(value: Int): UnreadBadgeMode = entries.firstOrNull { it.ordinal == value } ?: NUMBER
    }
}

/** 仅控制本地消息入口与未读标记形态；服务端接收范围由消息设置独立保存。 */
internal object MessageCenterPreferences {
    private val disableLikesKey = booleanPreferencesKey("disable_likes")
    private val dynamicBadgeModeKey = intPreferencesKey("dynamic_badge_mode")
    private val messageBadgeModeKey = intPreferencesKey("message_badge_mode")
    private fun store(context: Context) = MultiProcessPreferences.get(context.applicationContext, "message_center_preferences")
    fun disabledLikes(context: Context) = store(context).data.map { it[disableLikesKey] ?: false }
    suspend fun setDisabledLikes(context: Context, disabled: Boolean) { store(context).edit { it[disableLikesKey] = disabled } }

    fun dynamicBadgeMode(context: Context): Flow<UnreadBadgeMode> =
        store(context).data.map { UnreadBadgeMode.fromStored(it[dynamicBadgeModeKey] ?: UnreadBadgeMode.NUMBER.ordinal) }
    suspend fun setDynamicBadgeMode(context: Context, mode: UnreadBadgeMode) { store(context).edit { it[dynamicBadgeModeKey] = mode.ordinal } }

    fun messageBadgeMode(context: Context): Flow<UnreadBadgeMode> =
        store(context).data.map { UnreadBadgeMode.fromStored(it[messageBadgeModeKey] ?: UnreadBadgeMode.NUMBER.ordinal) }
    suspend fun setMessageBadgeMode(context: Context, mode: UnreadBadgeMode) { store(context).edit { it[messageBadgeModeKey] = mode.ordinal } }
}
