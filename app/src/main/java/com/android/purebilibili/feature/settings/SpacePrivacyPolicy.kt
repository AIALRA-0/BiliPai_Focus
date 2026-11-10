package com.android.purebilibili.feature.settings

internal enum class SpacePrivacyGroup(val title: String) {
    CONTENT("内容与列表"),
    PROFILE("资料与勋章"),
    SUBMISSIONS("投稿列表展示"),
}

internal enum class SpacePrivacyOption(
    val field: String,
    val title: String,
    val group: SpacePrivacyGroup,
    val reversed: Boolean = false,
) {
    FAVORITES("fav_video", "公开我的收藏", SpacePrivacyGroup.CONTENT),
    BANGUMI("bangumi", "公开我的追番追剧", SpacePrivacyGroup.CONTENT),
    COMICS("comic", "公开我的追漫", SpacePrivacyGroup.CONTENT),
    COINS("coins_video", "公开最近投币的视频", SpacePrivacyGroup.CONTENT),
    LIKES("likes_video", "公开最近点赞的视频", SpacePrivacyGroup.CONTENT),
    GAMES("played_game", "公开最近玩过的游戏", SpacePrivacyGroup.CONTENT),
    DRESS("dress_up", "公开拥有的粉丝装扮", SpacePrivacyGroup.CONTENT),
    FOLLOWING("disable_following", "公开我的关注列表", SpacePrivacyGroup.CONTENT, true),
    FANS("disable_show_fans", "公开我的粉丝列表", SpacePrivacyGroup.CONTENT, true),
    MEDAL("close_space_medal", "公开佩戴的粉丝勋章", SpacePrivacyGroup.PROFILE, true),
    MEDAL_WALL("only_show_wearing", "勋章墙公开显示所有粉丝勋章", SpacePrivacyGroup.PROFILE, true),
    SCHOOL("disable_show_school", "公开学校信息", SpacePrivacyGroup.PROFILE, true),
    LIVE_REPLAY("live_playback", "展示直播回放", SpacePrivacyGroup.SUBMISSIONS),
    CHARGE("charge_video", "展示包月充电专属视频", SpacePrivacyGroup.SUBMISSIONS),
    LESSON("lesson_video", "展示课堂视频", SpacePrivacyGroup.SUBMISSIONS);

    fun checked(raw: Int?): Boolean? = when (raw) {
        0 -> reversed
        1 -> !reversed
        else -> null
    }

    fun raw(checked: Boolean): Int = if (checked != reversed) 1 else 0
}

/** 只提交服务端提供、且本次实际修改的已知开关。 */
internal fun spacePrivacyChanges(original: Map<String, Int>?, draft: Map<String, Int>): Map<String, Int> =
    SpacePrivacyOption.entries.mapNotNull { option ->
        val before = original?.get(option.field)
        val after = draft[option.field]
        if (before != null && before in 0..1 && after != null && after in 0..1 && before != after) option.field to after else null
    }.toMap()
