package com.android.purebilibili.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.core.ui.AppContentColors
import com.android.purebilibili.core.ui.components.AppListItem
import com.android.purebilibili.core.ui.components.AppPreferenceSectionTitle
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.AppTextButton
import com.android.purebilibili.feature.settings.ui.SettingsPageScaffold

@Composable
internal fun LoginActivitySettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    model: LoginActivityViewModel = viewModel(),
) {
    val identity by TokenManager.accountIdentity.collectAsStateWithLifecycle()
    val rawState by model.state.collectAsStateWithLifecycle()
    val state = rawState.takeIf { it.identity == identity } ?: LoginActivityUiState(identity = identity)
    LaunchedEffect(model) { model.refresh() }
    LoginActivitySettingsContent(state, model::refresh, model::loadDevices, model::loadRecords, onBack, modifier)
}

@Composable
internal fun LoginActivitySettingsContent(
    state: LoginActivityUiState,
    onRefresh: () -> Unit,
    onRetryDevices: () -> Unit,
    onRetryRecords: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val loggedIn = (state.identity.mid ?: 0L) > 0L
    SettingsPageScaffold(
        title = settingsDestinationCopy(SettingsSearchTarget.LOGIN_ACTIVITY).title,
        onBack = onBack,
        backContentDescription = "返回",
        bottomContentPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        modifier = modifier,
        actions = {
            AppTextButton(onClick = onRefresh, enabled = loggedIn && !state.devices.loading && !state.records.loading) {
                AppText("刷新")
            }
        },
        lazyListContent = {
            item(key = "account") {
                AppText(
                    if (loggedIn) "当前账号 UID ${state.identity.mid}，以下为 B 站返回的登录设备和近期登录记录。"
                    else "请先登录后查看登录设备与记录。",
                    modifier = Modifier.padding(16.dp),
                )
            }
            if (loggedIn) {
                loginActivitySection("devices", "登录设备", "暂无登录设备记录", state.devices, onRetryDevices) { device ->
                    AppListItem(
                        headlineContent = { AppText(device.deviceName?.takeIf { it.isNotBlank() } ?: "未知设备") },
                        overlineContent = if (device.isCurrentDevice == true) ({ AppText("本机", color = AppContentColors.secondaryText()) }) else null,
                        supportingContent = {
                            Column {
                                AppText("最近登录：${device.latestLoginAt?.takeIf { it.isNotBlank() } ?: "未知"}")
                                AppText("来源：${device.source?.takeIf { it.isNotBlank() } ?: "未知"}")
                            }
                        },
                    )
                }
                loginActivitySection("records", "登录记录", "暂无近期登录记录", state.records, onRetryRecords) { record ->
                    AppListItem(
                        headlineContent = { AppText(record.timeAt?.takeIf { it.isNotBlank() } ?: "登录时间未知") },
                        supportingContent = {
                            Column {
                                AppText("IP：${record.ip?.takeIf { it.isNotBlank() } ?: "未知"}")
                                AppText("地区：${record.geo?.takeIf { it.isNotBlank() } ?: "未知"}")
                            }
                        },
                    )
                }
            }
        },
    )
}

private fun <T> LazyListScope.loginActivitySection(
    key: String,
    title: String,
    emptyText: String,
    section: LoginActivitySection<T>,
    onRetry: () -> Unit,
    itemContent: @Composable (T) -> Unit,
) {
    item(key = "${key}_title") { AppPreferenceSectionTitle(title) }
    if (section.loading) {
        item(key = "${key}_loading") { AppText("正在加载…", Modifier.padding(16.dp)) }
    }
    section.error?.let { error ->
        item(key = "${key}_error") {
            Column(Modifier.padding(horizontal = 16.dp)) {
                AppText(if (section.items != null) "$error\n以下保留上次加载的记录。" else error)
                AppTextButton(onClick = onRetry, enabled = !section.loading) { AppText("重试") }
            }
        }
    }
    val items = section.items
    if (items != null) {
        if (items.isEmpty() && !section.loading && section.error == null) {
            item(key = "${key}_empty") { AppText(emptyText, Modifier.padding(16.dp)) }
        }
        itemsIndexed(items, key = { index, _ -> "${key}_$index" }) { _, value -> itemContent(value) }
    } else if (!section.loading && section.error == null) {
        item(key = "${key}_pending") { AppText("等待加载…", Modifier.padding(16.dp)) }
    }
}
