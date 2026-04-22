package com.android.purebilibili.feature.live.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.feature.live.LiveDanmakuItem
import com.android.purebilibili.feature.live.rememberLiveChromePalette

@Composable
fun LiveSuperChatSection(
    items: List<LiveDanmakuItem>,
    modifier: Modifier = Modifier
) {
    val palette = rememberLiveChromePalette()
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items, key = { "${it.uid}_${it.text}_${it.superChatPrice}" }) { item ->
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = palette.surfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.border)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color(item.superChatBackgroundColor.takeIf { it != 0 } ?: 0xFFDD5B6A.toInt()).copy(alpha = 0.12f)
                        )
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = item.uname.ifBlank { "醒目留言" },
                            color = palette.primaryText,
                            fontSize = 15.sp
                        )
                        Text(
                            text = item.superChatPrice.ifBlank { "SC" },
                            color = palette.accentStrong,
                            fontSize = 13.sp
                        )
                    }
                    Text(
                        text = item.text,
                        color = palette.primaryText,
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
}
