package com.android.purebilibili.feature.live

import android.app.Application
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.outlined.ArrowOutward
import androidx.compose.material.icons.outlined.GridView
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
    val contentWidth = if (windowSizeClass.isExpandedScreen) {
        minOf(windowSizeClass.widthDp, 1100.dp)
    } else {
        windowSizeClass.widthDp
    }
    val gridColumns = remember(contentWidth) {
        if (windowSizeClass.isExpandedScreen) {
            (contentWidth / 220.dp).toInt().coerceIn(2, 5)
        } else {
            2
        }
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
                livingCount = state.livingCount,
                primaryFace = state.followItems.firstOrNull()?.face.orEmpty(),
                onBack = onBack,
                onSearchClick = {
                    Toast.makeText(context, "直播搜索即将接入", Toast.LENGTH_SHORT).show()
                },
                onInboxClick = { viewModel.setTab(2) },
                onAvatarClick = {
                    Toast.makeText(context, "直播功能页暂未接入个人入口", Toast.LENGTH_SHORT).show()
                }
            )
            LiveListPrimaryTabs(
                selectedTabIndex = state.currentTab,
                titles = listOf("推荐", "分区", "关注"),
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
                onTabSelected = viewModel::setTab
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
                        AnimatedContent(
                            targetState = state.currentTab,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "live_tab"
                        ) { tab ->
                            when (tab) {
                                0 -> RecommendTab(
                                    items = state.recommendItems,
                                    followItems = state.followItems,
                                    areaList = state.areaList,
                                    selectedAreaId = state.selectedAreaId,
                                    livingCount = state.livingCount,
                                    gridColumns = gridColumns,
                                    bottomPadding = gridBottomPadding,
                                    onLiveClick = onLiveClick,
                                    onJumpToArea = viewModel::openArea
                                )
                                1 -> AreaTab(
                                    areaList = state.areaList,
                                    selectedAreaId = state.selectedAreaId,
                                    areaItems = state.areaItems,
                                    isLoading = state.isAreaLoading,
                                    gridColumns = gridColumns,
                                    bottomPadding = gridBottomPadding,
                                    onAreaSelected = viewModel::loadAreaLive,
                                    onLiveClick = onLiveClick
                                )
                                else -> FollowTab(
                                    items = state.followItems,
                                    livingCount = state.livingCount,
                                    gridColumns = gridColumns,
                                    bottomPadding = gridBottomPadding,
                                    onLiveClick = onLiveClick
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveListHeader(
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
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                onClick = onBack,
                color = palette.surfaceMuted,
                shape = CircleShape,
                modifier = Modifier.size(42.dp)
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
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
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
                    color = palette.surfaceMuted,
                    shape = CircleShape,
                    modifier = Modifier.size(42.dp)
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
                modifier = Modifier.size(42.dp)
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
        Spacer(Modifier.height(14.dp))
        Text(
            text = "直播",
            color = palette.primaryText,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (livingCount > 0) {
                "我的关注里有 $livingCount 位主播正在直播"
            } else {
                "探索正在开播的直播间"
            },
            color = palette.secondaryText,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun LiveListPrimaryTabs(
    selectedTabIndex: Int,
    titles: List<String>,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = rememberLiveChromePalette()
    val colors = resolveLiveListTabColors(
        primary = palette.accent,
        onPrimary = palette.primaryText,
        surfaceVariant = palette.surfaceMuted,
        onSurfaceVariant = palette.secondaryText
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        titles.forEachIndexed { index, title ->
            val selected = index == selectedTabIndex
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onTabSelected(index) }
                    .padding(bottom = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    color = if (selected) colors.selectedContentColor else colors.unselectedContentColor,
                    fontSize = 18.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .height(4.dp)
                        .width(if (selected) 26.dp else 10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (selected) colors.selectedContainerColor else Color.Transparent)
                )
            }
        }
    }
}

@Composable
private fun RecommendTab(
    items: List<LiveRoomItem>,
    followItems: List<LiveRoomItem>,
    areaList: List<LiveAreaParent>,
    selectedAreaId: Int,
    livingCount: Int,
    gridColumns: Int,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onLiveClick: (Long, String, String) -> Unit,
    onJumpToArea: (Int) -> Unit
) {
    val palette = rememberLiveChromePalette()
    LazyVerticalGrid(
        columns = GridCells.Fixed(gridColumns),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = bottomPadding),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (followItems.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                LiveSectionHeader(
                    title = "我的关注",
                    subtitle = if (livingCount > 0) "$livingCount 人正在直播" else "关注开播动态"
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                LiveFollowAvatarRow(
                    items = followItems.take(10),
                    onLiveClick = onLiveClick
                )
            }
        }
        if (areaList.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                LiveSectionHeader(
                    title = "热门分区",
                    subtitle = "快速切到分区页",
                    actionLabel = "全部",
                    onActionClick = {
                        areaList.firstOrNull()?.let { onJumpToArea(it.id) }
                    }
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                LiveAreaChipRow(
                    areaList = areaList.take(10),
                    selectedAreaId = selectedAreaId,
                    onAreaSelected = onJumpToArea
                )
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            LiveSectionHeader(
                title = "推荐直播",
                subtitle = if (items.isNotEmpty()) "精选 ${items.size} 个直播间" else "暂无推荐内容"
            )
        }
        if (items.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyState("暂无推荐直播")
            }
        } else {
            items(items, key = { it.roomId }) { item ->
                LiveRoomCard(
                    item = item,
                    accentBrush = Brush.horizontalGradient(
                        colors = listOf(palette.accentStrong, palette.accent)
                    ),
                    onClick = { onLiveClick(item.roomId, item.title, item.uname) }
                )
            }
        }
    }
}

@Composable
private fun AreaTab(
    areaList: List<LiveAreaParent>,
    selectedAreaId: Int,
    areaItems: List<LiveRoomItem>,
    isLoading: Boolean,
    gridColumns: Int,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onAreaSelected: (Int) -> Unit,
    onLiveClick: (Long, String, String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(gridColumns),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = bottomPadding),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            LiveSectionHeader(
                title = "分区筛选",
                subtitle = if (selectedAreaId == 0) "选择一个分区" else "切换不同直播分区"
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            LiveAreaChipRow(
                areaList = areaList,
                selectedAreaId = selectedAreaId,
                onAreaSelected = onAreaSelected
            )
        }
        when {
            isLoading -> {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LiveListLoadingState()
                }
            }
            areaItems.isEmpty() -> {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState("该分区暂无直播")
                }
            }
            else -> {
                items(areaItems, key = { it.roomId }) { item ->
                    LiveRoomCard(
                        item = item,
                        onClick = { onLiveClick(item.roomId, item.title, item.uname) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FollowTab(
    items: List<LiveRoomItem>,
    livingCount: Int,
    gridColumns: Int,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onLiveClick: (Long, String, String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(gridColumns),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = bottomPadding),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            LiveSectionHeader(
                title = "开播提醒",
                subtitle = if (livingCount > 0) "$livingCount 位主播在线" else "关注的主播开播后会出现在这里"
            )
        }
        if (items.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                LiveFollowAvatarRow(
                    items = items.take(12),
                    onLiveClick = onLiveClick
                )
            }
            items(items, key = { it.roomId }) { item ->
                LiveRoomCard(
                    item = item,
                    onClick = { onLiveClick(item.roomId, item.title, item.uname) }
                )
            }
        } else {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyState("暂无关注的直播")
            }
        }
    }
}

@Composable
private fun LiveSectionHeader(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    val palette = rememberLiveChromePalette()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = palette.primaryText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = palette.secondaryText,
                fontSize = 13.sp
            )
        }
        if (actionLabel != null && onActionClick != null) {
            Surface(
                onClick = onActionClick,
                color = palette.surfaceMuted,
                shape = RoundedCornerShape(999.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = actionLabel,
                        color = palette.primaryText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Outlined.ArrowOutward,
                        contentDescription = null,
                        tint = palette.secondaryText,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveFollowAvatarRow(
    items: List<LiveRoomItem>,
    onLiveClick: (Long, String, String) -> Unit
) {
    val palette = rememberLiveChromePalette()
    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(items, key = { it.roomId }) { item ->
            Column(
                modifier = Modifier
                    .width(74.dp)
                    .clickable { onLiveClick(item.roomId, item.title, item.uname) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(palette.accentStrong, palette.accent)
                                )
                            )
                            .padding(3.dp)
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
                    Surface(
                        color = palette.accentStrong,
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.padding(2.dp)
                    ) {
                        Text(
                            text = "LIVE",
                            color = palette.onAccent,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = item.uname,
                    color = palette.primaryText,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun LiveAreaChipRow(
    areaList: List<LiveAreaParent>,
    selectedAreaId: Int,
    onAreaSelected: (Int) -> Unit
) {
    val palette = rememberLiveChromePalette()
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(areaList, key = { it.id }) { area ->
            val selected = area.id == selectedAreaId
            Surface(
                onClick = { onAreaSelected(area.id) },
                color = if (selected) palette.accentSoft else palette.surfaceMuted,
                shape = RoundedCornerShape(999.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (selected) palette.accent.copy(alpha = 0.45f) else palette.border
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selected) {
                        Icon(
                            imageVector = Icons.Outlined.GridView,
                            contentDescription = null,
                            tint = palette.accent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = area.name,
                        color = if (selected) palette.accentStrong else palette.primaryText,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
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
    accentBrush: Brush? = null,
    onClick: () -> Unit
) {
    val palette = rememberLiveChromePalette()
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = palette.surfaceElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, palette.border),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 238.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
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
                Surface(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                accentBrush ?: Brush.horizontalGradient(
                                    listOf(palette.accentStrong, palette.accent)
                                ),
                                RoundedCornerShape(999.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "LIVE",
                            color = palette.onAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.4.sp
                        )
                    }
                }
                if (item.areaName.isNotBlank()) {
                    Surface(
                        color = palette.scrim.copy(alpha = if (palette.isDark) 0.52f else 0.42f),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = item.areaName,
                            color = Color.White,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.areaName.ifBlank { "直播间" },
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${formatLiveViewerCount(item.online)}人气",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = item.title,
                    color = palette.primaryText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 24.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = item.face.ifBlank { item.cover },
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(palette.surfaceMuted)
                    )
                    Spacer(Modifier.width(8.dp))
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
}
