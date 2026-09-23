package com.android.purebilibili.core.ui.common

import androidx.compose.ui.geometry.Offset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LongPressCopyPolicyTest {

    @Test
    fun longPressTimeout_usesExplicitOrRaisesSystemDefault() {
        assertEquals(
            700L,
            resolveLongPressCopyTimeoutMs(
                systemLongPressTimeoutMs = 400L,
                explicitTimeoutMs = 700L,
            ),
        )
        assertEquals(
            400L,
            resolveLongPressCopyTimeoutMs(
                systemLongPressTimeoutMs = 400L,
                explicitTimeoutMs = null,
            ),
        )
        assertEquals(
            700L,
            resolveLongPressCopyTimeoutMs(
                systemLongPressTimeoutMs = 400L,
                explicitTimeoutMs = null,
                minTimeoutMs = 700L,
            ),
        )
    }

    @Test
    fun longPressCopy_cancelsWhenFingerMovesPastSlop() {
        val start = Offset(10f, 10f)
        assertFalse(
            hasExceededLongPressCopySlop(
                start = start,
                current = Offset(12f, 11f),
                touchSlopPx = 16f,
            )
        )
        assertTrue(
            hasExceededLongPressCopySlop(
                start = start,
                current = Offset(10f, 40f),
                touchSlopPx = 16f,
            )
        )
    }

    @Test
    fun replyItemSource_doesNotAttachUsernameLongPressCopy() {
        val source = java.io.File(
            "src/main/java/com/android/purebilibili/feature/video/ui/components/ReplyComponents.kt"
        ).readText()
        // 用户名不再挂 copyOnLongPress，避免刷评论误复制
        assertFalse(
            source.contains("copyOnLongPress(item.member.uname"),
            "comment username must not use direct long-press copy",
        )
        assertTrue(source.contains("COPY_USERNAME"))
        assertTrue(source.contains("复制用户名"))
    }
}
