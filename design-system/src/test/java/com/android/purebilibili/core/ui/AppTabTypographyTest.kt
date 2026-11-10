package com.android.purebilibili.core.ui

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals

class AppTabTypographyTest {
    @Test
    fun `both materials preserve theme and explicit sizes`() {
        for (glass in listOf(false, true)) {
            for (size in listOf(12.sp, 13.sp, 15.sp, 17.sp, 1.em)) {
                assertEquals(size, resolveAppTabLabelFontSize(TextUnit.Unspecified, size, glass))
                assertEquals(size, resolveAppTabLabelFontSize(size, 12.sp, glass))
            }
        }
    }
}
