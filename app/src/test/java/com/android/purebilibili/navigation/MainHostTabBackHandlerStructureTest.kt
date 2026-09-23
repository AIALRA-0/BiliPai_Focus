package com.android.purebilibili.navigation

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MainHostTabBackHandlerStructureTest {

    @Test
    fun mainHostTabBackHandler_copiesBiliPaiMainScreenBackHandler() {
        val source = mainHostTabBackHandlerSource()

        // BiliPai MainScreenBackHandler: NavigationBackHandler + onBackCompleted only.
        assertTrue(source.contains("NavigationBackHandler("))
        assertTrue(source.contains("rememberNavigationEventState(NavigationEventInfo.None)"))
        assertTrue(source.contains("isBackEnabled = enabled"))
        assertTrue(source.contains("onBackCompleted = { commitTransition ->"))
        assertTrue(source.contains("onReturnToHomeTab()"))
        assertTrue(source.contains("commitTransition()"))

        // No self-invented predictive progress seek path.
        assertFalse(source.contains("snapshotFlow"))
        assertFalse(source.contains("onPredictiveProgress"))
        assertFalse(source.contains("NavigationEventTransitionState.InProgress"))
        assertFalse(source.contains("import androidx.activity.compose.BackHandler"))
        assertFalse(source.contains("androidx.activity.compose.BackHandler "))
    }

    private fun mainHostTabBackHandlerSource(): String {
        return listOf(
            File("app/src/main/java/com/android/purebilibili/navigation/MainHostTabBackHandler.kt"),
            File("src/main/java/com/android/purebilibili/navigation/MainHostTabBackHandler.kt"),
        ).first { it.exists() }.readText()
    }
}
