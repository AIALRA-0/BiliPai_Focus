package com.android.purebilibili.core.ui.components

/** Unknown counts have no numeric text; geometry reserves the bounded format from the first frame. */
fun resolveAppCountedTabLabel(label: String, count: Int?, limit: Int = 99): String {
    require(limit > 0)
    if (count == null || count < 0) return label
    return "$label ${if (count > limit) "$limit+" else count}"
}

fun <T> appCountedTabOption(value: T, label: String, count: Int?, limit: Int = 99): AppSegmentOption<T> {
    require(limit > 0)
    return AppSegmentOption(
        value = value,
        label = resolveAppCountedTabLabel(label, count, limit),
        measurementLabel = "$label $limit+",
    )
}
