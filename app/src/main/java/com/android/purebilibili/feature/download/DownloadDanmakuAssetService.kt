package com.android.purebilibili.feature.download

import com.android.purebilibili.data.repository.DanmakuRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
internal data class LocalDanmakuManifest(
    val bvid: String,
    val cid: Long,
    val aid: Long,
    val durationMs: Long,
    val segmentPaths: List<String>,
    val savedAt: Long
)

internal data class DownloadDanmakuAssetResult(
    val segmentPaths: List<String>,
    val metadataPath: String?
)

internal object DownloadDanmakuAssetService {
    private val json = Json { prettyPrint = true }

    suspend fun download(
        task: DownloadTask,
        taskDir: File,
        updateState: (DownloadAssetState) -> Unit
    ): DownloadDanmakuAssetResult = withContext(Dispatchers.IO) {
        if (!task.options.includeDanmaku) {
            updateState(
                DownloadAssetState(
                    kind = DownloadAssetKind.DANMAKU,
                    status = DownloadAssetStatus.SKIPPED
                )
            )
            return@withContext DownloadDanmakuAssetResult(emptyList(), null)
        }

        updateState(
            DownloadAssetState(
                kind = DownloadAssetKind.DANMAKU,
                status = DownloadAssetStatus.DOWNLOADING
            )
        )

        val durationMs = task.duration.coerceAtLeast(0) * 1000L
        val viewReply = if (task.aid > 0L) {
            DanmakuRepository.getDanmakuView(task.cid, task.aid)
        } else {
            null
        }
        val segments = DanmakuRepository.getDanmakuSegments(
            cid = task.cid,
            durationMs = durationMs,
            metadataSegmentCount = viewReply?.dmSge?.total?.toInt()
        ) + DanmakuRepository.getSpecialDanmakuSegments(viewReply?.specialDms.orEmpty())

        if (segments.isEmpty()) {
            updateState(
                DownloadAssetState(
                    kind = DownloadAssetKind.DANMAKU,
                    status = DownloadAssetStatus.FAILED,
                    errorMessage = "未获取到弹幕"
                )
            )
            return@withContext DownloadDanmakuAssetResult(emptyList(), null)
        }

        val danmakuDir = File(taskDir, "danmaku").apply { mkdirs() }
        val segmentPaths = segments.mapIndexed { index, bytes ->
            val file = File(danmakuDir, "${task.id}_seg_${index + 1}.pb")
            file.writeBytes(bytes)
            file.absolutePath
        }
        val manifestFile = File(danmakuDir, "${task.id}_manifest.json")
        manifestFile.writeText(
            json.encodeToString(
                LocalDanmakuManifest(
                    bvid = task.bvid,
                    cid = task.cid,
                    aid = task.aid,
                    durationMs = durationMs,
                    segmentPaths = segmentPaths,
                    savedAt = System.currentTimeMillis()
                )
            )
        )

        updateState(
            DownloadAssetState(
                kind = DownloadAssetKind.DANMAKU,
                status = DownloadAssetStatus.COMPLETED,
                totalBytes = segments.sumOf { it.size.toLong() },
                downloadedBytes = segments.sumOf { it.size.toLong() },
                filePath = manifestFile.absolutePath,
                segmentCount = segments.size
            )
        )
        DownloadDanmakuAssetResult(
            segmentPaths = segmentPaths,
            metadataPath = manifestFile.absolutePath
        )
    }

    fun readLocalSegments(task: DownloadTask): List<ByteArray> {
        return task.localDanmakuSegmentPaths.mapNotNull { path ->
            runCatching {
                File(path).takeIf { it.exists() && it.length() > 0L }?.readBytes()
            }.getOrNull()
        }
    }
}
