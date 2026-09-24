package com.android.purebilibili.feature.message

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChatScreenStructureTest {

    @Test
    fun chatBubblesReuseLargeCardGlassTintAndRelatedCoverMetrics() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/message/ChatScreen.kt"
        )
        assertTrue(source.contains("messageGlassContainer("))
        assertTrue(source.contains("val bubbleShape = AppShapes.container(ContainerLevel.Card)"))
        assertTrue(source.contains("shape = bubbleShape"))
        assertTrue(source.contains("contentColor = fallbackContentColor"))
        assertTrue(source.contains("linkColor = textColor"))
        assertTrue(source.contains("BoxWithConstraints("))
        assertTrue(source.contains("resolveMessageBubbleMaxWidth(maxWidth)"))
        assertTrue(source.contains("contentAlignment = if (isOwnMessage) Alignment.TopEnd else Alignment.TopStart"))
        assertTrue(source.contains("AppSpacingTokens.Medium"))
        assertTrue(source.contains("AppSpacingTokens.Small"))
        assertTrue(source.contains(".imePadding()"))
        assertTrue(source.contains(".consumeWindowInsets(paddingValues)"))
        assertTrue(source.contains("bottom = CHAT_MESSAGE_LIST_BOTTOM_PADDING"))
        assertTrue(source.contains("CHAT_INPUT_DOCK_HEIGHT +"))
        assertTrue(source.contains("CHAT_INPUT_VERTICAL_PADDING +"))
        assertTrue(source.contains("rememberMessageGlassContentColors("))
        assertTrue(source.contains("HorizontalVideoCardFrame("))
        assertTrue(source.contains("MessageHorizontalVideoCard("))
        assertTrue(source.contains("MessageLargeVideoCard("))
        assertTrue(source.contains("shouldUseLargeVideoLinkCard"))
        assertTrue(source.contains("aspectRatio(MESSAGE_LARGE_VIDEO_COVER_ASPECT_RATIO)"))
        assertTrue(source.contains("verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.ExtraSmall)"))
        assertTrue(source.contains("subtitle = supportingText"))
        assertTrue(source.contains("MESSAGE_LARGE_VIDEO_COVER_ASPECT_RATIO = 4f / 3f"))
        assertTrue(source.contains("textAlign = TextAlign.Center"))
        assertTrue(source.contains("feedContentTypography(FeedTitleHierarchy.Standard)"))
        assertTrue(source.contains("globalWallpaperAwareChromeColor("))
        assertTrue(source.contains("ChatWallpaperHost"))
        assertTrue(source.contains("HomeWallpaperBackdrop("))
        assertTrue(source.contains("topBarSurfaceColor = AppSurfaceTokens.chromeBackground()"))
        assertTrue(source.contains("preferProgressiveTopBlur = chatThemeConfig.progressiveTopBlurEnabled"))
        assertTrue(source.contains("chromeBackdropSource = chatChromeSource"))
        assertTrue(source.contains("externalHazeState = chatHazeState"))
        assertTrue(source.contains("rememberChromeBackdropSource()"))
        assertTrue(source.contains("hazeSourceCompat"))
        assertTrue(source.contains("append(\"弹幕\")"))
        assertTrue(source.contains("blurContentReady = !uiState.isLoading"))
        assertTrue(source.contains("containerColor = Color.Transparent"))
        assertTrue(source.contains("BottomBarMatchedReusableLiquidDock("))
        assertTrue(source.contains("rememberLayerBackdrop()"))
        assertTrue(source.contains("rememberCombinedBackdrop(chatWallpaperBackdrop, chatContentBackdrop)"))
        assertTrue(source.contains("wallpaperBackdrop = chatWallpaperBackdrop"))
        assertTrue(source.contains("wallpaperBackdrop?.let { Modifier.layerBackdrop(it) }"))
        assertTrue(source.contains("Modifier.layerBackdrop(it)"))
        assertTrue(source.contains("backdrop = chatInputBackdrop"))
        assertTrue(source.contains("modifier = Modifier.align(Alignment.BottomCenter)"))
        assertFalse(source.contains("bottomBar = {"))
        assertFalse(source.contains("LocalFloatingChromeBackdrop"))
        assertTrue(source.contains("shape = CircleShape"))
        assertFalse(source.contains(".height(100.dp)"))
        assertFalse(source.contains(".size(72.dp)"))
        assertFalse(source.contains("variant = AppCardVariant.Elevated"))
    }

    @Test
    fun replyFeedCardsUseSharedGlassSurface() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/message/feed/MessageFeedCommon.kt"
        )
        assertTrue(source.contains("messageGlassContainer("))
        assertTrue(source.contains("AppShapes.borderedContainer(surfaceSpec.cornerLevel)"))
        assertFalse(source.contains("AppSurface("))
    }

    @Test
    fun videoPreviewLoadsPlaybackAndDanmakuStats() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/message/ChatViewModel.kt"
        )
        assertTrue(source.contains("val viewCount: Long"))
        assertTrue(source.contains("val danmakuCount: Long"))
        assertTrue(source.contains("danmakuCount = viewInfo.stat.danmaku.toLong()"))
    }

    @Test
    fun replyMeTopBarStaysTransparentForWallpaper() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/message/feed/ReplyMeScreen.kt"
        )
        assertTrue(source.contains("containerColor = androidx.compose.ui.graphics.Color.Transparent"))
        assertTrue(source.contains("ReplyMeCard("))
        assertTrue(source.contains("MessageFeedCard("))
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        return listOf(File(path), File(normalizedPath)).firstOrNull(File::exists)?.readText()
            ?: error("Cannot locate $path from ${File(".").absolutePath}")
    }
}
