package com.android.purebilibili.feature.onboarding

import androidx.annotation.AnyRes
import com.android.purebilibili.R
import com.android.purebilibili.core.store.DEFAULT_APP_ICON_KEY
import com.android.purebilibili.feature.settings.getIconGroups

internal data class OnboardingHeroIconSpec(
    @param:AnyRes val iconRes: Int,
    val imageScale: Float
)

private const val DEFAULT_ICON_IMAGE_SCALE = 1f
private const val NORMAL_ICON_IMAGE_SCALE = 1f

internal fun resolveOnboardingHeroIconSpec(
    appIconKey: String
): OnboardingHeroIconSpec {
    val normalizedKey = appIconKey.ifBlank { DEFAULT_APP_ICON_KEY }
    return when (normalizedKey) {
        DEFAULT_APP_ICON_KEY -> OnboardingHeroIconSpec(
            iconRes = R.mipmap.ic_launcher_blue_snow_maid_round,
            imageScale = DEFAULT_ICON_IMAGE_SCALE
        )

        else -> {
            val iconRes = getIconGroups()
                .asSequence()
                .flatMap { it.icons.asSequence() }
                .firstOrNull { it.key == normalizedKey }
                ?.iconRes

            if (iconRes == null) {
                resolveOnboardingHeroIconSpec(DEFAULT_APP_ICON_KEY)
            } else {
                OnboardingHeroIconSpec(
                    iconRes = iconRes,
                    imageScale = NORMAL_ICON_IMAGE_SCALE
                )
            }
        }
    }
}
