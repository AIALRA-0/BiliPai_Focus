package com.android.purebilibili.feature.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import com.android.purebilibili.core.theme.LocalUiPreset
import com.android.purebilibili.core.theme.UiPreset
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*

internal enum class SettingsIconRole {
    INTERFACE_THEME,
    HOME_FEED,
    NAVIGATION,
    PLAYBACK_QUALITY,
    FULLSCREEN_GESTURE,
    INTERACTION_COMMENT,
    DATA_BACKUP,
    PRIVACY_PERMISSION,
    DIAGNOSTICS,
    ABOUT_SUPPORT,
    APPEARANCE,
    ANIMATION,
    PLAYBACK,
    BOTTOM_BAR,
    PERMISSION,
    BLOCKED_LIST,
    SETTINGS_SHARE,
    WEBDAV_BACKUP,
    DOWNLOAD_PATH,
    IMAGE_SAVE_PATH,
    CLEAR_CACHE,
    PLUGINS,
    EXPORT_LOGS,
    OPEN_SOURCE_LICENSES,
    OPEN_SOURCE_HOME,
    CHECK_UPDATE,
    VIEW_RELEASE_NOTES,
    REPLAY_ONBOARDING,
    TIPS,
    OPEN_LINKS,
    DONATE,
    DISCLAIMER,
    RELEASE_CHANNEL,
    CRASH_TRACKING,
    ANALYTICS,
    FEED_API,
    REFRESH_COUNT,
    DYNAMIC_PREVIEW_TEXT,
    DYNAMIC_TAB_VISIBILITY,
    EASTER_EGG,
    AUTO_CHECK_UPDATE,
    BUILD_SOURCE,
    BUILD_FINGERPRINT,
    BUILD_VERIFICATION,
    ANDROID_LIQUID_GLASS,
    DYNAMIC_COLOR,
    COLOR_STYLE,
    COLOR_SPEC,
    APP_LANGUAGE,
    FONT_FILE,
    SPLASH_WALLPAPER,
    RANDOM_WALLPAPER,
    DISPLAY_STYLE,
    HOME_COVER_GLASS,
    VIDEO_DURATION_BADGES,
    HOME_INFO_GLASS,
    HOME_WALLPAPER,
    WALLPAPER_EFFECT,
    HOME_UP_BADGES,
    ONLINE_COUNT,
    GRID_COLUMNS,
    HOME_CARD_WIDTH,
    CARD_ENTRANCE_ANIMATION,
    CARD_TRANSITION_ANIMATION,
    TOP_DOCK_GLASS,
    HOME_SEARCH_GLASS,
    BOTTOM_BAR_GLASS,
    TOP_BAR_BLUR,
    BOTTOM_BAR_BLUR,
    FLOATING_BOTTOM_BAR,
    HARDWARE_DECODER,
    PLAYBACK_SPEED,
    STOP_ON_EXIT,
    BACKGROUND_PLAYBACK,
    AUDIO_FOCUS,
    PIP_DANMAKU,
    AUDIO_MODE_PIP,
    PLAYER_DIAGNOSTICS,
    QUALITY_WARNING,
    SUBTITLE,
    COMMENT_DECORATION,
    AI_SUMMARY,
    VIDEO_NOTE,
    LIKE_INTERACTION,
    VIDEO_DESCRIPTION,
    FULLSCREEN_ORIENTATION,
    HORIZONTAL_ADAPTATION,
    FULLSCREEN_GESTURE_REVERSE,
    HIDE_STATUS_BAR,
    AUTO_ENTER_FULLSCREEN,
    AUTO_EXIT_FULLSCREEN,
    FULLSCREEN_LOCK,
    FULLSCREEN_SCREENSHOT,
    CLEAN_SCREENSHOT,
    BATTERY_STATUS,
    TIME_STATUS,
    PLAYER_ACTIONS,
    PRIVACY_CONTENT_AUTHENTICATION,
    PLAYER_STATS,
    PLAYER_DIAGNOSTIC_LOGS,
    QUALITY_WARNING_ONCE,
    DIRECTED_TRAFFIC,
    AUTO_HIGHEST_QUALITY,
    AUTO_PLAY_ON_OPEN,
    AUTO_PLAY_NEXT,
    VIDEO_NOTE_COLLAPSE,
    INTERACTIVE_COMMANDS,
    PORTRAIT_SWIPE_FULLSCREEN,
    CENTER_SWIPE_FULLSCREEN,
    SYSTEM_BRIGHTNESS,
    APP_ICON,
    HOME_CARD_STATS_COMPACT,
    BOTTOM_BAR_GLASS_PREVIEW
}

@Composable
internal fun rememberSettingsSemanticIcon(
    role: SettingsIconRole,
    uiPreset: UiPreset = LocalUiPreset.current
): ImageVector = remember(role, uiPreset) {
    resolveSettingsSemanticIcon(role, uiPreset)
}

internal fun resolveSettingsSearchTargetIconRole(
    target: SettingsSearchTarget
): SettingsIconRole = when (target) {
    SettingsSearchTarget.INTERFACE_THEME -> SettingsIconRole.INTERFACE_THEME
    SettingsSearchTarget.HOME_FEED -> SettingsIconRole.HOME_FEED
    SettingsSearchTarget.NAVIGATION -> SettingsIconRole.NAVIGATION
    SettingsSearchTarget.PLAYBACK_QUALITY -> SettingsIconRole.PLAYBACK_QUALITY
    SettingsSearchTarget.FULLSCREEN_GESTURE -> SettingsIconRole.FULLSCREEN_GESTURE
    SettingsSearchTarget.INTERACTION_COMMENT -> SettingsIconRole.INTERACTION_COMMENT
    SettingsSearchTarget.DATA_BACKUP -> SettingsIconRole.DATA_BACKUP
    SettingsSearchTarget.PRIVACY_PERMISSION -> SettingsIconRole.PRIVACY_PERMISSION
    SettingsSearchTarget.DIAGNOSTICS -> SettingsIconRole.DIAGNOSTICS
    SettingsSearchTarget.ABOUT_SUPPORT -> SettingsIconRole.ABOUT_SUPPORT
    SettingsSearchTarget.APPEARANCE -> SettingsIconRole.APPEARANCE
    SettingsSearchTarget.ANIMATION -> SettingsIconRole.ANIMATION
    SettingsSearchTarget.PLAYBACK -> SettingsIconRole.PLAYBACK
    SettingsSearchTarget.BOTTOM_BAR -> SettingsIconRole.BOTTOM_BAR
    SettingsSearchTarget.PERMISSION -> SettingsIconRole.PERMISSION
    SettingsSearchTarget.BLOCKED_LIST -> SettingsIconRole.BLOCKED_LIST
    SettingsSearchTarget.SETTINGS_SHARE -> SettingsIconRole.SETTINGS_SHARE
    SettingsSearchTarget.WEBDAV_BACKUP -> SettingsIconRole.WEBDAV_BACKUP
    SettingsSearchTarget.DOWNLOAD_PATH -> SettingsIconRole.DOWNLOAD_PATH
    SettingsSearchTarget.IMAGE_SAVE_PATH -> SettingsIconRole.IMAGE_SAVE_PATH
    SettingsSearchTarget.CLEAR_CACHE -> SettingsIconRole.CLEAR_CACHE
    SettingsSearchTarget.PLUGINS -> SettingsIconRole.PLUGINS
    SettingsSearchTarget.EXPORT_LOGS -> SettingsIconRole.EXPORT_LOGS
    SettingsSearchTarget.OPEN_SOURCE_LICENSES -> SettingsIconRole.OPEN_SOURCE_LICENSES
    SettingsSearchTarget.OPEN_SOURCE_HOME -> SettingsIconRole.OPEN_SOURCE_HOME
    SettingsSearchTarget.CHECK_UPDATE -> SettingsIconRole.CHECK_UPDATE
    SettingsSearchTarget.VIEW_RELEASE_NOTES -> SettingsIconRole.VIEW_RELEASE_NOTES
    SettingsSearchTarget.REPLAY_ONBOARDING -> SettingsIconRole.REPLAY_ONBOARDING
    SettingsSearchTarget.TIPS -> SettingsIconRole.TIPS
    SettingsSearchTarget.OPEN_LINKS -> SettingsIconRole.OPEN_LINKS
    SettingsSearchTarget.DONATE -> SettingsIconRole.DONATE
    SettingsSearchTarget.TELEGRAM -> SettingsIconRole.OPEN_LINKS
    SettingsSearchTarget.TWITTER -> SettingsIconRole.OPEN_LINKS
    SettingsSearchTarget.DISCLAIMER -> SettingsIconRole.DISCLAIMER
}

internal fun resolveSettingsSemanticIcon(
    role: SettingsIconRole,
    uiPreset: UiPreset = UiPreset.IOS
): ImageVector = if (uiPreset == UiPreset.MD3) {
    resolveMd3SettingsSemanticIcon(role)
} else {
    resolveIosSettingsSemanticIcon(role)
}

private fun resolveMd3SettingsSemanticIcon(role: SettingsIconRole): ImageVector = when (role) {
    SettingsIconRole.INTERFACE_THEME -> Icons.Outlined.ColorLens
    SettingsIconRole.HOME_FEED -> Icons.Outlined.Home
    SettingsIconRole.NAVIGATION -> Icons.Outlined.Dashboard
    SettingsIconRole.PLAYBACK_QUALITY -> Icons.Outlined.HighQuality
    SettingsIconRole.FULLSCREEN_GESTURE -> Icons.Outlined.TouchApp
    SettingsIconRole.INTERACTION_COMMENT -> Icons.Outlined.ChatBubbleOutline
    SettingsIconRole.DATA_BACKUP -> Icons.Outlined.Backup
    SettingsIconRole.PRIVACY_PERMISSION -> Icons.Outlined.Lock
    SettingsIconRole.DIAGNOSTICS -> Icons.Outlined.Terminal
    SettingsIconRole.ABOUT_SUPPORT -> Icons.Outlined.Info
    SettingsIconRole.APPEARANCE -> Icons.Outlined.Palette
    SettingsIconRole.ANIMATION -> Icons.Outlined.Animation
    SettingsIconRole.PLAYBACK -> Icons.Outlined.PlayCircle
    SettingsIconRole.BOTTOM_BAR -> Icons.Outlined.Widgets
    SettingsIconRole.PERMISSION -> Icons.Outlined.Security
    SettingsIconRole.BLOCKED_LIST -> Icons.Outlined.Block
    SettingsIconRole.SETTINGS_SHARE -> Icons.Outlined.Share
    SettingsIconRole.WEBDAV_BACKUP -> Icons.Outlined.CloudUpload
    SettingsIconRole.DOWNLOAD_PATH -> Icons.Outlined.Folder
    SettingsIconRole.IMAGE_SAVE_PATH -> Icons.Outlined.Photo
    SettingsIconRole.CLEAR_CACHE -> Icons.Outlined.DeleteOutline
    SettingsIconRole.PLUGINS -> Icons.Outlined.Extension
    SettingsIconRole.EXPORT_LOGS -> Icons.AutoMirrored.Outlined.Article
    SettingsIconRole.OPEN_SOURCE_LICENSES -> Icons.Outlined.Gavel
    SettingsIconRole.OPEN_SOURCE_HOME -> Icons.AutoMirrored.Outlined.OpenInNew
    SettingsIconRole.CHECK_UPDATE -> Icons.Outlined.SystemUpdate
    SettingsIconRole.VIEW_RELEASE_NOTES -> Icons.Outlined.Newspaper
    SettingsIconRole.REPLAY_ONBOARDING -> Icons.Outlined.Replay
    SettingsIconRole.TIPS -> Icons.Outlined.Lightbulb
    SettingsIconRole.OPEN_LINKS -> Icons.Outlined.Link
    SettingsIconRole.DONATE -> Icons.Outlined.CardGiftcard
    SettingsIconRole.DISCLAIMER -> Icons.Outlined.WarningAmber
    SettingsIconRole.RELEASE_CHANNEL -> Icons.Outlined.NotificationsActive
    SettingsIconRole.CRASH_TRACKING -> Icons.Outlined.BugReport
    SettingsIconRole.ANALYTICS -> Icons.Outlined.Analytics
    SettingsIconRole.FEED_API -> Icons.Outlined.RssFeed
    SettingsIconRole.REFRESH_COUNT -> Icons.Outlined.Refresh
    SettingsIconRole.DYNAMIC_PREVIEW_TEXT -> Icons.AutoMirrored.Outlined.TextSnippet
    SettingsIconRole.DYNAMIC_TAB_VISIBILITY -> Icons.Outlined.Visibility
    SettingsIconRole.EASTER_EGG -> Icons.Outlined.AutoAwesome
    SettingsIconRole.AUTO_CHECK_UPDATE -> Icons.Outlined.Update
    SettingsIconRole.BUILD_SOURCE -> Icons.Outlined.Tag
    SettingsIconRole.BUILD_FINGERPRINT -> Icons.Outlined.Fingerprint
    SettingsIconRole.BUILD_VERIFICATION -> Icons.Outlined.VerifiedUser
    SettingsIconRole.ANDROID_LIQUID_GLASS -> Icons.Outlined.WaterDrop
    SettingsIconRole.DYNAMIC_COLOR -> Icons.Outlined.FormatColorText
    SettingsIconRole.COLOR_STYLE -> Icons.Outlined.Brush
    SettingsIconRole.COLOR_SPEC -> Icons.Outlined.AutoFixHigh
    SettingsIconRole.APP_LANGUAGE -> Icons.Outlined.Language
    SettingsIconRole.FONT_FILE -> Icons.Outlined.FontDownload
    SettingsIconRole.SPLASH_WALLPAPER -> Icons.Outlined.Wallpaper
    SettingsIconRole.RANDOM_WALLPAPER -> Icons.Outlined.Shuffle
    SettingsIconRole.DISPLAY_STYLE -> Icons.Outlined.ViewCarousel
    SettingsIconRole.HOME_COVER_GLASS -> Icons.Outlined.Opacity
    SettingsIconRole.VIDEO_DURATION_BADGES -> Icons.Outlined.Timer
    SettingsIconRole.HOME_INFO_GLASS -> Icons.Outlined.Badge
    SettingsIconRole.HOME_WALLPAPER -> Icons.Outlined.Image
    SettingsIconRole.WALLPAPER_EFFECT -> Icons.Outlined.BlurOn
    SettingsIconRole.HOME_UP_BADGES -> Icons.Outlined.PersonAdd
    SettingsIconRole.ONLINE_COUNT -> Icons.Outlined.OnlinePrediction
    SettingsIconRole.GRID_COLUMNS -> Icons.Outlined.GridView
    SettingsIconRole.HOME_CARD_WIDTH -> Icons.Outlined.WidthNormal
    SettingsIconRole.CARD_ENTRANCE_ANIMATION -> Icons.Outlined.AutoAwesomeMotion
    SettingsIconRole.CARD_TRANSITION_ANIMATION -> Icons.Outlined.SyncAlt
    SettingsIconRole.TOP_DOCK_GLASS -> Icons.Outlined.Layers
    SettingsIconRole.HOME_SEARCH_GLASS -> Icons.AutoMirrored.Outlined.ManageSearch
    SettingsIconRole.BOTTOM_BAR_GLASS -> Icons.Outlined.BlurCircular
    SettingsIconRole.TOP_BAR_BLUR -> Icons.Outlined.ViewHeadline
    SettingsIconRole.BOTTOM_BAR_BLUR -> Icons.Outlined.BlurLinear
    SettingsIconRole.FLOATING_BOTTOM_BAR -> Icons.Outlined.ViewAgenda
    SettingsIconRole.HARDWARE_DECODER -> Icons.Outlined.Memory
    SettingsIconRole.PLAYBACK_SPEED -> Icons.Outlined.Speed
    SettingsIconRole.STOP_ON_EXIT -> Icons.Outlined.StopCircle
    SettingsIconRole.BACKGROUND_PLAYBACK -> Icons.Outlined.MusicNote
    SettingsIconRole.AUDIO_FOCUS -> Icons.Outlined.Headphones
    SettingsIconRole.PIP_DANMAKU -> Icons.Outlined.Textsms
    SettingsIconRole.AUDIO_MODE_PIP -> Icons.Outlined.PictureInPicture
    SettingsIconRole.PLAYER_DIAGNOSTICS -> Icons.Outlined.QueryStats
    SettingsIconRole.QUALITY_WARNING -> Icons.Outlined.ReportProblem
    SettingsIconRole.SUBTITLE -> Icons.Outlined.Subtitles
    SettingsIconRole.COMMENT_DECORATION -> Icons.Outlined.ModeComment
    SettingsIconRole.AI_SUMMARY -> Icons.Outlined.SmartToy
    SettingsIconRole.VIDEO_NOTE -> Icons.AutoMirrored.Outlined.Notes
    SettingsIconRole.LIKE_INTERACTION -> Icons.Outlined.ThumbUpOffAlt
    SettingsIconRole.VIDEO_DESCRIPTION -> Icons.AutoMirrored.Outlined.Subject
    SettingsIconRole.FULLSCREEN_ORIENTATION -> Icons.Outlined.ScreenRotation
    SettingsIconRole.HORIZONTAL_ADAPTATION -> Icons.Outlined.AspectRatio
    SettingsIconRole.FULLSCREEN_GESTURE_REVERSE -> Icons.Outlined.SwipeVertical
    SettingsIconRole.HIDE_STATUS_BAR -> Icons.Outlined.VisibilityOff
    SettingsIconRole.AUTO_ENTER_FULLSCREEN -> Icons.Outlined.Fullscreen
    SettingsIconRole.AUTO_EXIT_FULLSCREEN -> Icons.Outlined.FullscreenExit
    SettingsIconRole.FULLSCREEN_LOCK -> Icons.Outlined.ScreenLockRotation
    SettingsIconRole.FULLSCREEN_SCREENSHOT -> Icons.Outlined.Screenshot
    SettingsIconRole.CLEAN_SCREENSHOT -> Icons.Outlined.CameraAlt
    SettingsIconRole.BATTERY_STATUS -> Icons.Outlined.BatteryFull
    SettingsIconRole.TIME_STATUS -> Icons.Outlined.AccessTime
    SettingsIconRole.PLAYER_ACTIONS -> Icons.Outlined.IosShare
    SettingsIconRole.PRIVACY_CONTENT_AUTHENTICATION -> Icons.Outlined.Verified
    SettingsIconRole.PLAYER_STATS -> Icons.Outlined.InsertChartOutlined
    SettingsIconRole.PLAYER_DIAGNOSTIC_LOGS -> Icons.Outlined.ReportGmailerrorred
    SettingsIconRole.QUALITY_WARNING_ONCE -> Icons.Outlined.NotificationImportant
    SettingsIconRole.DIRECTED_TRAFFIC -> Icons.Outlined.NetworkLocked
    SettingsIconRole.AUTO_HIGHEST_QUALITY -> Icons.Outlined.SettingsSuggest
    SettingsIconRole.AUTO_PLAY_ON_OPEN -> Icons.Outlined.PlayArrow
    SettingsIconRole.AUTO_PLAY_NEXT -> Icons.AutoMirrored.Outlined.PlaylistPlay
    SettingsIconRole.VIDEO_NOTE_COLLAPSE -> Icons.AutoMirrored.Outlined.ShortText
    SettingsIconRole.INTERACTIVE_COMMANDS -> Icons.Outlined.CommentsDisabled
    SettingsIconRole.PORTRAIT_SWIPE_FULLSCREEN -> Icons.Outlined.SwipeUp
    SettingsIconRole.CENTER_SWIPE_FULLSCREEN -> Icons.Outlined.Swipe
    SettingsIconRole.SYSTEM_BRIGHTNESS -> Icons.Outlined.BrightnessMedium
    SettingsIconRole.APP_ICON -> Icons.Outlined.AppSettingsAlt
    SettingsIconRole.HOME_CARD_STATS_COMPACT -> Icons.Outlined.StackedBarChart
    SettingsIconRole.BOTTOM_BAR_GLASS_PREVIEW -> Icons.Outlined.LensBlur
}

private fun resolveIosSettingsSemanticIcon(role: SettingsIconRole): ImageVector = when (role) {
    SettingsIconRole.INTERFACE_THEME -> CupertinoIcons.Outlined.Sparkles
    SettingsIconRole.HOME_FEED -> CupertinoIcons.Outlined.House
    SettingsIconRole.NAVIGATION -> CupertinoIcons.Outlined.RectangleStack
    SettingsIconRole.PLAYBACK_QUALITY -> CupertinoIcons.Outlined.VideoCircle
    SettingsIconRole.FULLSCREEN_GESTURE -> CupertinoIcons.Outlined.HandTap
    SettingsIconRole.INTERACTION_COMMENT -> CupertinoIcons.Outlined.TextBubble
    SettingsIconRole.DATA_BACKUP -> CupertinoIcons.Outlined.DocOnDoc
    SettingsIconRole.PRIVACY_PERMISSION -> CupertinoIcons.Outlined.EyeSlash
    SettingsIconRole.DIAGNOSTICS -> CupertinoIcons.Outlined.Terminal
    SettingsIconRole.ABOUT_SUPPORT -> CupertinoIcons.Outlined.InfoCircle
    SettingsIconRole.APPEARANCE -> CupertinoIcons.Outlined.Paintpalette
    SettingsIconRole.ANIMATION -> CupertinoIcons.Outlined.WandAndStars
    SettingsIconRole.PLAYBACK -> CupertinoIcons.Outlined.PlayCircle
    SettingsIconRole.BOTTOM_BAR -> CupertinoIcons.Outlined.SquareStack3dUp
    SettingsIconRole.PERMISSION -> CupertinoIcons.Outlined.Shield
    SettingsIconRole.BLOCKED_LIST -> CupertinoIcons.Outlined.XmarkCircle
    SettingsIconRole.SETTINGS_SHARE -> CupertinoIcons.Outlined.SquareOnSquare
    SettingsIconRole.WEBDAV_BACKUP -> CupertinoIcons.Outlined.IcloudAndArrowUp
    SettingsIconRole.DOWNLOAD_PATH -> CupertinoIcons.Outlined.Folder
    SettingsIconRole.IMAGE_SAVE_PATH -> CupertinoIcons.Outlined.Photo
    SettingsIconRole.CLEAR_CACHE -> CupertinoIcons.Outlined.Trash
    SettingsIconRole.PLUGINS -> CupertinoIcons.Outlined.PuzzlepieceExtension
    SettingsIconRole.EXPORT_LOGS -> CupertinoIcons.Outlined.DocPlaintext
    SettingsIconRole.OPEN_SOURCE_LICENSES -> CupertinoIcons.Outlined.Book
    SettingsIconRole.OPEN_SOURCE_HOME -> CupertinoIcons.Outlined.Safari
    SettingsIconRole.CHECK_UPDATE -> CupertinoIcons.Outlined.ArrowTriangle2Circlepath
    SettingsIconRole.VIEW_RELEASE_NOTES -> CupertinoIcons.Outlined.Newspaper
    SettingsIconRole.REPLAY_ONBOARDING -> CupertinoIcons.Outlined.ArrowCounterclockwise
    SettingsIconRole.TIPS -> CupertinoIcons.Outlined.Lightbulb
    SettingsIconRole.OPEN_LINKS -> CupertinoIcons.Outlined.Link
    SettingsIconRole.DONATE -> CupertinoIcons.Outlined.Gift
    SettingsIconRole.DISCLAIMER -> CupertinoIcons.Outlined.ExclamationmarkTriangle
    SettingsIconRole.RELEASE_CHANNEL -> CupertinoIcons.Outlined.Megaphone
    SettingsIconRole.CRASH_TRACKING -> CupertinoIcons.Outlined.XmarkSeal
    SettingsIconRole.ANALYTICS -> CupertinoIcons.Outlined.ChartBar
    SettingsIconRole.FEED_API -> CupertinoIcons.Outlined.Network
    SettingsIconRole.REFRESH_COUNT -> CupertinoIcons.Outlined.ArrowClockwise
    SettingsIconRole.DYNAMIC_PREVIEW_TEXT -> CupertinoIcons.Outlined.TextMagnifyingglass
    SettingsIconRole.DYNAMIC_TAB_VISIBILITY -> CupertinoIcons.Outlined.Eye
    SettingsIconRole.EASTER_EGG -> CupertinoIcons.Outlined.PartyPopper
    SettingsIconRole.AUTO_CHECK_UPDATE -> CupertinoIcons.Outlined.BellBadge
    SettingsIconRole.BUILD_SOURCE -> CupertinoIcons.Outlined.Tag
    SettingsIconRole.BUILD_FINGERPRINT -> CupertinoIcons.Outlined.Touchid
    SettingsIconRole.BUILD_VERIFICATION -> CupertinoIcons.Outlined.CheckmarkSeal
    SettingsIconRole.ANDROID_LIQUID_GLASS -> CupertinoIcons.Outlined.Drop
    SettingsIconRole.DYNAMIC_COLOR -> CupertinoIcons.Outlined.Eyedropper
    SettingsIconRole.COLOR_STYLE -> CupertinoIcons.Outlined.PaintbrushPointed
    SettingsIconRole.COLOR_SPEC -> CupertinoIcons.Outlined.SliderHorizontal3
    SettingsIconRole.APP_LANGUAGE -> CupertinoIcons.Outlined.GlobeDesk
    SettingsIconRole.FONT_FILE -> CupertinoIcons.Outlined.Highlighter
    SettingsIconRole.SPLASH_WALLPAPER -> CupertinoIcons.Outlined.PhotoTv
    SettingsIconRole.RANDOM_WALLPAPER -> CupertinoIcons.Outlined.Shuffle
    SettingsIconRole.DISPLAY_STYLE -> CupertinoIcons.Outlined.Grid
    SettingsIconRole.HOME_COVER_GLASS -> CupertinoIcons.Outlined.Square3Layers3dDownLeft
    SettingsIconRole.VIDEO_DURATION_BADGES -> CupertinoIcons.Outlined.Timer
    SettingsIconRole.HOME_INFO_GLASS -> CupertinoIcons.Outlined.Lanyardcard
    SettingsIconRole.HOME_WALLPAPER -> CupertinoIcons.Outlined.PhotoStack
    SettingsIconRole.WALLPAPER_EFFECT -> CupertinoIcons.Outlined.CameraFilters
    SettingsIconRole.HOME_UP_BADGES -> CupertinoIcons.Outlined.PersonTextRectangle
    SettingsIconRole.ONLINE_COUNT -> CupertinoIcons.Outlined.DotRadiowavesUpForward
    SettingsIconRole.GRID_COLUMNS -> CupertinoIcons.Outlined.ListBulletCircle
    SettingsIconRole.HOME_CARD_WIDTH -> CupertinoIcons.Outlined.Ruler
    SettingsIconRole.CARD_ENTRANCE_ANIMATION -> CupertinoIcons.Outlined.WandAndStarsInverse
    SettingsIconRole.CARD_TRANSITION_ANIMATION -> CupertinoIcons.Outlined.ArrowLeftAndRight
    SettingsIconRole.TOP_DOCK_GLASS -> CupertinoIcons.Outlined.MenubarRectangle
    SettingsIconRole.HOME_SEARCH_GLASS -> CupertinoIcons.Outlined.PlusMagnifyingglass
    SettingsIconRole.BOTTOM_BAR_GLASS -> CupertinoIcons.Outlined.Square3Layers3dDownRight
    SettingsIconRole.TOP_BAR_BLUR -> CupertinoIcons.Outlined.SquareSplit2x1
    SettingsIconRole.BOTTOM_BAR_BLUR -> CupertinoIcons.Outlined.SquareStack
    SettingsIconRole.FLOATING_BOTTOM_BAR -> CupertinoIcons.Outlined.RectangleConnectedToLineBelow
    SettingsIconRole.HARDWARE_DECODER -> CupertinoIcons.Outlined.Cpu
    SettingsIconRole.PLAYBACK_SPEED -> CupertinoIcons.Outlined.Speedometer
    SettingsIconRole.STOP_ON_EXIT -> CupertinoIcons.Outlined.StopCircle
    SettingsIconRole.BACKGROUND_PLAYBACK -> CupertinoIcons.Outlined.Headphones
    SettingsIconRole.AUDIO_FOCUS -> CupertinoIcons.Outlined.SpeakerWave2
    SettingsIconRole.PIP_DANMAKU -> CupertinoIcons.Outlined.Pip
    SettingsIconRole.AUDIO_MODE_PIP -> CupertinoIcons.Outlined.PipEnter
    SettingsIconRole.PLAYER_DIAGNOSTICS -> CupertinoIcons.Outlined.WaveformAndMagnifyingglass
    SettingsIconRole.QUALITY_WARNING -> CupertinoIcons.Outlined.ExclamationmarkArrowTriangle2Circlepath
    SettingsIconRole.SUBTITLE -> CupertinoIcons.Outlined.Character
    SettingsIconRole.COMMENT_DECORATION -> CupertinoIcons.Outlined.BubbleLeft
    SettingsIconRole.AI_SUMMARY -> CupertinoIcons.Outlined.Brain
    SettingsIconRole.VIDEO_NOTE -> CupertinoIcons.Outlined.NoteText
    SettingsIconRole.LIKE_INTERACTION -> CupertinoIcons.Outlined.HandPointUp
    SettingsIconRole.VIDEO_DESCRIPTION -> CupertinoIcons.Outlined.DocTextMagnifyingglass
    SettingsIconRole.FULLSCREEN_ORIENTATION -> CupertinoIcons.Outlined.RotateRight
    SettingsIconRole.HORIZONTAL_ADAPTATION -> CupertinoIcons.Outlined.IpadAndIphone
    SettingsIconRole.FULLSCREEN_GESTURE_REVERSE -> CupertinoIcons.Outlined.ArrowUpArrowDown
    SettingsIconRole.HIDE_STATUS_BAR -> CupertinoIcons.Outlined.Menucard
    SettingsIconRole.AUTO_ENTER_FULLSCREEN -> CupertinoIcons.Outlined.RectanglePortraitArrowtriangle2Outward
    SettingsIconRole.AUTO_EXIT_FULLSCREEN -> CupertinoIcons.Outlined.ArrowDownRightAndArrowUpLeft
    SettingsIconRole.FULLSCREEN_LOCK -> CupertinoIcons.Outlined.LockCircle
    SettingsIconRole.FULLSCREEN_SCREENSHOT -> CupertinoIcons.Outlined.Camera
    SettingsIconRole.CLEAN_SCREENSHOT -> CupertinoIcons.Outlined.CameraViewfinder
    SettingsIconRole.BATTERY_STATUS -> CupertinoIcons.Outlined.Battery100
    SettingsIconRole.TIME_STATUS -> CupertinoIcons.Outlined.Clock
    SettingsIconRole.PLAYER_ACTIONS -> CupertinoIcons.Outlined.SquareAndArrowUp
    SettingsIconRole.PRIVACY_CONTENT_AUTHENTICATION -> CupertinoIcons.Outlined.Faceid
    SettingsIconRole.PLAYER_STATS -> CupertinoIcons.Outlined.ChartLineUptrendXyaxis
    SettingsIconRole.PLAYER_DIAGNOSTIC_LOGS -> CupertinoIcons.Outlined.Recordingtape
    SettingsIconRole.QUALITY_WARNING_ONCE -> CupertinoIcons.Outlined.ExclamationmarkCircle
    SettingsIconRole.DIRECTED_TRAFFIC -> CupertinoIcons.Outlined.AntennaRadiowavesLeftAndRight
    SettingsIconRole.AUTO_HIGHEST_QUALITY -> CupertinoIcons.Outlined.Tv
    SettingsIconRole.AUTO_PLAY_ON_OPEN -> CupertinoIcons.Outlined.IphoneBadgePlay
    SettingsIconRole.AUTO_PLAY_NEXT -> CupertinoIcons.Outlined.ForwardEnd
    SettingsIconRole.VIDEO_NOTE_COLLAPSE -> CupertinoIcons.Outlined.ListBulletIndent
    SettingsIconRole.INTERACTIVE_COMMANDS -> CupertinoIcons.Outlined.BellSlash
    SettingsIconRole.PORTRAIT_SWIPE_FULLSCREEN -> CupertinoIcons.Outlined.ArrowUpAndDown
    SettingsIconRole.CENTER_SWIPE_FULLSCREEN -> CupertinoIcons.Outlined.HandDraw
    SettingsIconRole.SYSTEM_BRIGHTNESS -> CupertinoIcons.Outlined.LightMax
    SettingsIconRole.APP_ICON -> CupertinoIcons.Outlined.MacwindowBadgePlus
    SettingsIconRole.HOME_CARD_STATS_COMPACT -> CupertinoIcons.Outlined.ChartLineDowntrendXyaxis
    SettingsIconRole.BOTTOM_BAR_GLASS_PREVIEW -> CupertinoIcons.Outlined.CheckerboardShield
}
