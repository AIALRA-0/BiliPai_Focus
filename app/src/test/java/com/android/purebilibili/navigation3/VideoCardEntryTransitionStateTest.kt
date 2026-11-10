package com.android.purebilibili.navigation3

import com.android.purebilibili.core.ui.transition.MiuixVideoCardTransitionState
import com.android.purebilibili.core.ui.transition.VideoCardSourceChromeSnapshot
import kotlin.test.Test
import kotlin.test.assertSame

class VideoCardEntryTransitionStateTest {
    private fun transition(title: String) = MiuixVideoCardTransitionState(
        enabled = true,
        sourceChromeSnapshot = VideoCardSourceChromeSnapshot(title = title, ownerName = title),
    )

    @Test
    fun returningFirstCardKeepsItsTextAndDriverWhenSecondHomeCardOpens() {
        val first = transition("第一张卡片")
        val second = transition("第二张卡片")
        val source = BiliPaiNavSourceMetadata(sourceRoute = "home", sourceKey = "home:BV2")

        assertSame(first, resolveVideoCardEntryTransitionState(
            BiliPaiNavKey.VideoDetail(bvid = "BV1", sourceRoute = "home"),
            source, second, first,
        ))
        assertSame(second, resolveVideoCardEntryTransitionState(
            BiliPaiNavKey.VideoDetail(bvid = "BV2", sourceRoute = "home"),
            source, second, MiuixVideoCardTransitionState(),
        ))
    }

    @Test
    fun restoredParentCanReceiveItsOwnReturnSessionAgain() {
        val parent = transition("父详情来源")
        val source = BiliPaiNavSourceMetadata(sourceRoute = "home", sourceKey = "home:BV1")

        assertSame(parent, resolveVideoCardEntryTransitionState(
            BiliPaiNavKey.VideoDetail(bvid = "BV1", sourceRoute = "home"),
            source, parent, transition("旧驱动"),
        ))
    }

    @Test
    fun sameVideoFromAnotherPageCannotReplaceRetainedSource() {
        val retained = transition("首页卡片")
        val source = BiliPaiNavSourceMetadata(sourceRoute = "search", sourceKey = "search:BV1")

        assertSame(retained, resolveVideoCardEntryTransitionState(
            BiliPaiNavKey.VideoDetail(bvid = "BV1", sourceRoute = "home"),
            source, transition("搜索卡片"), retained,
        ))
    }
}
