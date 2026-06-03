package com.android.purebilibili.feature.screenshot

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppScreenshotGesturePolicyTest {

    @Test
    fun topRightTwoFingerLongPress_triggersAfterThresholdInsideHotZone() {
        val result = shouldTriggerTopRightTwoFingerLongPress(
            enabled = true,
            blocked = false,
            pointerCount = 2,
            pointerPositions = listOf(
                AppScreenshotPointerPosition(x = 1010f, y = 24f),
                AppScreenshotPointerPosition(x = 1048f, y = 72f)
            ),
            elapsedMillis = 620L,
            screenWidthPx = 1080f,
            hotZoneSizePx = 96f
        )

        assertTrue(result)
    }

    @Test
    fun topRightTwoFingerLongPress_rejectsSingleFingerNonHotZoneShortPressAndBlockedStates() {
        val commonPositions = listOf(
            AppScreenshotPointerPosition(x = 1010f, y = 24f),
            AppScreenshotPointerPosition(x = 1048f, y = 72f)
        )

        assertFalse(
            shouldTriggerTopRightTwoFingerLongPress(
                enabled = true,
                blocked = false,
                pointerCount = 1,
                pointerPositions = commonPositions.take(1),
                elapsedMillis = 700L,
                screenWidthPx = 1080f,
                hotZoneSizePx = 96f
            )
        )
        assertFalse(
            shouldTriggerTopRightTwoFingerLongPress(
                enabled = true,
                blocked = false,
                pointerCount = 2,
                pointerPositions = listOf(
                    AppScreenshotPointerPosition(x = 850f, y = 24f),
                    AppScreenshotPointerPosition(x = 1048f, y = 72f)
                ),
                elapsedMillis = 700L,
                screenWidthPx = 1080f,
                hotZoneSizePx = 96f
            )
        )
        assertFalse(
            shouldTriggerTopRightTwoFingerLongPress(
                enabled = true,
                blocked = false,
                pointerCount = 2,
                pointerPositions = commonPositions,
                elapsedMillis = 590L,
                screenWidthPx = 1080f,
                hotZoneSizePx = 96f
            )
        )
        assertFalse(
            shouldTriggerTopRightTwoFingerLongPress(
                enabled = false,
                blocked = false,
                pointerCount = 2,
                pointerPositions = commonPositions,
                elapsedMillis = 700L,
                screenWidthPx = 1080f,
                hotZoneSizePx = 96f
            )
        )
        assertFalse(
            shouldTriggerTopRightTwoFingerLongPress(
                enabled = true,
                blocked = true,
                pointerCount = 2,
                pointerPositions = commonPositions,
                elapsedMillis = 700L,
                screenWidthPx = 1080f,
                hotZoneSizePx = 96f
            )
        )
    }

    @Test
    fun threeFingerSwipeDown_triggersOnlyForExplicitModeAndVerticalDrag() {
        assertTrue(
            shouldTriggerThreeFingerSwipeDown(
                enabled = true,
                blocked = false,
                mode = AppScreenshotGestureMode.THREE_FINGER_SWIPE_DOWN,
                pointerCount = 3,
                totalDragX = 20f,
                totalDragY = 112f,
                triggerDistancePx = 96f,
                maxHorizontalToVerticalRatio = 0.6f
            )
        )
        assertFalse(
            shouldTriggerThreeFingerSwipeDown(
                enabled = true,
                blocked = false,
                mode = AppScreenshotGestureMode.TOP_RIGHT_TWO_FINGER_LONG_PRESS,
                pointerCount = 3,
                totalDragX = 20f,
                totalDragY = 112f,
                triggerDistancePx = 96f,
                maxHorizontalToVerticalRatio = 0.6f
            )
        )
        assertFalse(
            shouldTriggerThreeFingerSwipeDown(
                enabled = true,
                blocked = false,
                mode = AppScreenshotGestureMode.THREE_FINGER_SWIPE_DOWN,
                pointerCount = 3,
                totalDragX = 80f,
                totalDragY = 112f,
                triggerDistancePx = 96f,
                maxHorizontalToVerticalRatio = 0.6f
            )
        )
    }

    @Test
    fun screenshotName_usesPngAndSanitizesIllegalChars() {
        val fileName = buildAppScreenshotFileName(
            timestampMs = 1700000000000L,
            prefix = "Bili/Pai:*?\"<>|"
        )

        assertTrue(fileName.endsWith(".png"))
        assertFalse(fileName.contains('/'))
        assertFalse(fileName.contains(':'))
        assertFalse(fileName.contains('*'))
        assertFalse(fileName.contains('?'))
        assertFalse(fileName.contains('"'))
        assertFalse(fileName.contains('<'))
        assertFalse(fileName.contains('>'))
        assertFalse(fileName.contains('|'))
    }

    @Test
    fun gestureModeDefault_isTopRightTwoFingerLongPress() {
        assertEquals(
            AppScreenshotGestureMode.TOP_RIGHT_TWO_FINGER_LONG_PRESS,
            AppScreenshotGestureMode.fromValue(999)
        )
    }

    @Test
    fun screenshotSharePrompt_onlyOfferedForLandscapeSuccessfulSaveWithUri() {
        assertTrue(
            shouldOfferAppScreenshotShare(
                isLandscape = true,
                result = AppScreenshotResult.Success,
                hasShareUri = true
            )
        )
        assertFalse(
            shouldOfferAppScreenshotShare(
                isLandscape = false,
                result = AppScreenshotResult.Success,
                hasShareUri = true
            )
        )
        assertFalse(
            shouldOfferAppScreenshotShare(
                isLandscape = true,
                result = AppScreenshotResult.SaveFailed,
                hasShareUri = true
            )
        )
        assertFalse(
            shouldOfferAppScreenshotShare(
                isLandscape = true,
                result = AppScreenshotResult.Success,
                hasShareUri = false
            )
        )
    }
}
