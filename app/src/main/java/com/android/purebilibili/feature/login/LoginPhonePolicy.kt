package com.android.purebilibili.feature.login

import com.android.purebilibili.data.model.response.PassportCountryItem
import com.android.purebilibili.data.model.response.PassportCountryListData
import kotlin.math.roundToInt

/**
 * 短信登录国际地区。
 *
 * [cid] 对应 bilibili 接口字段 `cid`，取值来自国家列表的 `id`（中国大陆 = 1），
 * 不是拨号区号。拨号区号见 [countryId] / [dialingCode]。
 */
data class PhoneRegion(
    val cid: Int,
    val countryId: String,
    val dialingCode: String,
    val name: String,
    val minDigits: Int,
    val maxDigits: Int,
    val isCommon: Boolean = false,
)

data class CaptchaDialogLayoutPolicy(
    val widthPx: Int,
    val heightPx: Int,
    val dimAmount: Float
)

/** 中国大陆在 passport 国家列表中的 id（短信接口 cid）。 */
const val DEFAULT_PHONE_REGION_CID = 1

/**
 * 离线兜底列表：id / country_id 与
 * `GET https://passport.bilibili.com/web/generic/country/list` 对齐。
 * 在线成功后会用完整 common+others 替换。
 */
fun resolveFallbackPhoneRegions(): List<PhoneRegion> {
    return listOf(
        phoneRegion(cid = 1, countryId = "86", name = "中国大陆", isCommon = true),
        phoneRegion(cid = 5, countryId = "852", name = "中国香港特别行政区", isCommon = true),
        phoneRegion(cid = 2, countryId = "853", name = "中国澳门特别行政区", isCommon = true),
        phoneRegion(cid = 3, countryId = "886", name = "中国台湾", isCommon = true),
        phoneRegion(cid = 4, countryId = "1", name = "美国", isCommon = true),
        phoneRegion(cid = 9, countryId = "1", name = "加拿大", isCommon = true),
        phoneRegion(cid = 10, countryId = "81", name = "日本", isCommon = true),
        phoneRegion(cid = 12, countryId = "82", name = "韩国", isCommon = true),
        phoneRegion(cid = 11, countryId = "65", name = "新加坡", isCommon = true),
        phoneRegion(cid = 13, countryId = "60", name = "马来西亚", isCommon = true),
        phoneRegion(cid = 7, countryId = "61", name = "澳大利亚", isCommon = true),
        phoneRegion(cid = 14, countryId = "44", name = "英国", isCommon = true),
        phoneRegion(cid = 8, countryId = "33", name = "法国", isCommon = true),
        phoneRegion(cid = 16, countryId = "49", name = "德国", isCommon = true),
    )
}

/** @deprecated 使用 [resolveFallbackPhoneRegions]；保留别名避免旧调用断裂。 */
fun resolveSupportedPhoneRegions(): List<PhoneRegion> = resolveFallbackPhoneRegions()

fun mapPassportCountryListToPhoneRegions(data: PassportCountryListData): List<PhoneRegion> {
    val common = data.common.mapNotNull { it.toPhoneRegionOrNull(isCommon = true) }
    val others = data.others
        .mapNotNull { it.toPhoneRegionOrNull(isCommon = false) }
        .sortedBy { it.name }
    // 常用在前；others 去重（按 cid）
    val seen = common.map { it.cid }.toHashSet()
    val uniqueOthers = others.filter { seen.add(it.cid) }
    return common + uniqueOthers
}

fun filterPhoneRegions(regions: List<PhoneRegion>, query: String): List<PhoneRegion> {
    val normalized = query.trim().lowercase()
    if (normalized.isEmpty()) return regions
    val digitsOnly = normalized.filter { it.isDigit() }
    return regions.filter { region ->
        region.name.lowercase().contains(normalized) ||
            region.dialingCode.lowercase().contains(normalized) ||
            region.countryId.contains(digitsOnly.takeIf { it.isNotEmpty() } ?: normalized) ||
            region.cid.toString() == digitsOnly
    }
}

fun resolveDefaultPhoneRegion(regions: List<PhoneRegion>): PhoneRegion {
    return regions.firstOrNull { it.cid == DEFAULT_PHONE_REGION_CID }
        ?: regions.firstOrNull { it.countryId == "86" }
        ?: regions.first()
}

fun resolvePhoneDigitRange(countryId: String): IntRange {
    return when (countryId) {
        "86" -> 11..11
        "852", "853", "65" -> 8..8
        "886" -> 9..10
        "1" -> 10..10
        "81" -> 10..11
        "82" -> 9..11
        "44" -> 10..11
        "61" -> 9..9
        "60" -> 9..11
        "33", "49", "39", "7" -> 9..12
        else -> 6..15 // E.164 国内号码通常不超过 15 位
    }
}

fun isPhoneDigitsValidForRegion(phoneDigits: String, region: PhoneRegion): Boolean {
    if (phoneDigits.isBlank()) return false
    if (!phoneDigits.all { it.isDigit() }) return false
    return phoneDigits.length in region.minDigits..region.maxDigits
}

fun isPhoneEligibleForCaptcha(phoneDigits: String, region: PhoneRegion): Boolean {
    return isPhoneDigitsValidForRegion(phoneDigits = phoneDigits, region = region)
}

/**
 * App SMS/login `cid` used by PiliPlus `LoginHttp.sendSmsCode`.
 *
 * Passport's country list `id` (China = 1) is only for UI grouping.
 * The Android-HD SMS endpoints expect `country_id` (China = 86).
 */
fun resolveSmsApiCid(region: PhoneRegion): Int {
    return region.countryId.toIntOrNull() ?: region.cid
}

fun resolveCaptchaDialogLayoutPolicy(
    screenWidthPx: Int,
    screenHeightPx: Int,
    density: Float
): CaptchaDialogLayoutPolicy {
    val widthPx = (screenWidthPx * 0.96f).roundToInt().coerceAtLeast(1)
    val heightPx = (screenHeightPx * 0.88f).roundToInt().coerceAtLeast(1)

    return CaptchaDialogLayoutPolicy(
        widthPx = widthPx,
        heightPx = heightPx,
        dimAmount = 0.42f
    )
}

private fun phoneRegion(
    cid: Int,
    countryId: String,
    name: String,
    isCommon: Boolean,
): PhoneRegion {
    val range = resolvePhoneDigitRange(countryId)
    return PhoneRegion(
        cid = cid,
        countryId = countryId,
        dialingCode = "+$countryId",
        name = name,
        minDigits = range.first,
        maxDigits = range.last,
        isCommon = isCommon,
    )
}

private fun PassportCountryItem.toPhoneRegionOrNull(isCommon: Boolean): PhoneRegion? {
    if (id <= 0) return null
    val countryId = countryId.trim().trimStart('+')
    if (countryId.isEmpty() || !countryId.all { it.isDigit() }) return null
    val name = cname.trim().ifEmpty { return null }
    return phoneRegion(
        cid = id,
        countryId = countryId,
        name = name,
        isCommon = isCommon,
    )
}
