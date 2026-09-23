package com.android.purebilibili.feature.dynamic

import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.model.response.DynamicModules
import com.android.purebilibili.data.model.response.DynamicStatModule
import com.android.purebilibili.data.model.response.StatItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DynamicLikeCountPolicyTest {

    @Test
    fun requestGateRejectsConcurrentMutationForSameDynamicUntilReleased() {
        val gate = DynamicLikeRequestGate()

        assertTrue(gate.tryAcquire("dynamic-1"))
        assertFalse(gate.tryAcquire("dynamic-1"))
        assertTrue(gate.tryAcquire("dynamic-2"))

        gate.release("dynamic-1")
        assertTrue(gate.tryAcquire("dynamic-1"))
    }

    @Test
    fun explicitLocalUnlikeOverridesStaleServerLike() {
        assertEquals(
            false,
            resolveDynamicLikeState(localOverride = false, localLiked = false, serverLiked = true),
        )
        assertEquals(
            true,
            resolveDynamicLikeState(localOverride = true, localLiked = false, serverLiked = false),
        )
    }

    @Test
    fun applyDynamicLikeCountChange_incrementsWhenLiked() {
        val items = listOf(
            DynamicItem(
                id_str = "100",
                modules = DynamicModules(
                    module_stat = DynamicStatModule(
                        like = StatItem(count = 9)
                    )
                )
            )
        )

        val updated = applyDynamicLikeCountChange(items, dynamicId = "100", toLiked = true)

        assertEquals(10, updated.first().modules.module_stat?.like?.count)
    }

    @Test
    fun applyDynamicLikeCountChange_decrementsWhenUnliked() {
        val items = listOf(
            DynamicItem(
                id_str = "100",
                modules = DynamicModules(
                    module_stat = DynamicStatModule(
                        like = StatItem(count = 9)
                    )
                )
            )
        )

        val updated = applyDynamicLikeCountChange(items, dynamicId = "100", toLiked = false)

        assertEquals(8, updated.first().modules.module_stat?.like?.count)
    }

    @Test
    fun applyDynamicLikeCountChange_neverGoesBelowZero() {
        val items = listOf(
            DynamicItem(
                id_str = "100",
                modules = DynamicModules(
                    module_stat = DynamicStatModule(
                        like = StatItem(count = 0)
                    )
                )
            )
        )

        val updated = applyDynamicLikeCountChange(items, dynamicId = "100", toLiked = false)

        assertEquals(0, updated.first().modules.module_stat?.like?.count)
    }

    @Test
    fun applyDynamicLikeCountChange_updatesServerLikeStatus() {
        val item = DynamicItem(
            id_str = "100",
            modules = DynamicModules(
                module_stat = DynamicStatModule(like = StatItem(count = 1))
            )
        )

        val liked = applyDynamicLikeCountChange(listOf(item), "100", true).first()
        val unliked = applyDynamicLikeCountChange(listOf(liked), "100", false).first()

        assertEquals(true, liked.modules.module_stat?.like?.status)
        assertEquals(false, unliked.modules.module_stat?.like?.status)
    }
}
