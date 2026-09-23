package com.android.purebilibili.feature.profile

import coil3.request.crossfade
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppText

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.android.purebilibili.core.store.SettingsManager
import androidx.compose.material.icons.Icons
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.purebilibili.core.ui.AdaptiveLoadingIndicator
import com.android.purebilibili.core.ui.rememberAppClearIcon
import com.android.purebilibili.core.ui.rememberAppPhotoIcon
import com.android.purebilibili.core.ui.rememberAppCheckCircleIcon
import com.android.purebilibili.core.ui.AppModalBottomSheet
import com.android.purebilibili.core.ui.components.AppButton
import com.android.purebilibili.core.ui.components.AppCircularProgressIndicator
import com.android.purebilibili.core.ui.components.AppIconButton
import com.android.purebilibili.core.ui.components.AppOutlinedButton
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppSwitch
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.util.Logger
import com.android.purebilibili.core.util.PickGalleryVisualMedia
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * 🖼️ 开屏壁纸选择器 (用于设置页)
 * 仅用于选择开屏壁纸，简化的单一用途组件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplashWallpaperPickerSheet(
    viewModel: ProfileViewModel = viewModel(),
    target: WallpaperPickerTarget = WallpaperPickerTarget.SPLASH,
    onDismiss: () -> Unit
) {
    val clearIcon = rememberAppClearIcon()
    val photoIcon = rememberAppPhotoIcon()
    val context = LocalContext.current
    val officialWallpapers by viewModel.officialWallpapers.collectAsStateWithLifecycle()
    val isLoading by viewModel.officialWallpapersLoading.collectAsStateWithLifecycle()
    val error by viewModel.officialWallpapersError.collectAsStateWithLifecycle()
    val saveState by viewModel.splashSaveState.collectAsStateWithLifecycle()

    var selectedUrl by remember { mutableStateOf<String?>(null) }
    var saveToGallery by remember { mutableStateOf(false) }
    var showSplashAdjustmentSheet by remember { mutableStateOf(false) }
    var isImportingWallpaper by remember { mutableStateOf(false) }
    var importedWallpaper by remember { mutableStateOf<File?>(null) }
    val importScope = rememberCoroutineScope()
    DisposableEffect(importedWallpaper) {
        val previewFile = importedWallpaper
        onDispose { previewFile?.delete() }
    }
    val initialSplashMobileBias by viewModel.getSplashAlignment(false).collectAsStateWithLifecycle(initialValue = 0f
        )
    val initialSplashTabletBias by viewModel.getSplashAlignment(true).collectAsStateWithLifecycle(initialValue = 0f
        )
    val customWallpaperPickerLauncher = rememberLauncherForActivityResult(
        contract = PickGalleryVisualMedia()
    ) { uri ->
        if (uri != null) {
            isImportingWallpaper = true
            showSplashAdjustmentSheet = false
            importScope.launch {
                try {
                    val file = if (target == WallpaperPickerTarget.HOME) importWallpaperMedia(
                        context, uri, File(context.cacheDir, "wallpaper_imports"),
                    ) else importWallpaperImage(
                        context = context,
                        source = uri,
                        destinationDirectory = File(context.cacheDir, "wallpaper_imports"),
                    )
                    importedWallpaper = file
                    selectedUrl = Uri.fromFile(file).toString()
                    saveToGallery = false
                    showSplashAdjustmentSheet = target == WallpaperPickerTarget.SPLASH
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Logger.w("SplashWallpaperPicker", "Unable to import selected wallpaper", error)
                    Toast.makeText(context, "无法导入壁纸，请确认文件已下载到本机且格式受支持", Toast.LENGTH_LONG).show()
                } finally {
                    isImportingWallpaper = false
                }
            }
        }
    }
    val openCustomWallpaperPicker = {
        if (!isImportingWallpaper) {
            customWallpaperPickerLauncher.launch(
                PickVisualMediaRequest(if (target == WallpaperPickerTarget.HOME) ActivityResultContracts.PickVisualMedia.ImageAndVideo else ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }
    val titleText = when (target) {
        WallpaperPickerTarget.SPLASH -> "选择开屏壁纸"
        WallpaperPickerTarget.HOME -> "选择首页壁纸"
    }
    val actionText = when (target) {
        WallpaperPickerTarget.SPLASH -> "设为开屏壁纸"
        WallpaperPickerTarget.HOME -> "设为首页壁纸"
    }

    // 初始化加载
    LaunchedEffect(Unit) {
        if (officialWallpapers.isEmpty()) {
            viewModel.loadOfficialWallpapers()
        }
    }
    LaunchedEffect(officialWallpapers) {
        val randomPool = resolveVisibleSplashWallpaperPool(officialWallpapers)
        SettingsManager.setSplashRandomPoolUris(context, randomPool)
    }

    AppModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
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
                    AppIconButton(onClick = onDismiss) {
                        AppIcon(clearIcon, contentDescription = "关闭")
                    }

                    AppText(
                        text = titleText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.size(48.dp))
                }
            }

            // 2. 内容区
            when {
                isLoading && officialWallpapers.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize().weight(1f),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AppOutlinedButton(onClick = openCustomWallpaperPicker) {
                            AppIcon(photoIcon, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            AppText("从相册选择")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        AdaptiveLoadingIndicator()
                    }
                }
                error != null && officialWallpapers.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize().weight(1f),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AppText(text = error ?: "加载失败", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))
                        AppOutlinedButton(onClick = openCustomWallpaperPicker) {
                            AppIcon(photoIcon, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            AppText("从相册选择")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        AppButton(onClick = { viewModel.loadOfficialWallpapers() }) {
                            AppText("重试")
                        }
                    }
                }
                officialWallpapers.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize().weight(1f),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AppText(text = "暂无官方壁纸", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))
                        AppOutlinedButton(onClick = openCustomWallpaperPicker) {
                            AppIcon(photoIcon, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            AppText("从相册选择")
                        }
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
                        item {
                            SplashCustomWallpaperTile(
                                isSelected = isUserSelectedSplashWallpaperUri(selectedUrl),
                                onClick = openCustomWallpaperPicker
                            )
                        }

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
                                            .clip(AppShapes.container(ContainerLevel.Chip))
                                            .border(
                                                width = if (isSelected) 2.dp else 0.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                shape = AppShapes.container(ContainerLevel.Chip)
                                            )
                                    )

                                    if (isSelected) {
                                        AppIcon(
                                            imageVector = rememberAppCheckCircleIcon(),
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

                                AppText(
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

            if (target == WallpaperPickerTarget.HOME && isUserSelectedSplashWallpaperUri(selectedUrl)) {
                com.android.purebilibili.core.ui.wallpaper.WallpaperMedia(
                    uri = selectedUrl.orEmpty(),
                    modifier = Modifier.fillMaxWidth().height(140.dp)
                        .clip(AppShapes.container(ContainerLevel.Card)),
                )
            }

            // 3. 底部操作栏
            AppSurface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val isSaving = saveState is WallpaperSaveState.Loading || isImportingWallpaper
                    val saveSelectedWallpaper = {
                        selectedUrl?.let { url ->
                            when (target) {
                                WallpaperPickerTarget.SPLASH -> {
                                    showSplashAdjustmentSheet = true
                                }

                                WallpaperPickerTarget.HOME -> {
                                    if (isUserSelectedSplashWallpaperUri(url)) {
                                        viewModel.setCustomHomeWallpaper(uri = url) {
                                            onDismiss()
                                            Toast.makeText(context, "首页壁纸设置成功", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        viewModel.setAsHomeWallpaper(
                                            url = url,
                                            saveToGallery = saveToGallery
                                        ) {
                                            onDismiss()
                                            Toast.makeText(context, "首页壁纸设置成功", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (showSplashAdjustmentSheet && selectedUrl != null) {
                        WallpaperAdjustmentSheet(
                            imageUri = normalizeSplashWallpaperUrl(selectedUrl),
                            initialMobileBias = initialSplashMobileBias,
                            initialTabletBias = initialSplashTabletBias,
                            onDismiss = { showSplashAdjustmentSheet = false },
                            onSave = { mBias, tBias ->
                                showSplashAdjustmentSheet = false
                                selectedUrl?.let { url ->
                                    if (isUserSelectedSplashWallpaperUri(url)) {
                                        viewModel.setCustomSplashWallpaper(
                                            uri = url,
                                            mobileBias = mBias,
                                            tabletBias = tBias
                                        ) {
                                            onDismiss()
                                            Toast.makeText(context, "自定义壁纸设置成功", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
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
                            }
                        )
                    }

                    // 保存到相册开关
                    AnimatedVisibility(
                        visible = !isUserSelectedSplashWallpaperUri(selectedUrl)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .clickable { saveToGallery = !saveToGallery },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            AppText(
                                text = "同时保存到相册",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            AppSwitch(
                                checked = saveToGallery,
                                onCheckedChange = { saveToGallery = it },
                                modifier = Modifier.scale(0.8f)
                            )
                        }
                    }

                    // 确认按钮
                    AppButton(
                        onClick = {
                            saveSelectedWallpaper()
                        },
                        enabled = selectedUrl != null && !isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = AppShapes.container(ContainerLevel.Floating)
                    ) {
                        if (isSaving) {
                            AppCircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            AppText(actionText, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (saveState is WallpaperSaveState.Error) {
                        AppText(
                            text = (saveState as WallpaperSaveState.Error).message,
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

@Composable
private fun SplashCustomWallpaperTile(
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val photoIcon = rememberAppPhotoIcon()
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(9f / 16f)
                .clip(AppShapes.container(ContainerLevel.Chip))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                    shape = AppShapes.container(ContainerLevel.Chip)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AppIcon(
                    photoIcon,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                AppText(
                    text = "相册",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                AppIcon(
                    imageVector = rememberAppCheckCircleIcon(),
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

        AppText(
            text = "从相册选择",
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
