package com.android.purebilibili.core.performance

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.android.purebilibili.BuildConfig
import com.android.purebilibili.core.util.LogCollector
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Versioned, ASCII-only state; never store content IDs, titles, URLs or input here. */
internal data class ProcessExitContext(
    val versionCode: Int,
    val versionName: String,
    val commit: String,
    val screen: String = "unknown",
    val event: String = "unknown",
    val stage: String = "unknown",
    val foreground: Boolean? = null,
    val updatedAtMillis: Long = 0,
    val processAgeMillis: Long = 0,
) {
    fun encode(): ByteArray = ByteArrayOutputStream().also { bytes ->
        DataOutputStream(bytes).use { output ->
            output.writeInt(0x42504431) // BPD1
            output.writeInt(versionCode)
            output.writeUTF(token(versionName, 12))
            output.writeUTF(token(commit, 12))
            output.writeUTF(token(screen, 24))
            output.writeUTF(token(event, 20))
            output.writeUTF(token(stage, 24))
            output.writeByte(when (foreground) { true -> 1; false -> 0; null -> 2 })
            output.writeLong(updatedAtMillis)
            output.writeLong(processAgeMillis)
        }
    }.toByteArray().also { require(it.size <= 128) }

    fun describe(): String = "app=$versionName($versionCode), commit=$commit, " +
        "screen=$screen, event=$event, stage=$stage, foreground=${foreground ?: "unknown"}, " +
        "updatedAtMs=$updatedAtMillis, processAgeMs=$processAgeMillis"

    companion object {
        private fun token(value: String, limit: Int): String =
            value.filter { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it in "_.-" }
                .take(limit)

        fun decode(bytes: ByteArray?): ProcessExitContext? {
            if (bytes == null || bytes.size > 128) return null
            return runCatching {
                DataInputStream(bytes.inputStream()).use { input ->
                    require(input.readInt() == 0x42504431)
                    val code = input.readInt()
                    val version = input.readUTF()
                    val commit = input.readUTF()
                    val screen = input.readUTF()
                    val event = input.readUTF()
                    val stage = input.readUTF()
                    val foreground = when (input.readUnsignedByte()) {
                        0 -> false
                        1 -> true
                        2 -> null
                        else -> error("Invalid foreground state")
                    }
                    ProcessExitContext(code, version, commit, screen, event, stage, foreground,
                        input.readLong(), input.readLong()).also {
                        require(input.available() == 0)
                        require(it.encode().contentEquals(bytes))
                    }
                }
            }.getOrNull()
        }
    }
}

/** Local diagnostics are independent of remote analytics/crash collection settings. */
internal object ProcessExitContextRecorder {
    private val lock = Any()
    private val executor = Executors.newSingleThreadScheduledExecutor()
    private val startedAt = SystemClock.elapsedRealtime()
    private var manager: ActivityManager? = null
    private var scheduled = false
    private var lastAttemptAt = -2_000L
    private var state = ProcessExitContext(BuildConfig.VERSION_CODE, BuildConfig.VERSION_NAME,
        BuildConfig.BUILD_COMMIT_SHA.take(12))

    fun initialize(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        synchronized(lock) {
            manager = context.applicationContext.getSystemService(ActivityManager::class.java)
            scheduleLocked()
        }
    }

    fun screen(name: String) {
        // Fixed application screen categories only, including for unrecognized callers.
        val safe = name.takeIf { it in setOf("HomeScreen", "ProfileScreen", "SearchScreen",
            "VideoDetailScreen", "DynamicScreen", "SettingsScreen") } ?: "other"
        update { copy(screen = safe) }
    }

    fun event(name: String) {
        val category = when (name) {
            "analytics_collection_enabled" -> "analytics_on"
            "analytics_collection_disabled" -> "analytics_off"
            else -> name.substringBefore(':')
        }
        val safe = category.takeIf { it in setOf("screen_view", "video_play",
            "search", "app_open", "login", "logout", "setting_change", "video_error",
            "live_error", "app_foreground", "app_background", "daily_active",
            "analytics_on", "analytics_off") } ?: "other"
        update { copy(event = safe) }
    }

    fun stage(name: String) = update { copy(stage = name.takeIf {
        it.length <= 24 && it.all { c -> c in 'a'..'z' || c == '_' }
    } ?: "other") }

    fun foreground(value: Boolean) = update { copy(foreground = value) }

    private fun update(change: ProcessExitContext.() -> ProcessExitContext) {
        synchronized(lock) {
            val next = state.change()
            if (next == state) return
            state = next.copy(updatedAtMillis = System.currentTimeMillis(),
                processAgeMillis = SystemClock.elapsedRealtime() - startedAt)
            LogCollector.add("I", "ProcessContext",
                "screen=${state.screen}, event=${state.event}, stage=${state.stage}, " +
                    "foreground=${state.foreground ?: "unknown"}", basicDiagnostic = true)
            scheduleLocked()
        }
    }

    private fun scheduleLocked() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || manager == null || scheduled) return
        scheduled = true
        val delay = (2_000L - (SystemClock.elapsedRealtime() - lastAttemptAt)).coerceAtLeast(0L)
        executor.schedule({
            val snapshot = synchronized(lock) {
                scheduled = false
                lastAttemptAt = SystemClock.elapsedRealtime()
                state
            }
            runCatching { manager?.setProcessStateSummary(snapshot.encode()) }
                .onFailure {
                    Log.w("ProcessExitContext", "System state summary unavailable", it)
                    LogCollector.add("W", "ProcessExitContext",
                        "System state summary unavailable: ${it.javaClass.simpleName}")
                }
        }, delay, TimeUnit.MILLISECONDS)
    }
}
