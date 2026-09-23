package com.android.purebilibili.core.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.renderer.material3.AppMaterial3Card
import com.android.purebilibili.core.ui.renderer.miuix.AppMiuixCard

@Immutable
sealed interface AppCardShape {
    @Immutable
    data class Semantic(val level: ContainerLevel) : AppCardShape

    @Immutable
    data class Uniform(val cornerRadius: Dp) : AppCardShape
}

@Immutable
data class AppCardColors(
    val containerColor: Color,
    val contentColor: Color = Color.Unspecified,
)

enum class AppCardVariant {
    Filled,
    Elevated,
}

object AppCardDefaults {
    fun colors(
        containerColor: Color,
        contentColor: Color = Color.Unspecified,
    ): AppCardColors = AppCardColors(
        containerColor = containerColor,
        contentColor = contentColor,
    )
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    shape: AppCardShape? = null,
    colors: AppCardColors? = null,
    variant: AppCardVariant = AppCardVariant.Filled,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    when (LocalAppUiStyle.current) {
        AppUiStyle.MATERIAL3 -> AppMaterial3Card(
            modifier = modifier,
            shape = shape,
            colors = colors,
            variant = variant,
            onClick = onClick,
            onLongClick = onLongClick,
            content = content,
        )
        AppUiStyle.MIUIX -> AppMiuixCard(
            // Miuix exposes one native Card variant; both app variants map to that component.
            modifier = modifier,
            shape = shape,
            colors = colors,
            onClick = onClick,
            onLongClick = onLongClick,
            content = content,
        )
    }
}
