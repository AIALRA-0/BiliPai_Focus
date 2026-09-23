package com.android.purebilibili.feature.onboarding

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingButtonShapeStructureTest {

    @Test
    fun acknowledgeButtonMatchesMiuixButtonRadiusOnlyOutsideLiquidGlass() {
        val source = File(
            "src/main/java/com/android/purebilibili/feature/onboarding/OnboardingScreen.kt",
        ).readText()

        assertTrue(source.contains("if (isMiuixNonGlassEnabled())"))
        assertTrue(
            source.contains(
                "RoundedCornerShape(AppChromeSizeTokens.MiuixNativeCompactCornerRadiusDp.dp)",
            )
        )
        assertTrue(source.contains("else {\n                    ButtonDefaults.outlinedShape"))
        assertTrue(source.contains("shape = acknowledgeButtonShape"))
    }
}
