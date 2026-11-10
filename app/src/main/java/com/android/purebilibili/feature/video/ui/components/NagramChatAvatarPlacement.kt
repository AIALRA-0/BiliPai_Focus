/** 评论头像位置约束。保持约束顺序：视口底部、分组顶部、单元格底部。 */
package com.android.purebilibili.feature.video.ui.components

internal const val NAGRAM_CHAT_AVATAR_SIZE_DP = 42

internal fun resolveNagramChatAvatarTop(
    groupTopPx: Int,
    cellBottomPx: Int,
    viewportBottomPx: Int,
    minimumSlotPx: Int,
    imageBottomOffsetPx: Int,
): Int {
    var y = cellBottomPx
    if (y > viewportBottomPx) {
        y = viewportBottomPx
    }
    if (y - minimumSlotPx < groupTopPx) {
        y = groupTopPx + minimumSlotPx
    }
    if (y > cellBottomPx) {
        y = cellBottomPx
    }
    return y - imageBottomOffsetPx
}
