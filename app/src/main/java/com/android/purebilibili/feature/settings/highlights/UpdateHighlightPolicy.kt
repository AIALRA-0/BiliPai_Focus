package com.android.purebilibili.feature.settings.highlights

import com.android.purebilibili.core.store.AppThemeSettings
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.feature.settings.Md3ColorSource
import com.materialkolor.PaletteStyle

internal enum class UpdateHighlightAction { PREVIEW_CMF }

internal data class UpdateHighlight(
    val id: String,
    val introducedVersionCode: Int,
    val title: String,
    val description: String,
    val action: UpdateHighlightAction? = null,
)

/** Append release highlights here. IDs are permanent; fixes belong in the changelog. */
internal val updateHighlightCatalog = listOf(
    UpdateHighlight(
        id = "cmf_spec_2026",
        introducedVersionCode = 487,
        title = "试试新的 CMF 配色",
        description = "新增 SPEC_2026，让主题色、容器和背景有新的搭配。先看看深浅色效果，再决定是否使用。",
        action = UpdateHighlightAction.PREVIEW_CMF,
    ),
)

internal data class UpdateHighlightHistory(
    val lastVersionCode: Int = 0,
    val pendingIds: Set<String> = emptySet(),
    val handledIds: Set<String> = emptySet(),
)

/** A first install establishes a baseline. Legacy upgraded installs can receive this first release.
 * A downgrade never lowers the high-water mark or erases previous decisions. */
internal fun prepareUpdateHighlights(
    history: UpdateHighlightHistory,
    currentVersionCode: Int,
    isExistingInstall: Boolean,
    catalog: List<UpdateHighlight> = updateHighlightCatalog,
): UpdateHighlightHistory {
    if (currentVersionCode <= history.lastVersionCode) return history
    val previous = history.lastVersionCode
    val candidates = catalog.filter { it.introducedVersionCode <= currentVersionCode &&
        (previous == 0 || it.introducedVersionCode > previous) }.map { it.id }.toSet()
    if (previous == 0 && !isExistingInstall) return UpdateHighlightHistory(
        lastVersionCode = currentVersionCode, handledIds = history.handledIds + candidates,
    )
    return history.copy(
        lastVersionCode = currentVersionCode,
        pendingIds = (history.pendingIds + candidates) - history.handledIds,
    )
}

internal fun latestReleaseHighlights(versionCode: Int, catalog: List<UpdateHighlight> = updateHighlightCatalog): List<UpdateHighlight> {
    val available = catalog.filter { it.introducedVersionCode <= versionCode }
    val latest = available.maxOfOrNull { it.introducedVersionCode } ?: return emptyList()
    return available.filter { it.introducedVersionCode == latest }.take(3)
}

internal fun canPreviewUpdateHighlight(highlight: UpdateHighlight, settings: AppThemeSettings): Boolean = when (highlight.action) {
    UpdateHighlightAction.PREVIEW_CMF -> settings.uiStyle == AppUiStyle.MATERIAL3 && settings.md3ColorSource == Md3ColorSource.CUSTOM
    null -> true
}

internal fun isUpdateHighlightAlreadyEnabled(highlight: UpdateHighlight, settings: AppThemeSettings): Boolean = when (highlight.action) {
    UpdateHighlightAction.PREVIEW_CMF -> canPreviewUpdateHighlight(highlight, settings) &&
        !settings.seedPaletteStyle.enabled && settings.colorStyle is PaletteStyle.Cmf
    null -> false
}
