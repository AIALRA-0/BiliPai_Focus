package com.android.purebilibili.core.store

import android.content.Context
import com.android.purebilibili.core.theme.AppUiStyle

suspend fun applyOnboardingRecommendedUiStyle(context: Context) {
    SettingsManager.setUiStyle(context, AppUiStyle.MATERIAL3)
}
