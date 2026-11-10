package com.android.purebilibili.core.performance

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProcessExitContextTest {
    @Test
    fun `maximum state stays inside official 128 byte limit`() {
        val state = ProcessExitContext(510, "v".repeat(12), "a".repeat(12),
            "s".repeat(24), "e".repeat(20), "t".repeat(24), true, 12345L, 48000L)
        assertEquals(127, state.encode().size)
        assertEquals(state, ProcessExitContext.decode(state.encode()))
    }

    @Test
    fun `missing truncated unknown or trailing payload is rejected`() {
        val bytes = ProcessExitContext(510, "0.5.5", "local").encode()
        assertNull(ProcessExitContext.decode(null))
        assertNull(ProcessExitContext.decode(bytes.copyOf(bytes.size - 1)))
        assertNull(ProcessExitContext.decode(bytes + byteArrayOf(0)))
        assertNull(ProcessExitContext.decode(byteArrayOf(1, 2, 3, 4)))
        assertNull(ProcessExitContext.decode(ByteArray(129)))
    }

    @Test
    fun `encoding bounds non ASCII fields without breaking the wire format`() {
        val state = ProcessExitContext(510, "版本0.5.5", "a".repeat(100),
            "页".repeat(100), "e".repeat(100), "t".repeat(100))
        val decoded = requireNotNull(ProcessExitContext.decode(state.encode()))
        assertEquals("0.5.5", decoded.versionName)
        assertEquals("", decoded.screen)
        assertEquals(12, decoded.commit.length)
        assertTrue(state.encode().size <= 128)
        assertNull(decoded.foreground)
    }
}
