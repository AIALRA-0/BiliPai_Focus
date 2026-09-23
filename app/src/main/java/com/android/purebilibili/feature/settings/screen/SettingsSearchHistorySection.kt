package com.android.purebilibili.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.components.*

@Composable
internal fun SettingsSearchHistorySection(
    history: List<String>,
    onQueryClick: (String) -> Unit,
    onDelete: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AppText("搜索历史", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (history.isNotEmpty()) {
                AppTextButton(onClick = onClear, modifier = Modifier.heightIn(min = 48.dp)) {
                    AppText("清空全部")
                }
            }
        }
        if (history.isEmpty()) {
            AppText(
                "暂无搜索历史，搜索设置功能后会显示在这里",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp),
            )
        } else AppPreferenceGroup {
            history.forEachIndexed { index, query ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                            .clickable { onQueryClick(query) }.padding(horizontal = 16.dp, vertical = 12.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        AppText(query, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    AppIconButton(onClick = { onDelete(query) }, modifier = Modifier.size(48.dp)) {
                        AppIcon(Icons.Rounded.Close, contentDescription = "删除搜索历史：$query")
                    }
                }
                if (index != history.lastIndex) AppPreferenceDivider()
            }
        }
    }
}
