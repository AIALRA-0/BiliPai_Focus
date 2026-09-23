package com.android.purebilibili.core.ui.renderer.miuix

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.sizeIn
import com.android.purebilibili.core.ui.components.AppIconButtonColors
import com.android.purebilibili.core.ui.components.AppIconButtonVariant
import com.android.purebilibili.core.ui.components.appDesktopFocusableItemVisuals
import com.android.purebilibili.core.ui.AppChromeSizeTokens
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.ButtonDefaults as MiuixButtonDefaults
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.theme.LocalContentColor as MiuixLocalContentColor

@Composable
internal fun AppMiuixIconButton(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    colors: AppIconButtonColors?,
    interactionSource: MutableInteractionSource?,
    variant: AppIconButtonVariant,
    content: @Composable () -> Unit,
) {
    val defaultColors = when (variant) {
        AppIconButtonVariant.Standard -> AppIconButtonColors(
            containerColor = Color.Unspecified,
            contentColor = MiuixLocalContentColor.current,
            disabledContainerColor = Color.Unspecified,
            disabledContentColor = MiuixLocalContentColor.current,
        )
        AppIconButtonVariant.Filled -> MiuixButtonDefaults.buttonColorsPrimary().let {
            AppIconButtonColors(
                containerColor = it.color,
                contentColor = it.contentColor,
                disabledContainerColor = it.disabledColor,
                disabledContentColor = it.disabledContentColor,
            )
        }
    }
    val requestedColors = colors ?: AppIconButtonColors()
    val backgroundColor = if (enabled) {
        requestedColors.containerColor.takeOrElse { defaultColors.containerColor }
    } else {
        requestedColors.disabledContainerColor.takeOrElse { defaultColors.disabledContainerColor }
    }
    val contentColor = if (enabled) {
        requestedColors.contentColor.takeOrElse { defaultColors.contentColor }
    } else {
        requestedColors.disabledContentColor.takeOrElse { defaultColors.disabledContentColor }
    }
    val pointerMirror = if (interactionSource != null) {
        Modifier.mirrorMiuixPointerPressInteractions(
            interactionSource = interactionSource,
            enabled = enabled,
        )
    } else {
        Modifier
    }
    // Put the minimum before the caller modifier so an exact 32/36/40dp size is coerced to the
    // accessibility bound instead of overriding Miuix's defaultMinSize. The icon content keeps
    // its own visual size; only the selectable container grows.
    val nativeModifier = Modifier
        .sizeIn(
            minWidth = AppChromeSizeTokens.MinimumTouchTarget,
            minHeight = AppChromeSizeTokens.MinimumTouchTarget,
        )
        .then(modifier)
        .appDesktopFocusableItemVisuals(
            enabled = enabled,
            shape = RoundedCornerShape(AppChromeSizeTokens.MiuixNativeCompactCornerRadiusDp.dp),
        )
        .then(pointerMirror)

    CompositionLocalProvider(MiuixLocalContentColor provides contentColor) {
        MiuixIconButton(
            onClick = onClick,
            modifier = nativeModifier,
            enabled = enabled,
            backgroundColor = backgroundColor,
            minHeight = AppChromeSizeTokens.MinimumTouchTarget,
            minWidth = AppChromeSizeTokens.MinimumTouchTarget,
            cornerRadius = AppChromeSizeTokens.MiuixNativeCompactCornerRadiusDp.dp,
            content = content,
        )
    }
}

/**
 * Mirrors pointer presses into a caller-owned source without consuming input or adding click
 * behavior. Keyboard, D-pad, and semantic clicks remain native, but their pressed interaction is
 * intentionally not exported because Miuix IconButton does not expose its interaction source.
 */
private fun Modifier.mirrorMiuixPointerPressInteractions(
    interactionSource: MutableInteractionSource,
    enabled: Boolean,
): Modifier {
    if (!enabled) return this
    return pointerInput(interactionSource) {
        awaitEachGesture {
            val down = awaitFirstDown(
                requireUnconsumed = false,
                pass = PointerEventPass.Initial,
            )
            val press = PressInteraction.Press(down.position)
            interactionSource.tryEmit(press)
            var released = false
            try {
                released = waitForUpOrCancellation(PointerEventPass.Initial) != null
            } finally {
                interactionSource.tryEmit(
                    if (released) {
                        PressInteraction.Release(press)
                    } else {
                        PressInteraction.Cancel(press)
                    },
                )
            }
        }
    }
}
