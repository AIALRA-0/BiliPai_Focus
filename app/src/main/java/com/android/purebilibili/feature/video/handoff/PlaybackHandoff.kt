package com.android.purebilibili.feature.video.handoff

import android.app.HandoffActivityData
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.PersistableBundle
import androidx.annotation.RequiresApi
import com.android.purebilibili.navigation.ScreenRoutes
import com.android.purebilibili.navigation.VideoRoute

internal sealed interface PlaybackHandoffPayload {
    val resumePositionMs: Long

    sealed interface V1 : PlaybackHandoffPayload {
        data class Video(
            val bvid: String,
            val cid: Long,
            override val resumePositionMs: Long,
            val startAudio: Boolean = false
        ) : V1

        data class Bangumi(
            val seasonId: Long,
            val epId: Long,
            override val resumePositionMs: Long
        ) : V1
    }
}

internal object PlaybackHandoffCodec {
    private const val VERSION = 1
    private const val KEY_VERSION = "bilipai.handoff.version"
    private const val KEY_KIND = "bilipai.handoff.kind"
    private const val KEY_BVID = "bilipai.handoff.bvid"
    private const val KEY_CID = "bilipai.handoff.cid"
    private const val KEY_SEASON_ID = "bilipai.handoff.season_id"
    private const val KEY_EP_ID = "bilipai.handoff.ep_id"
    private const val KEY_POSITION_MS = "bilipai.handoff.position_ms"
    private const val KEY_START_AUDIO = "bilipai.handoff.start_audio"
    private const val KIND_VIDEO = "video"
    private const val KIND_BANGUMI = "bangumi"
    private val bvidPattern = Regex("^BV[0-9A-Za-z]{10}$", RegexOption.IGNORE_CASE)

    fun toExtras(payload: PlaybackHandoffPayload): PersistableBundle = PersistableBundle().apply {
        putInt(KEY_VERSION, VERSION)
        putLong(KEY_POSITION_MS, payload.resumePositionMs.coerceAtLeast(0L))
        when (payload) {
            is PlaybackHandoffPayload.V1.Video -> {
                putString(KEY_KIND, KIND_VIDEO)
                putString(KEY_BVID, payload.bvid)
                putLong(KEY_CID, payload.cid)
                putBoolean(KEY_START_AUDIO, payload.startAudio)
            }
            is PlaybackHandoffPayload.V1.Bangumi -> {
                putString(KEY_KIND, KIND_BANGUMI)
                putLong(KEY_SEASON_ID, payload.seasonId)
                putLong(KEY_EP_ID, payload.epId)
            }
        }
    }

    fun fromIntent(intent: Intent): PlaybackHandoffPayload? {
        return decodeV1(
            version = intent.getIntExtra(KEY_VERSION, 0),
            kind = intent.getStringExtra(KEY_KIND),
            bvid = intent.getStringExtra(KEY_BVID),
            cid = intent.getLongExtra(KEY_CID, 0L),
            seasonId = intent.getLongExtra(KEY_SEASON_ID, 0L),
            epId = intent.getLongExtra(KEY_EP_ID, 0L),
            positionMs = intent.getLongExtra(KEY_POSITION_MS, 0L),
            startAudio = intent.getBooleanExtra(KEY_START_AUDIO, false)
        )
    }

    internal fun decodeV1(
        version: Int,
        kind: String?,
        bvid: String?,
        cid: Long,
        seasonId: Long,
        epId: Long,
        positionMs: Long,
        startAudio: Boolean
    ): PlaybackHandoffPayload? {
        if (version != VERSION) return null
        val safePositionMs = positionMs.coerceAtLeast(0L)
        return when (kind) {
            KIND_VIDEO -> {
                val safeBvid = bvid?.trim().orEmpty()
                if (!bvidPattern.matches(safeBvid) || cid <= 0L) return null
                PlaybackHandoffPayload.V1.Video(
                    bvid = safeBvid,
                    cid = cid,
                    resumePositionMs = safePositionMs,
                    startAudio = startAudio
                )
            }
            KIND_BANGUMI -> {
                if (seasonId <= 0L || epId <= 0L) return null
                PlaybackHandoffPayload.V1.Bangumi(seasonId, epId, safePositionMs)
            }
            else -> null
        }
    }

    fun toRoute(payload: PlaybackHandoffPayload): String = when (payload) {
        is PlaybackHandoffPayload.V1.Video -> VideoRoute.resolveVideoRoutePath(
            bvid = payload.bvid,
            cid = payload.cid,
            encodedCover = "",
            startAudio = payload.startAudio,
            autoPortrait = true,
            fullscreen = false,
            resumePositionMs = payload.resumePositionMs
        )
        is PlaybackHandoffPayload.V1.Bangumi -> ScreenRoutes.BangumiPlayer.createRoute(
            seasonId = payload.seasonId,
            epId = payload.epId,
            resumePositionMs = payload.resumePositionMs
        )
    }

    fun fallbackUri(payload: PlaybackHandoffPayload): Uri = when (payload) {
        is PlaybackHandoffPayload.V1.Video ->
            Uri.parse("https://www.bilibili.com/video/${payload.bvid}")
        is PlaybackHandoffPayload.V1.Bangumi ->
            Uri.parse("https://www.bilibili.com/bangumi/play/ep${payload.epId}")
    }

    @RequiresApi(37)
    fun toPlatformData(
        payload: PlaybackHandoffPayload,
        receivingActivity: ComponentName
    ): HandoffActivityData =
        HandoffActivityData.Builder(receivingActivity)
            .setExtras(toExtras(payload))
            .setFallbackUri(fallbackUri(payload))
            .build()
}

/** Holds only the currently visible bangumi identifiers and a short-lived position provider. */
internal object PlaybackHandoffRegistry {
    private data class BangumiSession(
        val seasonId: Long,
        val epId: Long,
        val positionProvider: () -> Long
    )

    private var bangumiSession: BangumiSession? = null
    private var availabilityListener: (() -> Unit)? = null

    @Synchronized
    fun publishBangumi(
        seasonId: Long,
        epId: Long,
        positionProvider: () -> Long
    ) {
        bangumiSession = if (seasonId > 0L && epId > 0L) {
            BangumiSession(seasonId, epId, positionProvider)
        } else {
            null
        }
        availabilityListener?.invoke()
    }

    @Synchronized
    fun clearBangumi(seasonId: Long, epId: Long) {
        val current = bangumiSession
        if (current?.seasonId == seasonId && current.epId == epId) {
            bangumiSession = null
            availabilityListener?.invoke()
        }
    }

    @Synchronized
    fun currentBangumiPayload(): PlaybackHandoffPayload.V1.Bangumi? {
        val current = bangumiSession ?: return null
        return PlaybackHandoffPayload.V1.Bangumi(
            seasonId = current.seasonId,
            epId = current.epId,
            resumePositionMs = current.positionProvider().coerceAtLeast(0L)
        )
    }

    @Synchronized
    fun setAvailabilityListener(listener: (() -> Unit)?) {
        availabilityListener = listener
    }
}
