package com.android.purebilibili.feature.settings

import android.content.Context
import android.net.Uri
import com.android.purebilibili.core.database.entity.BlockedUp
import com.android.purebilibili.data.repository.buildBlockedUpShareJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class BlockedListFileService(private val context: Context) {

    suspend fun exportJsonToUri(
        uri: Uri,
        blockedUps: List<BlockedUp>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val json = buildBlockedUpShareJson(blockedUps)
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(json.toByteArray(Charsets.UTF_8))
            } ?: error("无法写入黑名单 JSON 文件")
        }
    }

    suspend fun readImportText(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader(Charsets.UTF_8).readText()
            } ?: error("无法读取黑名单 JSON 文件")
        }
    }
}

internal fun buildBlockedListJsonFileName(epochMs: Long = System.currentTimeMillis()): String {
    return "bilipai-blocked-ups-$epochMs.json"
}
