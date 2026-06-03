package com.android.purebilibili.navigation3

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BiliPaiNavDisplayHostStructureTest {

    @Test
    fun navDisplayHostOwnsNavigation3RenderingAndSharedTransitionScope() {
        val source = navDisplayHostSource()

        assertTrue(source.contains("NavDisplay("))
        assertTrue(source.contains("entryProvider"))
        assertTrue(source.contains("LocalNavAnimatedContentScope.current"))
        assertTrue(source.contains("ProvideAnimatedVisibilityScope("))
        assertTrue(source.contains("LocalVideoCardSharedElementSourceRoute provides key.toLegacyRoute()"))
        assertTrue(source.contains("sharedTransitionScope = sharedTransitionScope"))
        assertTrue(source.contains("predictivePopTransitionSpec"))
    }

    @Test
    fun navDisplayHostScopesEntryStateWithLifecycleNavigation3Decorator() {
        val source = navDisplayHostSource()
        val buildFile = buildFileSource()

        assertTrue(buildFile.contains("androidx.lifecycle:lifecycle-viewmodel-navigation3:"))
        assertTrue(buildFile.contains("androidx.navigationevent:navigationevent-compose:1.1.1"))
        assertTrue(source.contains("rememberDecoratedNavEntries("))
        assertTrue(source.contains("rememberSceneState("))
        assertTrue(source.contains("rememberSaveableStateHolderNavEntryDecorator"))
        assertTrue(source.contains("rememberViewModelStoreNavEntryDecorator"))
    }

    @Test
    fun navDisplayHostHoistsNavigationEventStateIntoNavDisplay() {
        val source = navDisplayHostSource()

        assertTrue(source.contains("rememberNavigationEventState("))
        assertTrue(source.contains("NavigationBackHandler("))
        assertTrue(source.contains("onBackCompleted = onBack"))
        assertTrue(source.contains("navigationEventState = navigationEventState"))
        assertTrue(source.contains("sceneState = sceneState"))
        kotlin.test.assertFalse(source.contains("NavDisplay(\n        backStack = safeBackStack"))
    }

    @Test
    fun navDisplayHostPreservesApplicationExtrasForEntryViewModels() {
        val source = navDisplayHostSource()

        assertTrue(source.contains("ProvideNavigation3ViewModelApplicationExtras("))
        assertTrue(source.contains("LocalViewModelStoreOwner provides patchedOwner"))
        assertTrue(source.contains("APPLICATION_KEY"))
    }

    @Test
    fun navDisplayHostDoesNotRegisterClassicBackInterceptor() {
        val source = navDisplayHostSource()

        assertTrue(source.contains("NavDisplay("))
        assertTrue(source.contains("onBack = onBack"))
        assertFalse(source.contains("import androidx.activity.compose.BackHandler"))
        assertFalse(source.contains("BackHandler(enabled"))
    }

    @Test
    fun navDisplayHostDoesNotKeepRemovedVideoReturnState() {
        val source = navDisplayHostSource()

        assertFalse(source.contains("LocalVideo" + "PredictiveReturnState"))
        assertFalse(source.contains("Video" + "PredictiveReturnState"))
        assertFalse(source.contains("video" + "PredictiveReturnToCardEnabled"))
        assertFalse(source.contains("video" + "PredictiveReturnSourceBounds"))
        assertFalse(source.contains("predictive" + "BackAnimationDecorator"))
        assertFalse(source.contains("rememberBiliPai" + "PredictiveBackMotion"))
        assertFalse(source.contains("onPredictiveBackGestureChange"))
        assertFalse(source.contains("LaunchedEffect(predictiveBackGestureState)"))
    }

    @Test
    fun navDisplayHostRoutesPlainPopThroughRouteTransitionPolicyBeforeDefaultMotion() {
        val source = navDisplayHostSource()

        assertTrue(source.contains("val popRouteTransition = remember("))
        assertTrue(source.contains("resolveBiliPaiNavPopContentTransform(popRouteTransition)"))
        assertFalse(source.contains("predictiveBackMotion"))
    }

    private fun navDisplayHostSource(): String {
        return listOf(
            File("app/src/main/java/com/android/purebilibili/navigation3/BiliPaiNavDisplayHost.kt"),
            File("src/main/java/com/android/purebilibili/navigation3/BiliPaiNavDisplayHost.kt")
        ).first { it.exists() }.readText()
    }

    private fun buildFileSource(): String {
        return listOf(
            File("app/build.gradle.kts"),
            File("build.gradle.kts")
        ).first { it.exists() }.readText()
    }
}
