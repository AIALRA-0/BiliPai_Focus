package com.android.purebilibili.feature.live

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.core.ui.AppAlertDialog
import com.android.purebilibili.core.ui.AppContentColors
import com.android.purebilibili.core.ui.components.*
import com.android.purebilibili.data.model.response.*
import com.android.purebilibili.core.ui.components.AppLazyColumn as LazyColumn

@Composable
internal fun LiveMatchGradeDialog(
    contestId: Long,
    onDismiss: () -> Unit,
    onLiveClick: (Long, String, String) -> Unit,
    onVideoClick: (String) -> Unit,
    onLoginClick: () -> Unit,
    onCommentClick: (Long, Long) -> Unit,
) {
    val model: LiveMatchGradeViewModel = viewModel(key = "live-match-grade")
    val state by model.state.collectAsStateWithLifecycle()
    val account by TokenManager.accountIdentity.collectAsStateWithLifecycle()
    val comments by model.comments.collectAsStateWithLifecycle()
    var playerId by rememberSaveable(contestId, account) { mutableLongStateOf(0L) }
    var game by rememberSaveable(contestId) { mutableIntStateOf(1) }
    LaunchedEffect(model, contestId, game, account) { playerId = 0L; model.load(contestId, game) }
    DisposableEffect(model) { onDispose { model.stop() } }
    val visible = if (state.contestId == contestId && state.game == game && state.account == account) state else LiveMatchGradeState(contestId, game)
    val player = visible.data?.let { it.teams.ifEmpty { listOfNotNull(it.home, it.away) } }
        ?.flatMap { it.players }?.firstOrNull { it.playerId == playerId }
    if (player != null) {
        LaunchedEffect(player.gradeId, game, account) { model.showComments(player) }
        LiveMatchPlayerGradeSheet(
            player = player, state = visible,
            comments = comments.takeIf { it.gradeId == player.gradeId } ?: LiveMatchGradeComments(gradeId = player.gradeId, loading = true),
            onDismiss = { playerId = 0L; model.clearComments() },
            onSubmit = { score, message -> model.submit(player, score, message) },
            onLoadComments = { mode, more -> model.showComments(player, mode, more) },
            onLoginClick = { onDismiss(); onLoginClick() },
            onCommentClick = { root -> onDismiss(); onCommentClick(visible.data?.aid ?: 0, root) },
        )
    } else LiveMatchGradeContent(
        state = visible,
        onPlayerClick = { model.clearFeedback(); playerId = it.playerId },
        onDismiss = onDismiss,
        onGameSelected = { game = it },
        onRetry = { model.load(contestId, game) },
        onLiveClick = onLiveClick,
        onVideoClick = onVideoClick,
    )
}

@Composable
private fun LiveMatchGradeContent(
    state: LiveMatchGradeState,
    onDismiss: () -> Unit,
    onGameSelected: (Int) -> Unit,
    onPlayerClick: (LiveMatchPlayerGrade) -> Unit,
    onRetry: () -> Unit,
    onLiveClick: (Long, String, String) -> Unit,
    onVideoClick: (String) -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val teams = state.data?.let { data -> data.teams.ifEmpty { listOfNotNull(data.home, data.away) } }.orEmpty()
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { AppText("选手评分 · 第 ${state.game} 局") },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                com.android.purebilibili.core.ui.components.AppGlassMenuAnchor(onOpen = { menu = true }, enabled = !state.loading && !state.submitting && state.availableGames > 1) {
                    AppTextButton(enabled = !state.loading && !state.submitting && state.availableGames > 1, onClick = { menu = true }) { AppText("切换局次") }
                    AppDropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        (1..state.availableGames.coerceIn(1, 99)).forEach { index ->
                            AppDropdownMenuItem(text = { AppText("第 $index 局") }, onClick = { menu = false; onGameSelected(index) })
                        }
                    }
                }
                when {
                    state.loading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { AppCircularProgressIndicator() }
                    state.error != null -> Column {
                        AppText(state.error, color = AppContentColors.secondaryText())
                        AppTextButton(onClick = onRetry) { AppText("重试") }
                    }
                    teams.all { it.players.isEmpty() } -> AppText("该场比赛暂未提供本局选手评分。", color = AppContentColors.secondaryText())
                    else -> LazyColumn(Modifier.heightIn(max = 440.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        teams.forEachIndexed { index, team ->
                            item(key = "team:$index") { AppText(team.name.ifBlank { "队伍" }, fontWeight = FontWeight.SemiBold) }
                            items(team.players) { player -> LiveMatchPlayerGradeRow(player, Modifier.fillMaxWidth(), onClick = { onPlayerClick(player) }) }
                        }
                    }
                }
                val data = state.data
                val bvid = data?.replayUrl?.let(::liveMatchPlaybackBvid)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (bvid != null) AppTextButton(onClick = { onDismiss(); onVideoClick(bvid) }) { AppText("本局回放") }
                    if (data != null && data.liveRoom > 0) AppTextButton(onClick = { onDismiss(); onLiveClick(data.liveRoom, "赛事直播", "") }) { AppText("直播间") }
                }
            }
        },
        confirmButton = { AppTextButton(onClick = onDismiss) { AppText("关闭") } },
    )
}

@Composable
private fun LiveMatchPlayerGradeRow(player: LiveMatchPlayerGrade, modifier: Modifier = Modifier, onClick: () -> Unit) {
    AppCard(modifier = modifier) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AsyncImage(model = liveMatchImageUrl(player.portrait), contentDescription = null, modifier = Modifier.size(36.dp))
                Column(Modifier.weight(1f)) {
                    AppText(player.nickname.ifBlank { "选手" }, fontWeight = FontWeight.SemiBold)
                    if (player.position.isNotBlank()) AppText(player.position, color = AppContentColors.secondaryText())
                }
                AppText(liveMatchGradeLabel(player.average, player.users), color = AppContentColors.accent())
            }
            AppText("${player.users?.let { "${it}人评分" } ?: "评分人数未知"} · K/D/A ${player.kill ?: "—"}/${player.death ?: "—"}/${player.assist ?: "—"}", color = AppContentColors.secondaryText())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                AppTextButton(enabled = player.playerId > 0, onClick = onClick) {
                    AppText(if (player.rated) "我的评分 ${player.userScore ?: "—"} · 评论" else "评分与评论")
                }
            }
        }
    }
}
