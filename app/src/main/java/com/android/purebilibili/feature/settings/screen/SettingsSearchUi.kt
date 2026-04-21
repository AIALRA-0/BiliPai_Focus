package com.android.purebilibili.feature.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.purebilibili.R
import com.android.purebilibili.core.theme.LocalAndroidNativeVariant
import com.android.purebilibili.core.theme.LocalUiPreset
import com.android.purebilibili.core.ui.rememberAppSettingsIcon
import com.android.purebilibili.core.ui.components.resolveAdaptiveListComponentVisualSpec
import com.android.purebilibili.core.ui.components.IOSClickableItem
import com.android.purebilibili.core.ui.components.IOSDivider
import com.android.purebilibili.core.ui.components.IOSGroup
import com.android.purebilibili.core.ui.components.IOSSearchBar

@Composable
internal fun SettingsSearchBarSection(
    query: String,
    onQueryChange: (String) -> Unit
) {
    IOSSearchBar(
        query = query,
        onQueryChange = onQueryChange,
        placeholder = stringResource(R.string.settings_search_placeholder),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
    )
}

@Composable
internal fun SettingsSearchResultsSection(
    results: List<SettingsSearchResult>,
    onResultClick: (SettingsSearchResult) -> Unit
) {
    val uiPreset = LocalUiPreset.current
    val androidNativeVariant = LocalAndroidNativeVariant.current
    val visualSpec = resolveAdaptiveListComponentVisualSpec(
        uiPreset = uiPreset,
        androidNativeVariant = androidNativeVariant
    )
    SettingsCategoryHeader(stringResource(R.string.settings_search_results_title))
    IOSGroup {
        if (results.isEmpty()) {
            IOSClickableItem(
                icon = rememberAppSettingsIcon(),
                title = stringResource(R.string.settings_search_empty_title),
                subtitle = stringResource(R.string.settings_search_empty_subtitle),
                onClick = null,
                showChevron = false,
                iconTint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            results.forEachIndexed { index, result ->
                val visual = rememberSettingsEntryVisual(result.target, uiPreset)
                IOSClickableItem(
                    icon = visual.icon,
                    iconPainter = visual.iconResId?.let { painterResource(id = it) },
                    title = result.title,
                    subtitle = result.subtitle,
                    value = result.section,
                    onClick = { onResultClick(result) },
                    iconTint = visual.iconTint
                )
                if (index != results.lastIndex) {
                    IOSDivider(startIndent = visualSpec.dividerStartIndentDp.dp)
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}
