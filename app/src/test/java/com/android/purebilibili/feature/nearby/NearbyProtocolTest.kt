package com.android.purebilibili.feature.nearby

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class NearbyProtocolTest {
    private val video = NearbyVideo("BV1xx411c7mD", 1234, 43_210, 1.5f, "接力视频", "https://i0.hdslb.com/test.jpg")

    @Test fun playbackMetadataRoundTripsWithoutCredentials() {
        val message = NearbyMessage(kind = "video", id = "a".repeat(32), video = video)
        val buffer = ByteArrayOutputStream()
        DataOutputStream(buffer).writeNearby(message)
        val decoded = DataInputStream(ByteArrayInputStream(buffer.toByteArray())).readNearby()
        assertEquals(message, decoded)
        decoded.video!!.validate()
        val text = buffer.toString(Charsets.UTF_8.name())
        assertTrue(!text.contains("cookie", ignoreCase = true))
        assertTrue(!text.contains("sessdata", ignoreCase = true))
    }

    @Test fun legacyMessagesAndOversizedCorrelationIdsAreRejected() {
        val original = NearbyMessage(kind = "hello", id = "a".repeat(32))
        for (invalid in listOf(original.copy(version = 1), original.copy(requestId = "x".repeat(33)),
            original.copy(replyTo = "x".repeat(33)), original.copy(transferId = "x".repeat(33)))) {
            val buffer = ByteArrayOutputStream()
            DataOutputStream(buffer).writeNearby(invalid)
            assertFails { DataInputStream(ByteArrayInputStream(buffer.toByteArray())).readNearby() }
        }
    }

    @Test fun invalidFrameSizesAreRejectedBeforeAllocation() {
        for (size in listOf(-1, 0, 16_385, Int.MAX_VALUE)) {
            val buffer = ByteArrayOutputStream()
            DataOutputStream(buffer).writeInt(size)
            assertFailsWith<IllegalArgumentException> {
                DataInputStream(ByteArrayInputStream(buffer.toByteArray())).readNearby()
            }
        }
        assertFailsWith<EOFException> {
            DataInputStream(ByteArrayInputStream(byteArrayOf(0, 0, 0, 20, 1))).readNearby()
        }
    }

    @Test fun invalidPlaybackStateCannotReachThePlayer() {
        for (invalid in listOf(
            video.copy(bvid = "invalid"), video.copy(cid = 0), video.copy(positionMs = -1),
            video.copy(positionMs = Long.MAX_VALUE), video.copy(speed = Float.NaN),
            video.copy(speed = Float.POSITIVE_INFINITY), video.copy(speed = 0f),
            video.copy(title = "x".repeat(201)), video.copy(cover = "https://example.com/image.jpg"),
            video.copy(cover = "https://i0.hdslb.com:8888/image.jpg"),
        )) assertFails { invalid.validate() }
    }

    @Test fun onlyLiteralLocalAddressesAreAccepted() {
        for (host in listOf("192.168.1.3", "10.0.2.2", "127.0.0.1", "::1", "fd12::1")) requireLocalAddress(host)
        for (host in listOf("example.com", "dead.beef", "8.8.8.8", "0.0.0.0", "224.0.0.1", "999.1.1.1", "127.1")) {
            assertFails { requireLocalAddress(host) }
        }
    }

    @Test fun coversAreNormalizedWithoutAllowingArbitraryEndpoints() {
        assertEquals("https://i0.hdslb.com/test.jpg", normalizeNearbyCover("http://i0.hdslb.com/test.jpg"))
        assertEquals("https://i0.hdslb.com/test.jpg", normalizeNearbyCover("//i0.hdslb.com/test.jpg"))
        for (cover in listOf("file:///data/private", "https://evilhdslb.com/a", "https://i0.hdslb.com@evil.com/a")) {
            assertEquals("", normalizeNearbyCover(cover))
        }
    }

    @Test fun pairingTokensAndPinsAreIndependentAndFixedLength() {
        val token = nearbyToken()
        assertTrue(Regex("[a-f0-9]{32}").matches(token))
        assertNotEquals(token, nearbyToken())
        assertEquals(64, nearbyPin(byteArrayOf(1, 2, 3)).length)
        assertNotEquals(nearbyPin(byteArrayOf(1)), nearbyPin(byteArrayOf(2)))
    }
}
