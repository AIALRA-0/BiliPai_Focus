package com.android.purebilibili.feature.live

import com.android.purebilibili.data.model.response.LiveMatchGrades
import com.android.purebilibili.data.model.response.LiveMatchGradeTeam
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal val liveMatchRatingScores = listOf(0, 2, 4, 6, 8, 10)

internal fun liveMatchGradeFields(contest: Long, game: Int, player: Long, score: Int, csrf: String): Map<String, String> {
    require(contest > 0 && game > 0 && player > 0 && score in liveMatchRatingScores && csrf.isNotBlank())
    return mapOf("cid" to "$contest", "bo" to "$game", "player_id" to "$player", "score" to "$score", "csrf" to csrf)
}

internal fun liveMatchGradeCommentFields(aid: Long, gradeId: String, score: Int, message: String, csrf: String): Map<String, String> {
    require(aid > 0 && gradeId.toLongOrNull()?.let { it > 0 } == true)
    require(score in liveMatchRatingScores && message.isNotBlank() && message.length <= 1000 && csrf.isNotBlank())
    return mapOf("oid" to "$aid", "type" to "1", "plat" to "1", "message" to message.trim(),
        "grade_id" to gradeId, "grade_score" to "$score", "csrf" to csrf)
}

internal fun liveMatchGradeCommentQuery(aid: Long, gradeId: String, offset: String, mode: Int): Map<String, String> {
    require(aid > 0 && gradeId.toLongOrNull()?.let { it > 0 } == true && mode in listOf(2, 3))
    return mapOf("oid" to "$aid", "type" to "1", "mode" to "$mode", "flag_type" to "1", "flag_id" to gradeId,
        "web_location" to "1315875", "pagination_str" to buildJsonObject { put("offset", offset) }.toString())
}

/** 提交已确认的个人评分不受统计接口的延后回显覆盖，均分和人数仍采用服务端值。 */
internal fun LiveMatchGrades.withConfirmedRating(
    playerId: Long, gradeId: String, score: Int,
): LiveMatchGrades {
    fun update(team: LiveMatchGradeTeam) = team.copy(players = team.players.map {
        if (it.playerId == playerId && it.gradeId == gradeId) it.copy(rated = true, userScore = score) else it
    })
    return copy(teams = teams.map(::update), home = home?.let(::update), away = away?.let(::update))
}
