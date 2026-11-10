package com.android.purebilibili.core.theme

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals

/** Regression expectations for Material 3's published body and label scale. */
class Md3TypographyPolicyTest {
    @Test
    fun bodyRoles_followMaterialBaseline() {
        assertEquals(16.sp, Md3Typography.bodyLarge.fontSize)
        assertEquals(24.sp, Md3Typography.bodyLarge.lineHeight)
        assertEquals(14.sp, Md3Typography.bodyMedium.fontSize)
        assertEquals(20.sp, Md3Typography.bodyMedium.lineHeight)
        assertEquals(12.sp, Md3Typography.bodySmall.fontSize)
        assertEquals(16.sp, Md3Typography.bodySmall.lineHeight)
        assertEquals(FontWeight.Normal, Md3Typography.bodyLarge.fontWeight)
    }

    @Test
    fun labelRoles_followMaterialBaseline() {
        assertEquals(14.sp, Md3Typography.labelLarge.fontSize)
        assertEquals(20.sp, Md3Typography.labelLarge.lineHeight)
        assertEquals(12.sp, Md3Typography.labelMedium.fontSize)
        assertEquals(16.sp, Md3Typography.labelMedium.lineHeight)
        assertEquals(11.sp, Md3Typography.labelSmall.fontSize)
        assertEquals(16.sp, Md3Typography.labelSmall.lineHeight)
        assertEquals(FontWeight.Medium, Md3Typography.labelLarge.fontWeight)
    }
}
