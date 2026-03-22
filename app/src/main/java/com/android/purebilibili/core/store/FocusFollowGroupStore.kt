package com.android.purebilibili.core.store

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random

const val DEFAULT_FOCUS_FOLLOW_GROUP_ID = "default"
const val DEFAULT_FOCUS_FOLLOW_GROUP_NAME = "默认分组"

@Serializable
data class FocusFollowGroup(
    val id: String = DEFAULT_FOCUS_FOLLOW_GROUP_ID,
    val name: String = DEFAULT_FOCUS_FOLLOW_GROUP_NAME,
    val visible: Boolean = true
)

@Serializable
data class FocusFollowGroupConfig(
    val groups: List<FocusFollowGroup> = listOf(defaultFocusFollowGroup()),
    val assignments: Map<String, String> = emptyMap()
)

private val KEY_FOCUS_FOLLOW_GROUP_CONFIG =
    stringPreferencesKey("focus_follow_group_config_json_v1")

private val focusFollowGroupJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

fun defaultFocusFollowGroup(): FocusFollowGroup {
    return FocusFollowGroup(
        id = DEFAULT_FOCUS_FOLLOW_GROUP_ID,
        name = DEFAULT_FOCUS_FOLLOW_GROUP_NAME,
        visible = true
    )
}

fun normalizeFocusFollowGroupName(name: String): String {
    return name.trim().ifBlank { "未命名分组" }
}

fun normalizeFocusFollowGroupConfig(
    config: FocusFollowGroupConfig,
    knownUserMids: Set<Long>? = null
): FocusFollowGroupConfig {
    val defaultGroupVisible = config.groups
        .firstOrNull { it.id == DEFAULT_FOCUS_FOLLOW_GROUP_ID }
        ?.visible
        ?: true

    val customGroups = config.groups
        .asSequence()
        .filter { it.id.isNotBlank() && it.id != DEFAULT_FOCUS_FOLLOW_GROUP_ID }
        .distinctBy { it.id }
        .map { group ->
            group.copy(name = normalizeFocusFollowGroupName(group.name))
        }
        .toList()

    val groups = buildList {
        add(defaultFocusFollowGroup().copy(visible = defaultGroupVisible))
        addAll(customGroups)
    }
    val validGroupIds = groups.map { it.id }.toSet()
    val validKnownUsers = knownUserMids?.filter { it > 0L }?.toSet()

    val assignments = buildMap {
        config.assignments.forEach { (midKey, groupId) ->
            val mid = midKey.toLongOrNull()?.takeIf { it > 0L } ?: return@forEach
            if (validKnownUsers != null && mid !in validKnownUsers) return@forEach
            if (groupId == DEFAULT_FOCUS_FOLLOW_GROUP_ID || groupId !in validGroupIds) return@forEach
            put(mid.toString(), groupId)
        }
    }

    return FocusFollowGroupConfig(
        groups = groups,
        assignments = assignments
    )
}

fun resolveFocusFollowGroup(config: FocusFollowGroupConfig, groupId: String): FocusFollowGroup {
    return config.groups.firstOrNull { it.id == groupId } ?: defaultFocusFollowGroup()
}

fun resolveFocusFollowGroupIdForUser(config: FocusFollowGroupConfig, mid: Long): String {
    if (mid <= 0L) return DEFAULT_FOCUS_FOLLOW_GROUP_ID
    val assigned = config.assignments[mid.toString()].orEmpty()
    return assigned.takeIf { candidate ->
        candidate.isNotBlank() && config.groups.any { it.id == candidate }
    } ?: DEFAULT_FOCUS_FOLLOW_GROUP_ID
}

fun resolveFocusFollowGroupForUser(config: FocusFollowGroupConfig, mid: Long): FocusFollowGroup {
    return resolveFocusFollowGroup(
        config = config,
        groupId = resolveFocusFollowGroupIdForUser(config, mid)
    )
}

fun isFocusFollowUserVisible(config: FocusFollowGroupConfig, mid: Long): Boolean {
    if (mid <= 0L) return true
    return resolveFocusFollowGroupForUser(config, mid).visible
}

fun countFocusFollowGroupMembers(
    followingMids: Iterable<Long>,
    config: FocusFollowGroupConfig
): Map<String, Int> {
    val counts = config.groups.associate { it.id to 0 }.toMutableMap()
    followingMids.forEach { mid ->
        if (mid <= 0L) return@forEach
        val groupId = resolveFocusFollowGroupIdForUser(config, mid)
        counts[groupId] = (counts[groupId] ?: 0) + 1
    }
    return counts
}

fun canCreateFocusFollowGroup(
    name: String,
    existingGroups: List<FocusFollowGroup>
): Boolean {
    val normalizedName = name.trim()
    if (normalizedName.isBlank()) return false
    return existingGroups.none { it.name.equals(normalizedName, ignoreCase = true) }
}

fun withFocusFollowGroupCreated(
    config: FocusFollowGroupConfig,
    name: String,
    idProvider: () -> String = ::generateFocusFollowGroupId
): FocusFollowGroupConfig {
    if (!canCreateFocusFollowGroup(name, config.groups)) return normalizeFocusFollowGroupConfig(config)
    val updated = config.copy(
        groups = config.groups + FocusFollowGroup(
            id = idProvider(),
            name = normalizeFocusFollowGroupName(name),
            visible = true
        )
    )
    return normalizeFocusFollowGroupConfig(updated)
}

fun withFocusFollowGroupRenamed(
    config: FocusFollowGroupConfig,
    groupId: String,
    name: String
): FocusFollowGroupConfig {
    if (groupId == DEFAULT_FOCUS_FOLLOW_GROUP_ID) return normalizeFocusFollowGroupConfig(config)
    val normalizedName = name.trim()
    if (normalizedName.isBlank()) return normalizeFocusFollowGroupConfig(config)
    val updatedGroups = config.groups.map { group ->
        if (group.id == groupId) group.copy(name = normalizeFocusFollowGroupName(normalizedName)) else group
    }
    return normalizeFocusFollowGroupConfig(config.copy(groups = updatedGroups))
}

fun withFocusFollowGroupDeleted(
    config: FocusFollowGroupConfig,
    groupId: String
): FocusFollowGroupConfig {
    if (groupId == DEFAULT_FOCUS_FOLLOW_GROUP_ID) return normalizeFocusFollowGroupConfig(config)
    val updatedGroups = config.groups.filterNot { it.id == groupId }
    val updatedAssignments = config.assignments.filterValues { it != groupId }
    return normalizeFocusFollowGroupConfig(
        config.copy(
            groups = updatedGroups,
            assignments = updatedAssignments
        )
    )
}

fun withUserAssignedToFocusFollowGroup(
    config: FocusFollowGroupConfig,
    mid: Long,
    groupId: String
): FocusFollowGroupConfig {
    if (mid <= 0L) return normalizeFocusFollowGroupConfig(config)
    if (groupId == DEFAULT_FOCUS_FOLLOW_GROUP_ID) {
        return normalizeFocusFollowGroupConfig(
            config.copy(assignments = config.assignments - mid.toString())
        )
    }
    if (config.groups.none { it.id == groupId }) return normalizeFocusFollowGroupConfig(config)
    return normalizeFocusFollowGroupConfig(
        config.copy(
            assignments = config.assignments + (mid.toString() to groupId)
        )
    )
}

fun withFocusFollowGroupVisibility(
    config: FocusFollowGroupConfig,
    groupId: String,
    visible: Boolean
): FocusFollowGroupConfig {
    val updatedGroups = config.groups.map { group ->
        if (group.id == groupId) group.copy(visible = visible) else group
    }
    return normalizeFocusFollowGroupConfig(config.copy(groups = updatedGroups))
}

private fun generateFocusFollowGroupId(): String {
    val entropy = Random.nextInt(1000, 9999)
    return "group_${System.currentTimeMillis()}_$entropy"
}

private fun decodeFocusFollowGroupConfig(raw: String?): FocusFollowGroupConfig {
    if (raw.isNullOrBlank()) return FocusFollowGroupConfig()
    return runCatching {
        normalizeFocusFollowGroupConfig(
            focusFollowGroupJson.decodeFromString<FocusFollowGroupConfig>(raw)
        )
    }.getOrDefault(FocusFollowGroupConfig())
}

private fun encodeFocusFollowGroupConfig(config: FocusFollowGroupConfig): String {
    return focusFollowGroupJson.encodeToString(
        normalizeFocusFollowGroupConfig(config)
    )
}

object FocusFollowGroupStore {
    fun getConfig(context: Context): Flow<FocusFollowGroupConfig> {
        return context.settingsDataStore.data
            .map { preferences ->
                decodeFocusFollowGroupConfig(preferences[KEY_FOCUS_FOLLOW_GROUP_CONFIG])
            }
            .distinctUntilChanged()
    }

    suspend fun createGroup(context: Context, name: String) {
        updateConfig(context) { current ->
            withFocusFollowGroupCreated(current, name)
        }
    }

    suspend fun renameGroup(context: Context, groupId: String, name: String) {
        updateConfig(context) { current ->
            withFocusFollowGroupRenamed(current, groupId, name)
        }
    }

    suspend fun deleteGroup(context: Context, groupId: String) {
        updateConfig(context) { current ->
            withFocusFollowGroupDeleted(current, groupId)
        }
    }

    suspend fun setGroupVisible(context: Context, groupId: String, visible: Boolean) {
        updateConfig(context) { current ->
            withFocusFollowGroupVisibility(current, groupId, visible)
        }
    }

    suspend fun assignUserToGroup(context: Context, mid: Long, groupId: String) {
        updateConfig(context) { current ->
            withUserAssignedToFocusFollowGroup(current, mid, groupId)
        }
    }

    private suspend fun updateConfig(
        context: Context,
        transform: (FocusFollowGroupConfig) -> FocusFollowGroupConfig
    ) {
        context.settingsDataStore.edit { preferences ->
            val current = decodeFocusFollowGroupConfig(preferences[KEY_FOCUS_FOLLOW_GROUP_CONFIG])
            val updated = normalizeFocusFollowGroupConfig(transform(current))
            if (updated == FocusFollowGroupConfig()) {
                preferences.remove(KEY_FOCUS_FOLLOW_GROUP_CONFIG)
            } else {
                preferences[KEY_FOCUS_FOLLOW_GROUP_CONFIG] = encodeFocusFollowGroupConfig(updated)
            }
        }
    }
}
