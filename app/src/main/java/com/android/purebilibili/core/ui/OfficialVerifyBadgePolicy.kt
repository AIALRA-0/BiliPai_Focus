package com.android.purebilibili.core.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class OfficialVerifyBadgeTone {
    PERSONAL,
    ORGANIZATION
}

data class OfficialVerifyBadgeSpec(
    val text: String,
    val contentDescription: String,
    val tone: OfficialVerifyBadgeTone
)

fun resolveOfficialVerifyBadge(
    type: Int?,
    title: String? = null,
    desc: String? = null,
    compact: Boolean = false
): OfficialVerifyBadgeSpec? {
    val tone = when (type) {
        0 -> OfficialVerifyBadgeTone.PERSONAL
        1 -> OfficialVerifyBadgeTone.ORGANIZATION
        else -> return null
    }
    return buildOfficialVerifyBadgeSpec(
        tone = tone,
        title = title,
        desc = desc,
        compact = compact
    )
}

fun resolveOfficialVerifyBadgeFromRole(
    type: Int?,
    role: Int?,
    title: String? = null,
    desc: String? = null,
    compact: Boolean = false
): OfficialVerifyBadgeSpec? {
    if ((type ?: -1) < 0) return null
    val tone = when (role) {
        3, 4, 5, 6 -> OfficialVerifyBadgeTone.ORGANIZATION
        else -> OfficialVerifyBadgeTone.PERSONAL
    }
    return buildOfficialVerifyBadgeSpec(
        tone = tone,
        title = title,
        desc = desc,
        compact = compact
    )
}

private fun buildOfficialVerifyBadgeSpec(
    tone: OfficialVerifyBadgeTone,
    title: String?,
    desc: String?,
    compact: Boolean
): OfficialVerifyBadgeSpec {
    val fullText = title.orEmpty().ifBlank { desc.orEmpty() }.ifBlank {
        when (tone) {
            OfficialVerifyBadgeTone.PERSONAL -> "个人认证"
            OfficialVerifyBadgeTone.ORGANIZATION -> "机构认证"
        }
    }
    val visibleText = if (compact) {
        when (tone) {
            OfficialVerifyBadgeTone.PERSONAL -> "个人"
            OfficialVerifyBadgeTone.ORGANIZATION -> "机构"
        }
    } else {
        fullText
    }
    return OfficialVerifyBadgeSpec(
        text = visibleText,
        contentDescription = fullText,
        tone = tone
    )
}

@Composable
fun OfficialVerifyBadge(
    badge: OfficialVerifyBadgeSpec,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val containerColor = when (badge.tone) {
        OfficialVerifyBadgeTone.PERSONAL -> Color(0xFFFFF3CD)
        OfficialVerifyBadgeTone.ORGANIZATION -> Color(0xFFDCEBFF)
    }
    val contentColor = when (badge.tone) {
        OfficialVerifyBadgeTone.PERSONAL -> Color(0xFF7A4B00)
        OfficialVerifyBadgeTone.ORGANIZATION -> Color(0xFF174EA6)
    }
    Surface(
        modifier = modifier.widthIn(max = if (compact) 48.dp else 120.dp),
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = badge.text,
            fontSize = if (compact) 9.sp else 10.sp,
            lineHeight = if (compact) 10.sp else 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(
                horizontal = if (compact) 4.dp else 6.dp,
                vertical = if (compact) 1.dp else 2.dp
            )
        )
    }
}
