@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.android.bilipai.tv.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.foundation.layout.Box
import com.android.bilipai.tv.ui.components.TvStateFeedback
import com.android.purebilibili.core.ui.MaidAnimation
import com.android.purebilibili.core.ui.motion.AppMotionEasing
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.focusable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import android.view.KeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.android.bilipai.tv.ui.components.TvAppButton
import com.android.bilipai.tv.ui.components.TvNavigationItem
import com.android.bilipai.tv.ui.components.TvVideoCard
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.theme.DarkSurfaceElevated
import com.android.purebilibili.core.util.FormatUtils
import com.android.bilipai.tv.QrPhase
import com.android.bilipai.tv.TvUiState
import com.android.purebilibili.data.model.VideoQuality
import com.android.purebilibili.data.model.response.VideoItem
import kotlinx.coroutines.launch

@Composable
internal fun TvSearchInput(state: TvUiState, requester: FocusRequester, onSearch: (String) -> Unit,
    onSuggest: (String) -> Unit = {}) {
    val interactive = LocalTvInteractive.current
    var draft by rememberSaveable { mutableStateOf(state.query) }
    val submitFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    fun submit() { if (interactive && draft.isNotBlank()) { keyboard?.hide(); onSearch(draft) } }
        LaunchedEffect(requester, interactive) { if (interactive) requester.requestFocus() }
    // 实时联想（SearchRepository.getSuggest）：输入防抖 300ms；清空输入回落到历史/热词。
    LaunchedEffect(draft) { kotlinx.coroutines.delay(300); onSuggest(draft) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            BasicTextField(value = draft, onValueChange = { draft = it }, singleLine = true,
                readOnly = !interactive, textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { submit() }),
                modifier = Modifier.weight(1f).focusRequester(requester).focusProperties { canFocus = interactive }.testTag("tv-search-input")
                    .tvGlass(TvUiTokens.shape(ContainerLevel.Card), sampleBackdrop = false).padding(18.dp)
                    .onPreviewKeyEvent { event ->
                        when (event.nativeKeyEvent.keyCode) {
                            KeyEvent.KEYCODE_DPAD_CENTER -> {
                                if (event.nativeKeyEvent.action == KeyEvent.ACTION_UP) keyboard?.show()
                                true
                            }
                            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                                if (event.nativeKeyEvent.action == KeyEvent.ACTION_UP) submit()
                                true
                            }
                            KeyEvent.KEYCODE_DPAD_DOWN -> {
                                if (draft.isBlank()) {
                                    keyboard?.hide()
                                    return@onPreviewKeyEvent false
                                }
                                if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                                    keyboard?.hide()
                                    submitFocus.requestFocus()
                                }
                                true
                            }
                            else -> false
                        }
                    },
                decorationBox = { field ->
                    if (draft.isEmpty()) Text("输入视频关键词", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    field()
                })
            TvAppButton(onClick = { submit() }, enabled = draft.isNotBlank(), modifier = Modifier.focusRequester(submitFocus)) { Text("搜索") }
        }
        val suggestions = (if (draft.isNotBlank()) state.suggest
            else state.searchHistory.take(3) + state.trending.take(3)).distinct().take(5)
        if (suggestions.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                suggestions.forEach { word ->
                    TvAppButton(onClick = { draft = word; submit() }, modifier = Modifier.weight(1f)) {
                        Text(word, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
internal fun TvDetailContent(
    state: TvUiState,
    requester: FocusRequester,
    onPlay: (Long) -> Unit,
    onWatchLater: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onRestoredAction: () -> Unit = {}, onLike: () -> Unit = {}, onFavorite: () -> Unit = {}, onSpace: () -> Unit = {}, onCoin: (Int, Boolean) -> Unit = { _, _ -> },
    onComments: () -> Unit = {},
    onOpenVideo: (VideoItem) -> Unit = {},
    onPlayEpisode: (String, Long, Long, String) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier,
) {
    if (state.detailLoading) {
        TvStateFeedback("正在加载视频详情…", null, "返回列表", onBack, requester, modifier)
        return
    }
    val info = state.detail
    if (info == null) {
        TvStateFeedback(state.detailError ?: "未找到视频", MaidAnimation.RETRY, "重试", onRetry, requester, modifier)
        return
    }
    val interactive = LocalTvInteractive.current
    var expanded by rememberSaveable(info.bvid) { mutableStateOf(false) }
    var focusedAction by rememberSaveable(info.bvid) { mutableStateOf("play") }
    var coinDialog by rememberSaveable(info.bvid) { mutableStateOf(false) }
    var descriptionFocused by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()
    val descriptionScroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    val actionIds = listOf("play", "later", "like", "coin", "favorite", "comments", "space", "expand", "description") +
        (if (info.pages.size > 1) info.pages.map { "part:${it.cid}" } else emptyList()) +
        (if (info.ugc_season != null) listOf("season") else emptyList())
    val actionRequesters = remember(info.bvid, actionIds) {
        actionIds.associateWith { FocusRequester() }
    }
    val restoredAction = state.resumeAction?.takeIf { it in actionRequesters } ?: focusedAction
    val entryAction = restoredAction.takeIf { it in actionRequesters && (it != "description" || expanded) } ?: "play"
    fun actionModifier(id: String): Modifier = Modifier
        .then(if (id == entryAction) Modifier.focusRequester(requester) else Modifier)
        .focusRequester(actionRequesters.getValue(id))
        .onFocusChanged { if (it.isFocused) focusedAction = id }

    LaunchedEffect(requester, info.bvid, interactive, state.favoriteFolders != null, state.resumeAction) {
        if (interactive && state.favoriteFolders == null) {
            actionRequesters.getValue(entryAction).requestFocus()
            if (state.resumeAction != null) onRestoredAction()
        }
    }
    BackHandler(enabled = expanded && interactive) {
        expanded = false
        actionRequesters.getValue("expand").requestFocus()
    }
    // 投币弹窗关闭后恢复投币按钮；子档位选择走 onCoin 即关即投。
    if (coinDialog) TvChoiceDialog(
        "投币",
        listOf(1 to "1 枚硬币", 2 to "2 枚硬币 · 同时点赞"),
        onDismiss = { coinDialog = false },
        onChoose = { count -> coinDialog = false; onCoin(count, count == 2) },
        selectedValue = null,
    )
    val density = LocalDensity.current
    val fontScale = density.fontScale
    val descriptionStep = with(density) { AppSpacingTokens.TripleExtraLarge.roundToPx() }
    BoxWithConstraints(modifier) {
        val wide = maxWidth >= 720.dp && fontScale <= 1.3f
        val coverWidth = (maxWidth * 0.4f).coerceAtMost(320.dp)
        val cover: @Composable () -> Unit = {
            AsyncImage(
                model = info.pic.let { if (it.startsWith("//")) "https:$it" else it },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .then(if (wide) Modifier.width(coverWidth) else Modifier.widthIn(max = 320.dp).fillMaxWidth())
                    .aspectRatio(16f / 9f)
                    .clip(TvUiTokens.shape(ContainerLevel.MediaCover)),
            )
        }
        val summary: @Composable () -> Unit = {
            Text(
                info.title,
                style = MaterialTheme.typography.headlineMedium,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(info.owner.name, style = MaterialTheme.typography.titleMedium)
            // UP 主卡片数据（与手机端 getCreatorCardStats 同链路）：粉丝/投稿数为选片决策上下文。
            state.creator?.let { creator ->
                Text(
                    "${FormatUtils.formatStat(creator.followerCount.toLong())} 粉丝 · ${FormatUtils.formatStat(creator.videoCount.toLong())} 投稿",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // BV 式数据行：点赞 · 投币 · 收藏 · 发布日期，小字次级色，提供选片决策上下文。
            Text(
                buildString {
                    append(FormatUtils.formatStat(info.stat.like.toLong()) + " 点赞")
                    append(" · " + FormatUtils.formatStat(info.stat.coin.toLong()) + " 投币")
                    append(" · " + FormatUtils.formatStat(info.stat.favorite.toLong()) + " 收藏")
                    if (info.pubdate > 0) {
                        append(" · " + java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.CHINA)
                            .format(java.util.Date(info.pubdate * 1000)))
                    }
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(AppSpacingTokens.Large),
                verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small),
            ) {
                TvAppButton(
                    onClick = { onPlay(info.cid) },
                    modifier = actionModifier("play").testTag("tv-play"),
                ) { Text(if (state.detailResumePositionMs > 0) "继续观看" else "播放") }
                TvAppButton(onClick = onWatchLater, isLoading = state.actionBusy, modifier = actionModifier("later")) { Text("稍后再看") }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TvAppButton(onLike, actionModifier("like"), isLoading = state.actionBusy) { Text(if (state.liked == true) "已点赞 · 取消" else "点赞") }
                TvAppButton(onClick = { coinDialog = true }, isLoading = state.actionBusy, modifier = actionModifier("coin")) { Text("投币") }
                TvAppButton(onFavorite, actionModifier("favorite"), isLoading = state.favoriteLoading || state.actionBusy) { Text("收藏") }
                TvAppButton(onClick = onComments, modifier = actionModifier("comments").testTag("tv-comments-entry")) {
                    Text("评论" + if (info.stat.reply > 0) " · ${FormatUtils.formatStat(info.stat.reply.toLong())}" else "")
                }
                TvAppButton(onSpace, actionModifier("space")) { Text("UP 主空间") }
            }
            if (state.detailResumePositionMs > 0) {
                Text(
                    "上次看到 " + FormatUtils.formatDuration(state.detailResumePositionMs),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }

        }
        Column(
            Modifier.verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.ExtraLarge),
        ) {
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacingTokens.DoubleExtraLarge)) {
                    cover()
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Large)) { summary() }
                }
            } else {
                cover()
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Large)) { summary() }
            }
            Text(
                info.desc.ifBlank { "暂无简介" },
                style = MaterialTheme.typography.bodyLarge,
                // BV 式两级灰度：简介默认弱化为次级色，展开阅读并聚焦时提亮为正文色。
                color = if (descriptionFocused) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis,
                modifier = if (expanded) actionModifier("description")
                    .heightIn(max = 240.dp)
                    .onFocusChanged { descriptionFocused = it.isFocused }
                    .border(TvUiTokens.focusBorderWidth,
                        if (descriptionFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                        TvUiTokens.shape(ContainerLevel.Card))
                    .padding(TvUiTokens.cardPadding)
                    .verticalScroll(descriptionScroll)
                    .onPreviewKeyEvent { event ->
                        val delta = when (event.nativeKeyEvent.keyCode) {
                            KeyEvent.KEYCODE_DPAD_DOWN -> descriptionStep
                            KeyEvent.KEYCODE_DPAD_UP -> -descriptionStep
                            else -> 0
                        }
                        val canScroll = if (delta > 0) descriptionScroll.canScrollForward else descriptionScroll.canScrollBackward
                        if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN && delta != 0 && canScroll) {
                            scope.launch { descriptionScroll.scrollTo((descriptionScroll.value + delta).coerceIn(0, descriptionScroll.maxValue)) }
                            true
                        } else false
                    }
                    .focusProperties { canFocus = interactive }
                    .focusable()
                    .testTag("tv-detail-description") else Modifier,
            )
            TvAppButton(
                onClick = { expanded = !expanded },
                modifier = actionModifier("expand").testTag("tv-detail-expand"),
            ) { Text(if (expanded) "收起详情" else "展开详情") }
            if (info.pages.size > 1) {
                Text("选集", style = MaterialTheme.typography.titleLarge)
                info.pages.forEach { page ->
                    TvNavigationItem(
                        selected = page.cid == info.cid,
                        onClick = { onPlay(page.cid) },
                        modifier = actionModifier("part:${page.cid}")
                            .fillMaxWidth()
                            .testTag("tv-part:${page.cid}"),
                    ) {
                        Text(
                            (if (page.cid == info.cid) "当前 · " else "") + "P${page.page} · ${page.part}",
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            // 联合创作成员：被动展示（与手机端 staff 归并语义一致，owner 之外的成员列出）。
            if (info.staff.size > 1) {
                Text("联合创作", style = MaterialTheme.typography.titleLarge)
                info.staff.forEach { member ->
                    Text(
                        member.name + member.title.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            // 合集入口：弹窗列出分集，确认直接播放该集；当前集在弹窗中标出。
            info.ugc_season?.let { season ->
                val episodes = season.sections.flatMap { it.episodes }.filter { it.cid > 0 }
                if (episodes.isNotEmpty()) {
                    var seasonDialog by rememberSaveable(info.bvid) { mutableStateOf(false) }
                    val seasonTitle = season.title.ifBlank { "合集" }
                    TvAppButton(
                        onClick = { seasonDialog = true },
                        modifier = actionModifier("season").testTag("tv-season-entry"),
                    ) {
                        Text("$seasonTitle · ${episodes.size} 集")
                    }
                    if (seasonDialog) TvChoiceDialog(
                        seasonTitle,
                        episodes.map { it to (it.title.ifBlank { "第 ${episodes.indexOf(it) + 1} 集" }) },
                        onDismiss = { seasonDialog = false },
                        onChoose = { episode ->
                            seasonDialog = false
                            onPlayEpisode(episode.bvid, episode.aid, episode.cid, episode.title)
                        },
                        selectedValue = episodes.firstOrNull { it.bvid == info.bvid || (it.bvid.isBlank() && it.aid == info.aid) },
                    )
                }
            }
            // 相关推荐：与手机端同链路（VideoCatalogRepository.getRelatedVideos），确认打开新详情。
            if (state.related.isNotEmpty()) {
                Text("相关推荐", style = MaterialTheme.typography.titleLarge)
                state.related.take(12).chunked(3).forEach { rowVideos ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        rowVideos.forEach { video ->
                            TvVideoCard(
                                video = video,
                                onClick = { onOpenVideo(video) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(3 - rowVideos.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            } else if (state.relatedLoading) {
                Text("正在加载相关推荐…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun TvLoginContent(state: TvUiState, requester: FocusRequester, onRefresh: () -> Unit, onSignOut: () -> Unit) {
    val interactive = LocalTvInteractive.current
    val loggedIn = state.account != null || state.qr.phase == QrPhase.Success
    LaunchedEffect(requester, loggedIn, interactive) { if (interactive) requester.requestFocus() }
    BoxWithConstraints(Modifier.fillMaxSize().padding(TvUiTokens.pagePadding)) {
        val wide = maxWidth >= 640.dp && LocalDensity.current.fontScale <= 1.3f
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("账号", style = MaterialTheme.typography.headlineLarge)
            if (loggedIn) {
                Text("已登录 · ${state.account?.uname ?: "正在读取账号信息"}", style = MaterialTheme.typography.titleLarge)
                Text("收藏、历史和稍后再看与当前账号同步。")
                state.accountError?.let { Text(it) }
                TvAppButton(onSignOut, Modifier.focusRequester(requester)) { Text("退出登录") }
            } else {
                val qr: @Composable () -> Unit = {
                    // 二维码外层玻璃卡：白底二维码保持可扫，边缘与圆角由面板提供。
                    Box(Modifier.tvGlass(TvUiTokens.shape(ContainerLevel.Card), sampleBackdrop = false).padding(16.dp)) {
                        Box(Modifier.size(250.dp).background(Color.White, TvUiTokens.shape(ContainerLevel.MediaCover))) {
                            state.qr.bitmap?.let { Image(it.asImageBitmap(), "扫码登录二维码", Modifier.fillMaxSize().padding(8.dp)) }
                        }
                    }
                }
                val instructions: @Composable () -> Unit = {
                    Column(
                        Modifier.tvGlass(TvUiTokens.shape(ContainerLevel.Card), sampleBackdrop = false).padding(TvUiTokens.pagePadding),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        Text("使用哔哩哔哩手机 App 扫码，并在手机上确认登录。", style = MaterialTheme.typography.bodyLarge)
                        Text(when (state.qr.phase) {
                            QrPhase.Loading -> "正在生成二维码…"
                            QrPhase.Waiting -> "等待扫码"
                            QrPhase.Scanned -> "已扫码，请在手机上确认"
                            QrPhase.Expired -> "二维码已过期，请刷新"
                            QrPhase.Failed -> state.qr.error ?: "登录失败，请重试"
                            QrPhase.Success -> "登录成功"
                        }, modifier = Modifier.heightIn(min = 64.dp))
                        TvAppButton(onRefresh, Modifier.focusRequester(requester), isLoading = state.qr.phase == QrPhase.Loading) { Text("刷新二维码") }
                        Text("返回即可取消本次登录。", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (wide) Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) { qr(); Box(Modifier.weight(1f)) { instructions() } }
                else { qr(); instructions() }
            }
        }
    }
}

/** 设置分组面板：BV 式玻璃分组，标题 + 同组操作行收进同一块表面。 */
@Composable
private fun TvSettingsPanel(title: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().widthIn(max = 760.dp)
            .tvGlass(TvUiTokens.shape(ContainerLevel.Card), sampleBackdrop = false)
            .padding(TvUiTokens.pagePadding),
        verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        content()
    }
}

@Composable
internal fun TvSettingsContent(state: TvUiState, requester: FocusRequester, onQuality: (Int) -> Unit,
    onAutoContinue: () -> Unit, onDanmaku: () -> Unit, onPrivacy: () -> Unit, onClearSearchHistory: () -> Unit, onReduceMotion: () -> Unit = {}, onSimpleEffects: () -> Unit = {}, onCheckUpdate: () -> Unit = {}, onDensity: (Float) -> Unit = {}, onDynamicColor: () -> Unit = {}) {
    var chooseQuality by remember { mutableStateOf(false) }
    var densityDialog by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf(false) }
    val interactive = LocalTvInteractive.current
    val rowModifier = Modifier.fillMaxWidth()
    LaunchedEffect(requester, chooseQuality, interactive, feedback) { if (!chooseQuality && !feedback && interactive) requester.requestFocus() }
    Column(Modifier.fillMaxSize().padding(horizontal = TvUiTokens.pagePadding).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Large)) {
        Text("设置", style = MaterialTheme.typography.headlineMedium)
        TvSettingsPanel("播放偏好") {
            TvAppButton(onClick = { chooseQuality = true }, modifier = rowModifier.focusRequester(requester)) {
                Text("默认画质：${VideoQuality.fromCode(state.quality)?.description ?: state.quality}")
            }
            TvNavigationItem(selected = state.autoContinue, onClick = onAutoContinue, modifier = rowModifier) { Text("播完自动播放下一 P：${if (state.autoContinue) "开启" else "关闭"}") }
            TvNavigationItem(selected = state.danmakuEnabled, onClick = onDanmaku, modifier = rowModifier) { Text("弹幕显示：${if (state.danmakuEnabled) "开启" else "关闭"}") }
            Text("画质可用性由账号权限、视频内容和设备能力决定。", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary)
        }
        TvSettingsPanel("视觉与动画") {
            TvNavigationItem(state.reduceMotion, onReduceMotion, rowModifier) { Text("减少动画：${if (state.reduceMotion) "开启" else "关闭"}") }
            // 全局模糊总开关：关闭后氛围背景与所有玻璃面板回退官方 MD3 纯色表面。
            TvNavigationItem(state.simpleEffects, onSimpleEffects, rowModifier) { Text("模糊效果：${if (state.simpleEffects) "关闭" else "开启"}（关闭时使用系统默认表面）") }
            TvNavigationItem(state.dynamicColor, onDynamicColor, rowModifier) { Text("动态取色：${if (state.dynamicColor) "跟随壁纸与封面" else "关闭"}（关闭时使用品牌粉）") }
            TvAppButton(onClick = { densityDialog = true }, modifier = rowModifier) { Text("网格密度：${gridDensityLabel(state.gridDensity)}") }
            Text("关闭后氛围背景与毛玻璃面板改为纯色表面；网格列表页长按 OK 键也可随时调整列数。", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary)
        }
        TvSettingsPanel("隐私与记录") {
            TvNavigationItem(selected = state.privacyMode, onClick = onPrivacy, modifier = rowModifier) { Text("暂停上报观看历史：${if (state.privacyMode) "开启" else "关闭"}") }
            TvAppButton(onClick = onClearSearchHistory, modifier = rowModifier) { Text("清空搜索历史") }
        }
        TvSettingsPanel("版本与反馈") {
            val context = androidx.compose.ui.platform.LocalContext.current
            val version = remember(context) { context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty() }
            Text("BiliPai TV $version · 普通视频公开测试版", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            TvAppButton(onCheckUpdate, rowModifier, isLoading = state.update.loading) { Text("检查 TV 更新") }
            state.update.message?.let { Text(it, modifier = rowModifier) }
            state.update.pageUrl?.let { TvLinkQr(it, "手机扫码进入 TV 下载页") }
            TvAppButton({ feedback = true }, rowModifier) { Text("问题反馈") }
        }
    }
    if (feedback) TvDialogFrame({ feedback = false }) { dismiss ->
        val focus = remember { FocusRequester() }
        Column(Modifier.tvGlass(TvUiTokens.shape(ContainerLevel.Dialog), DarkSurfaceElevated, sampleBackdrop = false).padding(32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TvLinkQr(com.android.bilipai.tv.TvUpdateRepository.feedbackUrl, "扫码提交 TV 问题，请附上版本、视频和复现步骤")
            FocusButton("关闭", dismiss, focus)
        }
    }
    if (chooseQuality) TvChoiceDialog("默认画质", VideoQuality.entries.map { it.code to it.description },
        onDismiss = { chooseQuality = false }, onChoose = onQuality, selectedValue = state.quality)
    if (densityDialog) TvChoiceDialog("网格密度", GRID_DENSITY_STEPS.map { it to gridDensityLabel(it) },
        onDismiss = { densityDialog = false }, onChoose = { densityDialog = false; onDensity(it) },
        selectedValue = state.gridDensity)
}

@Composable
internal fun <T> TvChoiceDialog(
    title: String,
    options: List<Pair<T, String>>,
    onDismiss: () -> Unit,
    onChoose: (T) -> Unit,
    selectedValue: T? = null,
    modifier: Modifier = Modifier,
) {
    val requester = remember { FocusRequester() }
    val selectedIndex = options.indexOfFirst { it.first == selectedValue }.coerceAtLeast(0)
    TvDialogFrame(onDismiss) { dismiss ->
        Column(modifier.widthIn(max = 520.dp).fillMaxWidth().heightIn(max = 440.dp)
            .tvGlass(TvUiTokens.shape(ContainerLevel.Dialog), DarkSurfaceElevated, sampleBackdrop = false)
            .verticalScroll(rememberScrollState()).padding(TvUiTokens.pagePadding),
            verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Large)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            options.forEachIndexed { index, (value, label) ->
                TvNavigationItem(selected = value == selectedValue, onClick = { onChoose(value); dismiss() },
                    modifier = Modifier.fillMaxWidth()
                        .then(if (index == selectedIndex) Modifier.focusRequester(requester) else Modifier)) {
                    Text((if (value == selectedValue) "当前 · " else "") + label)
                }
            }
            TvAppButton(onClick = dismiss,
                modifier = if (options.isEmpty()) Modifier.focusRequester(requester) else Modifier) { Text("取消") }
        }
        val interactive = LocalTvInteractive.current
        LaunchedEffect(requester, interactive) { if (interactive) requester.requestFocus() }
    }
}

/** Retain the surface for its exit; disable input at the start of closing. */
@Composable
internal fun TvDialogFrame(onDismiss: () -> Unit, content: @Composable (dismiss: () -> Unit) -> Unit) {
    val parentInteractive = LocalTvInteractive.current
    val transition = remember { MutableTransitionState(false).apply { targetState = true } }
    LaunchedEffect(parentInteractive) { if (!parentInteractive) transition.targetState = false }
    val reduce = LocalTvReduceMotion.current
    val latestDismiss by rememberUpdatedState(onDismiss)
    val dismiss = { transition.targetState = false; Unit }
    Dialog(onDismissRequest = dismiss) {
        AnimatedVisibility(transition,
            enter = fadeIn(tween(if (reduce) 0 else TvMotion.enterMs, easing = AppMotionEasing.Continuity)) +
                scaleIn(
                    initialScale = 0.96f,
                    animationSpec = tween(if (reduce) 0 else TvMotion.enterMs, easing = AppMotionEasing.Continuity),
                    transformOrigin = TransformOrigin.Center,
                ),
            exit = fadeOut(tween(if (reduce) 0 else TvMotion.exitMs, easing = AppMotionEasing.Continuity)) +
                scaleOut(
                    targetScale = 0.96f,
                    animationSpec = tween(if (reduce) 0 else TvMotion.exitMs, easing = AppMotionEasing.Continuity),
                    transformOrigin = TransformOrigin.Center,
                )) {
            CompositionLocalProvider(LocalTvInteractive provides (parentInteractive && transition.targetState), LocalTvReturnTarget provides null) {
                Box(if (transition.targetState) Modifier else Modifier.focusProperties { canFocus = false }.onPreviewKeyEvent { true }) { content(dismiss) }
            }
        }
    }
    LaunchedEffect(transition.isIdle, transition.currentState, transition.targetState) {
        if (transition.isIdle && !transition.currentState && !transition.targetState) latestDismiss()
    }
}
