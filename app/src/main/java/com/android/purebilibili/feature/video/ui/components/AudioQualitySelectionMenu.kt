package com.android.purebilibili.feature.video.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntRect
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.feature.video.playback.audio.AudioQualityOption

private val HiResGold = Color(0xFFFFD36A)
private val DolbyBlue = Color(0xFF8DCDFF)

@Composable
fun HiResBadge(modifier: Modifier = Modifier) = AudioFormatBadge("Hi-Res", HiResGold, Color(0xFF332A14), modifier)

@Composable
fun DolbyBadge(modifier: Modifier = Modifier) = AudioFormatBadge("DOLBY", DolbyBlue, Color(0xFF142A3A), modifier)

@Composable
private fun AudioFormatBadge(text: String, accent: Color, background: Color, modifier: Modifier) {
    AppSurface(
        modifier = modifier, color = background.copy(alpha = 0.9f), contentColor = accent,
        shape = AppShapes.container(ContainerLevel.Tag), border = BorderStroke(0.75.dp, accent),
    ) {
        AppText(
            text = text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        )
    }
}

@Composable
fun AudioQualitySelectionMenu(
    options: List<AudioQualityOption>, requestedAudioQuality: Int,
    onAudioQualitySelected: (Int) -> Unit, onDismiss: () -> Unit,
    placement: PlayerListPopupPlacement = PlayerListPopupPlacement.CENTER,
    anchorBounds: IntRect? = null,
    selectedAudioQuality: Int = requestedAudioQuality,
) {
    PlayerSelectionPopup(
        title = "音质选择", showTitle = false, onDismissRequest = onDismiss, placement = placement,
        anchorBounds = anchorBounds,
    ) {
        options.forEachIndexed { index, option ->
            val selected = option.preferenceId == selectedAudioQuality
            val summary = option.codecDescription.takeIf { it.isNotBlank() }
            PlayerSelectionItem(
                modifier = Modifier.playerPopupSelectedOption(selected),
                text = option.label, summary = summary, selected = selected,
                index = index, optionCount = options.size,
                enabled = option.preferenceId != requestedAudioQuality,
                onClick = { onAudioQualitySelected(option.preferenceId) },
            )
        }
    }
}

@Composable
fun AudioQualitySelectionMenuDialog(
    options: List<AudioQualityOption>, requestedAudioQuality: Int,
    onAudioQualitySelected: (Int) -> Unit, onDismiss: () -> Unit,
    placement: PlayerListPopupPlacement = PlayerListPopupPlacement.CENTER,
    anchorBounds: IntRect? = null,
    selectedAudioQuality: Int = requestedAudioQuality,
) = AudioQualitySelectionMenu(
    options, requestedAudioQuality, onAudioQualitySelected, onDismiss, placement, anchorBounds,
    selectedAudioQuality
)
