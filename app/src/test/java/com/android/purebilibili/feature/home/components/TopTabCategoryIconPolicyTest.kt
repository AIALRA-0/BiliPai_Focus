package com.android.purebilibili.feature.home.components

import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.ui.graphics.vector.ImageVector
import com.android.purebilibili.core.theme.UiPreset
import com.android.purebilibili.feature.home.HomeCategory
import io.github.alexzhirkevich.cupertino.icons.outlined.Cpu
import io.github.alexzhirkevich.cupertino.icons.outlined.PlayCircle
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import androidx.compose.material.icons.Icons
import kotlin.test.Test
import kotlin.test.assertEquals

class TopTabCategoryIconPolicyTest {

    @Test
    fun topTabCategoryIconPolicy_usesSemanticIosIcons() {
        assertSameVectorAsset(
            CupertinoIcons.Outlined.PlayCircle,
            resolveTopTabCategoryIcon(HomeCategory.GAME.name, UiPreset.IOS)
        )
        assertSameVectorAsset(
            CupertinoIcons.Outlined.Cpu,
            resolveTopTabCategoryIcon(HomeCategory.TECH.name, UiPreset.IOS)
        )
    }

    @Test
    fun topTabCategoryIconPolicy_usesSemanticMd3Icons() {
        assertSameVectorAsset(
            Icons.Outlined.SportsEsports,
            resolveTopTabCategoryIcon(HomeCategory.GAME.name, UiPreset.MD3)
        )
        assertSameVectorAsset(
            Icons.Outlined.SmartToy,
            resolveTopTabCategoryIcon(HomeCategory.TECH.name, UiPreset.MD3)
        )
    }

    private fun assertSameVectorAsset(expected: ImageVector, actual: ImageVector) {
        assertEquals(expected.name, actual.name)
        assertEquals(expected.defaultWidth, actual.defaultWidth)
        assertEquals(expected.defaultHeight, actual.defaultHeight)
        assertEquals(expected.viewportWidth, actual.viewportWidth)
        assertEquals(expected.viewportHeight, actual.viewportHeight)
    }
}
