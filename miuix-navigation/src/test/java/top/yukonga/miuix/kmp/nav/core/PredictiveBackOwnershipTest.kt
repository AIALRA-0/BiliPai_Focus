package top.yukonga.miuix.kmp.nav.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PredictiveBackOwnershipTest {
    @Test
    fun lateCancelCannotReleaseRegrabbedGesture() {
        val ownership = PredictiveBackOwnership()
        val first = ownership.acquire()
        assertTrue(ownership.release(first))
        val restoreGeneration = ownership.generation
        val regrab = ownership.acquire()

        assertFalse(ownership.release(first))
        assertEquals(regrab, ownership.generation)
        assertTrue(ownership.generation != restoreGeneration)
        assertTrue(ownership.release(regrab))
    }
}
