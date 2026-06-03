package com.android.purebilibili.feature.video.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.feature.video.interaction.InteractiveChoicePanelUiState

@Composable
fun InteractiveChoiceOverlay(
    state: InteractiveChoicePanelUiState,
    onSelectChoice: (edgeId: Long, cid: Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.visible || state.choices.isEmpty()) return

    val isCoordinateMode = state.questionType == 2 &&
        state.sourceVideoWidth > 0 &&
        state.sourceVideoHeight > 0 &&
        state.choices.any { it.x != null && it.y != null }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.28f))
            .padding(horizontal = 16.dp, vertical = 20.dp),
        contentAlignment = if (isCoordinateMode) Alignment.Center else Alignment.BottomCenter
    ) {
        if (isCoordinateMode) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                CoordinateModeHeader(
                    title = state.title,
                    remainingMs = state.remainingMs,
                    onDismiss = onDismiss,
                    modifier = Modifier.align(Alignment.TopEnd)
                )

                state.choices.forEach { choice ->
                    val x = choice.x ?: return@forEach
                    val y = choice.y ?: return@forEach
                    val ratioX = (x.toFloat() / state.sourceVideoWidth.toFloat()).coerceIn(0f, 1f)
                    val ratioY = (y.toFloat() / state.sourceVideoHeight.toFloat()).coerceIn(0f, 1f)
                    val textAlign = when (choice.textAlign) {
                        1 -> TextAlign.Left
                        3 -> TextAlign.Right
                        else -> TextAlign.Center
                    }
                    Button(
                        onClick = { onSelectChoice(choice.edgeId, choice.cid) },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(
                                x = (maxWidth * ratioX) - 84.dp,
                                y = (maxHeight * ratioY) - 22.dp
                            )
                            .width(168.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        val label = if (choice.isDefault) "${choice.text}（默认）" else choice.text
                        Text(text = label, textAlign = textAlign)
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = state.title,
                            modifier = Modifier.weight(1f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        val countdown = state.remainingMs
                        if (countdown != null) {
                            Text(
                                text = "${(countdown / 1000L).coerceAtLeast(0L)}s",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(width = 56.dp, height = 32.dp)
                        ) {
                            Text("关闭")
                        }
                    }

                    state.choices.forEach { choice ->
                        val label = if (choice.isDefault) "${choice.text}（默认）" else choice.text
                        Button(
                            onClick = { onSelectChoice(choice.edgeId, choice.cid) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Text(text = label)
                        }
                    }

                }
            }
        }
    }
}

@Composable
private fun CoordinateModeHeader(
    title: String,
    remainingMs: Long?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (remainingMs != null) {
            Text(
                text = "${(remainingMs / 1000L).coerceAtLeast(0L)}s",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.error
            )
        }
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.size(width = 56.dp, height = 28.dp)
        ) {
            Text(
                text = "关闭",
                fontSize = 12.sp
            )
        }
    }
}
