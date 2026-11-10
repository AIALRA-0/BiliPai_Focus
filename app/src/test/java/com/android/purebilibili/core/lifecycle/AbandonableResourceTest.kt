package com.android.purebilibili.core.lifecycle

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composition
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.remember
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AbandonableResourceTest {
    @Test
    fun abortedCompositionReleasesResourceBeforeDisposableEffectWasRegistered() {
        var liveResources = 0
        var effectRegistrations = 0
        repeat(20) {
            val recomposer = Recomposer(EmptyCoroutineContext)
            val composition = Composition(UnitApplier(), recomposer)
            try {
                assertFailsWith<IllegalStateException> {
                    composition.setContent {
                        val resource = remember {
                            liveResources++
                            AbandonableResource(Any()) { liveResources-- }
                        }.value
                        DisposableEffect(resource) {
                            effectRegistrations++
                            onDispose { liveResources-- }
                        }
                        error("Abort before applying effects")
                    }
                }
                assertEquals(0, liveResources)
                assertEquals(0, effectRegistrations)
            } finally {
                composition.dispose()
                recomposer.cancel()
            }
        }
    }

    @Test
    fun committedCompositionTransfersCleanupToItsNormalOwnerExactlyOnce() {
        var releases = 0
        val recomposer = Recomposer(EmptyCoroutineContext)
        val composition = Composition(UnitApplier(), recomposer)
        try {
            composition.setContent {
                val resource = remember {
                    AbandonableResource(Any()) { releases++ }
                }.value
                DisposableEffect(resource) {
                    onDispose { releases++ }
                }
            }
            assertEquals(0, releases)
            composition.dispose()
            assertEquals(1, releases)
        } finally {
            composition.dispose()
            recomposer.cancel()
        }
    }

    private class UnitApplier : AbstractApplier<Unit>(Unit) {
        override fun insertTopDown(index: Int, instance: Unit) = Unit
        override fun insertBottomUp(index: Int, instance: Unit) = Unit
        override fun remove(index: Int, count: Int) = Unit
        override fun move(from: Int, to: Int, count: Int) = Unit
        override fun onClear() = Unit
    }
}
