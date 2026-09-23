package com.android.purebilibili.feature.profile

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WallpaperAdjustmentSheetStructureTest {
    @Test
    fun `wallpaper device selector uses theme adaptive native and liquid chrome`() {
        val source = File(
            "src/main/java/com/android/purebilibili/feature/profile/WallpaperAdjustmentSheet.kt"
        ).readText()

        assertTrue(source.contains("AppThemeAdaptiveTabRow("))
        assertTrue(source.contains("WallpaperDeviceTabRow("))
        assertTrue(source.contains("roundMatchedLiquidIndicatorHeightDp(48f)"))
        assertFalse(source.contains("private fun TabItem("))
        assertFalse(source.contains("Icons.Outlined.PhoneAndroid"))
        assertFalse(source.contains("Icons.Outlined.TabletAndroid"))
    }
}
