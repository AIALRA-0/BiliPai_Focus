package com.android.purebilibili.feature.onboarding

import com.android.purebilibili.R
import kotlin.test.Test
import kotlin.test.assertEquals

class OnboardingHeroIconPolicyTest {

    @Test
    fun defaultIconUsesBlueSnowMaidRoundResource() {
        val spec = resolveOnboardingHeroIconSpec("icon_blue_snow_maid")

        assertEquals(R.mipmap.ic_launcher_blue_snow_maid_round, spec.iconRes)
        assertEquals(1f, spec.imageScale)
    }

    @Test
    fun unknownIconFallsBackToDefaultIconSpec() {
        val defaultSpec = resolveOnboardingHeroIconSpec("icon_blue_snow_maid")
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
