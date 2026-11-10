package com.android.purebilibili.feature.message

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.AppTextButton
import kotlinx.serialization.json.*

internal data class ChatNoticeLink(val label: String, val url: String)
internal data class ChatNoticeDetail(val label: String, val value: String)
internal data class ChatNoticeContent(val title: String, val text: String, val details: List<ChatNoticeDetail>, val links: List<ChatNoticeLink>)
internal data class ChatRecommendation(val title: String, val subtitle: String, val cover: String, val url: String)

internal fun parseChatNotice(content: String): ChatNoticeContent? {
    val root = runCatching { Json.parseToJsonElement(content) as? JsonObject }.getOrNull() ?: return null
    fun JsonObject.text(key: String) = (get(key) as? JsonPrimitive)?.contentOrNull.orEmpty()
    val details = (root["modules"] as? JsonArray).orEmpty().mapNotNull { item ->
        (item as? JsonObject)?.let { ChatNoticeDetail(it.text("title"), it.text("detail")) }
    }
    val links = listOf("", "_2", "_3").mapNotNull { suffix ->
        root.text("jump_uri$suffix").takeIf { it.isNotBlank() }?.let { ChatNoticeLink(root.text("jump_text$suffix").ifBlank { "查看详情" }, it) }
    }
    return ChatNoticeContent(root.text("title"), root.text("text").ifBlank { root.text("content") }, details, links)
}

internal fun parseChatRecommendations(content: String): Pair<String, List<ChatRecommendation>>? {
    val root = runCatching { Json.parseToJsonElement(content) as? JsonObject }.getOrNull() ?: return null
    fun JsonObject.text(key: String) = (get(key) as? JsonPrimitive)?.contentOrNull.orEmpty()
    val cards = (root["sub_cards"] as? JsonArray).orEmpty().mapNotNull { item ->
        (item as? JsonObject)?.let { ChatRecommendation(it.text("field1"), listOf(it.text("field2"), it.text("field3")).filter(String::isNotBlank).joinToString("\n"), it.text("cover_url"), it.text("jump_url")) }
    }
    return root.text("main_title") to cards
}

internal fun parseChatTip(content: String): String {
    fun extract(element: JsonElement, depth: Int): String {
        if (depth > 4) return ""
        return when (element) {
            is JsonArray -> element.joinToString("") { extract(it, depth + 1) }
            is JsonObject -> (element["content"] ?: element["text"])
                ?.let { extract(it, depth + 1) }.orEmpty()
            is JsonPrimitive -> {
                val value = element.contentOrNull.orEmpty()
                val trimmed = value.trimStart()
                if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
                    runCatching { extract(Json.parseToJsonElement(value), depth + 1) }.getOrDefault("")
                } else value
            }
            else -> ""
        }
    }
    return runCatching { extract(Json.parseToJsonElement(content), 0) }.getOrDefault("")
}

@Composable
internal fun ChatNoticeMessage(content: String, onOpenLink: (String) -> Unit, modifier: Modifier = Modifier) {
    val notice = remember(content) { parseChatNotice(content) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (notice == null) AppText("[通知内容暂不可用]") else {
            if (notice.title.isNotBlank()) AppText(notice.title, fontWeight = FontWeight.Medium)
            if (notice.text.isNotBlank()) AppText(notice.text)
            notice.details.forEach { detail ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppText(detail.label, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    AppText(detail.value, Modifier.weight(2f), style = MaterialTheme.typography.bodySmall)
                }
            }
            notice.links.forEach { link -> AppTextButton(onClick = { onOpenLink(link.url) }) { AppText(link.label) } }
        }
    }
}

@Composable
internal fun ChatRecommendationMessage(content: String, onOpenLink: (String) -> Unit, modifier: Modifier = Modifier) {
    val recommendation = remember(content) { parseChatRecommendations(content) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (recommendation == null) AppText("[推荐内容暂不可用]") else {
            AppText(recommendation.first, fontWeight = FontWeight.Medium)
            recommendation.second.forEach { card ->
                Row(Modifier.fillMaxWidth().clickable(enabled = card.url.isNotBlank()) { onOpenLink(card.url) }, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AsyncImage(card.cover, "封面", Modifier.widthIn(max = 110.dp).weight(1f).aspectRatio(16f / 9f))
                    Column(Modifier.weight(1.4f)) {
                        AppText(card.title, maxLines = 2)
                        AppText(card.subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 3)
                    }
                }
            }
        }
    }
}
