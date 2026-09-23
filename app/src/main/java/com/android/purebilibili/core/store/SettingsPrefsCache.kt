package com.android.purebilibili.core.store

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * SharedPreferences 影子缓存的读写工具。
 *
 * `SettingsManager` 的真值存在 DataStore 里，但 DataStore 只能挂起读取。为了让
 * `PureApplication` 启动、`NetworkUtils`、播放器初始化这类**必须同步拿到值**的地方能
 * 工作，项目里长期用 SharedPreferences 做影子缓存：每次 `set*` 都双写一份。
 *
 * 这些工具刻意放在 `SettingsManager.kt` 之外的独立文件：那个门面文件正在按领域拆成
 * 多个 store，且有行数棘轮（`SettingsManagerSizeRatchetTest`）盯着只减不增。
 * 通用基建本来也不属于门面。
 */

/**
 * 把 DataStore 与影子缓存视作一次有顺序保证的双写。
 *
 * 这些写入原先直接写在 `suspend fun` 体内，而 `suspend` 并不意味着离开主线程——
 * 调用方多是 `viewModelScope.launch { }`（Main 调度器），`settingsDataStore.edit{}`
 * 挂起返回后续体恢复在主线程，于是 `commit()` 就是一次**主线程同步写盘**。
 * lint 的 `ApplySharedPref` 报的正是这一批，此前被 lint-baseline 静默抑制着。
 *
 * 刻意保留 `commit()` 而不是换成 `apply()`：影子缓存存在的意义就是「下次进程启动时
 * 能同步读到」，其中切换应用图标那处还有硬前提——切 activity-alias 往往会立刻杀掉
 * 进程，`apply()` 的异步写入可能来不及落盘。换成 `apply()` 是拿正确性换性能。
 * 只把第二段 `commit()` 丢到 IO 线程并不安全：两个连续 setter 可能让旧的 commit
 * 最后完成，调用方取消也可能发生在 DataStore 已落盘、影子缓存尚未写入之间。因此这里
 * 从 DataStore edit 开始串行化；一旦首段开始，两段都在 NonCancellable 中完成。这样
 * 同步缓存始终与最后一次完整设置一致，同时 `commit()` 仍不阻塞主线程。
 *
 * `ApplySharedPref` 是**纯语法检查**——它只认 `commit()` 这个调用本身，与线程无关，
 * 所以上面的修复并不会让它变绿；而它建议的替代方案在这里是错的。因此就地抑制
 * 并写明理由，取代原先散在 lint-baseline 里的 4 条静默条目：
 * 抑制写在代码旁边，下一个读到的人能立刻看到取舍；写在 baseline 里则没人会看见。
 */
@SuppressLint("ApplySharedPref")
internal suspend fun editSettingsAndCommitPrefs(
    context: Context,
    name: String,
    editSettings: MutablePreferences.() -> Unit,
    editPrefs: SharedPreferences.Editor.() -> Unit,
): Boolean = settingsCacheWriter.write(
    writeDataStore = {
        context.settingsDataStore.edit { preferences -> preferences.editSettings() }
    },
    writeCache = {
        withContext(Dispatchers.IO) {
            context.getSharedPreferences(name, Context.MODE_PRIVATE)
                .edit()
                .apply(editPrefs)
                .commit()
        }
    },
)

internal class SerializedSettingsCacheWriter {
    private val writeMutex = Mutex()

    suspend fun <T> write(
        writeDataStore: suspend () -> Unit,
        writeCache: suspend () -> T,
    ): T = writeMutex.withLock {
        withContext(NonCancellable) {
            writeDataStore()
            writeCache()
        }
    }
}

private val settingsCacheWriter = SerializedSettingsCacheWriter()
