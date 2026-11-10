package com.android.purebilibili.feature.settings

import com.android.purebilibili.navigation3.BiliPaiNavKey
import com.android.purebilibili.navigation3.legacyRouteToBiliPaiNavKey
import com.android.purebilibili.navigation3.toLegacyRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpacePrivacyPolicyTest {
    @Test
    fun publicSwitchesRespectServerHideFlags() {
        listOf(
            SpacePrivacyOption.FOLLOWING,
            SpacePrivacyOption.FANS,
            SpacePrivacyOption.MEDAL,
            SpacePrivacyOption.MEDAL_WALL,
            SpacePrivacyOption.SCHOOL,
        ).forEach { option ->
            assertEquals(true, option.checked(0))
            assertEquals(false, option.checked(1))
            assertEquals(0, option.raw(true))
            assertEquals(1, option.raw(false))
        }
        assertEquals(false, SpacePrivacyOption.FAVORITES.checked(0))
        assertEquals(true, SpacePrivacyOption.FAVORITES.checked(1))
    }

    @Test
    fun absentAndUnexpectedValuesRemainUnknown() {
        SpacePrivacyOption.entries.forEach { option ->
            assertNull(option.checked(null))
            assertNull(option.checked(2))
            assertNull(option.checked(-1))
        }
    }

    @Test
    fun saveIncludesOnlyChangedKnownFieldsPresentInOriginal() {
        val original = mapOf("fav_video" to 0, "disable_following" to 0, "future_field" to 0)
        val draft = original + mapOf("fav_video" to 1, "disable_show_fans" to 1, "future_field" to 1)
        assertEquals(mapOf("fav_video" to 1), spacePrivacyChanges(original, draft))
        assertTrue(spacePrivacyChanges(null, draft).isEmpty())
        assertTrue(spacePrivacyChanges(original, original).isEmpty())
        assertTrue(spacePrivacyChanges(original, original + ("fav_video" to 2)).isEmpty())
    }

    @Test
    fun togglingBackToServerValueRemovesPendingChange() {
        val original = mapOf("disable_following" to 0)
        val hidden = original + ("disable_following" to SpacePrivacyOption.FOLLOWING.raw(false))
        assertFalse(spacePrivacyChanges(original, hidden).isEmpty())
        assertTrue(spacePrivacyChanges(original, hidden + ("disable_following" to SpacePrivacyOption.FOLLOWING.raw(true))).isEmpty())
    }

    @Test
    fun routeBridgeAndSettingsCategoryAgree() {
        val key = BiliPaiNavKey.SpacePrivacySettings
        assertEquals(key, legacyRouteToBiliPaiNavKey(key.toLegacyRoute()))
        assertEquals(SettingsRootCategory.PRIVACY_PERMISSION, resolveSettingsRootCategoryForNavKey(key))
        assertEquals(2, resolveSettingsNavDepth(key.routeBase))
        assertEquals(key, resolveSettingsContainerTarget(SettingsSearchTarget.SPACE_PRIVACY))
    }
}
