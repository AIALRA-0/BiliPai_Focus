package com.android.purebilibili

import androidx.appcompat.app.AppCompatActivity
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.InflaterInputStream
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
    fun splashTheme_shouldReuseLauncherMipmapAssets() {
        val lightThemes = loadResourceText("values/themes.xml")
        val nightThemes = loadResourceText("values-night/themes.xml")

        assertTrue(
            lightThemes.contains("""<item name="windowSplashScreenAnimatedIcon">@mipmap/ic_launcher_bilipai</item>"""),
            "Light splash theme should reuse the launcher mipmap instead of packaging duplicate splash bitmaps"
        )
        assertTrue(
            nightThemes.contains("""<item name="windowSplashScreenAnimatedIcon">@mipmap/ic_launcher_bilipai</item>"""),
            "Night splash theme should reuse the launcher mipmap instead of packaging duplicate splash bitmaps"
        )
        assertTrue(
            lightThemes.contains("""<item name="windowSplashScreenIconBackgroundColor">@android:color/transparent</item>"""),
            "Light splash theme should not add a second icon background around the adaptive icon"
        )
        assertTrue(
            !lightThemes.contains("""windowSplashScreenAnimatedIcon">@drawable/splash_icon_""") &&
                !nightThemes.contains("""windowSplashScreenAnimatedIcon">@drawable/splash_icon_"""),
            "Splash themes should not reference duplicate drawable-nodpi splash assets"
        )
        assertTrue(
            !splashDrawableVectorExists(),
            "Splash theme should not keep the hand-drawn drawable foreground vector"
        )
    }

    @Test
    fun bilipaiWhiteSplashTheme_shouldUseReadableLightBackground() {
        val lightThemes = loadResourceText("values/themes.xml")
        val bilipaiWhiteTheme = Regex(
            """<style name="Theme\.PureBiliBili\.Splash\.BiliPaiWhite"[\s\S]*?</style>"""
        ).find(lightThemes)?.value.orEmpty()

        assertTrue(
            bilipaiWhiteTheme.contains("""<item name="windowSplashScreenAnimatedIcon">@mipmap/ic_launcher_bilipai_white</item>"""),
            "BiliPai white splash theme should use the matching white icon"
        )
        assertTrue(
            bilipaiWhiteTheme.contains("""<item name="windowSplashScreenBackground">@color/splash_bilipai_white_background</item>"""),
            "BiliPai white splash theme should not inherit a white splash background because its rounded white shell becomes invisible"
        )
    }

    @Test
    fun splashIcons_shouldNotPackageDuplicateDrawableAssets() {
        listOf(
            "splash_icon_3d.png",
            "splash_icon_bilipai.png",
            "splash_icon_bilipai_pink.png",
            "splash_icon_bilipai_white.png",
            "splash_icon_bilipai_monet.png",
            "splash_icon_flat.png",
            "splash_icon_telegram_blue.png",
            "splash_icon_telegram_dark.png",
            "splash_icon_yuki.png",
            "splash_icon_anime.png",
            "splash_icon_headphone.png"
        ).forEach { fileName ->
            assertTrue(
                !resourcePathExists("drawable-nodpi/$fileName"),
                "$fileName should not be packaged separately; splash should reuse existing launcher mipmaps"
            )
        }
    }

    @Test
    fun legacyLauncherBitmaps_shouldBeRgbaPngWithTransparentCorners() {
        val iconNames = listOf(
            "ic_launcher.png",
            "ic_launcher_round.png",
            "ic_launcher_3d.png",
            "ic_launcher_3d_round.png",
            "ic_launcher_anime.png",
            "ic_launcher_bilipai.png",
            "ic_launcher_bilipai_round.png",
            "ic_launcher_bilipai_monet.png",
            "ic_launcher_bilipai_monet_round.png",
            "ic_launcher_bilipai_pink.png",
            "ic_launcher_bilipai_pink_round.png",
            "ic_launcher_bilipai_white.png",
            "ic_launcher_bilipai_white_round.png",
            "ic_launcher_flat.png",
            "ic_launcher_flat_round.png",
            "ic_launcher_headphone.png"
        )

        listOf("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi").forEach { density ->
            iconNames.forEach { fileName ->
                val imageFile = loadResourceFile("mipmap-$density/$fileName")
                assertTrue(readPngHeader(imageFile).colorType == 6, "$density/$fileName should be an RGBA PNG")
                assertTrue(
                    readPngCornerAlphaValues(imageFile).all { it == 0 },
                    "$density/$fileName should not expose square bitmap corners when used as a fallback launcher icon"
                )
            }
        }
    }

    @Test
    fun launcherAliases_shouldBindMatchingSplashThemesForSelectedIcons() {
        val manifest = loadResourceText("../AndroidManifest.xml")

        mapOf(
            "MainActivityAlias3DLauncher" to SplashAliasContract("MainActivitySplashIcon3D", "Theme.PureBiliBili.Splash.Icon3D", "ic_launcher_3d"),
            "MainActivityAlias3D" to SplashAliasContract("MainActivitySplashIcon3D", "Theme.PureBiliBili.Splash.Icon3D", "ic_launcher_3d"),
            "MainActivityAliasBiliPai" to SplashAliasContract("MainActivitySplashBiliPai", "Theme.PureBiliBili.Splash.BiliPai", "ic_launcher_bilipai"),
            "MainActivityAliasBiliPaiPink" to SplashAliasContract("MainActivitySplashBiliPaiPink", "Theme.PureBiliBili.Splash.BiliPaiPink", "ic_launcher_bilipai_pink"),
            "MainActivityAliasBiliPaiWhite" to SplashAliasContract("MainActivitySplashBiliPaiWhite", "Theme.PureBiliBili.Splash.BiliPaiWhite", "ic_launcher_bilipai_white"),
            "MainActivityAliasBiliPaiMonet" to SplashAliasContract("MainActivitySplashBiliPaiMonet", "Theme.PureBiliBili.Splash.BiliPaiMonet", "ic_launcher_bilipai_monet"),
            "MainActivityAliasFlat" to SplashAliasContract("MainActivitySplashFlat", "Theme.PureBiliBili.Splash.Flat", "ic_launcher_flat"),
            "MainActivityAliasTelegramBlue" to SplashAliasContract("MainActivitySplashTelegramBlue", "Theme.PureBiliBili.Splash.TelegramBlue", "ic_launcher_telegram_blue"),
            "MainActivityAliasDark" to SplashAliasContract("MainActivitySplashTelegramDark", "Theme.PureBiliBili.Splash.TelegramDark", "ic_launcher_telegram_dark"),
            "MainActivityAliasYuki" to SplashAliasContract("MainActivitySplashYuki", "Theme.PureBiliBili.Splash.Yuki", "ic_launcher"),
            "MainActivityAliasAnime" to SplashAliasContract("MainActivitySplashAnime", "Theme.PureBiliBili.Splash.Anime", "ic_launcher_anime"),
            "MainActivityAliasHeadphone" to SplashAliasContract("MainActivitySplashHeadphone", "Theme.PureBiliBili.Splash.Headphone", "ic_launcher_headphone")
        ).forEach { (alias, contract) ->
            val aliasBlock = Regex(
                """<activity-alias\b(?=[^>]*android:name="\.$alias")[\s\S]*?</activity-alias>"""
            ).find(manifest)?.value.orEmpty()
            val targetActivityBlock = Regex(
                """<activity\b(?=[^>]*android:name="\.${contract.targetActivity}")[\s\S]*?(?:</activity>|/>)"""
            ).find(manifest)?.value.orEmpty()

            assertTrue(
                aliasBlock.contains("""android:targetActivity=".${contract.targetActivity}""""),
                "$alias should target ${contract.targetActivity} so Android splash can use the selected icon theme"
            )
            assertTrue(
                aliasBlock.contains("""android:icon="@mipmap/${contract.launcherIcon}""""),
                "$alias should keep the adaptive launcher icon for the home screen"
            )
            assertTrue(
                targetActivityBlock.contains("""android:theme="@style/${contract.theme}""""),
                "${contract.targetActivity} should bind ${contract.theme} so Android splash follows the selected launcher icon"
            )
            assertTrue(
                targetActivityBlock.contains("""android:icon="@mipmap/${contract.launcherIcon}""""),
                "${contract.targetActivity} should reuse ${contract.launcherIcon} instead of exposing duplicate splash drawables"
            )
        }
    }

    @Test
    fun noIconLauncherAliases_shouldKeepLauncherIconButUseTransparentSplashTheme() {
        val manifest = loadResourceText("../AndroidManifest.xml")
        val lightThemes = loadResourceText("values/themes.xml")
        val nightThemes = loadResourceText("values-night/themes.xml")

        assertTrue(
            lightThemes.contains("""<style name="Theme.PureBiliBili.Splash.NoIcon" parent="Theme.PureBiliBili">""") &&
                lightThemes.contains("""<item name="windowSplashScreenAnimatedIcon">@drawable/splash_no_icon</item>"""),
            "Light no-icon splash theme should use the transparent splash icon drawable"
        )
        assertTrue(
            nightThemes.contains("""<style name="Theme.PureBiliBili.Splash.NoIcon" parent="Theme.PureBiliBili">""") &&
                nightThemes.contains("""<item name="windowSplashScreenAnimatedIcon">@drawable/splash_no_icon</item>"""),
            "Night no-icon splash theme should use the transparent splash icon drawable"
        )

        listOf(
            "MainActivityAlias3DNoIcon" to "ic_launcher_3d",
            "MainActivityAliasBiliPaiNoIcon" to "ic_launcher_bilipai",
            "MainActivityAliasBiliPaiPinkNoIcon" to "ic_launcher_bilipai_pink",
            "MainActivityAliasBiliPaiWhiteNoIcon" to "ic_launcher_bilipai_white",
            "MainActivityAliasBiliPaiMonetNoIcon" to "ic_launcher_bilipai_monet",
            "MainActivityAliasFlatNoIcon" to "ic_launcher_flat",
            "MainActivityAliasTelegramBlueNoIcon" to "ic_launcher_telegram_blue",
            "MainActivityAliasDarkNoIcon" to "ic_launcher_telegram_dark",
            "MainActivityAliasYukiNoIcon" to "ic_launcher",
            "MainActivityAliasAnimeNoIcon" to "ic_launcher_anime",
            "MainActivityAliasHeadphoneNoIcon" to "ic_launcher_headphone"
        ).forEach { (alias, launcherIcon) ->
            val aliasBlock = Regex(
                """<activity-alias\b(?=[^>]*android:name="\.$alias")[\s\S]*?</activity-alias>"""
            ).find(manifest)?.value.orEmpty()

            assertTrue(
                aliasBlock.contains("""android:targetActivity=".MainActivitySplashNoIcon""""),
                "$alias should target the transparent splash activity"
            )
            assertTrue(
                aliasBlock.contains("""android:icon="@mipmap/$launcherIcon""""),
                "$alias should keep the selected launcher icon on the home screen"
            )
        }
    }

    @Test
    fun splashFlyout_shouldReuseLauncherIconForSelectedLauncherComponent() {
        mapOf(
            "com.android.purebilibili.MainActivityAlias3DLauncher" to R.mipmap.ic_launcher_3d,
            "com.android.purebilibili.MainActivitySplashIcon3D" to R.mipmap.ic_launcher_3d,
            "com.android.purebilibili.MainActivityAliasBiliPai" to R.mipmap.ic_launcher_bilipai,
            "com.android.purebilibili.MainActivitySplashBiliPai" to R.mipmap.ic_launcher_bilipai,
            "com.android.purebilibili.MainActivityAliasBiliPaiPink" to R.mipmap.ic_launcher_bilipai_pink,
            "com.android.purebilibili.MainActivityAliasBiliPaiWhite" to R.mipmap.ic_launcher_bilipai_white,
            "com.android.purebilibili.MainActivityAliasBiliPaiMonet" to R.mipmap.ic_launcher_bilipai_monet,
            "com.android.purebilibili.MainActivityAliasFlat" to R.mipmap.ic_launcher_flat,
            "com.android.purebilibili.MainActivityAliasTelegramBlue" to R.mipmap.ic_launcher_telegram_blue,
            "com.android.purebilibili.MainActivityAliasDark" to R.mipmap.ic_launcher_telegram_dark,
            "com.android.purebilibili.MainActivityAliasYuki" to R.mipmap.ic_launcher,
            "com.android.purebilibili.MainActivityAliasAnime" to R.mipmap.ic_launcher_anime,
            "com.android.purebilibili.MainActivityAliasHeadphone" to R.mipmap.ic_launcher_headphone,
            "com.android.purebilibili.MainActivityAlias3DNoIcon" to R.mipmap.ic_launcher_3d
        ).forEach { (className, iconResId) ->
            assertTrue(
                resolveSplashIconResIdForComponentClassName(className) == iconResId,
                "$className should resolve to the matching launcher mipmap"
            )
        }
    }

    @Test
    fun appIconSwitch_shouldNotRequestAppRestartOrRecreate() {
        val settingsViewModelSource = loadSettingsViewModelSource()
        val launcherAliasSwitchBody = Regex(
            """private suspend fun applyLauncherAliasForCurrentSplashIconSetting\(iconKey: String\) \{[\s\S]*?\n    \}"""
        ).find(settingsViewModelSource)?.value ?: Regex(
            """fun setAppIcon\(iconKey: String\) \{[\s\S]*?\n    \}"""
        ).find(settingsViewModelSource)?.value.orEmpty()

        assertTrue(
            launcherAliasSwitchBody.contains("PackageManager.DONT_KILL_APP"),
            "Icon switching should request DONT_KILL_APP to avoid reloading the running app"
        )
        assertTrue(
            !launcherAliasSwitchBody.contains("restartApp") && !launcherAliasSwitchBody.contains(".recreate("),
            "Icon switching should not explicitly restart or recreate the current app UI"
        )
    }

    @Test
    fun mainActivity_shouldUseCachedAppLanguageAsComposeInitialValue() {
        val mainActivitySource = loadMainActivitySource()
        val themeSource = loadThemeSource()

        assertTrue(
            mainActivitySource.contains(".getAppThemeSettings(context)"),
            "MainActivity should collect startup theme settings through one DataStore Flow"
        )
        assertTrue(
            mainActivitySource.contains("initialValue = SettingsManager.getInitialAppThemeSettings(context)"),
            "MainActivity should bootstrap appLanguage from cached settings to avoid locale flip-flop during recreation"
        )
        assertTrue(
            themeSource.contains("ThemeController("),
            "Theme root should build a miuix ThemeController"
        )
        assertTrue(
            mainActivitySource.contains("appThemeSettings.uiPreset"),
            "MainActivity should keep reading UiPreset when iOS and Android Native presets are available again"
        )
        assertTrue(
            mainActivitySource.contains("AppThemeSettings(") ||
                mainActivitySource.contains("getInitialAppThemeSettings(context)"),
            "MainActivity should bootstrap first install with the MD3 preset"
        )
    }

    private fun loadResourceFile(resourcePath: String): File {
        val candidates = listOf(
            File("app/src/main/res/$resourcePath"),
            File("src/main/res/$resourcePath")
        )
        return candidates.firstOrNull { it.exists() }
            ?: error("Cannot locate $resourcePath from ${File(".").absolutePath}")
    }

    private fun resourcePathExists(resourcePath: String): Boolean {
        return listOf(
            File("app/src/main/res/$resourcePath"),
            File("src/main/res/$resourcePath")
        ).any { it.exists() }
    }

    private fun loadResourceText(resourcePath: String): String {
        return loadResourceFile(resourcePath).readText()
    }

    private data class ImageSize(val width: Int, val height: Int)

    private data class PngHeader(val width: Int, val height: Int, val colorType: Int)

    private data class SplashAliasContract(
        val targetActivity: String,
        val theme: String,
        val launcherIcon: String
    )

    private fun readPngHeader(file: File): PngHeader {
        val bytes = file.readBytes()
        assertTrue(
            bytes.size >= 24 &&
                bytes[0] == 0x89.toByte() &&
                bytes[1] == 'P'.code.toByte() &&
                bytes[2] == 'N'.code.toByte() &&
                bytes[3] == 'G'.code.toByte(),
            "${file.name} should be a real PNG file"
        )
        return PngHeader(
            width = bytes.readBigEndianInt(offset = 16),
            height = bytes.readBigEndianInt(offset = 20),
            colorType = bytes[25].toInt() and 0xFF
        )
    }

    private fun readPngCornerAlphaValues(file: File): List<Int> {
        val bytes = file.readBytes()
        var offset = 8
        var width = 0
        var height = 0
        var bitDepth = 0
        var colorType = 0
        val idat = ByteArrayOutputStream()

        while (offset + 12 <= bytes.size) {
            val length = bytes.readBigEndianInt(offset)
            val type = String(bytes, offset + 4, 4)
            val dataOffset = offset + 8
            when (type) {
                "IHDR" -> {
                    width = bytes.readBigEndianInt(dataOffset)
                    height = bytes.readBigEndianInt(dataOffset + 4)
                    bitDepth = bytes[dataOffset + 8].toInt() and 0xFF
                    colorType = bytes[dataOffset + 9].toInt() and 0xFF
                }
                "IDAT" -> idat.write(bytes, dataOffset, length)
                "IEND" -> break
            }
            offset += 12 + length
        }

        assertTrue(bitDepth == 8 && colorType == 6, "${file.name} should be an 8-bit RGBA PNG")

        val inflated = InflaterInputStream(idat.toByteArray().inputStream()).readBytes()
        val rows = decodePngRgbaRows(
            inflated = inflated,
            width = width,
            height = height
        )
        fun alphaAt(x: Int, y: Int): Int = rows[y][x * 4 + 3]
        return listOf(
            alphaAt(0, 0),
            alphaAt(width - 1, 0),
            alphaAt(0, height - 1),
            alphaAt(width - 1, height - 1)
        )
    }

    private fun readPngOrJpegSize(file: File): ImageSize {
        val bytes = file.readBytes()
        if (bytes.size >= 24 &&
            bytes[0] == 0x89.toByte() &&
            bytes[1] == 'P'.code.toByte() &&
            bytes[2] == 'N'.code.toByte() &&
            bytes[3] == 'G'.code.toByte()
        ) {
            return ImageSize(
                width = bytes.readBigEndianInt(offset = 16),
                height = bytes.readBigEndianInt(offset = 20)
            )
        }

        if (bytes.size >= 4 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()) {
            var offset = 2
            while (offset + 9 < bytes.size) {
                while (offset < bytes.size && bytes[offset] != 0xFF.toByte()) {
                    offset++
                }
                if (offset + 3 >= bytes.size) break
                val marker = bytes[offset + 1].toInt() and 0xFF
                offset += 2
                if (marker in 0xD0..0xD9 || marker == 0x01) continue
                val segmentLength = bytes.readUnsignedShort(offset)
                if (marker in listOf(0xC0, 0xC1, 0xC2, 0xC3, 0xC5, 0xC6, 0xC7, 0xC9, 0xCA, 0xCB, 0xCD, 0xCE, 0xCF)) {
                    return ImageSize(
                        width = bytes.readUnsignedShort(offset + 5),
                        height = bytes.readUnsignedShort(offset + 3)
                    )
                }
                offset += segmentLength
            }
        }

        error("Unsupported image header for ${file.path}")
    }

    private fun ByteArray.readBigEndianInt(offset: Int): Int {
        return ((this[offset].toInt() and 0xFF) shl 24) or
            ((this[offset + 1].toInt() and 0xFF) shl 16) or
            ((this[offset + 2].toInt() and 0xFF) shl 8) or
            (this[offset + 3].toInt() and 0xFF)
    }

    private fun ByteArray.readUnsignedShort(offset: Int): Int {
        return ((this[offset].toInt() and 0xFF) shl 8) or
            (this[offset + 1].toInt() and 0xFF)
    }

    private fun decodePngRgbaRows(
        inflated: ByteArray,
        width: Int,
        height: Int
    ): List<IntArray> {
        val bytesPerPixel = 4
        val stride = width * bytesPerPixel
        var inputOffset = 0
        var previous = IntArray(stride)
        return List(height) {
            val filter = inflated[inputOffset++].toInt() and 0xFF
            val row = IntArray(stride)
            for (i in 0 until stride) {
                val raw = inflated[inputOffset++].toInt() and 0xFF
                val left = if (i >= bytesPerPixel) row[i - bytesPerPixel] else 0
                val up = previous[i]
                val upLeft = if (i >= bytesPerPixel) previous[i - bytesPerPixel] else 0
                row[i] = when (filter) {
                    0 -> raw
                    1 -> (raw + left) and 0xFF
                    2 -> (raw + up) and 0xFF
                    3 -> (raw + ((left + up) / 2)) and 0xFF
                    4 -> (raw + paethPredictor(left, up, upLeft)) and 0xFF
                    else -> error("Unsupported PNG filter $filter")
                }
            }
            previous = row
            row
        }
    }

    private fun readVisibleDarkOuterEdgePngPixelCount(file: File): Int {
        val bytes = file.readBytes()
        var offset = 8
        var width = 0
        var height = 0
        var bitDepth = 0
        var colorType = 0
        val idat = ByteArrayOutputStream()

        while (offset + 12 <= bytes.size) {
            val length = bytes.readBigEndianInt(offset)
            val type = String(bytes, offset + 4, 4)
            val dataOffset = offset + 8
            when (type) {
                "IHDR" -> {
                    width = bytes.readBigEndianInt(dataOffset)
                    height = bytes.readBigEndianInt(dataOffset + 4)
                    bitDepth = bytes[dataOffset + 8].toInt() and 0xFF
                    colorType = bytes[dataOffset + 9].toInt() and 0xFF
                }
                "IDAT" -> idat.write(bytes, dataOffset, length)
                "IEND" -> break
            }
            offset += 12 + length
        }

        assertTrue(bitDepth == 8 && colorType == 6, "${file.name} should be an 8-bit RGBA PNG")

        val inflated = InflaterInputStream(idat.toByteArray().inputStream()).readBytes()
        val rows = decodePngRgbaRows(
            inflated = inflated,
            width = width,
            height = height
        )
        var darkPixels = 0
        val outerEdgeInset = minOf(width, height) / 8

        rows.forEachIndexed { y, row ->
            for (x in 0 until width) {
                if (
                    x >= outerEdgeInset &&
                    x < width - outerEdgeInset &&
                    y >= outerEdgeInset &&
                    y < height - outerEdgeInset
                ) {
                    continue
                }
                val pixelOffset = x * 4
                val r = row[pixelOffset]
                val g = row[pixelOffset + 1]
                val b = row[pixelOffset + 2]
                val a = row[pixelOffset + 3]
                if (a > 0 && maxOf(r, g, b) < 150) {
                    darkPixels++
                }
            }
        }

        return darkPixels
    }

    private fun paethPredictor(left: Int, up: Int, upLeft: Int): Int {
        val estimate = left + up - upLeft
        val leftDistance = kotlin.math.abs(estimate - left)
        val upDistance = kotlin.math.abs(estimate - up)
        val upLeftDistance = kotlin.math.abs(estimate - upLeft)
        return when {
            leftDistance <= upDistance && leftDistance <= upLeftDistance -> left
            upDistance <= upLeftDistance -> up
            else -> upLeft
        }
    }

    private fun splashDrawableVectorExists(): Boolean {
        return listOf(
            File("app/src/main/res/drawable/ic_launcher_bilipai_foreground.xml"),
            File("src/main/res/drawable/ic_launcher_bilipai_foreground.xml")
        ).any { it.exists() }
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

    private fun loadThemeSource(): String {
        val candidates = listOf(
            File("app/src/main/java/com/android/purebilibili/core/theme/Theme.kt"),
            File("src/main/java/com/android/purebilibili/core/theme/Theme.kt")
        )
        val sourceFile = candidates.firstOrNull { it.exists() }
            ?: error("Cannot locate Theme.kt from ${File(".").absolutePath}")
        return sourceFile.readText()
    }

    private fun loadSettingsViewModelSource(): String {
        val candidates = listOf(
            File("app/src/main/java/com/android/purebilibili/feature/settings/SettingsViewModel.kt"),
            File("src/main/java/com/android/purebilibili/feature/settings/SettingsViewModel.kt")
        )
        val sourceFile = candidates.firstOrNull { it.exists() }
            ?: error("Cannot locate SettingsViewModel.kt from ${File(".").absolutePath}")
        return sourceFile.readText()
    }
}
