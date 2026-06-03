package com.android.purebilibili.feature.home.components

data class DrawerGlassPalette(
    val drawerBaseAlpha: Float,
    val itemSurfaceAlpha: Float,
    val itemBorderAlpha: Float,
    val dividerAlpha: Float,
    val hazeBackgroundAlpha: Float,
    val hazeTintAlpha: Float
)

internal fun resolveDrawerGlassPalette(
    isDark: Boolean,
    blurEnabled: Boolean,
    budget: DrawerMotionBudget = DrawerMotionBudget.FULL
): DrawerGlassPalette {
    if (!blurEnabled) {
        return if (isDark) {
            DrawerGlassPalette(
                drawerBaseAlpha = 0.94f,
                itemSurfaceAlpha = 0.22f,
                itemBorderAlpha = 0.28f,
                dividerAlpha = 0.30f,
                hazeBackgroundAlpha = 0.60f,
                hazeTintAlpha = 0.40f
            )
        } else {
            DrawerGlassPalette(
                drawerBaseAlpha = 0.95f,
                itemSurfaceAlpha = 0.84f,
                itemBorderAlpha = 0.12f,
                dividerAlpha = 0.18f,
                hazeBackgroundAlpha = 0.64f,
                hazeTintAlpha = 0.34f
            )
        }
    }

    if (budget == DrawerMotionBudget.REDUCED) {
        return if (isDark) {
            DrawerGlassPalette(
                drawerBaseAlpha = 0.48f,
                itemSurfaceAlpha = 0.24f,
                itemBorderAlpha = 0.24f,
                dividerAlpha = 0.24f,
                hazeBackgroundAlpha = 0.36f,
                hazeTintAlpha = 0.18f
            )
        } else {
            DrawerGlassPalette(
                drawerBaseAlpha = 0.44f,
                itemSurfaceAlpha = 0.24f,
                itemBorderAlpha = 0.14f,
                dividerAlpha = 0.16f,
                hazeBackgroundAlpha = 0.30f,
                hazeTintAlpha = 0.10f
            )
        }
    }

    return if (isDark) {
        DrawerGlassPalette(
            drawerBaseAlpha = 0.34f,
            itemSurfaceAlpha = 0.20f,
            itemBorderAlpha = 0.24f,
            dividerAlpha = 0.24f,
            hazeBackgroundAlpha = 0.24f,
            hazeTintAlpha = 0.14f
        )
    } else {
        DrawerGlassPalette(
            drawerBaseAlpha = 0.30f,
            itemSurfaceAlpha = 0.20f,
            itemBorderAlpha = 0.14f,
            dividerAlpha = 0.16f,
            hazeBackgroundAlpha = 0.18f,
            hazeTintAlpha = 0.06f
        )
    }
}

internal fun resolveHomeDrawerScrimAlpha(blurEnabled: Boolean): Float {
    return if (blurEnabled) 0.14f else 0.28f
}
