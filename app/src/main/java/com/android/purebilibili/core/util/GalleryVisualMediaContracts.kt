package com.android.purebilibili.core.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts

/**
 * Opens an installed gallery/media provider first, matching PiliPlus' ImageSource.gallery flow.
 * AndroidX's visual-media contract remains the fallback for devices without a GET_CONTENT handler.
 */
class PickGalleryVisualMedia : ActivityResultContract<PickVisualMediaRequest, Uri?>() {
    private val fallback = ActivityResultContracts.PickVisualMedia()

    override fun createIntent(context: Context, input: PickVisualMediaRequest): Intent =
        galleryIntent(input, allowMultiple = false)
            .takeIf { it.resolveActivity(context.packageManager) != null }
            ?: fallback.createIntent(context, input)

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        intent
            ?.takeIf { resultCode == Activity.RESULT_OK }
            ?.let { result -> result.data ?: result.clipData?.getItemAt(0)?.uri }
}

class PickMultipleGalleryVisualMedia(
    private val maxItems: Int,
) : ActivityResultContract<PickVisualMediaRequest, List<Uri>>() {
    private val fallback = ActivityResultContracts.PickMultipleVisualMedia(maxItems)

    init {
        require(maxItems > 1) { "Max items must be higher than 1" }
    }

    override fun createIntent(context: Context, input: PickVisualMediaRequest): Intent =
        galleryIntent(input, allowMultiple = true)
            .takeIf { it.resolveActivity(context.packageManager) != null }
            ?: fallback.createIntent(context, input)

    override fun parseResult(resultCode: Int, intent: Intent?): List<Uri> {
        if (resultCode != Activity.RESULT_OK || intent == null) return emptyList()

        val selectedUris = linkedSetOf<Uri>()
        intent.data?.let(selectedUris::add)
        intent.clipData?.let { clipData ->
            repeat(clipData.itemCount) { index ->
                selectedUris += clipData.getItemAt(index).uri
            }
        }
        return selectedUris.take(maxItems)
    }
}

private fun galleryIntent(
    input: PickVisualMediaRequest,
    allowMultiple: Boolean,
): Intent = Intent(Intent.ACTION_GET_CONTENT).apply {
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
    type = visualMediaMimeType(input.mediaType)
    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple)
    if (type == null) {
        type = "*/*"
        putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
    }
}

private fun visualMediaMimeType(
    mediaType: ActivityResultContracts.PickVisualMedia.VisualMediaType,
): String? = when (mediaType) {
    ActivityResultContracts.PickVisualMedia.ImageOnly -> "image/*"
    ActivityResultContracts.PickVisualMedia.VideoOnly -> "video/*"
    ActivityResultContracts.PickVisualMedia.ImageAndVideo -> null
    is ActivityResultContracts.PickVisualMedia.SingleMimeType -> mediaType.mimeType
}
