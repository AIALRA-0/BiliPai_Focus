package com.android.purebilibili.feature.live

/** Main-thread request ownership for a single list, independent of transport cancellation. */
internal class LiveBrowseRequestPolicy {
    class Request internal constructor(val page: Int, val append: Boolean)

    private var active: Request? = null
    private var nextPage = 1

    fun refresh(replace: Boolean = false): Request? {
        if (!replace && active?.append == false) return null
        return Request(page = 1, append = false).also { active = it }
    }

    fun loadMore(hasMore: Boolean): Request? {
        if (active != null || !hasMore) return null
        return Request(page = nextPage, append = true).also { active = it }
    }

    fun owns(request: Request): Boolean = active === request

    fun succeed(request: Request, followingPage: Int = request.page + 1): Boolean {
        if (!owns(request)) return false
        nextPage = followingPage
        active = null
        return true
    }

    fun fail(request: Request): Boolean {
        if (!owns(request)) return false
        active = null
        return true
    }

    fun invalidate() {
        active = null
        nextPage = 1
    }
}
