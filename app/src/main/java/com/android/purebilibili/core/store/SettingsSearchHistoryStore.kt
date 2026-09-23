package com.android.purebilibili.core.store

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.map
import org.json.JSONArray

internal fun updatedSettingsSearchHistory(history: List<String>, query: String): List<String> {
    val keyword = query.trim()
    if (keyword.isEmpty()) return history
    return (listOf(keyword) + history.filterNot { it.equals(keyword, ignoreCase = true) }).take(20)
}

object SettingsSearchHistoryStore {
    private val key = stringPreferencesKey("settings_search_history")

    private fun decode(raw: String?): List<String> = runCatching {
        val array = JSONArray(raw ?: "[]")
        List(array.length()) { array.getString(it) }
    }.getOrDefault(emptyList())

    fun observe(context: Context) = context.settingsDataStore.data.map { decode(it[key]) }

    suspend fun record(context: Context, query: String) {
        if (query.isBlank()) return
        context.settingsDataStore.edit {
            it[key] = JSONArray(updatedSettingsSearchHistory(decode(it[key]), query)).toString()
        }
    }

    suspend fun delete(context: Context, query: String) {
        context.settingsDataStore.edit {
            it[key] = JSONArray(decode(it[key]).filterNot { entry -> entry == query }).toString()
        }
    }

    suspend fun clear(context: Context) {
        context.settingsDataStore.edit { it.remove(key) }
    }
}
