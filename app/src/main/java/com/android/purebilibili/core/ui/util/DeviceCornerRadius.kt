package com.android.purebilibili.core.ui.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.RoundedCorner
import android.view.View
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun rememberDeviceCornerRadius(defaultRadius: Dp = 16.dp): Dp {
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current

    return remember(context, view, density, defaultRadius) {
        val radiusPx = getDeviceCornerRadiusPx(context, view)
        if (radiusPx > 0) {
            with(density) { radiusPx.toDp() }
        } else {
            defaultRadius
        }
    }
}

@Composable
fun rememberDeviceCornerShape(defaultRadius: Dp = 16.dp): RoundedCornerShape {
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current

    return remember(context, view, density, defaultRadius) {
        val radiiPx = getDeviceCornerRadiiPx(context, view)
        if (radiiPx.any { it > 0 }) {
            with(density) {
                RoundedCornerShape(
                    topStart = radiiPx[0].toDp(),
                    topEnd = radiiPx[1].toDp(),
                    bottomEnd = radiiPx[2].toDp(),
                    bottomStart = radiiPx[3].toDp()
                )
            }
        } else {
            RoundedCornerShape(defaultRadius)
        }
    }
}

private fun getDeviceCornerRadiusPx(context: Context, view: View): Int {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val publicRadius = getRoundedCornerRadiusPx(view)
        if (publicRadius > 0) return publicRadius
    }
    return getCornerRadiusBottom(context)
}

private fun getRoundedCornerRadiusPx(view: View): Int {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return 0
    val insets = view.rootWindowInsets ?: return 0
    val corner = insets.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)
        ?: insets.getRoundedCorner(RoundedCorner.POSITION_TOP_RIGHT)
        ?: insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT)
        ?: insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_RIGHT)
    return corner?.radius ?: 0
}

private fun getDeviceCornerRadiiPx(context: Context, view: View): IntArray {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val publicRadii = getRoundedCornerRadiiPx(view)
        if (publicRadii.any { it > 0 }) return publicRadii
    }
    val fallback = getCornerRadiusBottom(context)
    return intArrayOf(fallback, fallback, fallback, fallback)
}

private fun getRoundedCornerRadiiPx(view: View): IntArray {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return intArrayOf(0, 0, 0, 0)
    val insets = view.rootWindowInsets ?: return intArrayOf(0, 0, 0, 0)
    return intArrayOf(
        insets.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)?.radius ?: 0,
        insets.getRoundedCorner(RoundedCorner.POSITION_TOP_RIGHT)?.radius ?: 0,
        insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_RIGHT)?.radius ?: 0,
        insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT)?.radius ?: 0
    )
}

@SuppressLint("DiscouragedApi")
private fun getCornerRadiusBottom(context: Context): Int {
    val resources = context.resources
    val bottomId = resources.getIdentifier("rounded_corner_radius_bottom", "dimen", "android")
    if (bottomId > 0) return resources.getDimensionPixelSize(bottomId)
    val generalId = resources.getIdentifier("rounded_corner_radius", "dimen", "android")
    return if (generalId > 0) {
        resources.getDimensionPixelSize(generalId)
    } else {
        0
    }
}
