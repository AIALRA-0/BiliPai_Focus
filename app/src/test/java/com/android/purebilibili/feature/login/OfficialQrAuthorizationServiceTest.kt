package com.android.purebilibili.feature.login

import com.android.purebilibili.core.network.PassportApi
import com.android.purebilibili.data.model.response.NavData
import com.android.purebilibili.data.model.response.NavResponse
import com.android.purebilibili.data.model.response.QrAuthorizationResponse
import com.android.purebilibili.data.model.response.QrAuthorizationSceneResponse
import com.android.purebilibili.data.model.response.QrAuthorizationSceneData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OfficialQrAuthorizationServiceTest {
    private val key = "0123456789abcdef0123456789abcdef"
    private val cookie = "SESSDATA=session%2Cvalue; bili_jct=csrf; DedeUserID=42; buvid3=buvid"
    private val loggedIn = NavResponse(data = NavData(isLogin = true, uname = "测试账号", mid = 42))

    private fun session() = QrAuthorizationSession("session%2Cvalue", "csrf", 42, "buvid", "access-key")
    private fun request(type: BilibiliLoginQrType) = BilibiliLoginQr(type, key)

    @Test
    fun `web prepares scanned state and only confirms after explicit user action`() = runTest {
        val api = mockk<PassportApi>()
        val qr = request(BilibiliLoginQrType.WEB)
        coEvery { api.validateCookieSession(cookie) } returns loggedIn
        coEvery { api.checkWebQrCode(any()) } returns QrAuthorizationResponse(0)
        coEvery { api.getWebQrScene(any()) } returns QrAuthorizationSceneResponse(0, data = QrAuthorizationSceneData())
        coEvery { api.confirmWebQrCode(any()) } returns QrAuthorizationResponse(0)
        val service = OfficialQrAuthorizationService(api, ::session)

        val prepared = service.prepare(qr)
        assertEquals("测试账号", prepared.accountName)
        assertEquals(42L, prepared.mid)
        coVerify(exactly = 0) { api.confirmWebQrCode(any()) }
        service.confirm(prepared)
        coVerifyOrder {
            api.validateCookieSession(cookie)
            api.checkWebQrCode(match { it["qrcode_key"] == key && it["access_key"] == "access-key" })
            api.getWebQrScene(match { it["qrcode_key"] == key })
            api.validateCookieSession(cookie)
            api.confirmWebQrCode(match {
                it["qrcode_key"] == key && it.containsKey("sign") && it.containsKey("env_key")
            })
        }
        coVerify(exactly = 0) { api.pollQrCode(any()) }
        coVerify(exactly = 0) { api.pollTvQrCode(any()) }
    }

    @Test
    fun `web authorization params carry platform-matched signing fields`() = runTest {
        val api = mockk<PassportApi>()
        coEvery { api.validateCookieSession(cookie) } returns loggedIn
        coEvery { api.checkWebQrCode(any()) } returns QrAuthorizationResponse(0)
        coEvery { api.getWebQrScene(any()) } returns QrAuthorizationSceneResponse(0, data = QrAuthorizationSceneData())
        coEvery { api.confirmWebQrCode(any()) } returns QrAuthorizationResponse(0)
        val service = OfficialQrAuthorizationService(api, ::session)
        service.prepare(request(BilibiliLoginQrType.WEB))
        coVerify {
            api.checkWebQrCode(match { params ->
                params["appkey"] == "4409e2ce8ffd12b8" &&
                    params["mobi_app"] == "android" &&
                    params["csrf"] == "csrf" &&
                    params["sign"]?.length == 32
            })
        }
        val hd = QrAuthorizationSession("session%2Cvalue", "csrf", 42, "buvid", "access-key", "android")
        OfficialQrAuthorizationService(api) { hd }.prepare(request(BilibiliLoginQrType.WEB))
        coVerify {
            api.checkWebQrCode(match { params ->
                params["appkey"] == "dfca71928277209b" && params["sign"]?.length == 32
            })
        }
    }

    @Test
    fun `current account page QR authorizes through the same signed endpoints`() = runTest {
        val api = mockk<PassportApi>()
        val qr = BilibiliLoginQr(BilibiliLoginQrType.WEB, key, usesAccountPage = true)
        coEvery { api.validateCookieSession(cookie) } returns loggedIn
        coEvery { api.checkWebQrCode(any()) } returns QrAuthorizationResponse(0)
        coEvery { api.getWebQrScene(any()) } returns QrAuthorizationSceneResponse(0,
            data = QrAuthorizationSceneData(locationDiffers = true, location = "上海"))
        coEvery { api.confirmWebQrCode(any()) } returns QrAuthorizationResponse(0)
        val service = OfficialQrAuthorizationService(api, ::session)
        val prepared = service.prepare(qr)
        assertEquals("上海", prepared.location)
        assertTrue(prepared.locationDiffers)
        service.confirm(prepared)
        coVerify(exactly = 1) { api.confirmWebQrCode(any()) }
    }

    @Test
    fun `verify_tel flag does not stop confirmation and verify fields are signed`() = runTest {
        val api = mockk<PassportApi>()
        coEvery { api.validateCookieSession(cookie) } returns loggedIn
        coEvery { api.checkWebQrCode(any()) } returns QrAuthorizationResponse(0)
        coEvery { api.getWebQrScene(any()) } returns QrAuthorizationSceneResponse(0,
            data = QrAuthorizationSceneData(phoneVerification = JsonPrimitive(1)))
        coEvery { api.confirmWebQrCode(any()) } returns QrAuthorizationResponse(0)
        val service = OfficialQrAuthorizationService(api, ::session)
        val prepared = service.prepare(request(BilibiliLoginQrType.WEB))
        service.confirm(prepared)
        coVerify(exactly = 1) {
            api.confirmWebQrCode(match {
                it["verify_type"] == "verify_tel" && it["verify_code"] == "" && it.containsKey("sign")
            })
        }
    }

    @Test
    fun `missing app credential stops web authorization before any request`() = runTest {
        val api = mockk<PassportApi>()
        coEvery { api.validateCookieSession(cookie) } returns loggedIn
        OfficialQrAuthorizationService(
            api, { QrAuthorizationSession("session%2Cvalue", "csrf", 42, "buvid", "") },
        ).let { service ->
            assertFailsWith<IllegalArgumentException> {
                service.prepare(request(BilibiliLoginQrType.WEB))
            }
        }
        coVerify(exactly = 0) { api.checkWebQrCode(any()) }
        coVerify(exactly = 0) { api.confirmWebQrCode(any()) }
    }

    @Test
    fun `TV authorizes the request ID with signed app credential and scanning type`() = runTest {
        val api = mockk<PassportApi>()
        val qr = request(BilibiliLoginQrType.TV)
        coEvery { api.validateCookieSession(cookie) } returns loggedIn
        coEvery { api.confirmTvQrCode(any(), any()) } returns QrAuthorizationResponse(0)
        val service = OfficialQrAuthorizationService(api, ::session)
        val prepared = service.prepare(qr)
        coVerify(exactly = 0) { api.confirmTvQrCode(any(), any()) }
        service.confirm(prepared)
        coVerify(exactly = 1) {
            api.confirmTvQrCode(
                match {
                    it["auth_code"] == key && it["access_key"] == "access-key" &&
                        it["scanning_type"] == "1" && it.containsKey("sign")
                },
                buvidHeader = "buvid",
            )
        }
    }

    @Test
    fun `account switch after preview prevents authorization`() = runTest {
        val api = mockk<PassportApi>()
        coEvery { api.validateCookieSession(cookie) } returns loggedIn
        var current = session()
        val service = OfficialQrAuthorizationService(api) { current }
        val prepared = service.prepare(request(BilibiliLoginQrType.TV))
        current = QrAuthorizationSession("other-session", "other-csrf", 7, "buvid")
        assertFailsWith<QrAuthorizationException> { service.confirm(prepared) }
        coVerify(exactly = 0) { api.confirmTvQrCode(any(), any()) }
    }

    @Test
    fun `expired web request never reaches confirm`() = runTest {
        val api = mockk<PassportApi>()
        coEvery { api.validateCookieSession(cookie) } returns loggedIn
        coEvery { api.checkWebQrCode(any()) } returns QrAuthorizationResponse(86038)
        val error = assertFailsWith<QrAuthorizationException> {
            OfficialQrAuthorizationService(api, ::session).prepare(request(BilibiliLoginQrType.WEB))
        }
        assertTrue(error.message.orEmpty().contains("过期"))
        coVerify(exactly = 0) { api.confirmWebQrCode(any()) }
    }

    @Test
    fun `valid cookie can still be rejected by TV authorization endpoint`() = runTest {
        val api = mockk<PassportApi>()
        coEvery { api.validateCookieSession(cookie) } returns loggedIn
        coEvery { api.confirmTvQrCode(any(), any()) } returns QrAuthorizationResponse(-101)
        val service = OfficialQrAuthorizationService(api, ::session)
        val prepared = service.prepare(request(BilibiliLoginQrType.TV))
        val error = assertFailsWith<QrAuthorizationException> { service.confirm(prepared) }
        assertTrue(error.message.orEmpty().contains("授权接口拒绝"))
    }

    @Test
    fun `expired authorizer session never reaches TV authorization`() = runTest {
        val api = mockk<PassportApi>()
        coEvery { api.validateCookieSession(cookie) } returnsMany listOf(loggedIn, NavResponse(code = -101))
        val service = OfficialQrAuthorizationService(api, ::session)
        val prepared = service.prepare(request(BilibiliLoginQrType.TV))
        assertFailsWith<QrAuthorizationException> { service.confirm(prepared) }
        coVerify(exactly = 0) { api.confirmTvQrCode(any(), any()) }
    }

    @Test
    fun `cancellation is propagated without treating it as authorization success`() = runTest {
        val api = mockk<PassportApi>()
        coEvery { api.validateCookieSession(cookie) } throws CancellationException("closed")
        assertFailsWith<CancellationException> {
            OfficialQrAuthorizationService(api, ::session).prepare(request(BilibiliLoginQrType.TV))
        }
    }

    @Test
    fun `missing response code is failure`() {
        assertEquals(-1, Json.decodeFromString(QrAuthorizationResponse.serializer(), "{}").code)
    }
}
