package com.android.purebilibili.core.ui.renderer.material3

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgeDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse

@Composable
internal fun AppMaterial3Badge(
    modifier: Modifier,
    containerColor: Color,
    contentColor: Color,
    content: @Composable (RowScope.() -> Unit)?,
) {
    val resolvedContainerColor = containerColor.takeOrElse { BadgeDefaults.containerColor }
    Badge(
        modifier = modifier,
        containerColor = resolvedContainerColor,
        contentColor = contentColor.takeOrElse { contentColorFor(resolvedContainerColor) },
        content = content,
    )
}
