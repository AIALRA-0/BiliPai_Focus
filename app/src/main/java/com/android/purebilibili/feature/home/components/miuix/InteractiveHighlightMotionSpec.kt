package com.android.purebilibili.feature.home.components.miuix

import androidx.compose.animation.core.SpringSpec
import androidx.compose.ui.geometry.Offset
import com.android.purebilibili.core.ui.components.appPressHighlightPressSpec
import com.android.purebilibili.core.ui.components.appPressHighlightPositionSpec

internal fun interactiveHighlightPressSpec(): SpringSpec<Float> = appPressHighlightPressSpec()

internal fun interactiveHighlightPositionSpec(): SpringSpec<Offset> = appPressHighlightPositionSpec()
