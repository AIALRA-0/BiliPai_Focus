package com.android.purebilibili.feature.live.components

import androidx.compose.runtime.Composable
import com.android.purebilibili.feature.share.ContentMessageShareSheet
import com.android.purebilibili.feature.share.buildLiveSharePayload

@Composable
internal fun LiveShareToMessageSheet(
    roomId: Long,
    roomTitle: String,
    onDismiss: () -> Unit,
    onResult: (Boolean, String) -> Unit,
    cover: String = "",
    author: String = "",
    authorId: Long = 0,
) {
    ContentMessageShareSheet(buildLiveSharePayload(roomId, roomTitle, cover, author, authorId), onDismiss) {
        onResult(true, "已分享给 $it 人")
    }
}
