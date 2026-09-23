package com.android.purebilibili.feature.settings

data class SettingsTabletLayoutPolicy(
    val primaryRatio: Float,
    val masterPanePaddingDp: Int,
    val detailPanePaddingDp: Int,
    val detailMaxWidthDp: Int,
    val rootPanelMaxWidthDp: Int
)

fun shouldUseSettingsSplitLayout(
    widthDp: Int
): Boolean = widthDp >= 840

fun shouldRenderSettingsSinglePaneContent(
    widthDp: Int,
    forceSinglePaneContent: Boolean
): Boolean = forceSinglePaneContent || !shouldUseSettingsSplitLayout(widthDp)

fun resolveSettingsTabletLayoutPolicy(
    widthDp: Int
): SettingsTabletLayoutPolicy {
    return when {
        widthDp >= 1600 -> SettingsTabletLayoutPolicy(
            primaryRatio = 0.30f,
            masterPanePaddingDp = 20,
            detailPanePaddingDp = 28,
            detailMaxWidthDp = 920,
            rootPanelMaxWidthDp = 700
        )
        widthDp >= 840 -> SettingsTabletLayoutPolicy(
            primaryRatio = 0.35f,
            masterPanePaddingDp = 16,
            detailPanePaddingDp = 24,
            detailMaxWidthDp = 800,
            rootPanelMaxWidthDp = 600
        )
        else -> SettingsTabletLayoutPolicy(
            primaryRatio = 0.38f,
            masterPanePaddingDp = 14,
            detailPanePaddingDp = 20,
            detailMaxWidthDp = 720,
            rootPanelMaxWidthDp = 560
        )
    }
}
