package com.android.purebilibili.feature.video.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.components.AppLazyColumn
import com.android.purebilibili.feature.home.HomeFeedCardLayout

/** Resolve the real pane width once for all rows in glass, MD3 and Miuix layouts. */
@Composable
internal fun RelatedVideoList(
    cardLayout: HomeFeedCardLayout,
    state: LazyListState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: LazyListScope.(RelatedVideoCardLayout) -> Unit,
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    BoxWithConstraints(modifier = modifier, propagateMinConstraints = true) {
        val cardPresentation = with(density) {
            // Match lazy content padding and row padding rounding independently in pixels.
            resolveRelatedVideoListCardLayout(
                viewportWidthPx = constraints.maxWidth,
                horizontalContentPaddingPx = contentPadding.calculateLeftPadding(layoutDirection).roundToPx() +
                    contentPadding.calculateRightPadding(layoutDirection).roundToPx(),
                cardHorizontalPaddingPx = cardLayout.outerPaddingDp.dp.roundToPx(),
                density = density.density,
            )
        }
        AppLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = state,
            contentPadding = contentPadding,
        ) {
            content(cardPresentation)
        }
    }
}
