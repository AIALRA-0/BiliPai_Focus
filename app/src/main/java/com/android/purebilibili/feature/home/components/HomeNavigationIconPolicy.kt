package com.android.purebilibili.feature.home.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.android.purebilibili.R
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Community
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.ContactsCircle
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.Folder
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Music
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.icon.extended.Recent
import top.yukonga.miuix.kmp.icon.extended.Recording
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Stopwatch
import top.yukonga.miuix.kmp.icon.extended.Store
import top.yukonga.miuix.kmp.icon.extended.Theme
import top.yukonga.miuix.kmp.icon.extended.TopDownloads

private enum class HomeNavigationIconRole {
    HOME,
    DYNAMIC,
    STORY,
    HISTORY,
    LISTEN_VIDEO,
    PROFILE,
    FAVORITE,
    LIVE,
    WATCH_LATER,
    SETTINGS,
    PLUGINS,
    FOLLOW,
    POPULAR,
    ANIME,
    GAME,
    PARTITION,
    KNOWLEDGE,
    TECH,
    SUBSCRIPTIONS,
}

internal enum class HomeNavigationIconSource {
    MIUIX,
}

private fun resolveHomeNavigationIconRole(tabId: String): HomeNavigationIconRole = when (tabId.trim().uppercase()) {
    "HOME", "RECOMMEND" -> HomeNavigationIconRole.HOME
    "DYNAMIC" -> HomeNavigationIconRole.DYNAMIC
    "STORY" -> HomeNavigationIconRole.STORY
    "HISTORY" -> HomeNavigationIconRole.HISTORY
    "LISTEN_VIDEO" -> HomeNavigationIconRole.LISTEN_VIDEO
    "PROFILE" -> HomeNavigationIconRole.PROFILE
    "FAVORITE" -> HomeNavigationIconRole.FAVORITE
    "LIVE" -> HomeNavigationIconRole.LIVE
    "WATCHLATER", "WATCH_LATER" -> HomeNavigationIconRole.WATCH_LATER
    "SETTINGS" -> HomeNavigationIconRole.SETTINGS
    "PLUGINS" -> HomeNavigationIconRole.PLUGINS
    "FOLLOW" -> HomeNavigationIconRole.FOLLOW
    "POPULAR" -> HomeNavigationIconRole.POPULAR
    "ANIME" -> HomeNavigationIconRole.ANIME
    "GAME" -> HomeNavigationIconRole.GAME
    "PARTITION" -> HomeNavigationIconRole.PARTITION
    "KNOWLEDGE" -> HomeNavigationIconRole.KNOWLEDGE
    "TECH" -> HomeNavigationIconRole.TECH
    "SUBSCRIPTIONS" -> HomeNavigationIconRole.SUBSCRIPTIONS
    else -> HomeNavigationIconRole.HOME
}

internal fun resolveMiuixPreferredHomeNavigationIconSource(
    tabId: String,
): HomeNavigationIconSource {
    resolveHomeNavigationIconRole(tabId)
    return HomeNavigationIconSource.MIUIX
}

/**
 * 首页底栏、侧栏和顶部分区的唯一图标入口。
 */
@Composable
internal fun resolveMiuixPreferredHomeNavigationIcon(
    tabId: String,
    selected: Boolean = false,
): ImageVector {
    val role = resolveHomeNavigationIconRole(tabId)
    return resolveMiuixHomeNavigationIcon(role, selected)
}

/**
 * Miuix Home and Recent remain visually solid even at Light weight. Their idle layer uses the
 * thin local outline while the selected layer uses a filled glyph, allowing the moving indicator
 * to crossfade real interior fill without leaving an idle black solid icon behind.
 */
@Composable
internal fun resolveMiuixBottomNavigationIcon(
    item: BottomNavItem,
    selected: Boolean,
): ImageVector = resolveMiuixPreferredHomeNavigationIcon(item.name, selected)

@Composable
private fun resolveMiuixHomeNavigationIcon(
    role: HomeNavigationIconRole,
    selected: Boolean,
): ImageVector {
    if (selected) {
        val filledResource = when (role) {
            HomeNavigationIconRole.HOME -> R.drawable.ms_home_fill_24
            HomeNavigationIconRole.HISTORY -> R.drawable.ms_history_fill_24
            HomeNavigationIconRole.SUBSCRIPTIONS -> R.drawable.ms_rss_feed_24
            else -> null
        }
        if (filledResource != null) return ImageVector.vectorResource(filledResource)
        // Keep the same Miuix silhouette under the moving tint mask. Unrelated Material
        // filled glyphs (for example a bell for Community) do not align with the idle icon.
    }
    return when (role) {
        HomeNavigationIconRole.HOME -> ImageVector.vectorResource(R.drawable.bp_nav_home_outline_24)
        HomeNavigationIconRole.DYNAMIC -> MiuixIcons.Light.Community
        HomeNavigationIconRole.STORY -> MiuixIcons.Light.Play
        HomeNavigationIconRole.HISTORY -> ImageVector.vectorResource(R.drawable.bp_nav_history_outline_24)
        HomeNavigationIconRole.LISTEN_VIDEO -> MiuixIcons.Light.Music
        HomeNavigationIconRole.PROFILE -> MiuixIcons.Light.ContactsCircle
        HomeNavigationIconRole.FAVORITE -> MiuixIcons.Light.Favorites
        HomeNavigationIconRole.LIVE -> MiuixIcons.Light.Recording
        HomeNavigationIconRole.WATCH_LATER -> MiuixIcons.Light.Stopwatch
        HomeNavigationIconRole.SETTINGS -> MiuixIcons.Light.Settings
        HomeNavigationIconRole.PLUGINS -> MiuixIcons.Light.Folder
        HomeNavigationIconRole.FOLLOW -> MiuixIcons.Light.Contacts
        HomeNavigationIconRole.POPULAR -> MiuixIcons.Light.TopDownloads
        HomeNavigationIconRole.ANIME -> MiuixIcons.Light.Play
        HomeNavigationIconRole.GAME -> MiuixIcons.Light.Store
        HomeNavigationIconRole.PARTITION -> MiuixIcons.Light.GridView
        HomeNavigationIconRole.KNOWLEDGE -> MiuixIcons.Light.Notes
        HomeNavigationIconRole.TECH -> MiuixIcons.Light.Theme
        HomeNavigationIconRole.SUBSCRIPTIONS -> ImageVector.vectorResource(R.drawable.ms_rss_feed_24)
    }
}
