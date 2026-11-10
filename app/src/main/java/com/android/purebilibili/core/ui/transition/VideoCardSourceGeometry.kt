package com.android.purebilibili.core.ui.transition

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRootCoordinates

/**
 * 转场落点使用完整布局边界，不能把滚动容器裁掉的部分当作封面的真实尺寸。
 * 卡片、封面和信息区必须使用同一 Root 坐标系；可见性仍由导航入口单独判断。
 * 此处只记录几何，不改变玻璃、MD3、Miuix 的布局、比例或裁切样式。
 */
internal fun LayoutCoordinates.videoCardSourceBoundsInRoot(): Rect =
    findRootCoordinates().localBoundingBoxOf(this, clipBounds = false)
