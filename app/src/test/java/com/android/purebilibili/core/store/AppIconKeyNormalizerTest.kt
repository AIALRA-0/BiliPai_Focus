package com.android.purebilibili.core.store

import kotlin.test.Test
import kotlin.test.assertEquals

class AppIconKeyNormalizerTest {

    @Test
    fun normalizeAppIconKey_mapsLegacyKeysToCanonicalKeys() {
        assertEquals("icon_telegram_blue", normalizeAppIconKey("Telegram Blue"))
        assertEquals("icon_telegram_dark", normalizeAppIconKey("Dark"))
        assertEquals("icon_bilipai", normalizeAppIconKey("BiliPai"))
        assertEquals("icon_bilipai_pink", normalizeAppIconKey("BiliPai Pink"))
        assertEquals("icon_bilipai_white", normalizeAppIconKey("BiliPai 白"))
        assertEquals("icon_bilipai_monet", normalizeAppIconKey("bilipai_monet"))
        assertEquals("Headphone", normalizeAppIconKey("icon_headphone"))
        assertEquals("icon_anime", normalizeAppIconKey("Anime"))
    }

    @Test
    fun normalizeAppIconKey_fallsBackToDefaultForUnknownOrBlankValues() {
        assertEquals("icon_3d", normalizeAppIconKey(""))
        assertEquals("icon_3d", normalizeAppIconKey("   "))
        assertEquals("icon_3d", normalizeAppIconKey("non-existent"))
        assertEquals("icon_3d", normalizeAppIconKey("icon_retro"))
        assertEquals("icon_3d", normalizeAppIconKey("Flat Material"))
        assertEquals("icon_3d", normalizeAppIconKey("Blue"))
        assertEquals("icon_3d", normalizeAppIconKey("Neon"))
        assertEquals("icon_3d", normalizeAppIconKey("Pink"))
        assertEquals("icon_3d", normalizeAppIconKey("Telegram Purple"))
        assertEquals("icon_3d", normalizeAppIconKey("Green"))
        assertEquals("icon_3d", normalizeAppIconKey("Telegram Blue Coin"))
    }
}
