package com.android.purebilibili

import androidx.appcompat.app.AppCompatActivity
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class MainActivityAppCompatContractTest {

    @Test
    fun mainActivity_shouldExtendAppCompatActivity_forRuntimeLocaleUpdates() {
        assertTrue(
            AppCompatActivity::class.java.isAssignableFrom(MainActivity::class.java)
        )
    }

    @Test
    fun splashPostTheme_shouldUseAppCompatDayNightMainTheme() {
        val lightThemes = loadResourceText("values/themes.xml")
        val nightThemes = loadResourceText("values-night/themes.xml")

        assertTrue(
            lightThemes.contains("""<item name="postSplashScreenTheme">@style/Theme.PureBiliBili.Main</item>"""),
            "Light splash theme should hand off to Theme.PureBiliBili.Main"
        )
        assertTrue(
            nightThemes.contains("""<item name="postSplashScreenTheme">@style/Theme.PureBiliBili.Main</item>"""),
            "Night splash theme should hand off to Theme.PureBiliBili.Main"
        )
        assertTrue(
            lightThemes.contains("""<style name="Theme.PureBiliBili.Main" parent="Theme.AppCompat.DayNight.NoActionBar">"""),
            "Light main theme must use an AppCompat descendant for MainActivity"
        )
        assertTrue(
            nightThemes.contains("""<style name="Theme.PureBiliBili.Main" parent="Theme.AppCompat.DayNight.NoActionBar">"""),
            "Night main theme must use an AppCompat descendant for MainActivity"
        )
    }

    @Test
    fun mainActivity_shouldUseCachedAppLanguageAsComposeInitialValue() {
        val mainActivitySource = loadMainActivitySource()

        assertTrue(
            Regex(
                """SettingsManager\.getAppLanguage\(context\)\.collectAsState\(\s*initial = SettingsManager\.getAppLanguageSync\(context\)\s*\)"""
            ).containsMatchIn(mainActivitySource),
            "MainActivity should bootstrap appLanguage from cached settings to avoid locale flip-flop during recreation"
        )
    }

    private fun loadResourceText(resourcePath: String): String {
        val candidates = listOf(
            File("app/src/main/res/$resourcePath"),
            File("src/main/res/$resourcePath")
        )
        val resourceFile = candidates.firstOrNull { it.exists() }
            ?: error("Cannot locate $resourcePath from ${File(".").absolutePath}")
        return resourceFile.readText()
    }

    private fun loadMainActivitySource(): String {
        val candidates = listOf(
            File("app/src/main/java/com/android/purebilibili/MainActivity.kt"),
            File("src/main/java/com/android/purebilibili/MainActivity.kt")
        )
        val sourceFile = candidates.firstOrNull { it.exists() }
            ?: error("Cannot locate MainActivity.kt from ${File(".").absolutePath}")
        return sourceFile.readText()
    }
}
