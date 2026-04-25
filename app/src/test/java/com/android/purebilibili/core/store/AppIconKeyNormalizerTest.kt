package com.android.purebilibili.core.store

import kotlin.test.Test
import kotlin.test.assertEquals

class AppIconKeyNormalizerTest {

    @Test
    fun normalizeAppIconKey_mapsLegacyKeysToCanonicalKeys() {
        assertEquals("icon_telegram_blue", normalizeAppIconKey("Telegram Blue"))
        assertEquals("icon_telegram_pink", normalizeAppIconKey("Pink"))
        assertEquals("icon_telegram_purple", normalizeAppIconKey("Telegram Purple"))
        assertEquals("icon_telegram_green", normalizeAppIconKey("Green"))
        assertEquals("icon_telegram_dark", normalizeAppIconKey("Dark"))
        assertEquals("icon_bilipai", normalizeAppIconKey("BiliPai"))
        assertEquals("Headphone", normalizeAppIconKey("icon_headphone"))
        assertEquals("icon_anime", normalizeAppIconKey("Anime"))
        assertEquals("icon_blue", normalizeAppIconKey("Blue"))
    }

    @Test
    fun normalizeAppIconKey_fallsBackToDefaultForUnknownOrBlankValues() {
        assertEquals("icon_3d", normalizeAppIconKey(""))
        assertEquals("icon_3d", normalizeAppIconKey("   "))
        assertEquals("icon_3d", normalizeAppIconKey("non-existent"))
        assertEquals("icon_3d", normalizeAppIconKey("icon_retro"))
        assertEquals("icon_3d", normalizeAppIconKey("Flat Material"))
    }
}
