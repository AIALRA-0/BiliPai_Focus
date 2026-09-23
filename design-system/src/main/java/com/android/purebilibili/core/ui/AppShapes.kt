package com.android.purebilibili.core.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle
import com.android.purebilibili.core.theme.resolveAndroidNativeChromeTokens
import com.android.purebilibili.core.theme.resolveCornerRadiusScale
import top.yukonga.miuix.kmp.squircle.addSquircleRect

/** Uses the same upstream path geometry as native Miuix controls. */
internal data class MiuixContainerShape(val radius: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(Path().apply {
            addSquircleRect(size.width, size.height, with(density) { radius.toPx() })
        })
}

/** Semantic container categories shared by the Material 3 and MIUIX themes. */
enum class ContainerLevel {
    /** Progress tracks / hairline chips. base = 1.5dp. */
    Micro,
    /** Tiny tags / badges. base = 4dp. */
    Tag,
    /** Small chips / micro-buttons. base = 6dp. */
    Chip,
    /** Input fields, search bars, small chip-like containers. base = 10dp. */
    Field,
    /** Standard surface cards. base = 12dp. */
    Card,
    /** Dense media cover; retains the legacy Card geometry outside non-glass MIUIX. */
    MediaCover,
    /** Prominent media / hero cards with a full large-radius outline. base = 20dp. */
    ProminentCard,
    /** Alert / confirm dialog containers. base = 14dp. */
    Dialog,
    /** Bottom sheet / modal sheet (top-rounded). base = 20dp. */
    Sheet,
    /** Floating elements — FABs, floating bars. base = 28dp. */
    Floating,
    /** Pill / segmented selectors — radius comes from chrome tokens directly. */
    Pill
}

/**
 * 两值主题的形状 tokens。使用 [AppShapes.container] 而非手写 `RoundedCornerShape(N.dp)`。
 * 基础值按主题风格缩放（MIUIX 更大、MATERIAL3 更紧凑）；Pill 级直接取 chrome tokens，
 * 使各风格保留原生胶囊曲率。
 */
object AppShapes {

    private fun baseDp(level: ContainerLevel): Float = when (level) {
        ContainerLevel.Micro -> 1.5f
        ContainerLevel.Tag -> 4f
        ContainerLevel.Chip -> 6f
        ContainerLevel.Field -> 10f
        ContainerLevel.Card -> 12f
        ContainerLevel.MediaCover -> 12f
        ContainerLevel.ProminentCard -> 20f
        ContainerLevel.Dialog -> 14f
        ContainerLevel.Sheet -> 20f
        ContainerLevel.Floating -> 28f
        ContainerLevel.Pill -> 0f
    }

    fun resolveContainerCornerDp(
        level: ContainerLevel,
        uiStyle: AppUiStyle,
        liquidGlassEnabled: Boolean = true,
    ): Dp {
        if (uiStyle == AppUiStyle.MIUIX) {
            when (level) {
                ContainerLevel.Card -> return 16.dp
                ContainerLevel.MediaCover -> return 12.dp
                ContainerLevel.ProminentCard -> return 20.dp
                ContainerLevel.Chip -> return 10.dp
                ContainerLevel.Dialog -> return 28.dp
                else -> Unit
            }
        }
        if (level == ContainerLevel.Pill) {
            return resolveAndroidNativeChromeTokens(uiStyle).pillCornerRadiusDp.dp
        }
        val scale = resolveCornerRadiusScale(uiStyle)
        return (baseDp(level) * scale).dp
    }

    fun resolveContainerShape(
        level: ContainerLevel,
        uiStyle: AppUiStyle,
        liquidGlassEnabled: Boolean = true,
    ): Shape {
        val dp = resolveContainerCornerDp(level, uiStyle, liquidGlassEnabled)
        if (uiStyle == AppUiStyle.MIUIX && !liquidGlassEnabled && level != ContainerLevel.Sheet) {
            return MiuixContainerShape(dp)
        }
        return if (level == ContainerLevel.Sheet) {
            topRounded(dp)
        } else {
            RoundedCornerShape(dp)
        }
    }

    /** Fill, clipping and border must share the same silhouette. */
    fun resolveBorderedContainerShape(
        level: ContainerLevel,
        uiStyle: AppUiStyle,
        liquidGlassEnabled: Boolean = true,
    ): Shape = resolveContainerShape(level, uiStyle, liquidGlassEnabled)

    /** Top corners only (sheet / stacked cover plate). Bottom stays square. */
    fun topRounded(radius: Dp): Shape =
        RoundedCornerShape(topStart = radius, topEnd = radius)

    /** Bottom corners only (stacked info plate under cover). Top stays square. */
    fun bottomRounded(radius: Dp): Shape =
        RoundedCornerShape(bottomStart = radius, bottomEnd = radius)

    /** Leading vertical edge rounded (e.g. mini-player stashed on the right). */
    fun startRounded(radius: Dp): Shape =
        RoundedCornerShape(topStart = radius, bottomStart = radius)

    /** Trailing vertical edge rounded (e.g. mini-player stashed on the left). */
    fun endRounded(radius: Dp): Shape =
        RoundedCornerShape(topEnd = radius, bottomEnd = radius)

    /** Single-corner badge (duration chip on cover). */
    fun topStartRounded(radius: Dp): Shape =
        RoundedCornerShape(topStart = radius)

    /** Diagonal corners (resize grip on mini-player). */
    fun diagonalTopStartBottomEnd(topStart: Dp, bottomEnd: Dp): Shape =
        RoundedCornerShape(topStart = topStart, bottomEnd = bottomEnd)

    /**
     * Chat / message bubble: large outer corners, small tail corner on the
     * near side of the speaker.
     */
    fun messageBubble(
        isOutgoing: Boolean,
        large: Dp,
        small: Dp,
    ): Shape = if (isOutgoing) {
        RoundedCornerShape(
            topStart = large,
            topEnd = large,
            bottomStart = large,
            bottomEnd = small,
        )
    } else {
        RoundedCornerShape(
            topStart = large,
            topEnd = large,
            bottomStart = small,
            bottomEnd = large,
        )
    }

    @Composable
    fun container(level: ContainerLevel): Shape = resolveContainerShape(
        level = level,
        uiStyle = LocalAppUiStyle.current,
        liquidGlassEnabled = LocalAppThemeConfig.current.liquidGlassEnabled,
    )

    @Composable
    fun borderedContainer(level: ContainerLevel): Shape = resolveBorderedContainerShape(
        level = level,
        uiStyle = LocalAppUiStyle.current,
        liquidGlassEnabled = LocalAppThemeConfig.current.liquidGlassEnabled,
    )

    @Composable
    fun containerCornerDp(level: ContainerLevel): Dp = resolveContainerCornerDp(
        level = level,
        uiStyle = LocalAppUiStyle.current,
        liquidGlassEnabled = LocalAppThemeConfig.current.liquidGlassEnabled,
    )

    /** Uses the 12dp media role in Miuix and preserves each legacy shape in MD3. */
    @Composable
    fun mediaCover(
        legacyLevel: ContainerLevel = ContainerLevel.Card,
    ): Shape = if (LocalAppUiStyle.current == AppUiStyle.MIUIX) {
        container(ContainerLevel.MediaCover)
    } else {
        container(legacyLevel)
    }

    @Composable
    fun borderedMediaCover(
        legacyLevel: ContainerLevel = ContainerLevel.Card,
    ): Shape = if (LocalAppUiStyle.current == AppUiStyle.MIUIX) {
        borderedContainer(ContainerLevel.MediaCover)
    } else {
        borderedContainer(legacyLevel)
    }

    @Composable
    fun mediaCoverCornerDp(
        legacyLevel: ContainerLevel = ContainerLevel.Card,
    ): Dp = if (LocalAppUiStyle.current == AppUiStyle.MIUIX) {
        containerCornerDp(ContainerLevel.MediaCover)
    } else {
        containerCornerDp(legacyLevel)
    }

    /** Scale a semantic radius (e.g. long-press hint size multiplier). */
    @Composable
    fun scaledContainer(level: ContainerLevel, scale: Float): Shape {
        val base = containerCornerDp(level)
        return RoundedCornerShape(base * scale.coerceAtLeast(0.1f))
    }

    @Composable
    fun messageBubble(isOutgoing: Boolean): Shape = messageBubble(
        isOutgoing = isOutgoing,
        large = containerCornerDp(ContainerLevel.Card),
        small = containerCornerDp(ContainerLevel.Tag),
    )
}
