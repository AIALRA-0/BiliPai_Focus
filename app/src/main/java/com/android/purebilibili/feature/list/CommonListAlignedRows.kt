package com.android.purebilibili.feature.list

import com.android.purebilibili.data.model.response.VideoItem

internal data class CommonListAlignedRow(val indices: IntRange, val fullWidth: Boolean, val key: String)

/** Full-width collections break the current row; incomplete rows keep their empty slots. */
internal fun resolveCommonListAlignedRows(
    items: List<VideoItem>,
    renderKeys: List<String>,
    columns: Int,
): List<CommonListAlignedRow> = buildList {
    val count = columns.coerceAtLeast(1)
    var start = 0
    while (start < items.size) {
        if (items[start].isCollectionResource) {
            add(CommonListAlignedRow(start..start, true, renderKeys[start]))
            start++
        } else {
            var end = start + 1
            while (end < items.size && end - start < count && !items[end].isCollectionResource) end++
            add(CommonListAlignedRow(start until end, false, "common_video_row_${renderKeys[start]}"))
            start = end
        }
    }
}
