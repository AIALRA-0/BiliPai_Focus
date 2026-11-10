package com.android.purebilibili.feature.home.components.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.AppContentColors
import com.android.purebilibili.core.ui.components.AppText

/** Server recommendation copy is independent of the optional UP identity badge. */
@Composable
internal fun VideoRecommendationBadge(text: String, modifier: Modifier = Modifier) {
    if (text.isNotBlank()) {
        AppText(
            text = text,
            modifier = modifier
                .widthIn(max = 100.dp)
                .background(AppContentColors.accent().copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = AppContentColors.accent(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            tapToCopyEnabled = false,
        )
    }
}
