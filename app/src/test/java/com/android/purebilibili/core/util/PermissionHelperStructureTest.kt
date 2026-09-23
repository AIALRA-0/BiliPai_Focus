package com.android.purebilibili.core.util

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PermissionHelperStructureTest {

    @Test
    fun permissionDialogsUseAppAlertChromeAndThemePrimary() {
        val source = File(
            "src/main/java/com/android/purebilibili/core/util/PermissionHelper.kt"
        ).readText()
        assertTrue(source.contains("AppAlertDialog("))
        assertTrue(source.contains("AppButton("))
        assertTrue(source.contains("AppTextButton("))
        assertTrue(source.contains("MaterialTheme.colorScheme.primary"))
        assertFalse(source.contains("BiliPink"))
        assertFalse(source.contains("AlertDialog("))
    }
}
