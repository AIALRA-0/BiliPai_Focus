package com.android.purebilibili.feature.profile

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.purebilibili.core.store.SettingsManager
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
    val context = LocalContext.current
    val officialWallpapers by viewModel.officialWallpapers.collectAsStateWithLifecycle()
    val isLoading by viewModel.officialWallpapersLoading.collectAsStateWithLifecycle()
    val error by viewModel.officialWallpapersError.collectAsStateWithLifecycle()
    val saveState by viewModel.splashSaveState.collectAsStateWithLifecycle()

    var selectedUrl by remember { mutableStateOf<String?>(null) }
    var saveToGallery by remember { mutableStateOf(false) }
    var showSplashAdjustmentSheet by remember { mutableStateOf(false) }
    val initialSplashMobileBias by viewModel.getSplashAlignment(false).collectAsStateWithLifecycle(initialValue = 0f
        )
    val initialSplashTabletBias by viewModel.getSplashAlignment(true).collectAsStateWithLifecycle(initialValue = 0f
        )
    val customWallpaperPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            selectedUrl = uri.toString()
            saveToGallery = false
            showSplashAdjustmentSheet = target == WallpaperPickerTarget.SPLASH
        }
    }
    val openCustomWallpaperPicker = {
        customWallpaperPickerLauncher.launch(arrayOf("image/*"))
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

    ModalBottomSheet(
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
                    IconButton(onClick = onDismiss) {
                        Icon(CupertinoIcons.Default.Xmark, contentDescription = "关闭")
                    }

                    Text(
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
                        OutlinedButton(onClick = openCustomWallpaperPicker) {
                            Icon(CupertinoIcons.Default.Photo, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("从相册选择")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
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
                        OutlinedButton(onClick = openCustomWallpaperPicker) {
                            Icon(CupertinoIcons.Default.Photo, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("从相册选择")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.loadOfficialWallpapers() }) {
                            Text("重试")
                        }
                    }
                }
                officialWallpapers.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize().weight(1f),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "暂无官方壁纸", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(onClick = openCustomWallpaperPicker) {
                            Icon(CupertinoIcons.Default.Photo, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("从相册选择")
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
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(
                                                width = if (isSelected) 2.dp else 0.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                    )

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

            // 3. 底部操作栏
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val isSaving = saveState is WallpaperSaveState.Loading
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
                            Text(
                                text = "同时保存到相册",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Switch(
                                checked = saveToGallery,
                                onCheckedChange = { saveToGallery = it },
                                modifier = Modifier.scale(0.8f)
                            )
                        }
                    }

                    // 确认按钮
                    Button(
                        onClick = {
                            saveSelectedWallpaper()
                        },
                        enabled = selectedUrl != null && !isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(actionText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(9f / 16f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    CupertinoIcons.Default.Photo,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "相册",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

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
            text = "从相册选择",
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
