package com.android.purebilibili.feature.dynamic.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * 记录缩略图在窗口中的坐标，供 [ImagePreviewDialog] 做「落位/回位」morph。
 *
 * 与 DrawGrid 内联的 `onGloballyPositioned + boundsInWindow` 捕获是同一模式，
 * 这里收敛成助手，供没有网格回调的入口（头像、封面、订阅文章图等）复用。
 */
@Composable
fun rememberImagePreviewSourceRect(): MutableState<Rect?> {
    return remember { mutableStateOf(null) }
}

fun Modifier.imagePreviewSourceBounds(target: MutableState<Rect?>): Modifier =
    onGloballyPositioned { coordinates ->
        target.value = coordinates.boundsInWindow()
    }

fun Modifier.imagePreviewGallerySourceBounds(
    target: MutableMap<Int, Rect>,
    pageIndex: Int,
): Modifier = onGloballyPositioned { coordinates ->
    target[pageIndex] = coordinates.boundsInWindow()
}

/**
 * 评论图片链路的落位锚点：缩略图窗口坐标 + 缩略图真实圆角。
 * 由 [CommentPictures] 在捕获处构造（单图 Card / 九宫格 Field 圆角不同），
 * 经 onImagePreview 回调透传到 ImagePreviewDialog 的 sourceRect/sourceCornerRadiusDp。
 */
data class ImagePreviewSourceAnchor(
    val rect: Rect,
    val cornerRadiusDp: Float,
    /** Bounds for the other thumbnails in this same gallery, keyed by preview page index. */
    val galleryRects: Map<Int, Rect> = emptyMap(),
    /** 稳定身份键（如图片 URL）：预览期间隐藏原位卡片优先按它匹配，几何判定兜底。 */
    val sourceKey: String? = null,
)
