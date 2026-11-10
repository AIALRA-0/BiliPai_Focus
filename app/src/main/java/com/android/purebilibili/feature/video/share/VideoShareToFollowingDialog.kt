package com.android.purebilibili.feature.video.share

import androidx.compose.runtime.Composable
import com.android.purebilibili.feature.share.ContentMessageShareSheet

@Composable
internal fun VideoShareToFollowingDialog(payload: VideoSharePayload, onDismiss: () -> Unit, onSuccess: (Int) -> Unit) {
    ContentMessageShareSheet(
        payload.asContentSharePayload(),
        onDismiss,
        presentationOverride = videoSharePresentation(isLandscapeVideoShare()),
        onSuccess = onSuccess,
    )
}
