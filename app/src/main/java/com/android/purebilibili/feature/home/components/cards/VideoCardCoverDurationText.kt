package com.android.purebilibili.feature.home.components.cards

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.android.purebilibili.core.ui.MediaContrastPalette
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.feedContentTypography

/** Cover duration: white text + soft shadow, matching home feed overlay (no black capsule). */
@Composable
internal fun VideoCardCoverDurationText(
    text: String,
    modifier: Modifier = Modifier,
) {
    val overlayStyle = remember {
        TextStyle(shadow = resolveVideoCardCoverOverlayTextShadow())
    }
    AppText(
        text = text,
        color = MediaContrastPalette.Foreground,
        style = feedContentTypography().coverBadge
            .copy(fontWeight = FontWeight.Medium)
            .merge(overlayStyle),
        maxLines = 1,
        tapToCopyEnabled = false,
        modifier = modifier,
    )
}
