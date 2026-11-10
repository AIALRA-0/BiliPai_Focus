package com.android.purebilibili.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntSize
import com.android.purebilibili.core.ui.motion.ExpandablePreviewMotion
import com.android.purebilibili.core.ui.motion.rememberExpandablePreviewMotion
import kotlin.math.roundToInt

internal typealias DonationPreviewMotion = ExpandablePreviewMotion

@Composable
internal fun rememberDonationPreviewMotion(): DonationPreviewMotion = rememberExpandablePreviewMotion()

internal fun donationPreviewContainerSize(compact: IntSize, preview: IntSize, progress: Float): IntSize {
    val fraction = progress.coerceIn(0f, 1f)
    return IntSize(
        (compact.width + (preview.width - compact.width) * fraction).roundToInt(),
        (compact.height + (preview.height - compact.height) * fraction).roundToInt(),
    )
}
