// File: feature/audio/screen/PlayHistorySheet.kt
package com.android.purebilibili.feature.audio.screen

import com.android.purebilibili.core.ui.AppContentColors

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import com.android.purebilibili.core.ui.components.AppLazyColumn as LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.android.purebilibili.core.store.PlayHistoryEntry
import com.android.purebilibili.core.store.PlayHistoryStore
import com.android.purebilibili.core.ui.AppModalBottomSheet
import com.android.purebilibili.core.ui.components.AppText
import androidx.compose.ui.platform.LocalContext

/** 最近播放：按播放时间倒序展示，含播放次数。点击立即切歌。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayHistorySheet(
    onPlay: (PlayHistoryEntry) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val entries by PlayHistoryStore.recent(context, limit = 50)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    AppModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = AppContentColors.primaryText()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            AppText(
                text = "最近播放",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            if (entries.isEmpty()) {
                AppText(
                    text = "还没有播放记录，去听几首歌吧。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppContentColors.secondaryText(),
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            } else {
                LazyColumn {
                    items(entries.size) { index ->
                        val entry = entries[index]
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPlay(entry) }
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                        ) {
                            AsyncImage(
                                model = entry.cover,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                AppText(
                                    text = entry.title.ifBlank { entry.bvid },
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                AppText(
                                    text = buildString {
                                        append(entry.owner.ifBlank { "未知 UP" })
                                        append(" · 播放 ")
                                        append(entry.playCount)
                                        append(" 次")
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppContentColors.secondaryText(),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
