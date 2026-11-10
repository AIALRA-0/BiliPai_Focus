package com.android.purebilibili.feature.live.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import com.android.purebilibili.data.model.response.LiveMessageMedal
import com.android.purebilibili.feature.live.LiveStatusPalette

private fun liveMedalLabel(medal: LiveMessageMedal): AnnotatedString = buildAnnotatedString {
    append(medal.name)
    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
    append(" ${medal.level}")
    pop()
}

@Composable
internal fun rememberLiveMedalSize(medal: LiveMessageMedal): DpSize {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val text = remember(medal.name, medal.level) { liveMedalLabel(medal) }
    val style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 10.sp)
    val measured = measurer.measure(text, style = style, maxLines = 1)
    return with(density) { DpSize(measured.size.width.toDp() + 12.dp, measured.size.height.toDp() + 6.dp) }
}

@Composable
internal fun LiveMedalBadge(medal: LiveMessageMedal, modifier: Modifier = Modifier) {
    val background = if (medal.backgroundColor != 0) Color(medal.backgroundColor) else LiveStatusPalette.MedalFallback
    Text(
        text = remember(medal.name, medal.level) { liveMedalLabel(medal) },
        color = Color(medal.textColor),
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 10.sp),
        maxLines = 1,
        modifier = modifier
            .background(background, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
    )
}
