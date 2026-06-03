package com.android.purebilibili.feature.settings

import androidx.annotation.DrawableRes
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.android.purebilibili.R
import com.android.purebilibili.core.theme.LocalDynamicColorActive
import com.android.purebilibili.core.theme.LocalUiPreset
import com.android.purebilibili.core.theme.UiPreset
import com.android.purebilibili.core.theme.iOSBlue
import com.android.purebilibili.core.theme.iOSGreen
import com.android.purebilibili.core.theme.iOSOrange
import com.android.purebilibili.core.theme.iOSPink
import com.android.purebilibili.core.theme.iOSPurple
import com.android.purebilibili.core.theme.iOSRed
import com.android.purebilibili.core.theme.iOSTeal
import com.android.purebilibili.core.ui.AppIcons

internal data class SettingsEntryVisual(
    val icon: ImageVector? = null,
    @DrawableRes val iconResId: Int? = null,
    val iconTint: Color
)

internal data class SettingsEntryThemePalette(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val error: Color
)

internal enum class SettingsEntryTintRole {
    PRIMARY,
    SECONDARY,
    TERTIARY,
    ERROR
}

private val PreviewMd3SettingsEntryThemePalette = SettingsEntryThemePalette(
    primary = lightColorScheme().primary,
    secondary = lightColorScheme().secondary,
    tertiary = lightColorScheme().tertiary,
    error = lightColorScheme().error
)

internal fun resolveMd3SettingsEntryThemePalette(
    colorScheme: ColorScheme,
    useSemanticAccentRoles: Boolean = true
): SettingsEntryThemePalette = SettingsEntryThemePalette(
    primary = colorScheme.primary,
    secondary = if (useSemanticAccentRoles) colorScheme.secondary else colorScheme.primary,
    tertiary = if (useSemanticAccentRoles) colorScheme.tertiary else colorScheme.primary,
    error = colorScheme.error
)

private fun SettingsEntryThemePalette.resolve(
    role: SettingsEntryTintRole
): Color = when (role) {
    SettingsEntryTintRole.PRIMARY -> primary
    SettingsEntryTintRole.SECONDARY -> secondary
    SettingsEntryTintRole.TERTIARY -> tertiary
    SettingsEntryTintRole.ERROR -> error
}

@Composable
internal fun rememberSettingsEntryTint(
    role: SettingsEntryTintRole,
    iosTint: Color,
    uiPreset: UiPreset = LocalUiPreset.current,
    dynamicColorActive: Boolean = LocalDynamicColorActive.current
): Color {
    val colorScheme = MaterialTheme.colorScheme
    val md3Palette = remember(colorScheme, dynamicColorActive) {
        resolveMd3SettingsEntryThemePalette(colorScheme, useSemanticAccentRoles = dynamicColorActive)
    }
    return remember(role, iosTint, uiPreset, dynamicColorActive, md3Palette) {
        if (uiPreset == UiPreset.MD3) {
            md3Palette.resolve(role)
        } else {
            iosTint
        }
    }
}

private fun resolveMd3SettingsEntryTintRole(
    target: SettingsSearchTarget
): SettingsEntryTintRole = when (target) {
    SettingsSearchTarget.INTERFACE_THEME,
    SettingsSearchTarget.HOME_FEED,
    SettingsSearchTarget.NAVIGATION,
    SettingsSearchTarget.APPEARANCE,
    SettingsSearchTarget.ANIMATION,
    SettingsSearchTarget.PLUGINS,
    SettingsSearchTarget.OPEN_SOURCE_HOME,
    SettingsSearchTarget.REPLAY_ONBOARDING,
    SettingsSearchTarget.TIPS -> SettingsEntryTintRole.TERTIARY

    SettingsSearchTarget.PLAYBACK_QUALITY,
    SettingsSearchTarget.FULLSCREEN_GESTURE,
    SettingsSearchTarget.INTERACTION_COMMENT,
    SettingsSearchTarget.DATA_BACKUP,
    SettingsSearchTarget.PLAYBACK,
    SettingsSearchTarget.BOTTOM_BAR,
    SettingsSearchTarget.SETTINGS_SHARE,
    SettingsSearchTarget.WEBDAV_BACKUP,
    SettingsSearchTarget.DOWNLOAD_PATH,
    SettingsSearchTarget.IMAGE_SAVE_PATH,
    SettingsSearchTarget.EXPORT_LOGS,
    SettingsSearchTarget.OPEN_SOURCE_LICENSES,
    SettingsSearchTarget.VIEW_RELEASE_NOTES,
    SettingsSearchTarget.OPEN_LINKS,
    SettingsSearchTarget.PERMISSION -> SettingsEntryTintRole.SECONDARY

    SettingsSearchTarget.PRIVACY_PERMISSION,
    SettingsSearchTarget.DIAGNOSTICS,
    SettingsSearchTarget.ABOUT_SUPPORT,
    SettingsSearchTarget.CHECK_UPDATE,
    SettingsSearchTarget.DONATE,
    SettingsSearchTarget.TELEGRAM,
    SettingsSearchTarget.TWITTER,
    SettingsSearchTarget.DISCLAIMER,
    SettingsSearchTarget.BLOCKED_LIST,
    SettingsSearchTarget.CLEAR_CACHE -> SettingsEntryTintRole.PRIMARY
}

private fun resolveIosSettingsEntryTint(
    target: SettingsSearchTarget
): Color = when (target) {
    SettingsSearchTarget.INTERFACE_THEME -> iOSPink
    SettingsSearchTarget.HOME_FEED -> iOSOrange
    SettingsSearchTarget.NAVIGATION -> iOSBlue
    SettingsSearchTarget.PLAYBACK_QUALITY -> iOSGreen
    SettingsSearchTarget.FULLSCREEN_GESTURE -> iOSPurple
    SettingsSearchTarget.INTERACTION_COMMENT -> iOSTeal
    SettingsSearchTarget.DATA_BACKUP -> iOSBlue
    SettingsSearchTarget.PRIVACY_PERMISSION -> iOSPurple
    SettingsSearchTarget.DIAGNOSTICS -> iOSTeal
    SettingsSearchTarget.ABOUT_SUPPORT -> iOSOrange
    SettingsSearchTarget.APPEARANCE -> iOSPink
    SettingsSearchTarget.ANIMATION -> iOSPink
    SettingsSearchTarget.PLAYBACK -> iOSGreen
    SettingsSearchTarget.BOTTOM_BAR -> iOSBlue
    SettingsSearchTarget.PERMISSION -> iOSTeal
    SettingsSearchTarget.BLOCKED_LIST -> iOSBlue
    SettingsSearchTarget.SETTINGS_SHARE -> iOSGreen
    SettingsSearchTarget.WEBDAV_BACKUP -> iOSBlue
    SettingsSearchTarget.DOWNLOAD_PATH -> iOSBlue
    SettingsSearchTarget.IMAGE_SAVE_PATH -> iOSTeal
    SettingsSearchTarget.CLEAR_CACHE -> iOSBlue
    SettingsSearchTarget.PLUGINS -> iOSPurple
    SettingsSearchTarget.EXPORT_LOGS -> iOSTeal
    SettingsSearchTarget.OPEN_SOURCE_LICENSES -> iOSOrange
    SettingsSearchTarget.OPEN_SOURCE_HOME -> iOSPurple
    SettingsSearchTarget.CHECK_UPDATE -> iOSBlue
    SettingsSearchTarget.VIEW_RELEASE_NOTES -> iOSTeal
    SettingsSearchTarget.REPLAY_ONBOARDING -> iOSPink
    SettingsSearchTarget.TIPS -> iOSOrange
    SettingsSearchTarget.OPEN_LINKS -> iOSTeal
    SettingsSearchTarget.DONATE -> iOSRed
    SettingsSearchTarget.TELEGRAM -> iOSBlue
    SettingsSearchTarget.TWITTER -> iOSBlue
    SettingsSearchTarget.DISCLAIMER -> iOSBlue
}

@Composable
internal fun rememberSettingsEntryVisual(
    target: SettingsSearchTarget,
    uiPreset: UiPreset = LocalUiPreset.current,
    dynamicColorActive: Boolean = LocalDynamicColorActive.current
): SettingsEntryVisual {
    val colorScheme = MaterialTheme.colorScheme
    val md3Palette = remember(colorScheme, dynamicColorActive) {
        resolveMd3SettingsEntryThemePalette(colorScheme, useSemanticAccentRoles = dynamicColorActive)
    }
    return remember(target, uiPreset, dynamicColorActive, md3Palette) {
        resolveSettingsEntryVisual(target, uiPreset, md3Palette)
    }
}

internal fun resolveSettingsEntryVisual(
    target: SettingsSearchTarget,
    uiPreset: UiPreset = UiPreset.IOS,
    md3Palette: SettingsEntryThemePalette = PreviewMd3SettingsEntryThemePalette
): SettingsEntryVisual {
    val iconTint = if (uiPreset == UiPreset.MD3) {
        md3Palette.resolve(resolveMd3SettingsEntryTintRole(target))
    } else {
        resolveIosSettingsEntryTint(target)
    }
    return when (target) {
        SettingsSearchTarget.TELEGRAM -> SettingsEntryVisual(
            iconResId = R.drawable.ic_telegram_mono,
            iconTint = iconTint
        )
        SettingsSearchTarget.TWITTER -> SettingsEntryVisual(
            icon = AppIcons.Twitter,
            iconTint = iconTint
        )
        else -> SettingsEntryVisual(
            icon = resolveSettingsSemanticIcon(
                role = resolveSettingsSearchTargetIconRole(target),
                uiPreset = uiPreset
            ),
            iconTint = iconTint
        )
    }
}
