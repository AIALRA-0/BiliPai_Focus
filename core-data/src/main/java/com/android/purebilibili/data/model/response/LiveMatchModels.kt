package com.android.purebilibili.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LiveMatchGamesResponse(
    val code: Int = 0,
    val message: String = "",
    val data: List<LiveMatchGame>? = null,
)

@Serializable
data class LiveMatchGame(val id: Int, val name: String)

@Serializable
data class LiveMatchScheduleResponse(
    val code: Int = 0,
    val message: String = "",
    val data: LiveMatchScheduleData? = null,
)

@Serializable
data class LiveMatchScheduleData(
    val page: LiveMatchPage = LiveMatchPage(),
    val contests: List<LiveMatchContest> = emptyList(),
)

@Serializable
data class LiveMatchPage(val num: Int = 1, val size: Int = 30, val total: Int? = null)

@Serializable
data class LiveMatchTeam(val name: String = "", val icon: String = "")

@Serializable
data class LiveMatchSeason(
    val id: Long = 0,
    val title: String = "",
    @SerialName("stime") val startTime: Long = 0,
    @SerialName("etime") val endTime: Long = 0,
)

@Serializable
data class LiveMatchContest(
    val id: Long,
    @SerialName("start_time") val startTime: Long = 0,
    @SerialName("game_stage") val stage: String = "",
    @SerialName("live_room") val liveRoom: Long = 0,
    @SerialName("play_back") val playback: String = "",
    @SerialName("contest_status") val status: Int = 0,
    @SerialName("home_score") val homeScore: Int? = null,
    @SerialName("away_score") val awayScore: Int? = null,
    val home: LiveMatchTeam? = null,
    val away: LiveMatchTeam? = null,
    val season: LiveMatchSeason? = null,
)

@Serializable
data class LiveMatchSeasonResponse(
    val code: Int = 0,
    val message: String = "",
    val data: LiveMatchSeasonDetail? = null,
)

@Serializable
data class LiveMatchSeasonDetail(
    val season: LiveMatchSeason? = null,
    val list: List<LiveMatchStageEntry> = emptyList(),
)

@Serializable
data class LiveMatchStageEntry(
    val series: LiveMatchStage? = null,
    val point: LiveMatchStanding? = null,
    @SerialName("knoc") val knockout: LiveMatchKnockout? = null,
)

@Serializable
data class LiveMatchStage(
    val id: Long = 0,
    @SerialName("parent_title") val title: String = "",
    @SerialName("child_title") val subtitle: String = "",
    val description: String = "",
    @SerialName("view_flag") val visible: Boolean = true,
)

@Serializable
data class LiveMatchStanding(
    @SerialName("use_team_group") val grouped: Boolean = false,
    @SerialName("use_small_score") val showSmallScore: Boolean = false,
    @SerialName("total_teams") val teams: List<LiveMatchStandingTeam>? = null,
    @SerialName("group_teams") val groups: List<LiveMatchStandingGroup>? = null,
    @SerialName("is_dogfall_match") val showDraws: Boolean = false,
)

@Serializable
data class LiveMatchStandingGroup(
    val name: String = "",
    val teams: List<LiveMatchStandingTeam> = emptyList(),
)

@Serializable
data class LiveMatchStandingTeam(
    @SerialName("team_id") val id: Long = 0,
    @SerialName("team_name") val name: String = "",
    @SerialName("icon_url") val icon: String = "",
    val rank: Int? = null,
    @SerialName("win_times") val wins: Int? = null,
    @SerialName("lose_times") val losses: Int? = null,
    @SerialName("tie_times") val draws: Int? = null,
    val score: Int? = null,
    @SerialName("small_score") val smallScore: Int? = null,
)

@Serializable
data class LiveMatchSeasonsResponse(
    val code: Int = 0,
    val message: String = "",
    val data: List<LiveMatchSeason>? = null,
)

@Serializable
data class LiveMatchKnockout(val groups: List<LiveMatchBracketNode> = emptyList())

@Serializable
data class LiveMatchBracketNode(
    @SerialName("contest_id") val contestId: Long = 0,
    @SerialName("home_team_id") val homeId: Long = 0,
    @SerialName("away_team_id") val awayId: Long = 0,
    @SerialName("home_team_name") val homeName: String = "",
    @SerialName("away_team_name") val awayName: String = "",
    @SerialName("home_score") val homeScore: Int? = null,
    @SerialName("away_score") val awayScore: Int? = null,
    @SerialName("win_team_id") val winnerId: Long = 0,
    @SerialName("game_stage") val stage: String = "",
    val children: List<LiveMatchBracketNode> = emptyList(),
    @SerialName("contest_info") val contest: LiveMatchContest? = null,
)

@Serializable
data class LiveMatchGradesResponse(
    val code: Int = 0,
    val message: String = "",
    val data: LiveMatchGrades? = null,
)

@Serializable
data class LiveMatchGrades(
    val aid: Long = 0,
    val teams: List<LiveMatchGradeTeam> = emptyList(),
    @SerialName("home_team") val home: LiveMatchGradeTeam? = null,
    @SerialName("away_team") val away: LiveMatchGradeTeam? = null,
    @SerialName("current_bo") val currentGame: Int? = null,
    @SerialName("replay_url") val replayUrl: String = "",
    @SerialName("live_room") val liveRoom: Long = 0,
)

@Serializable
data class LiveMatchGradeTeam(
    val name: String = "",
    @SerialName("player_grade_detail") val players: List<LiveMatchPlayerGrade> = emptyList(),
)

@Serializable
data class LiveMatchPlayerGrade(
    @SerialName("grade_id") val gradeId: String = "",
    @Serializable(with = FlexibleBooleanSerializer::class)
    @SerialName("is_grade") val rated: Boolean = false,
    @SerialName("user_grade") val userScore: Int? = null,
    @SerialName("grade_distribution") val distribution: List<String> = emptyList(),
    @SerialName("player_id") val playerId: Long = 0,
    val nickname: String = "",
    val portrait: String = "",
    val position: String = "",
    @SerialName("avg_grade") val average: String? = null,
    @SerialName("grade_users") val users: Long? = null,
    val kill: Int? = null,
    val death: Int? = null,
    val assist: Int? = null,
)
