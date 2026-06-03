package com.android.purebilibili.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.android.purebilibili.core.ui.wallpaper.ProfileWallpaperTransform
import com.android.purebilibili.core.ui.wallpaper.applyGestureToProfileWallpaperTransform
import com.android.purebilibili.core.ui.wallpaper.sanitizeProfileWallpaperTransform
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TabletAndroid
import androidx.compose.material.icons.outlined.PhoneAndroid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperAdjustmentSheet(
    imageUri: String,
    initialMobileBias: Float = 0f,
    initialTabletBias: Float = 0f,
    onSave: (mobileBias: Float, tabletBias: Float) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Mobile, 1: Tablet
    var mobileBias by remember { mutableStateOf(initialMobileBias) }
    var tabletBias by remember { mutableStateOf(initialTabletBias) }
    
    val currentBias = if (selectedTab == 0) mobileBias else tabletBias
    
    // Bottom Sheet
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "取消",
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .clickable { onDismiss() },
                    color = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = "调整壁纸位置",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
                
                Text(
                    text = "保存",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .clickable { onSave(mobileBias, tabletBias) },
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // Tab Switcher (Mobile vs Tablet)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TabItem(
                    title = "手机端",
                    icon = Icons.Outlined.PhoneAndroid,
                    isSelected = selectedTab == 0,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTab = 0 }
                )
                TabItem(
                    title = "平板端",
                    icon = Icons.Outlined.TabletAndroid,
                    isSelected = selectedTab == 1,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTab = 1 }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Preview Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp) // Fixed height container for preview
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                // Device Mock Frame
                val aspectRatio = if (selectedTab == 0) 9f / 18f else 16f / 10f
                val width = if (selectedTab == 0) 140.dp else 280.dp
                val height = width / aspectRatio
                
                // Card simulating the device screen
                Card(
                    shape = RoundedCornerShape(if (selectedTab == 0) 16.dp else 12.dp),
                    elevation = CardDefaults.cardElevation(8.dp),
                    modifier = Modifier.size(width = width, height = height)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(imageUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            alignment = androidx.compose.ui.BiasAlignment(0f, currentBias),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        
                        // Fake UI Overlay to help context
                        // Gradient Overlay
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp) // Fake header height
                                .background(
                                    androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = listOf(Color.Black.copy(alpha = 0.4f), Color.Transparent)
                                    )
                                )
                        )
                        
                        // Text Overlay hint
                        Text(
                            text = "预览效果",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                .padding(4.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Slider Control
            Column(
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("顶部对齐", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    Text("居中", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    Text("底部对齐", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
                
                Slider(
                    value = currentBias,
                    onValueChange = { newValue ->
                        if (selectedTab == 0) mobileBias = newValue else tabletBias = newValue
                    },
                    valueRange = -1f..1f,
                    steps = 0, // Continuous
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text(
                    text = "上下拖动滑块调整图片显示区域",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileWallpaperAdjustmentSheet(
    imageUri: String,
    initialMobileTransform: ProfileWallpaperTransform = ProfileWallpaperTransform(),
    initialTabletTransform: ProfileWallpaperTransform = ProfileWallpaperTransform(),
    onSave: (mobileTransform: ProfileWallpaperTransform, tabletTransform: ProfileWallpaperTransform) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var mobileTransform by remember {
        mutableStateOf(sanitizeProfileWallpaperTransform(initialMobileTransform))
    }
    var tabletTransform by remember {
        mutableStateOf(sanitizeProfileWallpaperTransform(initialTabletTransform))
    }

    val currentTransform = if (selectedTab == 0) mobileTransform else tabletTransform
    val currentTransformState = rememberUpdatedState(currentTransform)
    fun updateCurrentTransform(transform: ProfileWallpaperTransform) {
        if (selectedTab == 0) {
            mobileTransform = sanitizeProfileWallpaperTransform(transform)
        } else {
            tabletTransform = sanitizeProfileWallpaperTransform(transform)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "取消",
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .clickable { onDismiss() },
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "调整壁纸位置",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )

                Text(
                    text = "保存",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .clickable { onSave(mobileTransform, tabletTransform) },
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TabItem(
                    title = "手机端",
                    icon = Icons.Outlined.PhoneAndroid,
                    isSelected = selectedTab == 0,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTab = 0 }
                )
                TabItem(
                    title = "平板端",
                    icon = Icons.Outlined.TabletAndroid,
                    isSelected = selectedTab == 1,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTab = 1 }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(328.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                val aspectRatio = if (selectedTab == 0) 9f / 18f else 16f / 10f
                val width = if (selectedTab == 0) 150.dp else 292.dp
                val height = width / aspectRatio

                Card(
                    shape = RoundedCornerShape(if (selectedTab == 0) 16.dp else 12.dp),
                    elevation = CardDefaults.cardElevation(8.dp),
                    modifier = Modifier.size(width = width, height = height)
                ) {
                    var previewSize by remember(selectedTab) { mutableStateOf(IntSize.Zero) }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged { previewSize = it }
                            .pointerInput(selectedTab, previewSize) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    updateCurrentTransform(
                                        applyGestureToProfileWallpaperTransform(
                                            current = currentTransformState.value,
                                            panX = pan.x,
                                            panY = pan.y,
                                            zoomChange = zoom,
                                            containerWidthPx = previewSize.width.toFloat(),
                                            containerHeightPx = previewSize.height.toFloat()
                                        )
                                    )
                                }
                            }
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(imageUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            alignment = androidx.compose.ui.BiasAlignment(
                                currentTransform.offsetX,
                                currentTransform.offsetY
                            ),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = currentTransform.scale,
                                    scaleY = currentTransform.scale
                                )
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.42f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(88.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.24f),
                                            Color.Black.copy(alpha = 0.46f)
                                        )
                                    )
                                )
                        )

                        Text(
                            text = "双指缩放  单指拖动",
                            color = Color.White.copy(alpha = 0.86f),
                            fontSize = 10.sp,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (selectedTab == 0) "手机端参数" else "平板端参数",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "缩放 ${"%.2f".format(currentTransform.scale)}x  横向 ${"%.2f".format(currentTransform.offsetX)}  纵向 ${"%.2f".format(currentTransform.offsetY)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                TextButton(
                    onClick = { updateCurrentTransform(ProfileWallpaperTransform()) }
                ) {
                    Text("重置位置")
                }
            }

            Text(
                text = "不同设备分别保存；首次设置会以居中参数作为默认值。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun TabItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.background else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
