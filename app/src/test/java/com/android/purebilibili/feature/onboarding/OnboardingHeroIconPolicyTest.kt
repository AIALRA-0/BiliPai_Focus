package com.android.purebilibili.feature.onboarding

import com.android.purebilibili.R
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OnboardingHeroIconPolicyTest {

    @Test
    fun defaultIconUsesForegroundResourceAndCropsSafeArea() {
        val spec = resolveOnboardingHeroIconSpec("icon_3d")

        assertEquals(R.mipmap.ic_launcher_3d_foreground, spec.iconRes)
        assertTrue(spec.imageScale > 1f)
    }

    @Test
    fun unknownIconFallsBackToDefaultIconSpec() {
        val defaultSpec = resolveOnboardingHeroIconSpec("icon_3d")
        val unknownSpec = resolveOnboardingHeroIconSpec("missing_icon")

        assertEquals(defaultSpec, unknownSpec)
    }

    @Test
    fun nonDefaultIconKeepsOriginalScale() {
        val spec = resolveOnboardingHeroIconSpec("icon_bilipai")

        assertEquals(R.mipmap.ic_launcher_bilipai_round, spec.iconRes)
        assertEquals(1f, spec.imageScale)
    }
}
