package com.android.purebilibili.core.plugin.skin

import com.android.purebilibili.testutil.projectSourceFile
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UiSkinSettingsStoreStructureTest {

    @Test
    fun preferenceListenerOnlyInvalidatesAndSnapshotsRunOnIo() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/core/plugin/skin/UiSkinSettingsStore.kt")
        val listenerBody = source
            .substringAfter("OnSharedPreferenceChangeListener {", missingDelimiterValue = "")
            .substringBefore("\n            }", missingDelimiterValue = "")

        assertTrue(listenerBody.contains("trySend(Unit)"))
        assertFalse(listenerBody.contains("readState"))
        assertTrue(source.contains(".buffer(Channel.CONFLATED)"))
        assertTrue(source.contains(".map { readState(appContext) }"))
        assertTrue(source.contains(".flowOn(Dispatchers.IO)"))
        assertTrue(source.contains("if (!selection.enabled) return UiSkinState()"))
    }

    @Test
    fun navigationOwnsOneRememberedSkinFlowForChildScreens() {
        val storeSource = loadSource(
            "app/src/main/java/com/android/purebilibili/core/plugin/skin/UiSkinSettingsStore.kt"
        )
        val navigationSource = loadSource("app/src/main/java/com/android/purebilibili/navigation/AppNavigation.kt")
        val homeSource = loadSource("app/src/main/java/com/android/purebilibili/feature/home/HomeScreen.kt")
        val pluginsSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/settings/screen/PluginsScreen.kt"
        )

        assertTrue(storeSource.contains("remember(appContext) { UiSkinSettingsStore.observe(appContext) }"))
        assertTrue(navigationSource.contains("val uiSkinState by rememberUiSkinState(context)"))
        assertTrue(navigationSource.contains("LocalUiSkinState provides uiSkinState"))
        assertTrue(homeSource.contains("val uiSkinState = LocalUiSkinState.current"))
        assertTrue(pluginsSource.contains("val uiSkinState = LocalUiSkinState.current"))
        assertFalse(homeSource.contains("rememberUiSkinState(context)"))
        assertFalse(pluginsSource.contains("rememberUiSkinState(context)"))
        assertTrue(pluginsSource.contains("LaunchedEffect(uiSkinStore)"))
        assertTrue(pluginsSource.contains("uiSkinStore.listInstalledPackages()"))
    }

    private fun loadSource(path: String): String = projectSourceFile(path).readText()
}
