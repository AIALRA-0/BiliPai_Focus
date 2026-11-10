@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.android.bilipai.tv.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import androidx.tv.material3.MaterialTheme
import com.android.bilipai.tv.TvCatalogState
import com.android.bilipai.tv.ui.components.TvAppButton
import com.android.bilipai.tv.ui.components.TvFilterChip
import com.android.bilipai.tv.ui.components.TvNavigationItem
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.theme.DarkSurfaceElevated
import com.android.purebilibili.data.model.response.FavFolder
import com.android.purebilibili.data.model.response.FollowingUser
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun TvListToolbar(catalog: TvCatalogState, onFilter: (String, Int) -> Unit, onManage: () -> Unit,
    onClear: () -> Unit, history: Boolean, watchLater: Boolean, favorites: Boolean = false,
    historyFilter: String = "all", onHistoryFilter: (String) -> Unit = {},
    favoriteOrder: String = "mtime", onFavoriteOrder: (String) -> Unit = {}) {
    val interactive = LocalTvInteractive.current
    var query by remember(catalog.keyword) { mutableStateOf(catalog.keyword) }
    var orderDialog by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().tvGlass(TvUiTokens.shape(ContainerLevel.Card), sampleBackdrop = false)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (history || watchLater || favorites) {
                BasicTextField(query, { query = it }, singleLine = true, readOnly = !interactive,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onFilter(query, catalog.viewed) }),
                    modifier = Modifier.width(240.dp).heightIn(min = 48.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, TvUiTokens.shape(ContainerLevel.Card))
                        .focusProperties { canFocus = interactive }.padding(12.dp), decorationBox = { field ->
                        if (query.isEmpty()) Text(
                            when { history -> "搜索历史"; watchLater -> "搜索稍后再看"; else -> "搜索收藏内容" },
                            color = MaterialTheme.colorScheme.secondary)
                        field()
                    })
                TvAppButton({ onFilter(query, catalog.viewed) }) { Text("搜索") }
                if (catalog.keyword.isNotEmpty()) TvAppButton({ onFilter("", catalog.viewed) }) { Text("清除搜索") }
            }
            // 历史类型筛选：与移动端 HistoryContentFilter 同值同语义；官方 FilterChip 选中语义。
            if (history) listOf("all" to "全部", "video" to "视频", "pgc" to "番剧", "live" to "直播", "article" to "专栏")
                .forEach { (value, label) ->
                    TvFilterChip(catalog.historyFilter == value, { onHistoryFilter(value) }) { Text(label) }
                }
            if (watchLater) listOf(0 to "全部", 2 to "未看完", 1 to "已看完").forEach { (value, label) ->
                TvFilterChip(catalog.viewed == value, { onFilter(query, value) }) { Text(label) }
            }
            if (favorites) TvAppButton({ orderDialog = true }) {
                Text("排序：" + when (favoriteOrder) {
                    "view" -> "播放量"; "pubtime" -> "投稿时间"; else -> "收藏时间"
                })
            }
            TvNavigationItem(catalog.managing, onManage) { Text(if (catalog.managing) "完成管理" else "管理") }
            if (history && catalog.managing) TvAppButton(onClear) { Text("清空历史") }
        }
        if (catalog.managing) Text("管理模式：确认卡片打开操作菜单，返回退出管理。", style = MaterialTheme.typography.bodyMedium)
    }
    if (orderDialog) TvChoiceDialog("收藏排序", listOf("mtime" to "收藏时间", "view" to "播放量", "pubtime" to "投稿时间"),
        onDismiss = { orderDialog = false }, onChoose = { orderDialog = false; onFavoriteOrder(it) },
        selectedValue = favoriteOrder)
}

@Composable
internal fun TvFollowingList(catalog: TvCatalogState, requester: FocusRequester, navigationFocus: FocusRequester,
    onOpen: (FollowingUser) -> Unit, onFocused: (String) -> Unit, onMore: () -> Unit, modifier: Modifier = Modifier) {
    val interactive = LocalTvInteractive.current
    val entry = resolveTvFocusIndex(catalog.users.map { "up:${it.mid}" }, catalog.focusedId, catalog.focusedIndex) ?: 0
    val scroll = androidx.compose.foundation.lazy.rememberLazyListState(initialFirstVisibleItemIndex = entry)
    LaunchedEffect(requester, interactive) { if (interactive) requester.requestFocus() }
    LazyColumn(modifier, state = scroll, verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(8.dp)) {
        itemsIndexed(catalog.users, key = { _, user -> user.mid }) { index, user ->
            TvAppButton({ onOpen(user) }, Modifier.fillMaxWidth()
                .then(if (index == entry) Modifier.focusRequester(requester) else Modifier)
                .focusProperties { left = navigationFocus }
                .onFocusChanged { if (it.isFocused) onFocused("up:${user.mid}") }) {
                Text(user.uname + user.sign.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty(), maxLines = 2)
            }
        }
        if (catalog.hasMore) item { TvAppButton(onMore, isLoading = catalog.loading) { Text("加载更多关注") } }
    }
}

@Composable
internal fun TvFavoriteDialog(folders: List<FavFolder>, busy: Boolean, saved: Boolean,
    onSave: (Set<Long>) -> Unit, onDismiss: () -> Unit) {
    var selected by remember { mutableStateOf(folders.filter { it.fav_state == 1 }.map { it.id }.toSet()) }
    val requester = remember { FocusRequester() }
    TvDialogFrame(onDismiss) { dismiss ->
        val interactive = LocalTvInteractive.current
        LaunchedEffect(saved) { if (saved) dismiss() }
        Column(Modifier.widthIn(max = 560.dp).fillMaxWidth().heightIn(max = 440.dp)
            .tvGlass(TvUiTokens.shape(com.android.purebilibili.core.ui.ContainerLevel.Dialog), DarkSurfaceElevated, sampleBackdrop = false)
            .verticalScroll(rememberScrollState()).padding(32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("选择收藏夹", style = MaterialTheme.typography.titleLarge)
            if (folders.isEmpty()) Text("当前账号没有收藏夹，可在手机端创建后重试。")
            folders.forEachIndexed { index, folder ->
                TvNavigationItem(folder.id in selected, {
                    if (!busy) selected = if (folder.id in selected) selected - folder.id else selected + folder.id
                }, Modifier.fillMaxWidth().then(if (index == 0) Modifier.focusRequester(requester) else Modifier)) {
                    Text((if (folder.id in selected) "✓ " else "") + folder.title)
                }
            }
            TvAppButton({ onSave(selected) }, isLoading = busy,
                modifier = if (folders.isEmpty()) Modifier.focusRequester(requester) else Modifier) { Text("保存") }
            TvAppButton({ if (!busy) dismiss() }, enabled = !busy) { Text("取消") }
        }
        LaunchedEffect(requester, interactive) { if (interactive) requester.requestFocus() }
    }
}

@Composable
internal fun TvLinkQr(url: String, label: String) {
    var bitmap by remember(url) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(url) {
        bitmap = withContext(Dispatchers.Default) {
            val matrix = QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, 300, 300)
            val pixels = IntArray(90_000) { index -> if (matrix[index % 300, index / 300]) android.graphics.Color.BLACK else android.graphics.Color.WHITE }
            android.graphics.Bitmap.createBitmap(pixels, 300, 300, android.graphics.Bitmap.Config.ARGB_8888)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        bitmap?.let { Image(it.asImageBitmap(), label, Modifier.size(200.dp).background(Color.White).padding(8.dp)) }
    }
}
