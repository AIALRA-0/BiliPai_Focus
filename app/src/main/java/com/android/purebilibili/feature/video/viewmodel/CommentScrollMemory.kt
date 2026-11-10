package com.android.purebilibili.feature.video.viewmodel

/** 仅保留当前评论主体与排序的位置；旧面板的延迟回写不能覆盖新会话。 */
internal class CommentScrollMemory {
    internal class Session(
        val aid: Long,
        val sortMode: CommentSortMode,
        var index: Int = 0,
        var offset: Int = 0,
    )

    private var current: Session? = null

    fun open(aid: Long, sortMode: CommentSortMode): Session {
        return current?.takeIf { it.aid == aid && it.sortMode == sortMode }
            ?: Session(aid, sortMode).also { current = it }
    }

    fun save(session: Session, index: Int, offset: Int) {
        if (current !== session) return
        session.index = index.coerceAtLeast(0)
        session.offset = offset.coerceAtLeast(0)
    }

    fun clear() {
        current = null
    }
}
