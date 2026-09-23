package com.android.purebilibili.core.ui.components

import com.android.purebilibili.core.theme.AppUiStyle

enum class AppClickableItemRenderer {
    CUPERTINO,
    MD3_BASIC,
    MIUIX_ARROW,
    MIUIX_BASIC,
}

fun resolveAppClickableItemRenderer(
    uiStyle: AppUiStyle,
    onClick: (() -> Unit)?,
    showChevron: Boolean,
    centered: Boolean,
): AppClickableItemRenderer = when {
    centered -> AppClickableItemRenderer.CUPERTINO
    uiStyle == AppUiStyle.MIUIX -> {
        if (onClick != null && showChevron) {
            AppClickableItemRenderer.MIUIX_ARROW
        } else {
            AppClickableItemRenderer.MIUIX_BASIC
        }
    }
    else -> AppClickableItemRenderer.MD3_BASIC
}

fun shouldRouteClickableItemToMiuixArrowPreference(
    uiStyle: AppUiStyle,
    onClick: (() -> Unit)?,
    showChevron: Boolean,
    centered: Boolean,
): Boolean = resolveAppClickableItemRenderer(
    uiStyle = uiStyle,
    onClick = onClick,
    showChevron = showChevron,
    centered = centered
) == AppClickableItemRenderer.MIUIX_ARROW

fun shouldRouteSwitchItemToMiuixSwitchPreference(
    uiStyle: AppUiStyle,
): Boolean = uiStyle == AppUiStyle.MIUIX

fun shouldRouteSliderPreferenceToMiuixSliderPreference(
    uiStyle: AppUiStyle,
): Boolean = uiStyle == AppUiStyle.MIUIX
