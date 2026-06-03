// 文件路径: feature/dynamic/components/DrawGrid.kt
package com.android.purebilibili.feature.dynamic.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
//  Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.imageLoader
import com.android.purebilibili.data.model.response.DrawItem
import androidx.compose.material3.Text
import androidx.compose.ui.unit.sp

/**
 *  图片九宫格V2（支持GIF + 点击预览）
 *  🎨 [优化] 更大圆角、单图大尺寸、多图角标
 *  📍 [新增] 支持返回图片位置用于展开动画
 */
@Composable
fun DrawGridV2(
    items: List<DrawItem>,
    gifImageLoader: ImageLoader,
    maxDisplayImages: Int? = 9,
    onImageClick: (Int, Rect?) -> Unit = { _, _ -> }  //  [修改] 图片点击回调，新增 Rect 参数
) {
    if (items.isEmpty()) return

    val context = LocalContext.current
    val defaultImageLoader = context.imageLoader
    val totalCount = items.size  //  保存总图片数
    val displayCount = resolveDrawGridDisplayCount(
        totalImages = totalCount,
        maxDisplayImages = maxDisplayImages
    )
    val displayItems = items.take(displayCount)
    val columns = when {
        displayItems.size == 1 -> 1
        displayItems.size <= 4 -> 2
        else -> 3
    }

    val isSingleImage = displayItems.size == 1
    val gridSpacing = resolveDrawGridSpacingDp().dp
    val cornerRadius = resolveDrawGridCornerRadiusDp().dp

    BoxWithConstraints {
        if (isSingleImage) {
            val singleItem = displayItems.first()
            DrawGridImage(
                item = singleItem,
                index = 0,
                totalCount = totalCount,
                displayCount = displayItems.size,
                modifier = Modifier
                    .fillMaxWidth(
                        resolveSingleImageWidthFraction(
                            width = singleItem.width,
                            height = singleItem.height
                        )
                    )
                    .aspectRatio(
                        resolveSingleImageAspectRatio(
                            width = singleItem.width,
                            height = singleItem.height
                        )
                    ),
                gifImageLoader = gifImageLoader,
                defaultImageLoader = defaultImageLoader,
                cornerRadius = cornerRadius,
                scaleMode = resolveDrawGridScaleMode(displayItems.size),
                onImageClick = onImageClick
            )
        } else {
            var globalIndex = 0
            Column(verticalArrangement = Arrangement.spacedBy(gridSpacing)) {
                displayItems.chunked(columns).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(gridSpacing)
                    ) {
                        row.forEach { item ->
                            val currentIndex = globalIndex++
                            DrawGridImage(
                                item = item,
                                index = currentIndex,
                                totalCount = totalCount,
                                displayCount = displayItems.size,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f),
                                gifImageLoader = gifImageLoader,
                                defaultImageLoader = defaultImageLoader,
                                cornerRadius = cornerRadius,
                                scaleMode = resolveDrawGridScaleMode(displayItems.size),
                                onImageClick = onImageClick
                            )
                        }
                        repeat(columns - row.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawGridImage(
    item: DrawItem,
    index: Int,
    totalCount: Int,
    displayCount: Int,
    modifier: Modifier,
    gifImageLoader: ImageLoader,
    defaultImageLoader: ImageLoader,
    cornerRadius: androidx.compose.ui.unit.Dp,
    scaleMode: DrawGridScaleMode,
    onImageClick: (Int, Rect?) -> Unit
) {
    val context = LocalContext.current
    val imageUrl = remember(item.src) {
        val rawSrc = item.src.trim()
        when {
            rawSrc.startsWith("https://") -> rawSrc
            rawSrc.startsWith("http://") -> rawSrc.replace("http://", "https://")
            rawSrc.startsWith("//") -> "https:$rawSrc"
            rawSrc.isNotEmpty() -> "https://$rawSrc"
            else -> ""
        }
    }
    val isGif = imageUrl.endsWith(".gif", ignoreCase = true)
    var imageRect by remember { mutableStateOf<Rect?>(null) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .onGloballyPositioned { coordinates ->
                imageRect = coordinates.boundsInWindow()
            }
            .clickable { onImageClick(index, imageRect) },
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl.isNotEmpty()) {
            AsyncImage(
                model = coil.request.ImageRequest.Builder(context)
                    .data(imageUrl)
                    .addHeader("Referer", "https://www.bilibili.com/")
                    .crossfade(!isGif)
                    .build(),
                imageLoader = if (isGif) gifImageLoader else defaultImageLoader,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = when (scaleMode) {
                    DrawGridScaleMode.FIT -> ContentScale.Fit
                    DrawGridScaleMode.CROP -> ContentScale.Crop
                }
            )
        } else {
            Icon(
                CupertinoIcons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = Color.Gray.copy(alpha = 0.5f)
            )
        }

        if (shouldDrawGridShowMoreBadge(index = index, displayCount = displayCount, totalCount = totalCount)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "+${totalCount - displayCount}",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }
        }
    }
}
