package com.android.purebilibili.data.model.response

import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer

/** Web returns {content: ...}; app returns a string. Missing/null reasons stay empty. */
object RecommendationReasonSerializer : JsonTransformingSerializer<String>(String.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement = when (element) {
        is JsonObject -> (element["content"] as? JsonPrimitive)
            ?.takeIf { it.isString } ?: JsonPrimitive("")
        is JsonPrimitive -> element.takeIf { it.isString } ?: JsonPrimitive("")
        else -> JsonPrimitive("")
    }
}

internal fun isFollowedRecommendationReason(reason: String): Boolean =
    reason.trim() in setOf("已关注", "新关注")

internal fun recommendationReasonLabel(reason: String): String = reason.trim().takeUnless {
    it == "竖屏" || isFollowedRecommendationReason(it)
}.orEmpty()

private val recommendationLikes = Regex("^([0-9]+(?:\\.[0-9]+)?)([千万亿]?)(?:点赞|赞)$")

/** Rounded server copy is only an approximate like count, never a replacement for exact stats. */
internal fun parseRecommendationLikeCount(reason: String): Int? {
    val match = recommendationLikes.matchEntire(reason.trim()) ?: return null
    val number = match.groupValues[1].toDoubleOrNull() ?: return null
    val unit = when (match.groupValues[2]) {
        "千" -> 1_000.0
        "万" -> 10_000.0
        "亿" -> 100_000_000.0
        else -> 1.0
    }
    return (number * unit).toInt()
}

/** Only extract numeric native identities; arbitrary card URIs are never opened as video IDs. */
internal fun recommendationEpisodeId(param: String, uri: String): Long =
    param.removePrefix("ep").toLongOrNull()?.takeIf { it > 0 }
        ?: Regex("(?:/ep|/bangumi/play/)([0-9]+)").find(uri)?.groupValues?.get(1)
            ?.toLongOrNull()?.takeIf { it > 0 } ?: 0L

internal fun recommendationDynamicId(param: String, uri: String): String =
    param.takeIf { it.isNotEmpty() && it.all(Char::isDigit) && it.any { char -> char != '0' } }
        ?: Regex("""(?:t\.bilibili\.com/|/dynamic/|/opus/|/following/detail/)([0-9]+)""")
            .find(uri)?.groupValues?.get(1)?.takeIf { it.any { char -> char != '0' } }.orEmpty()
