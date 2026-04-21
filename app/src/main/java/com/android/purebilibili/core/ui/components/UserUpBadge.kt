package com.android.purebilibili.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal data class UserUpBadgeVisualSpec(
    val cornerRadiusDp: Int,
    val horizontalPaddingDp: Int,
    val verticalPaddingDp: Int,
    val fontSp: Int
)

internal fun resolveUserUpBadgeVisualSpec(): UserUpBadgeVisualSpec {
    return UserUpBadgeVisualSpec(
        cornerRadiusDp = 3,
        horizontalPaddingDp = 3,
        verticalPaddingDp = 2,
        fontSp = 9
    )
}

@Composable
fun UserUpBadge(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    val spec = remember { resolveUserUpBadgeVisualSpec() }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(spec.cornerRadiusDp.dp))
            .background(containerColor)
            .padding(
                horizontal = spec.horizontalPaddingDp.dp,
                vertical = spec.verticalPaddingDp.dp
            )
    ) {
        Text(
            text = "UP",
            fontSize = spec.fontSp.sp,
            lineHeight = spec.fontSp.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}
