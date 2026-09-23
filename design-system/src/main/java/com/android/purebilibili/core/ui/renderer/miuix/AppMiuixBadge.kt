package com.android.purebilibili.core.ui.renderer.miuix

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import top.yukonga.miuix.kmp.basic.Badge
import top.yukonga.miuix.kmp.basic.BadgeDefaults

@Composable
internal fun AppMiuixBadge(
    modifier: Modifier,
    containerColor: Color,
    contentColor: Color,
    content: @Composable (RowScope.() -> Unit)?,
) = Badge(
    modifier = modifier,
    containerColor = containerColor.takeOrElse { BadgeDefaults.containerColor },
    contentColor = contentColor.takeOrElse { BadgeDefaults.contentColor },
    content = content,
)
