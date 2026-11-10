package com.android.purebilibili.core.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp

/** Read animated geometry in measurement so the tab content does not recompose per frame. */
internal fun Modifier.tabIndicatorBounds(boundsProvider: () -> ElasticTabIndicatorBounds): Modifier =
    layout { measurable, constraints ->
        val bounds = boundsProvider()
        val width = bounds.widthDp.dp.roundToPx().coerceIn(0, constraints.maxWidth)
        val child = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
        layout(width, child.height) {
            child.placeRelative(bounds.leftDp.dp.roundToPx(), 0)
        }
    }
