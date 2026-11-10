package com.android.purebilibili.feature.home.components

internal data class Md3BottomBarFit(
    val showSelectedLabel: Boolean,
    val horizontalPaddingDp: Float,
    val spacingDp: Float,
)

/** 仅用于 MD3 悬浮导航；不足时先收留白和选中文字，保留 48dp 按钮触控范围。 */
internal fun resolveMd3BottomBarFit(
    availableWidthDp: Float,
    itemCount: Int,
    selectedLabelWidthDp: Float?,
): Md3BottomBarFit {
    val count = itemCount.coerceAtLeast(1)
    val buttonsWidth = count * 48f
    // 选中按钮为 24dp 图标 + 8dp 间距 + 32dp 内边距 + 文字，替换一个 48dp 图标按钮。
    val labelExtra = selectedLabelWidthDp?.let { it.coerceAtLeast(0f) + 16f } ?: 0f
    val hasLabel = selectedLabelWidthDp != null
    if (availableWidthDp >= buttonsWidth + labelExtra + 24f + (count - 1) * 8f) {
        return Md3BottomBarFit(hasLabel, 12f, 8f)
    }
    if (hasLabel && availableWidthDp >= buttonsWidth + labelExtra + 8f) {
        val spacing = ((availableWidthDp - buttonsWidth - labelExtra - 8f) /
            (count - 1).coerceAtLeast(1)).coerceIn(0f, 8f)
        return Md3BottomBarFit(true, 4f, spacing)
    }
    // 极窄分屏沿用容器的横向滚动，不通过缩小按钮隐藏入口或压缩触控范围。
    val padding = ((availableWidthDp - buttonsWidth) / 2f).coerceIn(0f, 4f)
    val spacing = ((availableWidthDp - buttonsWidth - padding * 2f) /
        (count - 1).coerceAtLeast(1)).coerceIn(0f, 8f)
    return Md3BottomBarFit(false, padding, spacing)
}
