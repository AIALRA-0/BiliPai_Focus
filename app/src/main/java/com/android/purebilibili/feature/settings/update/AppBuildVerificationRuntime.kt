package com.android.purebilibili.feature.settings

import android.content.Context
import com.android.purebilibili.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

data class InstalledAppBuildProvenance(
    val versionName: String,
    val commitSha: String,
    val gitRef: String,
    val workflowRunId: String,
    val workflowRunUrl: String,
    val releaseTag: String
)

internal fun readInstalledAppBuildProvenance(): InstalledAppBuildProvenance {
    return InstalledAppBuildProvenance(
        versionName = BuildConfig.VERSION_NAME,
        commitSha = BuildConfig.BUILD_COMMIT_SHA,
        gitRef = BuildConfig.BUILD_GIT_REF,
        workflowRunId = BuildConfig.BUILD_WORKFLOW_RUN_ID,
        workflowRunUrl = BuildConfig.BUILD_WORKFLOW_RUN_URL,
        releaseTag = BuildConfig.BUILD_RELEASE_TAG
    )
}

private object InstalledApkDigestCache {
    @Volatile
    private var cachedSourcePath: String? = null

    @Volatile
    private var cachedSha256: String? = null

    suspend fun sha256(context: Context): String? = withContext(Dispatchers.IO) {
        val sourcePath = context.applicationInfo?.sourceDir ?: return@withContext null
        val cachedPath = cachedSourcePath
        val cachedDigest = cachedSha256
        if (cachedPath == sourcePath && cachedDigest != null) {
            return@withContext cachedDigest
        }

        val digest = runCatching {
            sha256File(File(sourcePath))
        }.getOrNull()

        if (digest != null) {
            cachedSourcePath = sourcePath
            cachedSha256 = digest
        }
        digest
    }

    private fun sha256File(file: File): String {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                messageDigest.update(buffer, 0, read)
            }
        }
        return messageDigest.digest().joinToString(separator = "") { byte ->
            "%02x".format(byte)
        }
    }
}

internal suspend fun calculateInstalledApkSha256(context: Context): String? {
    return InstalledApkDigestCache.sha256(context.applicationContext)
}
