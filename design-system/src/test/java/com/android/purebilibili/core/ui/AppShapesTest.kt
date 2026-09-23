package com.android.purebilibili.core.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.theme.AppUiStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AppShapesTest {

    @Test
    fun nonGlassMiuix_fillAndBorderShareUpstreamSquircleGeometry() {
        val fill = AppShapes.resolveContainerShape(ContainerLevel.Card, AppUiStyle.MIUIX, false)
        val border = AppShapes.resolveBorderedContainerShape(ContainerLevel.Card, AppUiStyle.MIUIX, false)
        assertEquals(MiuixContainerShape(16.dp), fill)
        assertEquals(fill, border)
        assertIs<RoundedCornerShape>(
            AppShapes.resolveContainerShape(ContainerLevel.Card, AppUiStyle.MATERIAL3, false)
        )
        assertIs<RoundedCornerShape>(
            AppShapes.resolveContainerShape(ContainerLevel.Card, AppUiStyle.MIUIX, true)
        )
    }

    @Test
    fun nonGlassMiuix_usesRoleCornersWithoutChangingOtherModes() {
        val expected = mapOf(
            ContainerLevel.Card to 16.dp,
            ContainerLevel.MediaCover to 12.dp,
            ContainerLevel.ProminentCard to 20.dp,
            ContainerLevel.Chip to 10.dp,
        )
        expected.forEach { (role, radius) ->
            assertEquals(radius, AppShapes.resolveContainerCornerDp(role, AppUiStyle.MIUIX, false))
            assertEquals(AppShapes.resolveContainerCornerDp(role, AppUiStyle.MATERIAL3),
                AppShapes.resolveContainerCornerDp(role, AppUiStyle.MATERIAL3, false))
        }
        assertEquals(12.dp,
            AppShapes.resolveContainerCornerDp(ContainerLevel.MediaCover, AppUiStyle.MIUIX, true))
    }

    @Test
    fun pillRadius_material3_is28Dp() {
        val dp = AppShapes.resolveContainerCornerDp(
            level = ContainerLevel.Pill,
            uiStyle = AppUiStyle.MATERIAL3
        )
        assertEquals(28.dp, dp)
    }

    @Test
    fun pillRadius_miuix_is22Dp() {
        val dp = AppShapes.resolveContainerCornerDp(
            level = ContainerLevel.Pill,
            uiStyle = AppUiStyle.MIUIX
        )
        assertEquals(22.dp, dp)
    }

    @Test
    fun cardRadius_scalesByStyle() {
        val material3 = AppShapes.resolveContainerCornerDp(
            level = ContainerLevel.Card,
            uiStyle = AppUiStyle.MATERIAL3
        )
        val miuix = AppShapes.resolveContainerCornerDp(
            level = ContainerLevel.Card,
            uiStyle = AppUiStyle.MIUIX
        )
        // MATERIAL3 = base * 0.90, Miuix = base * 1.15
        assertTrue(miuix.value > material3.value, "Miuix card radius should be larger than MATERIAL3")
    }

    @Test
    fun prominentCard_isFullyRoundedWhileSheetIsTopRounded() {
        val prominentCard = AppShapes.resolveContainerShape(
            level = ContainerLevel.ProminentCard,
            uiStyle = AppUiStyle.MATERIAL3
        )
        val sheet = AppShapes.resolveContainerShape(
            level = ContainerLevel.Sheet,
            uiStyle = AppUiStyle.MATERIAL3
        )

        assertEquals(RoundedCornerShape(18.dp), prominentCard)
        assertEquals(RoundedCornerShape(18.dp, 18.dp, 0.dp, 0.dp), sheet)
    }

    @Test
    fun dialogRadius_scalesByStyle() {
        val material3 = AppShapes.resolveContainerCornerDp(
            level = ContainerLevel.Dialog,
            uiStyle = AppUiStyle.MATERIAL3
        )
        val miuix = AppShapes.resolveContainerCornerDp(
            level = ContainerLevel.Dialog,
            uiStyle = AppUiStyle.MIUIX
        )
        assertTrue(miuix.value > material3.value)
    }

    @Test
    fun fieldRadius_scalesByStyle() {
        val material3 = AppShapes.resolveContainerCornerDp(
            level = ContainerLevel.Field,
            uiStyle = AppUiStyle.MATERIAL3
        )
        val miuix = AppShapes.resolveContainerCornerDp(
            level = ContainerLevel.Field,
            uiStyle = AppUiStyle.MIUIX
        )
        assertTrue(miuix.value > material3.value)
    }

    @Test
    fun tagRadius_scalesByStyle() {
        val material3 = AppShapes.resolveContainerCornerDp(
            level = ContainerLevel.Tag,
            uiStyle = AppUiStyle.MATERIAL3
        )
        val miuix = AppShapes.resolveContainerCornerDp(
            level = ContainerLevel.Tag,
            uiStyle = AppUiStyle.MIUIX
        )
        assertTrue(miuix.value > material3.value)
    }

    @Test
    fun chipRadius_scalesByStyle() {
        val material3 = AppShapes.resolveContainerCornerDp(
            level = ContainerLevel.Chip,
            uiStyle = AppUiStyle.MATERIAL3
        )
        val miuix = AppShapes.resolveContainerCornerDp(
            level = ContainerLevel.Chip,
            uiStyle = AppUiStyle.MIUIX
        )
        assertTrue(miuix.value > material3.value)
    }

    @Test
    fun floatingRadius_scalesByStyle() {
        val material3 = AppShapes.resolveContainerCornerDp(
            level = ContainerLevel.Floating,
            uiStyle = AppUiStyle.MATERIAL3
        )
        val miuix = AppShapes.resolveContainerCornerDp(
            level = ContainerLevel.Floating,
            uiStyle = AppUiStyle.MIUIX
        )
        assertTrue(miuix.value > material3.value, "Miuix floating radius should be larger")
    }

    @Test
    fun containerShape_usesRoundedCornerShapeForBothStyles() {
        listOf(AppUiStyle.MATERIAL3, AppUiStyle.MIUIX).forEach { style ->
            val shape = AppShapes.resolveContainerShape(
                level = ContainerLevel.Card,
                uiStyle = style
            )
            assertIs<RoundedCornerShape>(shape)
        }
    }

    @Test
    fun sheetContainerShape_isTopRounded() {
        val shape = AppShapes.resolveContainerShape(
            level = ContainerLevel.Sheet,
            uiStyle = AppUiStyle.MATERIAL3
        ) as RoundedCornerShape
        // MATERIAL3 Sheet = 20 * 0.90 = 18，仅顶部圆角。
        assertEquals(RoundedCornerShape(18.dp, 18.dp, 0.dp, 0.dp), shape)
    }

    @Test
    fun borderedContainerShape_usesRoundedCornerShape() {
        val shape = AppShapes.resolveBorderedContainerShape(
            level = ContainerLevel.Dialog,
            uiStyle = AppUiStyle.MIUIX
        )
        assertIs<RoundedCornerShape>(shape)
    }
}
