package com.android.purebilibili.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle
import com.android.purebilibili.core.ui.currentAppContentColor
import com.android.purebilibili.core.ui.renderer.material3.AppMaterial3Icon
import com.android.purebilibili.core.ui.renderer.miuix.AppMiuixIcon

@Composable
fun AppIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = currentAppContentColor(),
) = when (LocalAppUiStyle.current) {
    AppUiStyle.MATERIAL3 -> AppMaterial3Icon(imageVector, contentDescription, modifier, tint)
    AppUiStyle.MIUIX -> AppMiuixIcon(imageVector, contentDescription, modifier, tint)
}

@Composable
fun AppIcon(
    bitmap: ImageBitmap,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = currentAppContentColor(),
) = when (LocalAppUiStyle.current) {
    AppUiStyle.MATERIAL3 -> AppMaterial3Icon(bitmap, contentDescription, modifier, tint)
    AppUiStyle.MIUIX -> AppMiuixIcon(bitmap, contentDescription, modifier, tint)
}

@Composable
fun AppIcon(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = currentAppContentColor(),
) = when (LocalAppUiStyle.current) {
    AppUiStyle.MATERIAL3 -> AppMaterial3Icon(painter, contentDescription, modifier, tint)
    AppUiStyle.MIUIX -> AppMiuixIcon(painter, contentDescription, modifier, tint)
}

@Composable
fun AppIcon(
    painter: Painter,
    tint: ColorProducer?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) = when (LocalAppUiStyle.current) {
    AppUiStyle.MATERIAL3 -> AppMaterial3Icon(painter, tint, contentDescription, modifier)
    AppUiStyle.MIUIX -> AppMiuixIcon(painter, tint, contentDescription, modifier)
}
