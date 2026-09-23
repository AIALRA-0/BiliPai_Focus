package com.android.purebilibili.navigation

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MainHostTabBackHandlerStructureTest {

    @Test
    fun mainHostTabBackHandlerUsesMiuixCompletionOnlyApi() {
        val source = mainHostTabBackHandlerSource()

        // The Miuix handler completes the route action directly; it has no commit callback.
        assertTrue(source.contains("NavigationBackHandler("))
        assertTrue(source.contains("rememberNavigationEventState(NavigationEventInfo.None)"))
        assertTrue(source.contains("isBackEnabled = enabled"))
        assertTrue(source.contains("onBackCompleted = onReturnToHomeTab"))
        assertFalse(source.contains("commitTransition"))

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
