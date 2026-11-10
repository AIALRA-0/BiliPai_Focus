package com.android.purebilibili.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

/**
 * 赛事比赛详情 (x/esports/match/info)
 *
 * 主站接口使用 snake_case 命名（game_stage / home_team / contest_status），
 * 综合搜索接口的 esports 块对同一结构使用驼峰命名（gameStage / homeTeam / contestStatus），
 * 因此每个字段显式声明两组备用名；队名块内层两种来源都是 title / logo / logoFull。
 */
@Serializable
data class MatchInfoResponse(
    val code: Int = 0,
    val message: String = "",
    val data: MatchInfoData? = null,
)

@Serializable
data class MatchInfoData(
    val contest: MatchContestInfo? = null,
)

@Serializable
data class MatchContestInfo(
    @JsonNames("ID")
    val id: Long = 0,
    @SerialName("game_stage")
    @JsonNames("gameStage")
    val gameStage: String = "",
    @SerialName("stime")
    val startTime: Long = 0,
    @SerialName("home_score")
    @JsonNames("homeScore")
    val homeScore: Int? = null,
    @SerialName("away_score")
    @JsonNames("awayScore")
    val awayScore: Int? = null,
    @SerialName("live_room")
    @JsonNames("liveRoom")
    val liveRoom: Long = 0,
    val season: MatchContestSeason? = null,
    @SerialName("home_team")
    @JsonNames("homeTeam")
    val homeTeam: MatchContestTeam? = null,
    @SerialName("away_team")
    @JsonNames("awayTeam")
    val awayTeam: MatchContestTeam? = null,
    @SerialName("contest_status")
    @JsonNames("contestStatus")
    val contestStatus: Int = 0,
    val playback: String = "",
)

@Serializable
data class MatchContestSeason(
    val title: String = "",
    val logo: String = "",
)

@Serializable
data class MatchContestTeam(
    val title: String = "",
    val logo: String = "",
    @SerialName("logoFull")
    val logoFull: String = "",
)

/** 比赛状态：1 未开始、2 直播中、3 已结束。 */
const val MATCH_CONTEST_STATUS_NOT_STARTED = 1
const val MATCH_CONTEST_STATUS_LIVING = 2
const val MATCH_CONTEST_STATUS_FINISHED = 3

/**
 * 队标完整地址：接口可能只带相对路径（如 /bfs/esports/xxx.png），需拼图床域名。
 */
fun MatchContestTeam?.resolveMatchContestLogoUrl(): String {
    if (this == null) return ""
    if (logoFull.isNotBlank()) return logoFull
    if (logo.isBlank()) return ""
    return when {
        logo.startsWith("http") -> logo
        logo.startsWith("//") -> "https:$logo"
        else -> "https://i1.hdslb.com$logo"
    }
}
