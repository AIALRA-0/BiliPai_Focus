package com.android.purebilibili.feature.profile

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import androidx.compose.ui.draw.scale
import android.widget.Toast
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * 修复壁纸图片 URL (不添加缩放后缀，保持原图质量)
 */
private fun fixWallpaperUrl(url: String?): String {
    if (url.isNullOrEmpty()) return ""
    var newUrl = url
    // 修复无协议头的链接 (//i0.hdslb.com...)
    if (newUrl.startsWith("//")) {
        newUrl = "https:$newUrl"
    }
    // 修复 http 链接
    if (newUrl.startsWith("http://")) {
        newUrl = newUrl.replace("http://", "https://")
    }
    return newUrl
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficialWallpaperSheet(
    viewModel: ProfileViewModel,
    onDismiss: () -> Unit
) {
    val officialWallpapers by viewModel.officialWallpapers.collectAsStateWithLifecycle()
    val isLoading by viewModel.officialWallpapersLoading.collectAsStateWithLifecycle()
    val error by viewModel.officialWallpapersError.collectAsStateWithLifecycle()

    var selectedUrl by remember { mutableStateOf<String?>(null) }
    
    // 初始化加载
    LaunchedEffect(Unit) {
        if (officialWallpapers.isEmpty()) {
            viewModel.loadOfficialWallpapers()
        }
    }

    // ModalBottomSheet 容器
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = null // 自定义头部
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding() // 适配底部
        ) {
            // 1. 顶部栏
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(CupertinoIcons.Default.Xmark, contentDescription = "Close")
                    }
                    
                    Text(
                        text = "开屏壁纸设置",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // 占位，保持标题居中
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }
            
            // 2. 内容区
            when {
                isLoading && officialWallpapers.isEmpty() -> {
                    Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                error != null && officialWallpapers.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize().weight(1f),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = error ?: "加载失败", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.loadOfficialWallpapers() }) {
                            Text("重试")
                        }
                    }
                }
                officialWallpapers.isEmpty() -> {
                    Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                        Text(text = "暂无壁纸", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 100.dp),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(officialWallpapers, key = { it.id }) { item ->
                            val detailUrl = resolveOfficialWallpaperDetailUrl(item)
                            val imageUrl = resolveOfficialWallpaperThumbnailUrl(item)
                            val isSelected = selectedUrl == detailUrl
                            
                            Column(
                                modifier = Modifier
                                    .clickable { selectedUrl = detailUrl }
                                    .animateContentSize(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(imageUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = item.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .aspectRatio(9f / 16f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(
                                                width = if (isSelected) 2.dp else 0.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                    )
                                    
                                    // 选中标记 (右上角)
                                    if (isSelected) {
                                        Icon(
                                            imageVector = CupertinoIcons.Default.CheckmarkCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(20.dp)
                                                .background(Color.White, CircleShape)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(6.dp))
                                
                                Text(
                                    text = item.title.ifEmpty { "未命名" },
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface 
                                )
                            }
                        }
                    }
                }
            }
            
            // 3. 底部保存栏
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // [New] Observe save state
                    val saveState by viewModel.wallpaperSaveState.collectAsStateWithLifecycle()
                    val splashSaveState by viewModel.splashSaveState.collectAsStateWithLifecycle()
                    
                    val isSaving = saveState is WallpaperSaveState.Loading || splashSaveState is WallpaperSaveState.Loading
                    var saveToGallery by remember { mutableStateOf(false) }

                    // Switch for Save to Album
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clickable { saveToGallery = !saveToGallery }, // Make row clickable
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "保存到系统相册",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface 
                        )
                        Switch(
                            checked = saveToGallery,
                            onCheckedChange = { saveToGallery = it },
                            modifier = Modifier.scale(0.8f) 
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Set as Profile Background
                        val context = LocalContext.current
                        
                        // [New] Adjustment Sheet Logic
                        var showAdjustmentSheet by remember { mutableStateOf(false) }
                        var showSplashAdjustmentSheet by remember { mutableStateOf(false) }
                        val initialSplashMobileBias by viewModel.getSplashAlignment(false).collectAsStateWithLifecycle(initialValue = 0f
        )
                        val initialSplashTabletBias by viewModel.getSplashAlignment(true).collectAsStateWithLifecycle(initialValue = 0f
        )
                        
                        // [New] Adjustment Sheet
                         if (showAdjustmentSheet && selectedUrl != null) {
                             ProfileWallpaperAdjustmentSheet(
                                 imageUri = fixWallpaperUrl(selectedUrl),
                                 initialMobileTransform = com.android.purebilibili.core.ui.wallpaper.ProfileWallpaperTransform(),
                                 initialTabletTransform = com.android.purebilibili.core.ui.wallpaper.ProfileWallpaperTransform(),
                                 onDismiss = { showAdjustmentSheet = false },
                                 onSave = { mobileTransform, tabletTransform ->
                                     showAdjustmentSheet = false
                                     // Save with adjustments
                                     selectedUrl?.let { url ->
                                        viewModel.saveWallpaper(
                                            url = url,
                                            mobileTransform = mobileTransform,
                                            tabletTransform = tabletTransform
                                        ) {
                                            onDismiss()
                                            Toast.makeText(context, "背景设置成功", Toast.LENGTH_SHORT).show()
                                        }
                                     }
                                 }
                             )
                        }

                        if (showSplashAdjustmentSheet && selectedUrl != null) {
                            WallpaperAdjustmentSheet(
                                imageUri = fixWallpaperUrl(selectedUrl),
                                initialMobileBias = initialSplashMobileBias,
                                initialTabletBias = initialSplashTabletBias,
                                onDismiss = { showSplashAdjustmentSheet = false },
                                onSave = { mBias, tBias ->
                                    showSplashAdjustmentSheet = false
                                    selectedUrl?.let { url ->
                                        viewModel.setAsSplashWallpaper(
                                            url = url,
                                            saveToGallery = saveToGallery,
                                            mobileBias = mBias,
                                            tabletBias = tBias
                                        ) {
                                            onDismiss()
                                            Toast.makeText(context, "开屏壁纸设置成功", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                        }
                        
                        Button(
                            onClick = { 
                                showAdjustmentSheet = true
                            },
                            enabled = selectedUrl != null && !isSaving,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(25.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            if (saveState is WallpaperSaveState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("设为背景", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        // Set as Splash Screen
                        Button(
                            onClick = { 
                                showSplashAdjustmentSheet = true
                            },
                            enabled = selectedUrl != null && !isSaving,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(25.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            if (splashSaveState is WallpaperSaveState.Loading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("设为开屏", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    
                    if (saveState is WallpaperSaveState.Error) {
                        Text(
                            text = (saveState as WallpaperSaveState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp).align(Alignment.CenterHorizontally)
                        )
                    }
                    if (splashSaveState is WallpaperSaveState.Error) {
                         Text(
                            text = (splashSaveState as WallpaperSaveState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp).align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }
    }
}
