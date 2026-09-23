package com.android.purebilibili.feature.video.danmaku

import org.json.JSONArray
import org.json.JSONObject

enum class CommandDanmakuType {
    UP,
    LINK,
    ATTENTION,
    TEXT,
    VOTE
}

/**
 * 投票弹幕种类
 * - VOTE: 互动投票弹幕（指令 VIDEO_VOTE_MSG / #VOTE#）
 * - GRADE: 打分弹幕（指令 #GRADE# / GRADE_MSG），提交走 x/v2/dm/command/grade/post
 */
enum class VoteDanmakuKind {
    UNKNOWN,
    VOTE,
    GRADE
}

/**
 * 投票/打分选项
 * @param score 打分弹幕的分数值（grade_score），普通投票为 null
 */
data class VoteOption(
    val id: String,
    val label: String,
    val score: Int? = null
)

/**
 * The grade endpoint accepts the five even score values 2..10. Keep these slots
 * independent from the order (or labels) supplied by a command payload.
 */
internal val GRADE_STAR_SCORES = listOf(2, 4, 6, 8, 10)

/**
 * Maps raw grade options to the five visible stars without inventing values.
 *
 * A star is enabled only when the payload contains the corresponding legal
 * score. Duplicate scores keep the first original option so submission still
 * carries the server-provided id/label.
 */
internal fun resolveGradeStarOptions(options: List<VoteOption>): List<VoteOption?> {
    val firstOptionByScore = mutableMapOf<Int, VoteOption>()
    options.forEach { option ->
        val score = option.score
        if (score != null && score in GRADE_STAR_SCORES && score !in firstOptionByScore) {
            firstOptionByScore[score] = option
        }
    }
    return GRADE_STAR_SCORES.map { score -> firstOptionByScore[score] }
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
    val attentionType: Int = 0,

    // [新增] 投票/打分弹幕字段
    val voteKind: VoteDanmakuKind = VoteDanmakuKind.UNKNOWN,
    val voteId: String = "",
    val voteTitle: String = "",
    val voteOptions: List<VoteOption> = emptyList()
)

internal const val COMMAND_DANMAKU_OVERLAY_DURATION_MS = 3000L
private const val LEGACY_ADVANCED_COMMAND_DURATION_MS = 5000L

// 投票弹幕需要更长的展示时间供用户点选
internal const val VOTE_DANMAKU_OVERLAY_DURATION_MS = 8000L

private val NON_VISUAL_COMMAND_TYPES = setOf(
    "UPOWER_STATE",
    "UPGRADE_STATE",
    "PANEL_STATE"
)

private val VOTE_COMMAND_NAMES = setOf(
    "#VOTE#",
    "VIDEO_VOTE_MSG"
)

private val GRADE_COMMAND_NAMES = setOf(
    "#GRADE#",
    "GRADE_MSG",
    "VIDEO_GRADE_MSG"
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
    // 投票/打分弹幕有独立卡片 UI，不降级为高级弹幕文本
    if (item.type == CommandDanmakuType.VOTE) return null
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
    val commandType = cmd.command.orEmpty().trim().uppercase()
    if (commandType in NON_VISUAL_COMMAND_TYPES) return null
    val extra = cmd.extra.orEmpty().trim()

    val voteKind = resolveVoteKind(commandType)
    val type = when {
        voteKind != VoteDanmakuKind.UNKNOWN -> CommandDanmakuType.VOTE
        commandType == "#UP#" -> CommandDanmakuType.UP
        commandType == "#LINK#" -> CommandDanmakuType.LINK
        commandType == "#ATTENTION#" -> CommandDanmakuType.ATTENTION
        else -> CommandDanmakuType.TEXT
    }

    // 投票/打分弹幕：优先解析结构化数据；解析失败时降级为文本提示
    if (type == CommandDanmakuType.VOTE) {
        val voteData = parseVoteDanmakuData(cmd, voteKind)
        if (voteData != null) {
            return CommandDanmakuItem(
                id = "cmd_${cmd.id}",
                type = type,
                content = voteData.title.ifBlank { "互动投票" },
                startTimeMs = cmd.progress.coerceAtLeast(0).toLong(),
                durationMs = VOTE_DANMAKU_OVERLAY_DURATION_MS,
                voteKind = voteKind,
                voteId = voteData.voteId,
                voteTitle = voteData.title,
                voteOptions = voteData.options
            )
        }
        // 解析失败：降级为文本卡片，保持原有"投票提示"展示行为
    }

    val fallbackType = if (type == CommandDanmakuType.VOTE) {
        CommandDanmakuType.TEXT
    } else {
        type
    }
    val text = extractReadableCommandText(cmd.content)
        ?: extractReadableCommandText(cmd.extra)
        ?: when (type) {
            CommandDanmakuType.ATTENTION -> "关注 UP"
            CommandDanmakuType.LINK -> extractJsonString(extra, "title")
            CommandDanmakuType.UP -> "UP 主提示"
            CommandDanmakuType.TEXT -> null
            CommandDanmakuType.VOTE -> null
        }
        ?: return null
    return CommandDanmakuItem(
        id = "cmd_${cmd.id}",
        type = fallbackType,
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

// ========== [新增] 投票/打分弹幕解析 ==========

private data class VoteDanmakuPayload(
    val voteId: String,
    val title: String,
    val options: List<VoteOption>
)

private fun resolveVoteKind(commandType: String): VoteDanmakuKind {
    return when {
        commandType in VOTE_COMMAND_NAMES -> VoteDanmakuKind.VOTE
        commandType in GRADE_COMMAND_NAMES -> VoteDanmakuKind.GRADE
        else -> VoteDanmakuKind.UNKNOWN
    }
}

/**
 * 容错解析投票/打分弹幕数据。
 * 尝试从 extra 或 content 的 JSON 中提取 id/title/options；
 * 打分弹幕在无结构化选项时生成默认分数档位 (2/4/6/8/10)。
 * 返回 null 表示无任何可用的结构化数据（调用方降级为文本提示）。
 */
private fun parseVoteDanmakuData(
    cmd: DanmakuProto.CommandDm,
    kind: VoteDanmakuKind
): VoteDanmakuPayload? {
    val payloadJson = parseJsonObject(cmd.extra.orEmpty().trim()) ?: parseJsonObject(cmd.content.orEmpty().trim())
    var voteId = ""
    var title = ""
    var options: List<VoteOption>? = null

    if (payloadJson != null) {
        voteId = payloadJson.optString("vote_id", "").orEmpty()
            .ifBlank { payloadJson.optString("id", "").orEmpty() }
            .ifBlank { payloadJson.optString("grade_id", "").orEmpty() }
        title = payloadJson.optString("title", "").orEmpty()
            .ifBlank { payloadJson.optString("question", "").orEmpty() }
        options = parseVoteOptions(payloadJson)
    }

    // 打分弹幕：从 content/extra 中兜底提取 grade_id（可能是纯数字）
    if (voteId.isBlank()) {
        voteId = extractJsonLong(cmd.extra, "grade_id")?.toString().orEmpty()
            .ifBlank { extractJsonLong(cmd.content, "grade_id")?.toString().orEmpty() }
    }

    if (kind == VoteDanmakuKind.GRADE && options.isNullOrEmpty() && voteId.isNotBlank()) {
        // 默认 5 档分数（2/4/6/8/10，偶数最大 10）
        options = listOf(2, 4, 6, 8, 10).map { score ->
            VoteOption(id = score.toString(), label = score.toString(), score = score)
        }
    }

    if (voteId.isBlank() && title.isBlank() && options.isNullOrEmpty()) return null
    return VoteDanmakuPayload(
        voteId = voteId,
        title = title,
        options = options.orEmpty()
    )
}

/**
 * 从 JSON 中提取选项列表，兼容多种格式：
 * - 数组 of 对象: [{"id":1,"title":"A"}, ...]（键也兼容 score/name/text/label）
 * - 数组 of 字符串: ["A","B"]
 * - 对象 map: {"1":"A","2":"B"}
 */
private fun parseVoteOptions(json: JSONObject): List<VoteOption>? {
    val raw = json.opt("options") ?: json.opt("choices") ?: return null
    val result = mutableListOf<VoteOption>()

    when (raw) {
        is JSONArray -> {
            for (i in 0 until raw.length()) {
                val element = raw.opt(i) ?: continue
                when (element) {
                    is JSONObject -> {
                        val id = element.optString("id", "").orEmpty()
                            .ifBlank { element.optString("value", "").orEmpty() }
                            .ifBlank { i.toString() }
                        val score = element.optInt("score", -1).takeIf { it >= 0 }
                        val label = element.optString("title", "").orEmpty()
                            .ifBlank { element.optString("name", "").orEmpty() }
                            .ifBlank { element.optString("text", "").orEmpty() }
                            .ifBlank { element.optString("label", "").orEmpty() }
                            // 打分弹幕选项可能只有 id+score，用分数兜底做可读标签
                            .ifBlank { score?.toString().orEmpty() }
                        if (label.isNotBlank()) {
                            result.add(VoteOption(id = id, label = label, score = score))
                        }
                    }
                    is String -> {
                        if (element.isNotBlank()) {
                            result.add(VoteOption(id = i.toString(), label = element))
                        }
                    }
                    is Number -> {
                        result.add(VoteOption(id = element.toString(), label = element.toString(), score = element.toInt()))
                    }
                }
            }
        }
        is JSONObject -> {
            val keys = raw.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = raw.optString(key, "").orEmpty()
                if (value.isNotBlank()) {
                    result.add(VoteOption(id = key, label = value))
                }
            }
        }
    }

    return result.takeIf { it.isNotEmpty() }
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
