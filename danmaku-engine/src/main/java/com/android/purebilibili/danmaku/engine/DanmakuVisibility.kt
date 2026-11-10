package com.android.purebilibili.danmaku.engine

/** One immutable selection shared by existing and newly created render data. */
internal class DanmakuVisibility {
    @Volatile
    private var hidden = HiddenItems(emptySet(), emptySet())
    private var closed = false

    private class HiddenItems(val ids: Set<Long>, val texts: Set<String>)

    @Synchronized
    fun setHiddenItems(ids: Set<Long>, texts: Set<String>): Boolean {
        val previous = hidden
        if (closed || (previous.ids == ids && previous.texts == texts)) return false
        // Copy only changed sets; never retain the caller's mutable collections.
        hidden = HiddenItems(
            if (previous.ids == ids) previous.ids else ids.toSet(),
            if (previous.texts == texts) previous.texts else texts.toSet()
        )
        return true
    }

    fun isVisible(danmakuId: Long, text: String? = null): Boolean {
        val selection = hidden
        if (danmakuId in selection.ids) return false
        return text == null || text !in selection.texts
    }

    @Synchronized
    fun close() {
        closed = true
        hidden = HiddenItems(emptySet(), emptySet())
    }
}
