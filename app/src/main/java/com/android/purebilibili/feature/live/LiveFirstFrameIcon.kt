package com.android.purebilibili.feature.live

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** 首帧模式使用 F 字标记，与普通封面图片图标区分。 */
internal val LiveFirstFrameIcon: ImageVector = ImageVector.Builder(
    name = "LiveFirstFrame",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
        moveTo(5f, 3f)
        lineTo(19f, 3f)
        quadTo(21f, 3f, 21f, 5f)
        lineTo(21f, 19f)
        quadTo(21f, 21f, 19f, 21f)
        lineTo(5f, 21f)
        quadTo(3f, 21f, 3f, 19f)
        lineTo(3f, 5f)
        quadTo(3f, 3f, 5f, 3f)
        close()
        moveTo(8f, 6f)
        lineTo(16f, 6f)
        lineTo(16f, 8f)
        lineTo(10f, 8f)
        lineTo(10f, 11f)
        lineTo(15f, 11f)
        lineTo(15f, 13f)
        lineTo(10f, 13f)
        lineTo(10f, 18f)
        lineTo(8f, 18f)
        close()
    }
}.build()
