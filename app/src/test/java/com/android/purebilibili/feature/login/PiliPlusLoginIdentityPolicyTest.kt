package com.android.purebilibili.feature.login

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PiliPlusLoginIdentityPolicyTest {

    @Test
    fun `identity follows PiliPlus Passport identifier formats`() {
        val identity = createPiliPlusLoginIdentity()

        assertTrue(identity.buvid.matches(Regex("XY[0-9a-f]{35}")))
        assertEquals(34, identity.deviceId.length)
        assertTrue(identity.deviceId.all { it.isDigit() || it in 'a'..'f' })
    }

    @Test
    fun `random device token matches PiliPlus alphabet`() {
        assertTrue(createPiliPlusRandomString(16).matches(Regex("[0-9a-z]{16}")))
    }
}
