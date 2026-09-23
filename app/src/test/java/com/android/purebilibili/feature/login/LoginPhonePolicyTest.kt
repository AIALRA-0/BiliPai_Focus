package com.android.purebilibili.feature.login

import com.android.purebilibili.data.model.response.PassportCountryItem
import com.android.purebilibili.data.model.response.PassportCountryListData
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LoginPhonePolicyTest {

    @Test
    fun `fallback regions use passport id as cid not dialing code`() {
        val regions = resolveFallbackPhoneRegions()
        val mainland = regions.first { it.countryId == "86" }
        val us = regions.first { it.name.contains("美国") }

        // 文档示例：中国大陆 id=1 / country_id=86；美国 id=4 / country_id=1
        assertEquals(1, mainland.cid)
        assertEquals("+86", mainland.dialingCode)
        assertEquals(4, us.cid)
        assertEquals("+1", us.dialingCode)
        assertTrue(regions.none { it.cid == 86 })
    }

    @Test
    fun `map passport country list preserves common first and maps id`() {
        val mapped = mapPassportCountryListToPhoneRegions(
            PassportCountryListData(
                common = listOf(
                    PassportCountryItem(id = 1, cname = "中国大陆", countryId = "86"),
                    PassportCountryItem(id = 4, cname = "美国", countryId = "1"),
                ),
                others = listOf(
                    PassportCountryItem(id = 22, cname = "阿富汗", countryId = "93"),
                    PassportCountryItem(id = 1, cname = "重复大陆", countryId = "86"),
                ),
            )
        )

        assertEquals(listOf(1, 4, 22), mapped.map { it.cid })
        assertTrue(mapped.first().isCommon)
        assertFalse(mapped.last().isCommon)
        assertEquals("+93", mapped.last().dialingCode)
    }

    @Test
    fun `filter phone regions by name or dialing code`() {
        val regions = resolveFallbackPhoneRegions()
        assertTrue(filterPhoneRegions(regions, "日本").any { it.countryId == "81" })
        assertTrue(filterPhoneRegions(regions, "852").any { it.cid == 5 })
        assertTrue(filterPhoneRegions(regions, "+44").any { it.countryId == "44" })
        assertTrue(filterPhoneRegions(regions, "不存在的国家").isEmpty())
    }

    @Test
    fun `international phone numbers can pass captcha eligibility`() {
        val us = resolveFallbackPhoneRegions().first { it.cid == 4 }
        assertTrue(isPhoneEligibleForCaptcha(phoneDigits = "4155552671", region = us))
        assertFalse(isPhoneEligibleForCaptcha(phoneDigits = "123", region = us))
    }

    @Test
    fun `default region is china mainland passport id`() {
        val region = resolveDefaultPhoneRegion(resolveFallbackPhoneRegions())
        assertEquals(DEFAULT_PHONE_REGION_CID, region.cid)
        assertEquals("86", region.countryId)
    }

    @Test
    fun `app sms cid uses PiliPlus countryId not passport list id`() {
        val mainland = resolveFallbackPhoneRegions().first { it.countryId == "86" }
        val us = resolveFallbackPhoneRegions().first { it.name.contains("美国") }

        assertEquals(1, mainland.cid)
        assertEquals(86, resolveSmsApiCid(mainland))
        assertEquals(4, us.cid)
        assertEquals(1, resolveSmsApiCid(us))
    }

    @Test
    fun `captcha dialog policy keeps usable height on mobile`() {
        val spec = resolveCaptchaDialogLayoutPolicy(
            screenWidthPx = 1080,
            screenHeightPx = 2400,
            density = 3f
        )

        assertEquals((1080 * 0.96f).roundToInt(), spec.widthPx)
        assertEquals((2400 * 0.88f).roundToInt(), spec.heightPx)
        assertEquals(0.42f, spec.dimAmount)
    }
}
