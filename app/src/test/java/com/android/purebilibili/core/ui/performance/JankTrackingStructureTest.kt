package com.android.purebilibili.core.ui.performance

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JankTrackingStructureTest {

    @Test
    fun stateValueMarkerUpdatesSynchronouslyAfterComposition() {
        val source = listOf(
            File("app/src/main/java/com/android/purebilibili/core/ui/performance/JankTracking.kt"),
            File("src/main/java/com/android/purebilibili/core/ui/performance/JankTracking.kt"),
        ).first { it.exists() }.readText()
        val stateValueBlock = source
            .substringAfter("fun TrackJankStateValue(")
            .substringBefore("fun TrackJankStateFlag(")

        assertTrue(stateValueBlock.contains("SideEffect {"))
        assertTrue(stateValueBlock.contains("putState(stateName, stateValue)"))
        assertTrue(stateValueBlock.contains("DisposableEffect(metrics, stateName)"))
        assertFalse(stateValueBlock.contains("LaunchedEffect("))
    }
}
