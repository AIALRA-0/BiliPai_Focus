package com.android.purebilibili.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PersonAddAlt1
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ThumbUpOffAlt
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.DynamicFeed
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WatchLater
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 统一语义图标映射测试：MIUIX 与 MD3 共用同一套图标，无参数直接返回单一映射，
 * 不再按主题分发，也不再返回 Cupertino 图标。
 */
class AppIconsPresetPolicyTest {

    @Test
    fun `chrome icons map to unified vectors`() {
        assertEquals(Icons.AutoMirrored.Filled.ArrowBack, resolveAppBackIcon())
        assertEquals(Icons.Filled.Search, resolveAppSearchIcon())
        assertEquals(Icons.Filled.Clear, resolveAppClearIcon())
        assertEquals(Icons.AutoMirrored.Outlined.KeyboardArrowRight, resolveAppChevronForwardIcon())
        assertEquals(Icons.Outlined.KeyboardArrowDown, resolveAppChevronDownIcon())
        assertEquals(Icons.Outlined.KeyboardArrowUp, resolveAppChevronUpIcon())
    }

    @Test
    fun `service and panel icons map to unified vectors`() {
        assertEquals(Icons.Outlined.History, resolveAppHistoryIcon())
        assertEquals(Icons.Outlined.BookmarkBorder, resolveAppBookmarkIcon())
        assertEquals(Icons.Outlined.MailOutline, resolveAppInboxIcon())
        assertEquals(Icons.Filled.Search, resolveAppEmptyIcon())
        assertEquals(Icons.Outlined.LiveTv, resolveAppTvIcon())
        assertEquals(Icons.AutoMirrored.Outlined.ExitToApp, resolveAppLogoutIcon())
        assertEquals(Icons.Outlined.Timer, resolveAppTimerIcon())
        assertEquals(Icons.Outlined.MusicNote, resolveAppMusicIcon())
        assertEquals(Icons.Outlined.SwapHoriz, resolveAppFlipHorizontalIcon())
        assertEquals(Icons.Outlined.SwapVert, resolveAppFlipVerticalIcon())
        assertEquals(Icons.Outlined.Headphones, resolveAppHeadphonesIcon())
        assertEquals(Icons.Outlined.PlayCircleOutline, resolveAppQualityIcon())
        assertEquals(Icons.Outlined.Memory, resolveAppCodecIcon())
        assertEquals(Icons.Outlined.Speed, resolveAppSpeedIcon())
        assertEquals(Icons.Outlined.TouchApp, resolveAppGestureTapIcon())
        assertEquals(Icons.Outlined.Wifi, resolveAppWifiIcon())
        assertEquals(Icons.Outlined.PersonAddAlt1, resolveAppProfileAddIcon())
        assertEquals(Icons.Outlined.Lock, resolveAppLockIcon())
        assertEquals(Icons.Outlined.BarChart, resolveAppAnalyticsIcon())
        assertEquals(Icons.Outlined.Info, resolveAppInfoIcon())
        assertEquals(Icons.Outlined.NotificationsNone, resolveAppNotificationIcon())
        assertEquals(Icons.Outlined.AutoAwesome, resolveAppSparklesIcon())
        assertEquals(Icons.Outlined.WatchLater, resolveAppWatchLaterIcon())
        assertEquals(AppIcons.BiliCoin, resolveAppCoinIcon())
    }

    @Test
    fun `navigation and interaction icons map to unified vectors`() {
        assertEquals(Icons.Outlined.Home, resolveAppHomeIcon())
        assertEquals(Icons.Outlined.DynamicFeed, resolveAppDynamicIcon())
        assertEquals(Icons.Outlined.PlayArrow, resolveAppPlayIcon())
        assertEquals(Icons.Outlined.FolderCopy, resolveAppCollectionIcon())
        assertEquals(Icons.AutoMirrored.Outlined.Comment, resolveAppCommentIcon())
        assertEquals(Icons.Outlined.ThumbUpOffAlt, resolveAppLikeIcon())
        assertEquals(Icons.Outlined.Share, resolveAppShareIcon())
        assertEquals(Icons.Outlined.Visibility, resolveAppVisibilityOnIcon())
        assertEquals(Icons.Outlined.VisibilityOff, resolveAppVisibilityOffIcon())
    }

    @Test
    fun `platform icon helper collapses to the material vector`() {
        assertEquals(Icons.Outlined.Home, resolvePlatformIcon(Icons.Outlined.Home))
    }
}
