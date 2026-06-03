package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.util.Logger
import com.android.purebilibili.data.model.response.BgmDetailData
import com.android.purebilibili.data.model.response.BgmInfo
import com.android.purebilibili.data.model.response.BgmRecommendVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object ViewGrpcRepository {
    private const val TAG = "BgmList"

    suspend fun getBgmList(aid: Long, bvid: String, cid: Long): Result<List<BgmInfo>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val response = NetworkModule.api.getBgmMultipleMusic(aid = aid, cid = cid)
                val list = response.data?.list.orEmpty().map { bgm ->
                    if (bgm.jumpUrl.isBlank() && bgm.musicId.isNotBlank()) {
                        bgm.copy(
                            jumpUrl = "https://music.bilibili.com/h5/music-detail" +
                                "?music_id=${bgm.musicId}&cid=$cid&aid=$aid&na_close_hide=1&isMulti=1"
                        )
                    } else bgm
                }
                Logger.w(TAG, "BGM list API returned ${list.size} entries for aid=$aid")
                list
            }.onFailure { e ->
                Logger.w(TAG, "BGM list API failed: ${e.message}")
            }
        }

    suspend fun getBgmDetail(musicId: String, aid: Long, cid: Long): Result<BgmDetailData?> =
        withContext(Dispatchers.IO) {
            runCatching {
                NetworkModule.api.getBgmDetail(
                    musicId = musicId,
                    aid = aid,
                    cid = cid
                ).data
            }.onFailure { e ->
                Logger.w(TAG, "BGM detail API failed: ${e.message}")
            }
        }

    suspend fun getBgmRecommendVideos(
        musicId: String,
        aid: Long,
        cid: Long,
        page: Int = 1,
        pageSize: Int = 5
    ): Result<List<BgmRecommendVideo>> =
        withContext(Dispatchers.IO) {
            runCatching {
                NetworkModule.api.getBgmRecommendList(
                    musicId = musicId,
                    aid = aid,
                    cid = cid,
                    pn = page,
                    ps = pageSize
                ).data?.list.orEmpty()
            }.onFailure { e ->
                Logger.w(TAG, "BGM recommend API failed: ${e.message}")
            }
        }
}
