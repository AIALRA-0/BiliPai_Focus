package com.android.purebilibili.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.purebilibili.core.ui.LocalComponentMotionEnabled
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DonationPreviewMotionTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun backCanInterruptOpeningCancelRestoreAndCommitWithoutRemountingEitherContent() {
        lateinit var motion: DonationPreviewMotion
        var cardMounts = 0
        var previewMounts = 0
        rule.mainClock.autoAdvance = false
        rule.setContent {
            MaterialTheme {
                val current = rememberDonationPreviewMotion()
                SideEffect { motion = current }
                DonationPreviewContainer(
                    progress = { current.progress.value },
                    previewActive = current.expanded,
                    compactMaxWidth = 280.dp,
                    compact = {
                        DisposableEffect(Unit) { cardMounts++; onDispose {} }
                        Box(Modifier.height(220.dp))
                    },
                    preview = {
                        DisposableEffect(Unit) { previewMounts++; onDispose {} }
                        Box(Modifier.fillMaxSize())
                    },
                )
            }
        }
        rule.runOnIdle { motion.open() }
        rule.mainClock.advanceTimeBy(96)
        rule.runOnIdle {
            val interrupted = motion.progress.value
            assertTrue(interrupted > 0f && interrupted < 1f)
            motion.onBackStarted()
            motion.onBackProgress(0.4f)
            assertEquals(interrupted * 0.6f, motion.progress.value, 0.0001f)
            motion.onBackCancelled()
        }
        rule.mainClock.advanceTimeBy(2000)
        rule.runOnIdle {
            assertTrue(motion.expanded)
            assertEquals(1f, motion.progress.value, 0.0001f)
            motion.onBackStarted()
            motion.onBackProgress(0.75f)
            motion.close()
        }
        rule.mainClock.advanceTimeBy(2000)
        rule.runOnIdle {
            assertFalse(motion.expanded)
            assertEquals(0f, motion.progress.value, 0.0001f)
            assertEquals(1, cardMounts)
            assertEquals(1, previewMounts)
        }
    }

    @Test
    fun disablingMotionDuringOpeningSnapsToCurrentTargetAndKeepsOrdinaryReturn() {
        val enabled = mutableStateOf(true)
        lateinit var motion: DonationPreviewMotion
        rule.mainClock.autoAdvance = false
        rule.setContent {
            CompositionLocalProvider(LocalComponentMotionEnabled provides enabled.value) {
                val current = rememberDonationPreviewMotion()
                SideEffect { motion = current }
            }
        }
        rule.runOnIdle { motion.open() }
        rule.mainClock.advanceTimeBy(64)
        rule.runOnIdle { enabled.value = false }
        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle {
            assertTrue(motion.expanded)
            assertFalse(motion.predictive)
            assertEquals(1f, motion.progress.value, 0.0001f)
            motion.close()
        }
        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle {
            assertFalse(motion.expanded)
            assertEquals(0f, motion.progress.value, 0.0001f)
        }
    }
}
