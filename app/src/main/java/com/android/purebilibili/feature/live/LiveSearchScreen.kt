package com.android.purebilibili.feature.live

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.android.purebilibili.data.model.response.LiveRoomSearchItem
import com.android.purebilibili.data.model.response.SearchUpItem
import com.android.purebilibili.data.repository.SearchRepository
import com.android.purebilibili.data.repository.SearchLiveOrder
import kotlinx.coroutines.launch

@Composable
fun LiveSearchScreen(
    onBack: () -> Unit,
    onLiveClick: (Long, String, String) -> Unit,
    onUserClick: (Long) -> Unit
) {
    val palette = rememberLiveChromePalette()
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var hasSubmitted by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val liveResults = remember { mutableStateListOf<LiveRoomSearchItem>() }
    val userResults = remember { mutableStateListOf<SearchUpItem>() }

    suspend fun submit() {
        val normalized = query.trim()
        if (normalized.isEmpty()) return
        if (normalized.all { it.isDigit() }) {
            onLiveClick(normalized.toLong(), "", "")
            return
        }
        keyboard?.hide()
        hasSubmitted = true
        isLoading = true
        error = null
        liveResults.clear()
        userResults.clear()
        SearchRepository.searchLive(
            keyword = normalized,
            page = 1,
            order = SearchLiveOrder.ONLINE
        ).onSuccess { (rooms, _) ->
            liveResults.addAll(rooms)
        }.onFailure { err ->
            error = err.message ?: "直播搜索失败"
        }
        SearchRepository.searchUp(
            keyword = normalized,
            page = 1
        ).onSuccess { (ups, _) ->
            userResults.addAll(ups)
        }.onFailure { err ->
            if (error == null) error = err.message ?: "主播搜索失败"
        }
        isLoading = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.backgroundBrush())
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "返回",
                        tint = palette.primaryText
                    )
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        if (it.isBlank()) {
                            hasSubmitted = false
                            error = null
                            liveResults.clear()
                            userResults.clear()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("搜索房间或主播") },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null
                        )
                    },
                    shape = RoundedCornerShape(20.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        scope.launch { submit() }
                    })
                )
                Spacer(modifier = Modifier.size(8.dp))
                Surface(
                    onClick = {
                        scope.launch { submit() }
                    },
                    color = palette.surfaceMuted,
                    shape = CircleShape,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "搜",
                            color = palette.primaryText,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (!hasSubmitted) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "输入关键词后搜索直播间或主播",
                        color = palette.secondaryText,
                        fontSize = 14.sp
                    )
                }
            } else {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("正在直播 ${if (liveResults.isNotEmpty()) liveResults.size else ""}") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("主播 ${if (userResults.isNotEmpty()) userResults.size else ""}") }
                    )
                }
                when {
                    isLoading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    error != null -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = error ?: "", color = palette.secondaryText)
                        }
                    }
                    selectedTab == 0 -> {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(liveResults, key = { it.roomid }) { item ->
                                LiveSearchRoomCard(
                                    item = item,
                                    onClick = { onLiveClick(item.roomid, item.title, item.uname) }
                                )
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(userResults, key = { it.mid }) { item ->
                                LiveSearchUserCard(item = item, onClick = { onUserClick(item.mid) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveSearchRoomCard(
    item: LiveRoomSearchItem,
    onClick: () -> Unit
) {
    val palette = rememberLiveChromePalette()
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = palette.surfaceElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, palette.border)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = item.cover.ifBlank { item.uface },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
                Surface(
                    color = palette.scrim.copy(alpha = 0.56f),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                ) {
                    Text(
                        text = item.area_v2_name.ifBlank { "直播" },
                        color = androidx.compose.ui.graphics.Color.White,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = item.title,
                    color = palette.primaryText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${item.uname} · ${item.online}在线",
                    color = palette.secondaryText,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun LiveSearchUserCard(
    item: SearchUpItem,
    onClick: () -> Unit
) {
    val palette = rememberLiveChromePalette()
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = palette.surfaceElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, palette.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.upic,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(54.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            )
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.uname,
                    color = palette.primaryText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.usign.ifBlank { "${item.fans} 粉丝 · ${item.videos} 投稿" },
                    color = palette.secondaryText,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
