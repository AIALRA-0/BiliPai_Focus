// 文件路径: data/model/response/SendDanmakuResponse.kt
package com.android.purebilibili.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import org.json.JSONObject

/**
 * 发送弹幕响应
 */
@Serializable
data class SendDanmakuResponse(
    val code: Int = 0,
    val message: String = "",
    val data: SendDanmakuData? = null
)

@Serializable
data class SendDanmakuData(
    val dmid: Long = 0,        // 弹幕 ID
    val dmid_str: String = "", // 弹幕 ID (字符串)
    val visible: Boolean = true
)

@Serializable
data class CommandDanmakuResponse(
    val code: Int = 0,
    val message: String = "",
    val ttl: Int = 1,
    val data: CommandDanmakuData? = null
)

@Serializable
data class CommandDanmakuData(
    val command: String = "",
    val content: String = "",
    val extra: String = "",
    val id: Long = 0,
    val idStr: String = "",
    val mid: Long = 0,
    val oid: Long = 0,
    val progress: Long = 0,
    val type: Int = 0
)

/** Statistics supplied by the grade command, never inferred from a submitted score. */
@Serializable
data class GradeDanmakuSummary(
    @SerialName("count") val participantCount: Long? = null,
    @SerialName("avg_score") val averageScore: Double? = null,
    @SerialName("mid_score") val userScore: Int? = null
) {
    fun normalized(): GradeDanmakuSummary {
        val count = participantCount?.takeIf { it >= 0L }
        val average = averageScore?.takeIf { it.isFinite() && it in 0.0..10.0 }
        val score = userScore?.takeIf { it == 2 || it == 4 || it == 6 || it == 8 || it == 10 }
        return if (count == participantCount && average == averageScore && score == userScore) {
            this
        } else {
            GradeDanmakuSummary(count, average, score)
        }
    }
}

/** Shared by initial command parsing and the authenticated metadata refresh. */
fun parseGradeDanmakuSummary(payload: JSONObject): GradeDanmakuSummary {
    val average = payload.opt("avg_score")
    val score = gradeIntegerValue(payload.opt("mid_score"))
    return GradeDanmakuSummary(
        participantCount = gradeIntegerValue(payload.opt("count")),
        averageScore = when (average) {
            is Number -> average.toDouble()
            is String -> average.toDoubleOrNull()
            else -> null
        },
        userScore = score?.takeIf { it in Int.MIN_VALUE..Int.MAX_VALUE }?.toInt()
    ).normalized()
}

private fun gradeIntegerValue(value: Any?): Long? = when (value) {
    is Int -> value.toLong()
    is Long -> value
    is String -> value.toLongOrNull()
    else -> null
}

fun parseGradeDanmakuSummary(payload: JsonObject): GradeDanmakuSummary =
    GradeDanmakuSummary(
        participantCount = (payload["count"] as? JsonPrimitive)?.longOrNull,
        averageScore = (payload["avg_score"] as? JsonPrimitive)?.doubleOrNull,
        userScore = (payload["mid_score"] as? JsonPrimitive)?.intOrNull
    ).normalized()

/**
 * 弹幕操作响应 (撤回/点赞/举报)
 */
@Serializable
data class DanmakuActionResponse(
    val code: Int = 0,
    val message: String = "",
    val ttl: Int = 1
)

/**
 * 云端弹幕屏蔽规则 (x/dm/filter/user 系列)
 * type: 0=关键词, 1=正则, 2=UID(crc32 hex)
 */
@Serializable
data class DanmakuFilterRuleItem(
    val id: Long = 0,
    val type: Int = 0,
    val filter: String = ""
)

@Serializable
data class DanmakuFilterRulesData(
    val rule: List<DanmakuFilterRuleItem> = emptyList(),
    val rule1: List<DanmakuFilterRuleItem> = emptyList(),
    val rule2: List<DanmakuFilterRuleItem> = emptyList(),
    val toast: String? = null
)

@Serializable
data class DanmakuFilterRulesResponse(
    val code: Int = 0,
    val message: String = "",
    val data: DanmakuFilterRulesData? = null
)

@Serializable
data class DanmakuFilterAddData(
    val id: Long = 0,
    val type: Int = 0,
    val filter: String = ""
)

@Serializable
data class DanmakuFilterAddResponse(
    val code: Int = 0,
    val message: String = "",
    val data: DanmakuFilterAddData? = null
)
