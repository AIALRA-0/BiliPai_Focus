package com.android.purebilibili.feature.settings

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
internal fun SettingsSectionFocusEffect(
    listState: LazyListState,
    target: SettingsSearchTarget,
    sectionKeys: List<String>,
    legacyKeys: Map<String, String> = emptyMap(),
) {
    val request by SettingsSearchFocusController.request.collectAsStateWithLifecycle()
    LaunchedEffect(request?.token, target, sectionKeys) {
        val current = request ?: return@LaunchedEffect
        if (current.target != target) return@LaunchedEffect
        val sectionKey = resolveSettingsItemScrollKey(current.settingId)
            ?: legacyKeys[current.focusId] ?: current.focusId
        val index = sectionKeys.indexOf(sectionKey)
        if (index < 0) {
            SettingsSearchFocusController.clear(current.token)
            return@LaunchedEffect
        }
        if (current.settingId == null) {
            listState.animateScrollToItem(index)
            SettingsSearchFocusController.clear(current.token)
        } else {
            listState.scrollToItem(index)
            clearUnclaimedSettingsFocusRequest(current.token)
        }
    }
}
