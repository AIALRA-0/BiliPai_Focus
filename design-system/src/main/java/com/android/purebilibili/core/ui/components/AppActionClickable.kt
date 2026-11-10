package com.android.purebilibili.core.ui.components

import androidx.compose.foundation.Indication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.semantics.Role

/** 页面自绘操作接入共用触感，保留原有涟漪、禁用状态和无障碍语义。 */
fun Modifier.appActionClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit,
): Modifier = composed {
    this.clickable(
        interactionSource = interactionSource,
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        onClick = appActionWithFeedback(onClick),
    )
}

fun Modifier.appActionClickable(
    interactionSource: MutableInteractionSource?,
    indication: Indication?,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit,
): Modifier = composed {
    this.clickable(
        interactionSource = interactionSource,
        indication = indication,
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        onClick = appActionWithFeedback(onClick),
    )
}
