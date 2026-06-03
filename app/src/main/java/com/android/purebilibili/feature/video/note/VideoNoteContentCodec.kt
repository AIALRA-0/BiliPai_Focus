package com.android.purebilibili.feature.video.note

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object VideoNoteContentCodec {
    private const val HIGHLIGHT_COLOR = "#fff359"
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    fun decode(title: String, content: String): VideoNoteEditorDocument {
        val blocks = runCatching {
            json.parseToJsonElement(content).jsonArray.mapNotNull(::decodeBlock)
        }.getOrDefault(emptyList())
        return VideoNoteEditorDocument(title = title, blocks = blocks)
    }

    fun encode(document: VideoNoteEditorDocument): EncodedVideoNoteContent {
        val ops = document.blocks.mapIndexed { index, block -> encodeBlock(index, block) }
        val content = json.encodeToString(JsonArray(ops))
        val tags = VideoNoteTagPolicy.encodeTags(document.blocks)
        val plainText = toPlainText(document)
        return EncodedVideoNoteContent(
            content = content,
            tags = tags,
            summary = plainText.replace('\n', ' ').trim().take(80),
            contentLength = plainText.length
        )
    }

    fun toPlainText(document: VideoNoteEditorDocument): String {
        return document.blocks.joinToString(separator = "") { block ->
            when (block) {
                is VideoNoteBlock.Text -> block.text
                is VideoNoteBlock.Timestamp -> "[${block.label}]"
            }
        }.trim()
    }

    private fun decodeBlock(element: JsonElement): VideoNoteBlock? {
        val obj = element.jsonObject
        val insert = obj["insert"] ?: return null
        val attributes = obj["attributes"]?.jsonObject
        if (insert is JsonPrimitive) {
            val text = insert.content
            if (text.isEmpty()) return null
            return VideoNoteBlock.Text(
                text = text,
                bold = attributes?.get("bold")?.jsonPrimitive?.booleanOrNull == true,
                highlight = attributes?.get("background")?.jsonPrimitive?.content == HIGHLIGHT_COLOR,
                unorderedList = attributes?.get("list")?.jsonPrimitive?.content == "bullet"
            )
        }
        val tag = insert.jsonObject["tag"]?.jsonObject ?: return null
        return VideoNoteBlock.Timestamp(
            seconds = tag["seconds"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
            cid = tag["cid"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
            index = tag["index"]?.jsonPrimitive?.intOrNull ?: 0,
            cidCount = tag["cidCount"]?.jsonPrimitive?.intOrNull ?: 1,
            label = tag["title"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
                ?: formatVideoNoteTimestamp(tag["seconds"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L)
        )
    }

    private fun encodeBlock(index: Int, block: VideoNoteBlock): JsonObject {
        return when (block) {
            is VideoNoteBlock.Text -> {
                val attributes = buildMap<String, JsonElement> {
                    if (block.bold) put("bold", JsonPrimitive(true))
                    if (block.highlight) put("background", JsonPrimitive(HIGHLIGHT_COLOR))
                    if (block.unorderedList) put("list", JsonPrimitive("bullet"))
                }
                buildJsonObject(
                    insert = JsonPrimitive(
                        if (block.unorderedList && !block.text.endsWith('\n')) block.text + "\n" else block.text
                    ),
                    attributes = attributes
                )
            }
            is VideoNoteBlock.Timestamp -> buildJsonObject(
                insert = JsonObject(
                    mapOf(
                        "tag" to JsonObject(
                            mapOf(
                                "cid" to JsonPrimitive(block.cid),
                                "status" to JsonPrimitive(0),
                                "index" to JsonPrimitive(block.index),
                                "seconds" to JsonPrimitive(block.seconds),
                                "cidCount" to JsonPrimitive(block.cidCount.coerceAtLeast(1)),
                                "key" to JsonPrimitive(index.toString()),
                                "title" to JsonPrimitive(block.label)
                            )
                        )
                    )
                ),
                attributes = emptyMap()
            )
        }
    }

    private fun buildJsonObject(
        insert: JsonElement,
        attributes: Map<String, JsonElement>
    ): JsonObject {
        return JsonObject(
            buildMap {
                put("insert", insert)
                if (attributes.isNotEmpty()) {
                    put("attributes", JsonObject(attributes))
                }
            }
        )
    }
}
