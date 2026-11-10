package com.android.purebilibili.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.reflect.KProperty

/**
 * 页面尚未编辑时跟随存储；首次编辑后由本地草稿持有当前值。
 * 存储不带提交版本，不能将相等值当作最新回执，否则 A→B→A 的旧回执会误解锁草稿。
 * 状态仅属于当前页面组合，重新进入页面时重新读取配置；显式恢复默认也通过编辑入口更新。
 */
internal class SettingsEditState<T>(initialValue: T) {
    private var currentValue by mutableStateOf(initialValue)
    private var edited = false

    fun acceptStoredValue(value: T) {
        if (!edited) currentValue = value
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = currentValue

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        edited = true
        currentValue = value
    }
}

@Composable
internal fun <T> rememberSettingsEditState(storedValue: T): SettingsEditState<T> {
    // 不以存储值作为 remember 键：存储回传不能销毁正在编辑的草稿。
    val state = remember { SettingsEditState(storedValue) }
    SideEffect { state.acceptStoredValue(storedValue) }
    return state
}
