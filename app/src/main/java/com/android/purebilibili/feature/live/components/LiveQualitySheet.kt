package com.android.purebilibili.feature.live.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.android.purebilibili.core.ui.AppModalBottomSheet
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.data.model.response.LiveQuality
import com.android.purebilibili.feature.live.LiveHomeSelectableChip

/**
 * 直播画质选择（BiliPai bottom-control 画质菜单的 Compose 形态）。
 * 选项使用 [LiveHomeSelectableChip]（中性 App* 组件）。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LiveQualitySheet(
    qualityList: List<LiveQuality>,
    currentQuality: Int,
    onQualitySelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AppModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = AppSpacingTokens.ExtraLarge,
                    vertical = AppSpacingTokens.Large,
                ),
            verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Large),
        ) {
            AppText(
                text = "画质",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (qualityList.isEmpty()) {
                AppText(
                    text = "暂无可用画质",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small),
                    verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small),
                ) {
                    qualityList.forEach { quality ->
                        LiveHomeSelectableChip(
                            label = quality.desc.ifBlank { quality.qn.toString() },
                            selected = quality.qn == currentQuality,
                            onClick = { onQualitySelected(quality.qn) },
                        )
                    }
                }
            }
        }
    }
}
