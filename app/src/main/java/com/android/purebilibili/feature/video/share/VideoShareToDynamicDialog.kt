package com.android.purebilibili.feature.video.share

import androidx.compose.runtime.Composable
import com.android.purebilibili.feature.share.ContentDynamicShareSheet

@Composable
internal fun VideoShareToDynamicDialog(payload: VideoSharePayload, onDismiss: () -> Unit) {
    ContentDynamicShareSheet(
        payload.asContentSharePayload(),
        onDismiss,
        presentationOverride = videoSharePresentation(isLandscapeVideoShare()),
    ) { VideoShareFeedbackEvents.prepared(payload.bvid) }
}
