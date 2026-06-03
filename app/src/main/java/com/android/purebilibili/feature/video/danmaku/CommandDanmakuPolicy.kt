package com.android.purebilibili.feature.video.danmaku

import org.json.JSONObject

enum class CommandDanmakuType {
    UP,
    LINK,
    ATTENTION,
    TEXT
}

data class CommandDanmakuItem(
    val id: String,
    val type: CommandDanmakuType,
    val content: String,
    val startTimeMs: Long,
    val durationMs: Long,
    val iconUrl: String = "",
    val linkAid: Long = 0L,
    val linkBvid: String = "",
    val linkTitle: String = "",
    val posX: Float = 0f,
    val posY: Float = 0f,
    val attentionType: Int = 0
)

internal const val COMMAND_DANMAKU_OVERLAY_DURATION_MS = 3000L
private const val LEGACY_ADVANCED_COMMAND_DURATION_MS = 5000L

private val NON_VISUAL_COMMAND_TYPES = setOf(
    "UPOWER_STATE",
    "UPGRADE_STATE",
    "PANEL_STATE"
)

private val TEXT_FIELD_CANDIDATES = listOf(
    "text",
    "content",
    "msg",
    "message",
    "title"
)

internal fun buildCommandDanmaku(cmd: DanmakuProto.CommandDm): AdvancedDanmakuData? {
    val item = buildCommandDanmakuItem(cmd) ?: return null
    if (item.type == CommandDanmakuType.ATTENTION) return null
    val text = item.content
    return AdvancedDanmakuData(
        id = "cmd_${cmd.id}",
        content = text,
        startTimeMs = cmd.progress.coerceAtLeast(0).toLong(),
        durationMs = LEGACY_ADVANCED_COMMAND_DURATION_MS,
        startX = 0.5f,
        startY = 0.1f,
        fontSize = 20f,
        color = 0xFFD700,
        alpha = 0.9f
    )
}

internal fun filterVisibleCommandDanmakuItems(
    items: List<CommandDanmakuItem>,
    hideInteractiveCommands: Boolean
): List<CommandDanmakuItem> {
    if (!hideInteractiveCommands) return items
    return emptyList()
}

internal fun buildCommandDanmakuItem(cmd: DanmakuProto.CommandDm): CommandDanmakuItem? {
    val commandType = cmd.command.trim().uppercase()
    if (commandType in NON_VISUAL_COMMAND_TYPES) return null
    val extra = cmd.extra.trim()
    val type = when (commandType) {
        "#UP#" -> CommandDanmakuType.UP
        "#LINK#" -> CommandDanmakuType.LINK
        "#ATTENTION#" -> CommandDanmakuType.ATTENTION
        else -> CommandDanmakuType.TEXT
    }
    val text = extractReadableCommandText(cmd.content)
        ?: extractReadableCommandText(cmd.extra)
        ?: when (type) {
            CommandDanmakuType.ATTENTION -> "关注 UP"
            CommandDanmakuType.LINK -> extractJsonString(extra, "title")
            CommandDanmakuType.UP -> "UP 主提示"
            CommandDanmakuType.TEXT -> null
        }
        ?: return null
    return CommandDanmakuItem(
        id = "cmd_${cmd.id}",
        type = type,
        content = when (type) {
            CommandDanmakuType.LINK -> extractJsonString(extra, "title").orEmpty().ifBlank { text }
            else -> text
        },
        startTimeMs = cmd.progress.coerceAtLeast(0).toLong(),
        durationMs = COMMAND_DANMAKU_OVERLAY_DURATION_MS,
        iconUrl = extractJsonString(extra, "icon").orEmpty(),
        linkAid = extractJsonLong(extra, "aid") ?: 0L,
        linkBvid = extractJsonString(extra, "bvid").orEmpty(),
        linkTitle = extractJsonString(extra, "title").orEmpty(),
        posX = extractJsonFloat(extra, "posX") ?: 0f,
        posY = extractJsonFloat(extra, "posY") ?: 0f,
        attentionType = extractJsonLong(extra, "type")?.toInt() ?: 0
    )
}

internal fun resolveCommandDanmakuText(cmd: DanmakuProto.CommandDm): String? {
    val commandType = cmd.command.trim().uppercase()
    if (commandType in NON_VISUAL_COMMAND_TYPES) return null
    return extractReadableCommandText(cmd.content)
        ?: extractReadableCommandText(cmd.extra)
}

private fun parseJsonObject(raw: String): JSONObject? {
    return try {
        val trimmed = raw.trim()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) JSONObject(trimmed) else null
    } catch (_: Exception) {
        null
    }
}

private fun extractJsonString(raw: String, key: String): String? {
    return Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"")
        .find(raw)
        ?.groupValues
        ?.getOrNull(1)
}

private fun extractJsonLong(raw: String, key: String): Long? {
    return Regex("\"$key\"\\s*:\\s*(-?\\d+)")
        .find(raw)
        ?.groupValues
        ?.getOrNull(1)
        ?.toLongOrNull()
}

private fun extractJsonFloat(raw: String, key: String): Float? {
    return Regex("\"$key\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)")
        .find(raw)
        ?.groupValues
        ?.getOrNull(1)
        ?.toFloatOrNull()
}

private fun extractReadableCommandText(raw: String): String? {
    val content = raw.trim()
    if (content.isEmpty()) return null

    if (looksLikeJson(content)) {
        return extractTextFromJson(content)
    }

    return sanitizeText(content)
}

private fun looksLikeJson(content: String): Boolean {
    val trimmed = content.trim()
    return (trimmed.startsWith("{") && trimmed.endsWith("}")) ||
        (trimmed.startsWith("[") && trimmed.endsWith("]"))
}

private fun extractTextFromJson(rawJson: String): String? {
    TEXT_FIELD_CANDIDATES.firstNotNullOfOrNull { key ->
        Regex("\"$key\"\\s*:\\s*\"([^\"]+)\"")
            .find(rawJson)
            ?.groupValues
            ?.getOrNull(1)
            ?.let(::sanitizeText)
    }?.let { return it }

    return try {
        val json = JSONObject(rawJson)

        TEXT_FIELD_CANDIDATES.firstNotNullOfOrNull { key ->
            sanitizeText(json.optString(key).orEmpty())
        } ?: run {
            val nested = json.optJSONObject("data")
                ?: json.optJSONObject("extra")
            nested?.let { nestedJson ->
                TEXT_FIELD_CANDIDATES.firstNotNullOfOrNull { key ->
                    sanitizeText(nestedJson.optString(key).orEmpty())
                }
            }
        }
    } catch (_: Exception) {
        null
    }
}

private fun sanitizeText(raw: String): String? {
    val normalized = raw.replace('\n', ' ')
        .replace(Regex("\\s+"), " ")
        .trim()

    if (normalized.isEmpty()) return null
    if (normalized.contains("upower_state", ignoreCase = true)) return null
    if (normalized.contains("\"type\":", ignoreCase = true)) return null
    if (normalized.contains("\",\"type\":", ignoreCase = true)) return null
    if (normalized.contains(".png\"", ignoreCase = true)) return null

    val punctuationDensity = normalized.count { it == ':' || it == ',' || it == '"' }
    if (normalized.length > 32 && punctuationDensity >= 4) return null

    return normalized
}
