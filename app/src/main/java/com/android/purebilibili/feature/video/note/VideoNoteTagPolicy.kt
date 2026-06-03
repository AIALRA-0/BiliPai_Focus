package com.android.purebilibili.feature.video.note

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

object VideoNoteTagPolicy {
    private val json = Json { encodeDefaults = false }

    fun encodeTags(blocks: List<VideoNoteBlock>): String {
        val tags = blocks.mapIndexedNotNull { index, block ->
            val timestamp = block as? VideoNoteBlock.Timestamp ?: return@mapIndexedNotNull null
            JsonObject(
                mapOf(
                    "cid" to JsonPrimitive(timestamp.cid),
                    "status" to JsonPrimitive(0),
                    "index" to JsonPrimitive(timestamp.index),
                    "seconds" to JsonPrimitive(timestamp.seconds),
                    "pos" to JsonPrimitive(index)
                )
            )
        }
        return json.encodeToString(JsonArray(tags))
    }
}
