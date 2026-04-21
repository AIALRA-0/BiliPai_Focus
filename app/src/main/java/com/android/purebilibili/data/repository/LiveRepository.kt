// 文件路径: data/repository/LiveRepository.kt
package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.data.model.response.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class LiveRoomH5Snapshot(
    val roomId: Long = 0,
    val title: String = "",
    val cover: String = "",
    val appBackground: String = "",
    val watchedText: String = "",
    val anchorName: String = "",
    val anchorFace: String = "",
    val liveStartTime: Long = 0,
    val online: Int = 0
)

data class LivePrefetchDanmaku(
    val uid: Long = 0,
    val uname: String = "",
    val text: String = "",
    val emoticonUrl: String? = null,
    val replyToName: String = "",
    val dmType: Int = 0,
    val idStr: String = "",
    val reportTs: Long = 0,
    val reportSign: String = ""
)

data class LiveSuperChatSeed(
    val uid: Long = 0,
    val uname: String = "",
    val message: String = "",
    val price: String = "",
    val backgroundColor: Int = 0
)

enum class LiveContributionRankType(
    val title: String,
    val switchValue: String
) {
    ONLINE("在线榜", "contribution_rank"),
    DAILY("日榜", "today_rank"),
    WEEKLY("周榜", "current_week_rank"),
    MONTHLY("月榜", "current_month_rank")
}

/**
 * 直播相关数据仓库
 * 从 VideoRepository 拆分出来，专注于直播功能
 */
object LiveRepository {
    private val api = NetworkModule.api

    private suspend fun resolveRealRoomId(roomId: Long): Long {
        return try {
            val resp = api.getLiveRoomInit(roomId)
            resp.data?.roomId?.takeIf { it > 0L } ?: roomId
        } catch (_: Exception) {
            roomId
        }
    }

    /**
     * 获取热门直播列表
     */
    suspend fun getLiveRooms(page: Int = 1): Result<List<LiveRoom>> = withContext(Dispatchers.IO) {
        try {
            val resp = api.getLiveList(page = page)
            // 使用 getAllRooms() 兼容新旧 API 格式
            val list = resp.data?.getAllRooms() ?: emptyList()
            list.firstOrNull()?.let {
                com.android.purebilibili.core.util.Logger.d("LiveRepo", "🟢 Popular Live: roomid=${it.roomid}, title=${it.title}, online=${it.online}")
            }
            com.android.purebilibili.core.util.Logger.d("LiveRepo", "🔴 getLiveRooms page=$page, count=${list.size}")
            Result.success(list)
        } catch (e: Exception) {
            com.android.purebilibili.core.util.Logger.e("LiveRepo", " getLiveRooms failed", e)
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun getLiveAreaIndex(): Result<List<LiveAreaParent>> = withContext(Dispatchers.IO) {
        try {
            val resp = api.getLiveAreaList()
            if (resp.code == 0) {
                Result.success(resp.data ?: emptyList())
            } else {
                Result.failure(Exception(resp.message.ifBlank { "获取直播标签失败" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAreaRooms(
        parentAreaId: Int,
        areaId: Int = 0,
        page: Int = 1,
        sortType: String = "online"
    ): Result<List<LiveRoom>> = withContext(Dispatchers.IO) {
        try {
            val resp = api.getLiveSecondAreaList(
                parentAreaId = parentAreaId,
                areaId = areaId,
                page = page,
                sortType = sortType
            )
            if (resp.code == 0) {
                Result.success(resp.data?.list ?: emptyList())
            } else {
                Result.failure(Exception(resp.message.ifBlank { "获取分区直播失败" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 获取关注的直播间（需要登录）
     */
    suspend fun getFollowedLive(page: Int = 1): Result<List<LiveRoom>> = withContext(Dispatchers.IO) {
        try {
            val resp = api.getFollowedLive(page = page, pageSize = 50)
            
            // 过滤只返回正在直播的（liveStatus == 1）
            val followedRooms = resp.data?.list
                ?.filter { it.liveStatus == 1 }
                ?: emptyList()
            
            // 关注直播 API 不返回在线人数，需要额外获取
            val liveRooms = followedRooms.map { room ->
                val liveRoom = room.toLiveRoom()
                try {
                    // 获取房间详情以得到在线人数
                    val roomInfo = api.getRoomInfo(room.roomid)
                    val online = roomInfo.data?.online ?: 0
                    com.android.purebilibili.core.util.Logger.d("LiveRepo", "🔴 Room ${room.roomid} online: $online")
                    liveRoom.copy(online = online)
                } catch (e: Exception) {
                    android.util.Log.w("LiveRepo", "Failed to get room info for ${room.roomid}: ${e.message}")
                    liveRoom  // 失败时使用原数据
                }
            }
            
            Result.success(liveRooms)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun getRoomH5Info(roomId: Long): Result<LiveRoomH5Snapshot> = withContext(Dispatchers.IO) {
        try {
            val realRoomId = resolveRealRoomId(roomId)
            val json = JSONObject(api.getLiveRoomH5Info(realRoomId).string())
            if (json.optInt("code", -1) != 0) {
                return@withContext Result.failure(Exception(json.optString("message", "获取直播间 H5 信息失败")))
            }
            val data = json.optJSONObject("data") ?: JSONObject()
            val room = data.optJSONObject("room_info") ?: JSONObject()
            val anchor = data.optJSONObject("anchor_info")?.optJSONObject("base_info") ?: JSONObject()
            val watched = data.optJSONObject("watched_show") ?: JSONObject()
            Result.success(
                LiveRoomH5Snapshot(
                    roomId = room.optLong("room_id", realRoomId),
                    title = room.optString("title"),
                    cover = room.optString("cover"),
                    appBackground = room.optString("app_background"),
                    watchedText = watched.optString("text_large").ifBlank { watched.optString("text_small") },
                    anchorName = anchor.optString("uname"),
                    anchorFace = anchor.optString("face"),
                    liveStartTime = room.optLong("live_start_time", 0L),
                    online = room.optInt("online", watched.optInt("num", 0))
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun signWithWbi(params: Map<String, String>): Map<String, String> {
        return try {
            val navResp = api.getNavInfo()
            val wbiImg = navResp.data?.wbi_img
            val imgKey = wbiImg?.img_url?.substringAfterLast("/")?.substringBefore(".") ?: ""
            val subKey = wbiImg?.sub_url?.substringAfterLast("/")?.substringBefore(".") ?: ""
            if (imgKey.isNotEmpty() && subKey.isNotEmpty()) {
                com.android.purebilibili.core.network.WbiUtils.sign(params, imgKey, subKey)
            } else {
                params
            }
        } catch (_: Exception) {
            params
        }
    }

    suspend fun getLiveDanmakuHistory(roomId: Long): Result<List<LivePrefetchDanmaku>> = withContext(Dispatchers.IO) {
        try {
            val realRoomId = resolveRealRoomId(roomId)
            val json = JSONObject(api.getLiveDanmakuHistory(realRoomId).string())
            if (json.optInt("code", -1) != 0) {
                return@withContext Result.failure(Exception(json.optString("message", "获取直播弹幕历史失败")))
            }
            val roomArray = json.optJSONObject("data")?.optJSONArray("room")
            val items = buildList {
                if (roomArray != null) {
                    for (index in 0 until roomArray.length()) {
                        val obj = roomArray.optJSONObject(index) ?: continue
                        val user = obj.optJSONObject("user")
                        val base = user?.optJSONObject("base")
                        val checkInfo = obj.optJSONObject("check_info")
                        val reply = obj.optJSONObject("reply")
                        add(
                            LivePrefetchDanmaku(
                                uid = user?.optLong("uid", 0L) ?: 0L,
                                uname = base?.optString("name").orEmpty(),
                                text = obj.optString("text"),
                                emoticonUrl = obj.optJSONObject("emoticon")?.optString("url"),
                                replyToName = reply?.optString("reply_uname").orEmpty(),
                                dmType = obj.optInt("dm_type", 0),
                                idStr = obj.optString("id_str"),
                                reportTs = checkInfo?.optLong("ts", 0L) ?: 0L,
                                reportSign = checkInfo?.optString("ct").orEmpty()
                            )
                        )
                    }
                }
            }
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLiveSuperChatMessages(roomId: Long): Result<List<LiveSuperChatSeed>> = withContext(Dispatchers.IO) {
        try {
            val realRoomId = resolveRealRoomId(roomId)
            val json = JSONObject(api.getLiveSuperChatMessages(realRoomId).string())
            if (json.optInt("code", -1) != 0) {
                return@withContext Result.failure(Exception(json.optString("message", "获取醒目留言失败")))
            }
            val list = json.optJSONObject("data")?.optJSONArray("list")
            val items = buildList {
                if (list != null) {
                    for (index in 0 until list.length()) {
                        val obj = list.optJSONObject(index) ?: continue
                        val user = obj.optJSONObject("user_info")
                        add(
                            LiveSuperChatSeed(
                                uid = obj.optLong("uid", user?.optLong("uid", 0L) ?: 0L),
                                uname = user?.optString("uname").orEmpty(),
                                message = obj.optString("message"),
                                price = obj.optInt("price", 0).takeIf { it > 0 }?.let { "¥$it" }.orEmpty(),
                                backgroundColor = parseLiveColorInt(
                                    obj.optString("background_bottom_color").ifBlank {
                                        obj.optString("background_color")
                                    }
                                )
                            )
                        )
                    }
                }
            }
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLiveContributionRank(
        roomId: Long,
        ruid: Long,
        type: LiveContributionRankType,
        page: Int = 1
    ): Result<List<LiveContributionRankItem>> = withContext(Dispatchers.IO) {
        try {
            val realRoomId = resolveRealRoomId(roomId)
            val params = signWithWbi(
                mapOf(
                    "ruid" to ruid.toString(),
                    "room_id" to realRoomId.toString(),
                    "page" to page.toString(),
                    "page_size" to "100",
                    "type" to type.name.lowercase(),
                    "switch" to type.switchValue,
                    "platform" to "web",
                    "web_location" to "444.8"
                )
            )
            val resp = api.getLiveContributionRank(params)
            if (resp.code == 0) {
                Result.success(resp.data?.item ?: emptyList())
            } else {
                Result.failure(Exception(resp.message.ifBlank { "获取高能榜失败" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun shieldLiveUser(
        roomId: Long,
        uid: Long,
        type: Int = 1
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val realRoomId = resolveRealRoomId(roomId)
            val csrf = com.android.purebilibili.core.store.TokenManager.csrfCache ?: ""
            if (csrf.isBlank()) return@withContext Result.failure(Exception("请先登录"))
            val resp = api.shieldLiveUser(
                uid = uid,
                roomId = realRoomId,
                type = type,
                csrf = csrf,
                csrfToken = csrf
            )
            if (resp.code == 0) Result.success(true) else Result.failure(Exception(resp.message.ifBlank { "直播间屏蔽失败" }))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 获取直播流 URL
     */
    suspend fun getLivePlayUrl(roomId: Long): Result<String> = withContext(Dispatchers.IO) {
        try {
            val realRoomId = resolveRealRoomId(roomId)
            com.android.purebilibili.core.util.Logger.d("LiveRepo", "🔴 Fetching live URL for roomId=$roomId(real=$realRoomId)")
            val resp = api.getLivePlayUrl(roomId = realRoomId)
            com.android.purebilibili.core.util.Logger.d("LiveRepo", "🔴 Live API response: code=${resp.code}, msg=${resp.message}")
            
            // 尝试从新 xlive API 结构获取 URL
            val playurlInfo = resp.data?.playurl_info
            if (playurlInfo != null) {
                com.android.purebilibili.core.util.Logger.d("LiveRepo", "🔴 Using new xlive API structure")
                val streams = playurlInfo.playurl?.stream ?: emptyList()
                // 优先选择 http_hls，其次 http_stream
                val stream = streams.find { it.protocolName == "http_hls" }
                    ?: streams.find { it.protocolName == "http_stream" }
                    ?: streams.firstOrNull()
                
                val format = stream?.format?.firstOrNull()
                val codec = format?.codec?.firstOrNull()
                val urlInfo = codec?.url_info?.firstOrNull()
                
                if (codec != null && urlInfo != null) {
                    val url = urlInfo.host + codec.baseUrl + urlInfo.extra
                    com.android.purebilibili.core.util.Logger.d("LiveRepo", " Xlive URL: ${url.take(100)}...")
                    return@withContext Result.success(url)
                }
            }
            
            // 回退到旧 API 结构
            com.android.purebilibili.core.util.Logger.d("LiveRepo", "🔴 Trying legacy durl structure...")
            val url = resp.data?.durl?.firstOrNull()?.url
            if (url != null) {
                com.android.purebilibili.core.util.Logger.d("LiveRepo", " Legacy URL: ${url.take(100)}...")
                return@withContext Result.success(url)
            }
            
            android.util.Log.e("LiveRepo", " No URL found in response")
            Result.failure(Exception("无法获取直播流"))
        } catch (e: Exception) {
            android.util.Log.e("LiveRepo", " getLivePlayUrl failed: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    /**
     * 获取直播流（带画质信息）- 用于画质切换
     */
    suspend fun getLivePlayUrlWithQuality(
        roomId: Long,
        qn: Int = 10000,
        onlyAudio: Boolean = false
    ): Result<LivePlayUrlData> = withContext(Dispatchers.IO) {
        try {
            val realRoomId = resolveRealRoomId(roomId)
            com.android.purebilibili.core.util.Logger.d("LiveRepo", "🔴 Fetching live URL with quality for roomId=$roomId(real=$realRoomId), qn=$qn, onlyAudio=$onlyAudio")
            
            // 使用旧版 API 补充可读画质描述，但不再把 legacy durl 当作主播放来源
            val legacyResp = try {
                api.getLivePlayUrlLegacy(cid = realRoomId, qn = qn)
            } catch (e: Exception) {
                android.util.Log.w("LiveRepo", "Legacy API failed: ${e.message}")
                null
            }
            
            val qualityList = legacyResp?.data?.quality_description ?: emptyList()
            val currentQuality = legacyResp?.data?.current_quality ?: 0
            val legacyHasUrl = legacyResp?.data?.durl?.firstOrNull()?.url != null
            com.android.purebilibili.core.util.Logger.d("LiveRepo", " Legacy API: qualityList=${qualityList.map { it.desc }}, current=$currentQuality, hasUrl=$legacyHasUrl")
            
            // 新版 xlive API 作为主播放来源
            com.android.purebilibili.core.util.Logger.d("LiveRepo", "🔴 Using xlive API as primary stream source...")
            val resp = api.getLivePlayUrl(
                roomId = realRoomId,
                quality = qn,
                onlyAudio = if (onlyAudio) 1 else null
            )
            
            if (resp.code == 0 && resp.data != null) {
                // 合并旧版画质列表到新版响应数据
                val mergedData = resp.data.copy(
                    quality_description = qualityList.takeIf { it.isNotEmpty() } ?: resp.data.quality_description,
                    current_quality = if (currentQuality > 0) currentQuality else resp.data.current_quality
                )
                com.android.purebilibili.core.util.Logger.d("LiveRepo", " Merged data: qualityList=${mergedData.quality_description?.map { it.desc }}")
                Result.success(mergedData)
            } else if (legacyResp?.code == 0 && legacyResp.data != null) {
                com.android.purebilibili.core.util.Logger.w("LiveRepo", "🔴 xlive API unavailable, falling back to legacy durl response")
                Result.success(legacyResp.data)
            } else {
                Result.failure(Exception("获取直播流失败: ${resp.message}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("LiveRepo", " getLivePlayUrlWithQuality failed: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    /**
     * 发送直播弹幕
     */
    suspend fun sendDanmaku(roomId: Long, msg: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val realRoomId = resolveRealRoomId(roomId)
            val csrf = com.android.purebilibili.core.store.TokenManager.csrfCache ?: ""
            if (csrf.isEmpty()) return@withContext Result.failure(Exception("请先登录"))
            
            val resp = api.sendLiveDanmaku(
                roomId = realRoomId,
                msg = msg,
                csrf = csrf,
                csrfToken = csrf
            )
            
            if (resp.code == 0) {
                Result.success(true)
            } else {
                Result.failure(Exception(resp.message ?: "发送失败"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    /**
     * 直播间点赞 (上报)
     */
    suspend fun clickLike(
        roomId: Long,
        uid: Long,
        anchorId: Long,
        clickTime: Int = 1
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val realRoomId = resolveRealRoomId(roomId)
            val csrf = com.android.purebilibili.core.store.TokenManager.csrfCache ?: ""
            
            val resp = api.clickLikeLiveRoom(
                clickTime = clickTime.coerceAtLeast(1),
                roomId = realRoomId,
                uid = uid,
                anchorId = anchorId,
                csrf = csrf,
                csrfToken = csrf
            )
            
            if (resp.code == 0) {
                Result.success(true)
            } else {
                Result.failure(Exception(resp.message))
            }
        } catch (e: Exception) {

            // 点赞失败静默处理
            Result.failure(e)
        }
    }

    /**
     * 获取直播弹幕表情
     * 返回: Map<关键词, 图片URL>
     */
    suspend fun getEmoticons(roomId: Long): Result<Map<String, String>> = withContext(Dispatchers.IO) {
        try {
            val realRoomId = resolveRealRoomId(roomId)
            val resp = api.getLiveEmoticons(roomId = realRoomId)
            if (resp.code == 0 && resp.data?.data != null) {
                val emojiMap = mutableMapOf<String, String>()
                resp.data.data.forEach { pkg ->
                    pkg.emoticons?.forEach { emotion ->
                        if (emotion.emoji.isNotEmpty() && emotion.url.isNotEmpty()) {
                            emojiMap[emotion.emoji] = emotion.url
                        }
                    }
                }
                com.android.purebilibili.core.util.Logger.d("LiveRepo", " Fetched ${emojiMap.size} emoticons for room $roomId(real=$realRoomId)")
                Result.success(emojiMap)
            } else {
                Result.failure(Exception(resp.msg))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // 失败不影响主要流程
            Result.failure(e)
        }
    }

    private fun parseLiveColorInt(raw: String): Int {
        val normalized = raw.removePrefix("#")
        return normalized.toLongOrNull(16)?.toInt() ?: 0
    }
}
