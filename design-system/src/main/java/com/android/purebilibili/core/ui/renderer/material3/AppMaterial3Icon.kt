package com.android.purebilibili.core.ui.renderer.material3

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
internal fun AppMaterial3Icon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier,
    tint: Color,
) = Icon(imageVector, contentDescription, modifier, tint)

@Composable
internal fun AppMaterial3Icon(
    bitmap: ImageBitmap,
    contentDescription: String?,
    modifier: Modifier,
    tint: Color,
) = Icon(bitmap, contentDescription, modifier, tint)

@Composable
internal fun AppMaterial3Icon(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier,
    tint: Color,
) = Icon(painter, contentDescription, modifier, tint)

@Composable
internal fun AppMaterial3Icon(
    painter: Painter,
    tint: ColorProducer?,
    contentDescription: String?,
    modifier: Modifier,
) = Icon(painter, tint, contentDescription, modifier)
