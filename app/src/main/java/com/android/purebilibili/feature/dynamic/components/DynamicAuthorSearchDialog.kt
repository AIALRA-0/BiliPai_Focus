package com.android.purebilibili.feature.dynamic.components

import com.android.purebilibili.core.ui.components.appActionClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.AppAlertDialog
import com.android.purebilibili.core.ui.components.AppLazyColumn
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.AppTextButton
import com.android.purebilibili.core.ui.components.PageAwareAsyncImage
import com.android.purebilibili.feature.dynamic.SidebarUser
import com.android.purebilibili.feature.dynamic.searchDynamicAuthors

@Composable
internal fun DynamicAuthorSearchDialog(
    users: List<SidebarUser>,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    onAuthorSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { AppText("搜索 UP 主") },
        text = {
            DynamicAuthorSearchContent(users, isLoading, onAuthorSelected)
        },
        confirmButton = { AppTextButton(onClick = onDismiss) { AppText("关闭") } },
    )
}

@Composable
internal fun DynamicAuthorSearchContent(
    users: List<SidebarUser>,
    isLoading: Boolean,
    onAuthorSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val matches = remember(users, query) { searchDynamicAuthors(users, query) }
    val listState = remember(query) { LazyListState() }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        com.android.purebilibili.core.ui.components.AppLiquidAwareSearchField(
            query = query,
            onQueryChange = { query = it },
            placeholder = "输入关注的 UP 主名字",
            compactHeight = 40.dp,
            inheritPopupGlass = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (isLoading) AppText("正在加载关注列表…")
        if (matches.isEmpty() && !isLoading) {
            AppText(if (query.isBlank()) "暂无可搜索的 UP 主" else "没有找到匹配的 UP 主")
        }
        AppLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
        ) {
            items(matches, key = { it.uid }) { user ->
                Row(
                    modifier = Modifier.fillMaxWidth().appActionClickable { onAuthorSelected(user.uid) }.padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PageAwareAsyncImage(model = user.face, contentDescription = null, modifier = Modifier.size(40.dp).clip(CircleShape))
                    AppText(text = user.name, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
