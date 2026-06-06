package com.android.purebilibili.feature.video.ui.components

import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.theme.calculateContrastRatio
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class BottomInputBarStructureTest {

    @Test
    fun bottomInputBar_usesSolidSurfaceColor() {
        val source = File("src/main/java/com/android/purebilibili/feature/video/ui/components/BottomInputBar.kt")
            .readText()

        assertTrue(source.contains("MaterialTheme.colorScheme.surface"))
        assertTrue(source.contains("MaterialTheme.colorScheme.surfaceContainerHighest"))
        assertTrue(!source.contains("surfaceVariant.copy(alpha = 0.65f)"))
        assertTrue(!source.contains("HazeState"))
        assertTrue(!source.contains("liquidGlassBackground"))
    }

    @Test
    fun bottomInputBarPlaceholderTextKeepsReadableContrastInLightTheme() {
        val inputContainerColor = Color(0xFFE6E0E9)
        val textColor = resolveBottomInputBarPlaceholderTextColor(
            inputContainerColor = inputContainerColor,
            onSurfaceColor = Color(0xFF1D1B20),
            onSurfaceVariantColor = Color(0xFF49454F)
        )

        assertTrue(
            calculateContrastRatio(textColor, inputContainerColor) >=
                BOTTOM_INPUT_BAR_PLACEHOLDER_MIN_CONTRAST
        )
    }

    @Test
    fun bottomInputBarPlaceholderTextKeepsReadableContrastInDarkTheme() {
        val inputContainerColor = Color(0xFF36343B)
        val textColor = resolveBottomInputBarPlaceholderTextColor(
            inputContainerColor = inputContainerColor,
            onSurfaceColor = Color(0xFFE6E1E5),
            onSurfaceVariantColor = Color(0xFFCAC4D0)
        )

        assertTrue(
            calculateContrastRatio(textColor, inputContainerColor) >=
                BOTTOM_INPUT_BAR_PLACEHOLDER_MIN_CONTRAST
        )
    }
}
