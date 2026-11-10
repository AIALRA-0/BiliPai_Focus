package com.android.purebilibili.core.ui.components

import com.android.purebilibili.core.ui.AppContentColors

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.android.purebilibili.core.ui.AppSurfaceTokens

/** Chrome supplies its foreground, including light controls over media backgrounds. */
internal val LocalAppActionContentColor = staticCompositionLocalOf { Color.Unspecified }

@Composable
internal fun ProvideAppActionContentColor(color: Color, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalAppActionContentColor provides color,
        androidx.compose.material3.LocalContentColor provides color,
        top.yukonga.miuix.kmp.theme.LocalContentColor provides color,
        content = content,
    )
}

/**
 * Ordinary actions are neutral in both renderers. Accent belongs to primary actions and
 * selected/active states; a page must not accent a search, overflow or sort entry arbitrarily.
 */
object AppTextButtonDefaults {
    @Composable
    fun colors(): androidx.compose.material3.ButtonColors {
        val chromeColor = LocalAppActionContentColor.current
        if (com.android.purebilibili.core.ui.isMiuixNonGlassEnabled()) {
            val native = top.yukonga.miuix.kmp.basic.ButtonDefaults.textButtonColors()
            return ButtonDefaults.textButtonColors(
                containerColor = native.color,
                contentColor = if (chromeColor == Color.Unspecified) native.textColor else chromeColor,
                disabledContainerColor = native.disabledColor,
                disabledContentColor = native.disabledTextColor,
            )
        }
        val contentColor = if (chromeColor == Color.Unspecified) AppContentColors.neutralAction() else chromeColor
        return ButtonDefaults.textButtonColors(
            contentColor = contentColor,
            disabledContentColor = contentColor.copy(alpha = 0.38f),
        )
    }

    /** Explicit opt-in for a primary text action, such as submit or confirm. */
    @Composable
    fun primaryColors(): androidx.compose.material3.ButtonColors {
        if (com.android.purebilibili.core.ui.isMiuixNonGlassEnabled()) {
            val native = top.yukonga.miuix.kmp.basic.ButtonDefaults.textButtonColorsPrimary()
            return ButtonDefaults.textButtonColors(
                containerColor = native.color,
                contentColor = native.textColor,
                disabledContainerColor = native.disabledColor,
                disabledContentColor = native.disabledTextColor,
            )
        }
        return ButtonDefaults.textButtonColors(
            contentColor = AppContentColors.link(),
            disabledContentColor = AppContentColors.disabled(),
        )
    }
}


/** Neutral thumbs adapt to the active surface without inheriting theme text/accent colors. */
@Composable
@ReadOnlyComposable
fun appControlThumbColor(): Color = if (AppSurfaceTokens.background().luminance() < 0.5f) {
    Color(0xFFD0D2D6)
} else {
    Color.White
}
