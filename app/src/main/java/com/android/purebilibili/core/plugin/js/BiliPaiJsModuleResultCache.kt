package com.android.purebilibili.core.plugin.js

import android.content.Context
import java.io.File
import java.security.MessageDigest
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * JS 插件模块结果缓存：按 `pluginId + moduleId + params` 缓存模块返回的 JSON，
 * 生命周期由模块声明的 `cacheDuration`（秒）决定。存放在 `cacheDir` 下，
 * 系统存储压力下可被整体回收，TTL 过期后自动失效。
 */
class BiliPaiJsModuleResultCache(
    private val cacheDir: File,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class Entry(
        val moduleId: String,
        val savedAtMillis: Long,
        val ttlSeconds: Long,
        val payload: String
    )

    @Synchronized
    fun read(
        pluginId: String,
        moduleId: String,
        paramsJson: String,
        cacheDurationSeconds: Long
    ): String? {
        if (cacheDurationSeconds <= 0L) return null
        val file = entryFile(pluginId, moduleId, paramsJson) ?: return null
        if (!file.exists()) return null
        val entry = runCatching {
            json.decodeFromString<Entry>(file.readText(Charsets.UTF_8))
        }.getOrNull()
        if (entry == null) {
            file.delete()
            return null
        }
        val expiresAtMillis = entry.savedAtMillis + entry.ttlSeconds * 1000L
        if (clock() >= expiresAtMillis || entry.moduleId != moduleId) {
            file.delete()
            return null
        }
        return entry.payload
    }

    @Synchronized
    fun write(
        pluginId: String,
        moduleId: String,
        paramsJson: String,
        cacheDurationSeconds: Long,
        payload: String
    ) {
        if (cacheDurationSeconds <= 0L || payload.isBlank()) return
        val file = entryFile(pluginId, moduleId, paramsJson) ?: return
        file.parentFile?.mkdirs()
        val entry = Entry(
            moduleId = moduleId,
            savedAtMillis = clock(),
            ttlSeconds = cacheDurationSeconds,
            payload = payload
        )
        runCatching {
            file.writeText(json.encodeToString(entry), Charsets.UTF_8)
        }
    }

    @Synchronized
    fun clearPlugin(pluginId: String) {
        File(cacheDir, pluginId.safeCacheName()).deleteRecursively()
    }

    @Synchronized
    fun clearAll() {
        cacheDir.takeIf { it.exists() }?.listFiles()?.forEach { it.deleteRecursively() }
    }

    private fun entryFile(
        pluginId: String,
        moduleId: String,
        paramsJson: String
    ): File? {
        val pluginDirName = pluginId.safeCacheName()
        if (pluginDirName.isBlank()) return null
        val key = sha256("$moduleId\n$paramsJson")
        return File(File(cacheDir, pluginDirName), "$key.json")
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { byte -> "%02x".format(byte) }.take(24)
    }

    private fun String.safeCacheName(): String {
        return replace(Regex("[^A-Za-z0-9_.-]"), "_").take(64)
    }

    companion object {
        fun createDefault(context: Context): BiliPaiJsModuleResultCache {
            return BiliPaiJsModuleResultCache(File(context.cacheDir, "bilipai_js_plugin_module_cache"))
        }
    }
}
