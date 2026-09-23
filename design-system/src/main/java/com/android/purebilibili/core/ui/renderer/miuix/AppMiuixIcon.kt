package com.android.purebilibili.core.ui.renderer.miuix

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import top.yukonga.miuix.kmp.basic.Icon

@Composable
internal fun AppMiuixIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier,
    tint: Color,
) = Icon(imageVector, contentDescription, modifier, tint)

@Composable
internal fun AppMiuixIcon(
    bitmap: ImageBitmap,
    contentDescription: String?,
    modifier: Modifier,
    tint: Color,
) = Icon(bitmap, contentDescription, modifier, tint)

@Composable
internal fun AppMiuixIcon(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier,
    tint: Color,
) = Icon(painter, contentDescription, modifier, tint)

@Composable
internal fun AppMiuixIcon(
    painter: Painter,
    tint: ColorProducer?,
    contentDescription: String?,
    modifier: Modifier,
) = Icon(painter, tint, contentDescription, modifier)
