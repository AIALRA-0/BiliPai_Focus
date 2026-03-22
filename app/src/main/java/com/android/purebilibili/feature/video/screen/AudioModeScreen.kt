package com.android.purebilibili.feature.video.screen

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.util.Rational
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.purebilibili.core.ui.AppIcons
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.feature.video.ui.components.CoinDialog
import com.android.purebilibili.feature.video.player.PlayMode
import com.android.purebilibili.feature.video.player.PlaylistManager
import com.android.purebilibili.feature.video.ui.components.CollectionSheet  // 📂 [新增] 合集弹窗
import com.android.purebilibili.feature.video.viewmodel.PlayerUiState
import com.android.purebilibili.feature.video.viewmodel.PlayerViewModel
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.*
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.roundToInt

internal fun resolveAudioPlayModeLabel(mode: PlayMode): String {
    return when (mode) {
        PlayMode.SEQUENTIAL -> "顺序播放"
        PlayMode.SHUFFLE -> "随机播放"
        PlayMode.REPEAT_ONE -> "单曲循环"
    }
}

internal enum class AudioModePlayPauseAction {
    PAUSE,
    RESUME,
    RESTART_FROM_BEGINNING,
    PREPARE_AND_RESUME
}

internal fun resolveAudioModePlayPauseAction(
    isPlaying: Boolean,
    playbackState: Int,
    playWhenReady: Boolean
): AudioModePlayPauseAction {
    if (isPlaying) return AudioModePlayPauseAction.PAUSE
    return when (playbackState) {
        Player.STATE_ENDED -> AudioModePlayPauseAction.RESTART_FROM_BEGINNING
        Player.STATE_IDLE -> AudioModePlayPauseAction.PREPARE_AND_RESUME
        Player.STATE_READY, Player.STATE_BUFFERING -> AudioModePlayPauseAction.RESUME
        else -> if (playWhenReady) AudioModePlayPauseAction.PAUSE else AudioModePlayPauseAction.RESUME
    }
}

internal fun shouldShowAudioModePipButton(sdkInt: Int): Boolean = sdkInt >= Build.VERSION_CODES.O

internal fun parseAudioModeSleepTimerInput(raw: String): Int? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    if (trimmed.all { it.isDigit() }) {
        return trimmed.toIntOrNull()?.takeIf { it > 0 }
    }

    val match = Regex("""^(\d{1,3})\s*[:：]\s*(\d{1,2})$""").matchEntire(trimmed) ?: return null
    val hours = match.groupValues[1].toIntOrNull() ?: return null
    val minutes = match.groupValues[2].toIntOrNull() ?: return null
    if (minutes !in 0..59) return null
    return (hours * 60 + minutes).takeIf { it > 0 }
}

internal fun formatAudioModeSleepTimerButtonLabel(minutes: Int?): String {
    minutes ?: return "定时关闭"
    return when {
        minutes < 60 -> "${minutes}分钟"
        minutes % 60 == 0 -> "${minutes / 60}小时"
        else -> "${minutes / 60}:${(minutes % 60).toString().padStart(2, '0')}"
    }
}

internal fun formatAudioModeSleepTimerInput(minutes: Int?): String {
    minutes ?: return ""
    return if (minutes >= 60 && minutes % 60 != 0) {
        "${minutes / 60}:${(minutes % 60).toString().padStart(2, '0')}"
    } else {
        minutes.toString()
    }
}

private tailrec fun Context.findHostActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findHostActivity()
    else -> null
}

private fun Player.handleAudioModePlayPause() {
    when (
        resolveAudioModePlayPauseAction(
            isPlaying = isPlaying,
            playbackState = playbackState,
            playWhenReady = playWhenReady
        )
    ) {
        AudioModePlayPauseAction.PAUSE -> pause()
        AudioModePlayPauseAction.RESUME -> play()
        AudioModePlayPauseAction.RESTART_FROM_BEGINNING -> {
            seekTo(0L)
            play()
        }
        AudioModePlayPauseAction.PREPARE_AND_RESUME -> {
            prepare()
            play()
        }
    }
}

private fun enterAudioModePip(activity: Activity?) {
    if (activity == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && activity.isInPictureInPictureMode) return
    val paramsBuilder = PictureInPictureParams.Builder()
        .setAspectRatio(Rational(16, 9))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        paramsBuilder.setSeamlessResizeEnabled(true)
    }
    activity.enterPictureInPictureMode(paramsBuilder.build())
}

@Composable
fun AudioModeScreen(
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    onVideoModeClick: (String) -> Unit,  //  传递当前视频的 bvid
    isInPipMode: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sleepTimerMinutes by viewModel.sleepTimerMinutes.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val showPipButton = remember { shouldShowAudioModePipButton(Build.VERSION.SDK_INT) }
    val enterPip = remember(context) { { enterAudioModePip(context.findHostActivity()) } }
    val renderPolicy = remember(isInPipMode) {
        resolveAudioModeRenderPolicy(isInPipMode = isInPipMode)
    }
    
    //  通过共享的 ViewModel 获取播放器实例，实现无缝音频播放
    val player = viewModel.currentPlayer
    
    //  投币对话框状态
    val coinDialogVisible by viewModel.coinDialogVisible.collectAsState()
    val currentCoinCount = (uiState as? PlayerUiState.Success)?.coinCount ?: 0
    
    //  缓存最后一次成功的状态，在加载时继续显示
    var cachedSuccessState by remember { mutableStateOf<PlayerUiState.Success?>(null) }
    
    // 更新缓存
    LaunchedEffect(uiState) {
        if (uiState is PlayerUiState.Success) {
            cachedSuccessState = uiState as PlayerUiState.Success
        }
    }
    
    // 使用缓存的成功状态或当前状态
    val displayState = when {
        uiState is PlayerUiState.Success -> uiState as PlayerUiState.Success
        cachedSuccessState != null -> cachedSuccessState!!
        else -> null
    }

    //  封面显示模式状态
    var isFullScreenCover by remember { mutableStateOf(false) }
    
    // 📂 [新增] 合集弹窗状态
    var showCollectionSheet by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    DisposableEffect(viewModel) {
        viewModel.setAudioMode(true)
        onDispose {
            viewModel.setAudioMode(false)
        }
    }
    
    Scaffold(
        containerColor = Color.Black,
        //  沉浸式导航栏 - 移除系统窗口内边距
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            // 在具体布局中根据需要放置 TopBar
        }
    ) { paddingValues ->
        // 忽略 Scaffold 的 paddingValues，自主控制布局
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            if (displayState != null) {
                val info = displayState.info
                val successState = displayState
                
                // ==================== 共享状态逻辑 ====================
                val playlist by PlaylistManager.playlist.collectAsState()
                val currentIndex by PlaylistManager.currentIndex.collectAsState()
                val currentPlayMode by PlaylistManager.playMode.collectAsState()
                
                // 预加载相邻封面 - 使用 Coil 单例
                val imageLoader = coil.Coil.imageLoader(context)
                LaunchedEffect(currentIndex, playlist) {
                    if (playlist.isNotEmpty()) {
                        val nextIndex = (currentIndex + 1).takeIf { it < playlist.size }
                        nextIndex?.let {
                            imageLoader.enqueue(
                                ImageRequest.Builder(context).data(FormatUtils.fixImageUrl(playlist[it].cover)).build()
                            )
                        }
                        val prevIndex = (currentIndex - 1).takeIf { it >= 0 }
                        prevIndex?.let {
                            imageLoader.enqueue(
                                ImageRequest.Builder(context).data(FormatUtils.fixImageUrl(playlist[it].cover)).build()
                            )
                        }
                    }
                }
                
                val pagerState = rememberPagerState(
                    initialPage = currentIndex.coerceIn(0, (playlist.size - 1).coerceAtLeast(0)),
                    pageCount = { playlist.size.coerceAtLeast(1) }
                )
                
                val density = LocalDensity.current

                // 同步 Pager 和 PlaylistManager
                LaunchedEffect(currentIndex) {
                    if (pagerState.currentPage != currentIndex && currentIndex in 0 until playlist.size) {
                        pagerState.animateScrollToPage(currentIndex)
                    }
                }
                // 当用户滑动 Pager 时，直接加载对应视频
                LaunchedEffect(pagerState.settledPage) {
                    val settledPage = pagerState.settledPage
                    if (settledPage != currentIndex && playlist.isNotEmpty() && settledPage in playlist.indices) {
                        val targetItem = PlaylistManager.playAt(settledPage)
                        targetItem?.let { viewModel.loadVideo(it.bvid) }
                    }
                }

                // ==================== 共享 UI 组件 (控制栏) ====================
                val controlsContent: @Composable () -> Unit = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(30.dp))

                        // 1. 按钮行 - 视频模式 + 封面模式切换
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 视频模式按钮
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .clickable { onVideoModeClick(info.bvid) }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        CupertinoIcons.Default.PlayCircle,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "视频模式",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                // 封面模式切换按钮
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .clickable { isFullScreenCover = !isFullScreenCover }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (isFullScreenCover) CupertinoIcons.Default.ArrowDownRightAndArrowUpLeft
                                        else CupertinoIcons.Default.ArrowUpLeftAndArrowDownRight,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // 2. 视频信息
                        Text(
                            text = info.title,
                            fontSize = 20.sp,
                            lineHeight = 26.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = if (isFullScreenCover) TextAlign.Start else TextAlign.Center 
                        )
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Text(
                            text = info.owner.name,
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = if (isFullScreenCover) TextAlign.Start else TextAlign.Center
                        )
                        
                        // 🎵 [新增] 分P指示器 - 显示当前播放的分P
                        val pages = info.pages
                        if (pages.size > 1) {
                            val currentPageIndex = pages.indexOfFirst { it.cid == info.cid }.coerceAtLeast(0)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "P${currentPageIndex + 1} / ${pages.size}",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = if (isFullScreenCover) TextAlign.Start else TextAlign.Center
                            )
                        }
                        
                        // 📂 [新增] 合集指示器 - 点击打开合集选择弹窗
                        info.ugc_season?.let { season ->
                            val allEpisodes = season.sections.flatMap { it.episodes }
                            val currentEpIndex = allEpisodes.indexOfFirst { it.bvid == info.bvid }
                            val currentPosition = if (currentEpIndex >= 0) currentEpIndex + 1 else 0
                            val totalCount = allEpisodes.size.takeIf { it > 0 } ?: season.ep_count
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                onClick = { showCollectionSheet = true },
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "合集",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = season.title,
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.9f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = 150.dp)
                                    )
                                    if (currentPosition > 0 && totalCount > 0) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "$currentPosition/$totalCount",
                                            fontSize = 12.sp,
                                            color = Color.White.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // 3. 互动按钮行
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            InteractionButton(
                                icon = if (successState.isLiked) CupertinoIcons.Filled.HandThumbsup else CupertinoIcons.Default.HandThumbsup,
                                label = FormatUtils.formatStat(info.stat.like.toLong()),
                                isActive = successState.isLiked,
                                onClick = { viewModel.toggleLike() }
                            )
                            InteractionButton(
                                icon = AppIcons.BiliCoin,
                                label = FormatUtils.formatStat(info.stat.coin.toLong()),
                                isActive = successState.coinCount > 0,
                                onClick = { viewModel.openCoinDialog() }
                            )
                            InteractionButton(
                                icon = if (successState.isFavorited) CupertinoIcons.Filled.Bookmark else CupertinoIcons.Default.Bookmark,
                                label = FormatUtils.formatStat(info.stat.favorite.toLong()),
                                isActive = successState.isFavorited,
                                onClick = { viewModel.toggleFavorite() }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // 4. 播放控制
                        if (player != null) {
                            PlayerControls(
                                player = player,
                                onPlayPause = { player.handleAudioModePlayPause() },
                                onSeek = { pos -> 
                                    player.seekTo(pos)
                                    // [修复] 确保 seek 后音量正常
                                    player.volume = 1.0f
                                },
                                onPrevious = { viewModel.playPreviousRecommended() },
                                // 🎵 [修复] 使用分P优先播放方法
                                onNext = { viewModel.playNextPageOrRecommended() },
                                currentPlayMode = currentPlayMode,
                                onSelectPlayMode = { PlaylistManager.setPlayMode(it) }
                            )
                        } else {
                            Text("Connecting to player...", color = Color.White)
                        }
                        
                        Spacer(modifier = Modifier.height(48.dp)) 
                    }
                }

                // ==================== 布局分支 ====================

                if (renderPolicy.showCompactPipCoverOnly) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = FormatUtils.fixImageUrl(info.pic),
                            contentDescription = "Audio cover",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else if (isFullScreenCover) {
                    // ==================== 全屏模式 (沉浸式) ====================
                    // 1. 底层：Pager 作为背景，填满全屏
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (playlist.isNotEmpty()) {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize(),
                                beyondViewportPageCount = 1,
                                key = { it }  // [修复] 使用索引作为 key，避免重复 bvid 导致崩溃
                            ) { page ->
                                Box(modifier = Modifier.fillMaxSize()) {
                                    AsyncImage(
                                        model = FormatUtils.fixImageUrl(playlist.getOrNull(page)?.cover ?: ""),
                                        contentDescription = "Cover",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .blur(20.dp), // 全屏模式背景模糊
                                        contentScale = ContentScale.Crop,
                                        alpha = 0.8f
                                    )
                                    // 黑色遮罩层 -> 让文字清晰
                                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f))) // 增加遮罩浓度
                                }
                            }
                        } else {
                             Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = FormatUtils.fixImageUrl(info.pic),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().blur(20.dp),
                                    contentScale = ContentScale.Crop,
                                    alpha = 0.8f
                                )
                                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
                             }
                        }
                    }
                    
                    // 2. 顶层：UI Overlay
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (renderPolicy.showTopBar) {
                            AudioModeTopBar(
                                onBack = onBack,
                                showPipButton = showPipButton,
                                onEnterPip = enterPip,
                                sleepTimerMinutes = sleepTimerMinutes,
                                onSleepTimerClick = { showSleepTimerDialog = true }
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        if (renderPolicy.showControlsContent) {
                            controlsContent()
                        }
                    }
                    
                } else {
                    // ==================== 居中模式 (Apple Music 风格) ====================
                    // 1. 底层：背景图 (模糊 + 遮罩)
                    // [修复] 使用 pager 当前页的封面，确保切换时背景同步
                    val currentCover = playlist.getOrNull(pagerState.currentPage)?.cover ?: info.pic
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = FormatUtils.fixImageUrl(currentCover),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().blur(60.dp),
                            contentScale = ContentScale.Crop,
                            alpha = 0.6f
                        )
                    }
                    
                    // 2. 内容层：TopBar + 中间 Pager + 底部 Controls
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (renderPolicy.showTopBar) {
                            AudioModeTopBar(
                                onBack = onBack,
                                showPipButton = showPipButton,
                                onEnterPip = enterPip,
                                sleepTimerMinutes = sleepTimerMinutes,
                                onSleepTimerClick = { showSleepTimerDialog = true }
                            )
                        }
                        
                        // 中间 Pager 区域 - 占据剩余空间
                        BoxWithConstraints(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            val coverSizeDp = resolveAudioModeCenteredCoverSizeDp(
                                availableWidthDp = maxWidth.value.roundToInt(),
                                availableHeightDp = maxHeight.value.roundToInt()
                            )
                            if (playlist.isNotEmpty()) {
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxSize(),
                                    // 关键：不设置 contentPadding，让 Pager 占满宽度，这样旋转时不会在边界被裁剪
                                    contentPadding = PaddingValues(horizontal = 0.dp),
                                    beyondViewportPageCount = 1,
                                    key = { it }  // [修复] 使用索引作为 key，避免重复 bvid 导致崩溃
                                ) { page ->
                                    val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                                    
                                    // 计算 3D 效果
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // 实际的卡片内容框 - 限制宽度为 75%
                                        Box(
                                            modifier = Modifier
                                                .size(coverSizeDp.dp)
                                                .graphicsLayer {
                                                    val rotationAngle = pageOffset * 45f
                                                    rotationY = rotationAngle
                                                    cameraDistance = 12f * density.density
                                                    transformOrigin = TransformOrigin(
                                                        pivotFractionX = if (pageOffset < 0) 1f else 0f,
                                                        pivotFractionY = 0.5f
                                                    )
                                                    val scale = 1f - (abs(pageOffset) * 0.15f).coerceIn(0f, 0.2f)
                                                    scaleX = scale
                                                    scaleY = scale
                                                    alpha = 1f - (abs(pageOffset) * 0.5f).coerceIn(0f, 0.5f)
                                                }
                                        ) {

                                            // 封面图片
                                            AsyncImage(
                                                model = FormatUtils.fixImageUrl(playlist.getOrNull(page)?.cover ?: ""),
                                                contentDescription = "Cover",
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(RoundedCornerShape(20.dp)) // 只裁剪封面本身
                                                    .background(Color.DarkGray),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }
                                }
                             } else {
                                // 空列表兜底
                                Box(
                                    modifier = Modifier
                                        .size(coverSizeDp.dp)
                                ) {
                                    AsyncImage(
                                        model = FormatUtils.fixImageUrl(info.pic),
                                        contentDescription = "Cover",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color.DarkGray),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                             }
                        }
                        
                        // 底部控制栏
                        if (renderPolicy.showControlsContent) {
                            controlsContent()
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }
    }
    
    //  投币对话框
    val userBalance by viewModel.userCoinBalance.collectAsState()
    CoinDialog(
        visible = coinDialogVisible,
        currentCoinCount = currentCoinCount,
        userBalance = userBalance,
        onDismiss = { viewModel.closeCoinDialog() },
        onConfirm = { count, alsoLike -> viewModel.doCoin(count, alsoLike) }
    )
    
    // 📂 [新增] 合集选择弹窗
    val currentInfo = (uiState as? PlayerUiState.Success)?.info
    currentInfo?.ugc_season?.let { season ->
        if (showCollectionSheet) {
            CollectionSheet(
                ugcSeason = season,
                currentBvid = currentInfo.bvid,
                currentCid = currentInfo.cid,
                onDismiss = { showCollectionSheet = false },
                onEpisodeClick = { episode ->
                    showCollectionSheet = false
                    viewModel.loadVideo(episode.bvid, cid = episode.cid)
                }
            )
        }
    }

    if (showSleepTimerDialog) {
        AudioModeSleepTimerDialog(
            currentMinutes = sleepTimerMinutes,
            onDismiss = { showSleepTimerDialog = false },
            onSelectPreset = { minutes ->
                viewModel.setSleepTimer(minutes)
                showSleepTimerDialog = false
            },
            onConfirmCustom = { minutes ->
                viewModel.setSleepTimer(minutes)
                showSleepTimerDialog = false
            }
        )
    }
}

@Composable
private fun AudioModeTopBar(
    onBack: () -> Unit,
    showPipButton: Boolean,
    onEnterPip: () -> Unit,
    sleepTimerMinutes: Int?,
    onSleepTimerClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()  //  添加状态栏内边距以实现沉浸效果
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onBack) {
            Icon(
                CupertinoIcons.Default.ChevronDown, // 下箭头表示收起
                contentDescription = "Back",
                tint = Color.White
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                onClick = onSleepTimerClick,
                shape = RoundedCornerShape(18.dp),
                color = Color.White.copy(alpha = if (sleepTimerMinutes != null) 0.2f else 0.14f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        CupertinoIcons.Default.Timer,
                        contentDescription = "Sleep timer",
                        tint = if (sleepTimerMinutes != null) MaterialTheme.colorScheme.primary else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = formatAudioModeSleepTimerButtonLabel(sleepTimerMinutes),
                        color = if (sleepTimerMinutes != null) MaterialTheme.colorScheme.primary else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (showPipButton) {
                IconButton(onClick = onEnterPip) {
                    Icon(
                        CupertinoIcons.Default.Pip,
                        contentDescription = "Picture in picture",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioModeSleepTimerDialog(
    currentMinutes: Int?,
    onDismiss: () -> Unit,
    onSelectPreset: (Int?) -> Unit,
    onConfirmCustom: (Int) -> Unit
) {
    var customInput by remember(currentMinutes) {
        mutableStateOf(formatAudioModeSleepTimerInput(currentMinutes))
    }
    val parsedCustomMinutes = remember(customInput) {
        parseAudioModeSleepTimerInput(customInput)
    }
    val showCustomError = customInput.isNotBlank() && parsedCustomMinutes == null
    val presetOptions = listOf<Int?>(null, 15, 30, 60, 90)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("定时关闭") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "选择常用时长，或输入分钟数 / 小时:分钟，例如 90、1:30",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetOptions.forEach { minutes ->
                        val isSelected = currentMinutes == minutes
                        Surface(
                            onClick = { onSelectPreset(minutes) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = formatAudioModeSleepTimerButtonLabel(minutes),
                                    fontSize = 13.sp,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = customInput,
                    onValueChange = { customInput = it.take(8) },
                    singleLine = true,
                    label = { Text("自定义时间") },
                    placeholder = { Text("例如 45 或 1:30") },
                    isError = showCustomError,
                    supportingText = {
                        if (showCustomError) {
                            Text("请输入正整数分钟，或 小时:分钟，分钟需在 0-59")
                        } else if (parsedCustomMinutes != null) {
                            Text("将于 ${formatAudioModeSleepTimerButtonLabel(parsedCustomMinutes)}后暂停播放")
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { parsedCustomMinutes?.let(onConfirmCustom) },
                enabled = parsedCustomMinutes != null
            ) {
                Text("应用")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun InteractionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.size(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) MaterialTheme.colorScheme.primary else Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = if (isActive) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerControls(
    player: Player,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    currentPlayMode: PlayMode,
    onSelectPlayMode: (PlayMode) -> Unit
) {
    var isDragging by remember { mutableStateOf(false) }
    var draggingProgress by remember { mutableFloatStateOf(0f) }
    
    // [修复] 进度状态 - 使用 key 确保切换视频时重置
    var currentPos by remember(player) { mutableLongStateOf(player.currentPosition) }
    var duration by remember(player) { mutableLongStateOf(player.duration.coerceAtLeast(0)) }
    var isPlaying by remember(player) { mutableStateOf(player.isPlaying) }
    
    LaunchedEffect(player) {
        // [修复] 立即读取当前状态
        currentPos = player.currentPosition
        duration = player.duration.coerceAtLeast(0)
        isPlaying = player.isPlaying
        // 然后开始轮询
        while (isActive) {
            delay(500)
            if (!isDragging) {
                currentPos = player.currentPosition
                duration = player.duration.coerceAtLeast(0)
            }
            isPlaying = player.isPlaying
        }
    }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        //  更细的进度条 - 使用自定义样式
        Slider(
            value = if (isDragging) draggingProgress else (if (duration > 0) currentPos.toFloat() / duration else 0f),
            onValueChange = { 
                isDragging = true
                draggingProgress = it
            },
            onValueChangeFinished = {
                val target = (draggingProgress * duration).toLong()
                // [修复] 记录 seek 前的播放状态
                val wasPlaying = player.isPlaying || player.playbackState == Player.STATE_BUFFERING
                onSeek(target)
                // [修复] 确保 seek 后恢复播放状态和音量
                player.volume = 1.0f
                if (wasPlaying) {
                    player.play()
                }
                isDragging = false
            },
            modifier = Modifier.height(20.dp),  // 减小整体高度
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            ),
            thumb = {
                //  更小的圆形滑块
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(Color.White, CircleShape)
                )
            },
            track = { sliderState ->
                //  更细的轨道
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(sliderState.value)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(Color.White)
                    )
                }
            }
        )
        
        // 时间
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = FormatUtils.formatDuration((currentPos / 1000).toInt()),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
            Text(
                text = FormatUtils.formatDuration((duration / 1000).toInt()),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(PlayMode.SEQUENTIAL, PlayMode.SHUFFLE, PlayMode.REPEAT_ONE).forEach { mode ->
                val isSelected = currentPlayMode == mode
                Surface(
                    onClick = { onSelectPlayMode(mode) },
                    shape = RoundedCornerShape(999.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                    } else {
                        Color.White.copy(alpha = 0.15f)
                    }
                ) {
                    Text(
                        text = resolveAudioPlayModeLabel(mode),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
        
        // 播放控制按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            //  上一个推荐视频
            IconButton(onClick = onPrevious) {
                Icon(
                    CupertinoIcons.Default.BackwardEnd,
                    contentDescription = "上一个",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            // 播放/暂停
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .size(64.dp)
                    .background(Color.White, CircleShape)
            ) {
                Icon(
                    imageVector = if (isPlaying) CupertinoIcons.Filled.Pause else CupertinoIcons.Filled.Play,
                    contentDescription = "Play/Pause",
                    tint = Color.Black,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            //  下一个推荐视频
            IconButton(onClick = onNext) {
                Icon(
                    CupertinoIcons.Default.ForwardEnd,
                    contentDescription = "下一个",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
