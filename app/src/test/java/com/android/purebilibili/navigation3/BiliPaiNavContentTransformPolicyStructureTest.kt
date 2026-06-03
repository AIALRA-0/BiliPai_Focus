package com.android.purebilibili.navigation3

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class BiliPaiNavContentTransformPolicyStructureTest {

    @Test
    fun disabledVideoDirectionalReturnKeepsTargetPageImmediatelyVisible() {
        val source = contentTransformPolicySource()
        val returnFunctionStart = source.indexOf("private fun disabledVideoDirectionReturnTransform")
        val returnFunctionEnd = source.length
        val returnFunction = source.substring(returnFunctionStart, returnFunctionEnd)

        assertTrue(returnFunction.contains("EnterTransition.None togetherWith"))
        assertTrue(returnFunction.contains("ExitTransition.None").not())
    }

    @Test
    fun disabledVideoDirectionalReturnMovesOnlyOutgoingPageHorizontally() {
        val source = contentTransformPolicySource()
        val returnFunctionStart = source.indexOf("private fun disabledVideoDirectionReturnTransform")
        val returnFunctionEnd = source.length
        val returnFunction = source.substring(returnFunctionStart, returnFunctionEnd)

        assertTrue(returnFunction.contains("slideOutHorizontally("))
        assertTrue(returnFunction.contains("slideInHorizontally(").not())
    }

    @Test
    fun disabledVideoDirectionalReturnUsesStrongExitTravel() {
        val source = contentTransformPolicySource()
        val returnFunctionStart = source.indexOf("private fun disabledVideoDirectionReturnTransform")
        val returnFunctionEnd = source.length
        val returnFunction = source.substring(returnFunctionStart, returnFunctionEnd)

        assertTrue(returnFunction.contains("targetOffsetX = { width -> directionSign * width / 2 }"))
    }

    @Test
    fun disabledVideoDirectionalReturnUsesLongerMotionWindowForVisibleScreenshots() {
        val source = contentTransformPolicySource()

        assertTrue(source.contains("NAV3_DISABLED_VIDEO_RETURN_MILLIS = 320"))
    }

    @Test
    fun spaceForwardUsesLightSlideAndFade() {
        val source = contentTransformPolicySource()

        assertTrue(source.contains("BiliPaiNavRouteTransition.SPACE_FORWARD"))
        assertTrue(source.contains("private fun spaceForwardTransform()"))
        assertTrue(source.contains("initialOffsetX = { width -> width / 8 }"))
        assertTrue(source.contains("fadeIn(animationSpec = tween(NAV3_SPACE_FORWARD_MILLIS))"))
    }

    private fun contentTransformPolicySource(): String {
        return listOf(
            File("app/src/main/java/com/android/purebilibili/navigation3/BiliPaiNavContentTransformPolicy.kt"),
            File("src/main/java/com/android/purebilibili/navigation3/BiliPaiNavContentTransformPolicy.kt")
        ).first { it.exists() }.readText()
    }
}
