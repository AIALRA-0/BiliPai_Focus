package com.android.purebilibili.feature.live

import android.app.Application
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.ui.LocalAnimatedVisibilityScope
import com.android.purebilibili.core.ui.LocalSharedTransitionScope
import com.android.purebilibili.core.ui.resolveBottomSafeAreaPadding
import com.android.purebilibili.core.util.LocalWindowSizeClass
import com.android.purebilibili.core.util.responsiveContentWidth
import com.android.purebilibili.data.model.response.LiveAreaParent
import com.android.purebilibili.data.repository.LiveRepository
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LiveRoomItem(
    val roomId: Long,
    val title: String,
    val cover: String,
    val uname: String,
    val face: String,
    val online: Int,
    val areaName: String,
    val liveStatus: Int = 1
)

data class LiveListUiState(
    val recommendItems: List<LiveRoomItem> = emptyList(),
    val followItems: List<LiveRoomItem> = emptyList(),
    val areaList: List<LiveAreaParent> = emptyList(),
    val selectedAreaId: Int = 0,
    val areaItems: List<LiveRoomItem> = emptyList(),
    val isLoading: Boolean = false,
    val isAreaLoading: Boolean = false,
    val error: String? = null,
    val currentTab: Int = 0,
    val livingCount: Int = 0
)

class LiveListViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(LiveListUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val recommendJob = launch { loadRecommendLive() }
                val areaJob = launch { loadAreaList() }
                val followJob = launch { loadFollowLive() }

                recommendJob.join()
                areaJob.join()
                followJob.join()

                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "加载失败"
                )
            }
        }
    }

    private suspend fun loadRecommendLive() {
        try {
            val response = NetworkModule.api.getLiveList(parentAreaId = 0, page = 1, pageSize = 30)
            if (response.code == 0 && response.data != null) {
                val items = response.data.getAllRooms().map { room ->
                    LiveRoomItem(
                        roomId = room.roomid,
                        title = room.title,
                        cover = room.cover.ifEmpty { room.userCover.ifEmpty { room.keyframe } },
                        uname = room.uname,
                        face = room.face,
                        online = room.online,
                        areaName = room.areaName
                    )
                }
                _uiState.value = _uiState.value.copy(recommendItems = items)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun loadAreaList() {
        try {
            val response = NetworkModule.api.getLiveAreaList()
            if (response.code == 0 && response.data != null) {
                _uiState.value = _uiState.value.copy(areaList = response.data)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadFollowLive() {
        viewModelScope.launch {
            try {
                LiveRepository.getFollowedLive(page = 1).onSuccess { rooms ->
                    val items = rooms.map { room ->
                        LiveRoomItem(
                            roomId = room.roomid,
                            title = room.title,
                            cover = room.cover.ifEmpty { room.userCover.ifEmpty { room.keyframe } },
                            uname = room.uname,
                            face = room.face,
                            online = room.online,
                            areaName = room.areaName,
                            liveStatus = 1
                        )
                    }
                    _uiState.value = _uiState.value.copy(
                        followItems = items,
                        livingCount = items.size
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadAreaLive(parentAreaId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isAreaLoading = true,
                selectedAreaId = parentAreaId
            )
            try {
                val response = NetworkModule.api.getLiveList(
                    parentAreaId = parentAreaId,
                    page = 1,
                    pageSize = 30
                )
                if (response.code == 0 && response.data != null) {
                    val items = response.data.getAllRooms().map { room ->
                        LiveRoomItem(
                            roomId = room.roomid,
                            title = room.title,
                            cover = room.cover.ifEmpty { room.userCover.ifEmpty { room.keyframe } },
                            uname = room.uname,
                            face = room.face,
                            online = room.online,
                            areaName = room.areaName
                        )
                    }
                    _uiState.value = _uiState.value.copy(
                        areaItems = items,
                        isAreaLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isAreaLoading = false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(isAreaLoading = false)
            }
        }
    }

    fun openArea(parentAreaId: Int) {
        _uiState.value = _uiState.value.copy(currentTab = 1)
        loadAreaLive(parentAreaId)
    }

    fun selectHomeArea(areaId: Int) {
        if (areaId == 0) {
            _uiState.value = _uiState.value.copy(selectedAreaId = 0, areaItems = emptyList())
            return
        }
        loadAreaLive(areaId)
    }

    fun setTab(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(currentTab = tabIndex)
        if (tabIndex == 2 && _uiState.value.followItems.isEmpty()) {
            loadFollowLive()
        }
        if (tabIndex == 1 && _uiState.value.areaList.isNotEmpty() && _uiState.value.selectedAreaId == 0) {
            loadAreaLive(_uiState.value.areaList.first().id)
        }
    }

    fun refresh() {
        loadInitialData()
        if (_uiState.value.currentTab == 1 && _uiState.value.selectedAreaId != 0) {
            loadAreaLive(_uiState.value.selectedAreaId)
        }
    }
}

@Composable
fun LiveListScreen(
    onBack: () -> Unit,
    onLiveClick: (Long, String, String) -> Unit,
    onSearchClick: () -> Unit,
    onAreaListClick: () -> Unit,
    onFollowingClick: () -> Unit,
    onAreaDetailClick: (Int, Int, String) -> Unit,
    viewModel: LiveListViewModel = viewModel(),
    globalHazeState: HazeState? = null
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val palette = rememberLiveChromePalette()

    DisposableEffect(Unit) {
        val window = (context as? android.app.Activity)?.window
        val originalNavBarColor = window?.navigationBarColor
        if (window != null) {
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
        }
        onDispose {
            if (window != null && originalNavBarColor != null) {
                window.navigationBarColor = originalNavBarColor
            }
        }
    }

    val windowSizeClass = LocalWindowSizeClass.current
    val metrics = resolveLivePiliPlusHomeMetrics()
    val contentWidth = if (windowSizeClass.isExpandedScreen) {
        minOf(windowSizeClass.widthDp, 1100.dp)
    } else {
        windowSizeClass.widthDp
    }
    val gridColumns = remember(contentWidth) {
        resolveLivePiliPlusGridColumns(contentWidth.value.toInt(), windowSizeClass.isExpandedScreen)
    }
    val gridBottomPadding = resolveBottomSafeAreaPadding(
        navigationBarsBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        extraBottomPadding = 100.dp
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.backgroundBrush())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .responsiveContentWidth(maxWidth = 1100.dp)
        ) {
            LiveListHeader(
                metrics = metrics,
                livingCount = state.livingCount,
                primaryFace = state.followItems.firstOrNull()?.face.orEmpty(),
                onBack = onBack,
                onSearchClick = onSearchClick,
                onInboxClick = onFollowingClick,
                onAvatarClick = onAreaListClick
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                when {
                    state.isLoading -> {
                        LiveListLoadingState()
                    }
                    state.error != null -> {
                        LiveListErrorState(
                            message = state.error ?: "未知错误",
                            onRetry = viewModel::refresh
                        )
                    }
                    else -> {
                        LiveHomeContent(
                            recommendItems = state.recommendItems,
                            followItems = state.followItems,
                            areaList = state.areaList,
                            selectedAreaId = state.selectedAreaId,
                            areaItems = state.areaItems,
                            livingCount = state.livingCount,
                            isAreaLoading = state.isAreaLoading,
                            gridColumns = gridColumns,
                            bottomPadding = gridBottomPadding,
                            metrics = metrics,
                            onLiveClick = onLiveClick,
                            onAreaSelected = viewModel::selectHomeArea,
                            onAreaDetailClick = onAreaDetailClick,
                            onAreaListClick = onAreaListClick,
                            onFollowingClick = onFollowingClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveHomeContent(
    recommendItems: List<LiveRoomItem>,
    followItems: List<LiveRoomItem>,
    areaList: List<LiveAreaParent>,
    selectedAreaId: Int,
    areaItems: List<LiveRoomItem>,
    livingCount: Int,
    isAreaLoading: Boolean,
    gridColumns: Int,
    bottomPadding: androidx.compose.ui.unit.Dp,
    metrics: LivePiliPlusHomeMetrics,
    onLiveClick: (Long, String, String) -> Unit,
    onAreaSelected: (Int) -> Unit,
    onAreaDetailClick: (Int, Int, String) -> Unit,
    onAreaListClick: () -> Unit,
    onFollowingClick: () -> Unit
) {
    val selectedArea = areaList.firstOrNull { it.id == selectedAreaId }
    val contentItems = if (selectedAreaId == 0) recommendItems else areaItems

    LazyVerticalGrid(
        columns = GridCells.Fixed(gridColumns),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = metrics.safeSpaceDp.dp,
            end = metrics.safeSpaceDp.dp,
            top = metrics.cardSpaceDp.dp,
            bottom = bottomPadding
        ),
        horizontalArrangement = Arrangement.spacedBy(metrics.cardSpaceDp.dp),
        verticalArrangement = Arrangement.spacedBy(metrics.cardSpaceDp.dp)
    ) {
        if (followItems.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                LiveFollowHeader(
                    livingCount = livingCount,
                    onActionClick = onFollowingClick
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                LiveFollowAvatarRow(
                    items = followItems.take(10),
                    metrics = metrics,
                    onLiveClick = onLiveClick
                )
            }
        }
        if (areaList.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                LiveAreaHomeChipRow(
                    areaList = areaList,
                    selectedAreaId = selectedAreaId,
                    onAreaSelected = onAreaSelected
                )
            }
            if (!selectedArea?.list.isNullOrEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LiveAreaChildChipRow(
                        items = selectedArea.list.orEmpty(),
                        parentAreaId = selectedAreaId,
                        onAreaDetailClick = onAreaDetailClick
                    )
                }
            }
        }
        when {
            isAreaLoading -> item(span = { GridItemSpan(maxLineSpan) }) { LiveListLoadingState() }
            contentItems.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) { EmptyState("暂无直播内容") }
            else -> items(contentItems, key = { it.roomId }) { item ->
                LiveRoomCard(
                    item = item,
                    onClick = { onLiveClick(item.roomId, item.title, item.uname) }
                )
            }
        }
    }
}

@Composable
private fun LiveListHeader(
    metrics: LivePiliPlusHomeMetrics,
    livingCount: Int,
    primaryFace: String,
    onBack: () -> Unit,
    onSearchClick: () -> Unit,
    onInboxClick: () -> Unit,
    onAvatarClick: () -> Unit
) {
    val palette = rememberLiveChromePalette()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = metrics.safeSpaceDp.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                onClick = onBack,
                color = Color.Transparent,
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "返回",
                        tint = palette.primaryText
                    )
                }
            }
            Surface(
                onClick = onSearchClick,
                color = palette.searchField,
                shape = RoundedCornerShape(32.dp),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = palette.secondaryText
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "搜索直播间 / 主播",
                        color = palette.secondaryText,
                        fontSize = 14.sp
                    )
                }
            }
            Box {
                Surface(
                    onClick = onInboxClick,
                    color = Color.Transparent,
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsNone,
                            contentDescription = "开播提醒",
                            tint = palette.primaryText
                        )
                    }
                }
                if (livingCount > 0) {
                    Badge(
                        containerColor = palette.accentStrong,
                        contentColor = palette.onAccent,
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Text(
                            text = if (livingCount > 99) "99+" else livingCount.toString(),
                            fontSize = 10.sp
                        )
                    }
                }
            }
            Surface(
                onClick = onAvatarClick,
                color = palette.surfaceMuted,
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                if (primaryFace.isNotBlank()) {
                    AsyncImage(
                        model = primaryFace,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "LIVE",
                            color = palette.primaryText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveFollowHeader(
    livingCount: Int,
    onActionClick: () -> Unit
) {
    val palette = rememberLiveChromePalette()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.Bottom) {
            Text(
                text = "我的关注  ",
                color = palette.primaryText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = livingCount.toString(),
                color = palette.accentStrong,
                fontSize = 13.sp
            )
            Text(
                text = " 人正在直播",
                color = palette.secondaryText,
                fontSize = 13.sp
            )
        }
        Row(
            modifier = Modifier.clickable(onClick = onActionClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "查看更多",
                color = palette.secondaryText,
                fontSize = 14.sp
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = ">",
                color = palette.secondaryText,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun LiveFollowAvatarRow(
    items: List<LiveRoomItem>,
    metrics: LivePiliPlusHomeMetrics,
    onLiveClick: (Long, String, String) -> Unit
) {
    val palette = rememberLiveChromePalette()
    LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        items(items, key = { it.roomId }) { item ->
            Column(
                modifier = Modifier
                    .width(metrics.followItemExtentDp.dp)
                    .clickable { onLiveClick(item.roomId, item.title, item.uname) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size((metrics.followAvatarSizeDp + 5).dp)
                            .clip(CircleShape)
                            .background(palette.accentStrong)
                            .padding(2.dp)
                    ) {
                        AsyncImage(
                            model = item.face.ifBlank { item.cover },
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(palette.surface)
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = item.uname,
                    color = palette.primaryText,
                    fontSize = 12.sp,
                    lineHeight = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun LiveAreaHomeChipRow(
    areaList: List<LiveAreaParent>,
    selectedAreaId: Int,
    onAreaSelected: (Int) -> Unit
) {
    val palette = rememberLiveChromePalette()
    val colorScheme = MaterialTheme.colorScheme
    val chipColors = resolveLivePiliPlusChipColors(
        selectedContainer = colorScheme.secondaryContainer,
        selectedContent = colorScheme.onSecondaryContainer,
        unselectedContent = colorScheme.onSurfaceVariant
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            val selected = selectedAreaId == 0
            Surface(
                onClick = { onAreaSelected(0) },
                color = if (selected) chipColors.selectedContainerColor else chipColors.unselectedContainerColor,
                shape = RoundedCornerShape(999.dp),
                border = null
            ) {
                Text(
                    text = "推荐",
                    color = if (selected) chipColors.selectedContentColor else chipColors.unselectedContentColor,
                    fontSize = 14.sp,
                    lineHeight = 14.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
        }
        items(areaList, key = { it.id }) { area ->
            val selected = area.id == selectedAreaId
            Surface(
                onClick = { onAreaSelected(area.id) },
                color = if (selected) chipColors.selectedContainerColor else chipColors.unselectedContainerColor,
                shape = RoundedCornerShape(999.dp),
                border = null
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = area.name,
                        color = if (selected) chipColors.selectedContentColor else chipColors.unselectedContentColor,
                        fontSize = 14.sp,
                        lineHeight = 14.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveAreaChildChipRow(
    items: List<com.android.purebilibili.data.model.response.LiveAreaChild>,
    parentAreaId: Int,
    onAreaDetailClick: (Int, Int, String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val chipColors = resolveLivePiliPlusChipColors(
        selectedContainer = colorScheme.secondaryContainer,
        selectedContent = colorScheme.onSecondaryContainer,
        unselectedContent = colorScheme.onSurfaceVariant
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items, key = { it.id }) { child ->
            Surface(
                onClick = {
                    onAreaDetailClick(
                        parentAreaId,
                        child.id.toIntOrNull() ?: 0,
                        child.name
                    )
                },
                color = chipColors.unselectedContainerColor,
                shape = RoundedCornerShape(999.dp),
                border = null
            ) {
                Text(
                    text = child.name,
                    color = chipColors.unselectedContentColor,
                    fontSize = 13.sp,
                    lineHeight = 13.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun LiveListLoadingState() {
    val palette = rememberLiveChromePalette()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "直播内容加载中…",
            color = palette.secondaryText,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun LiveListErrorState(
    message: String,
    onRetry: () -> Unit
) {
    val palette = rememberLiveChromePalette()
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            color = palette.primaryText,
            fontSize = 15.sp
        )
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onRetry) {
            Text("重试")
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    val palette = rememberLiveChromePalette()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(palette.surfaceMuted),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.NotificationsNone,
                contentDescription = null,
                tint = palette.secondaryText,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = message,
            color = palette.secondaryText,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun LiveRoomCard(
    item: LiveRoomItem,
    onClick: () -> Unit
) {
    val palette = rememberLiveChromePalette()
    val metrics = resolveLivePiliPlusHomeMetrics()
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(metrics.cardRadiusDp.dp),
        color = palette.surfaceElevated,
        border = null,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(metrics.coverAspectRatio)
                    .then(
                        if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                            with(sharedTransitionScope) {
                                Modifier.sharedElement(
                                    sharedContentState = rememberSharedContentState(key = "live_cover_${item.roomId}"),
                                    animatedVisibilityScope = animatedVisibilityScope
                                )
                            }
                        } else {
                            Modifier
                        }
                    )
            ) {
                AsyncImage(
                    model = item.cover.ifBlank { item.face },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    palette.scrim.copy(alpha = if (palette.isDark) 0.28f else 0.18f),
                                    palette.scrim
                                )
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(start = 10.dp, end = 10.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.areaName.ifBlank { "直播间" },
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${formatLiveViewerCount(item.online)}人看过",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Column(
                modifier = Modifier
                    .height(90.dp)
                    .padding(start = 5.dp, top = 8.dp, end = 5.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.title,
                    color = palette.primaryText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 22.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.uname,
                    color = palette.secondaryText,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
