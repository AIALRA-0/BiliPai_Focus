package com.android.purebilibili.feature.settings.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.purebilibili.feature.settings.SettingsSearchHistorySection
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.purebilibili.R
import com.android.purebilibili.core.ui.LocalBottomBarVisible
import com.android.purebilibili.core.ui.LocalAnimatedVisibilityScope
import com.android.purebilibili.core.ui.animation.EntranceGroup
import com.android.purebilibili.core.util.LocalWindowSizeClass
import com.android.purebilibili.feature.settings.SettingsPageScrollHost
import com.android.purebilibili.feature.settings.SettingsRootCategory
import com.android.purebilibili.feature.settings.SettingsRootCategoryEntranceSection
import com.android.purebilibili.feature.settings.SettingsSearchBarSection
import com.android.purebilibili.feature.settings.SettingsSearchFocusController
import com.android.purebilibili.feature.settings.SettingsSearchResult
import com.android.purebilibili.feature.settings.SettingsSearchResultsSection
import com.android.purebilibili.feature.settings.SettingsViewModel
import com.android.purebilibili.feature.settings.isSceneSettingsSearchTarget
import com.android.purebilibili.feature.settings.resolveSettingsContentBottomPadding
import com.android.purebilibili.feature.settings.resolveSettingsRootCategoryForSearchTarget
import com.android.purebilibili.feature.settings.resolveSettingsSearchResults
import com.android.purebilibili.feature.settings.shouldStartSettingsEntrance
import com.android.purebilibili.feature.settings.ui.SettingsPageScaffold
import dev.chrisbanes.haze.HazeState

@Composable
fun SettingsSearchScreen(
    viewModel: SettingsViewModel = viewModel(),
    onBack: () -> Unit,
    onSearchResultClick: (SettingsSearchResult) -> Unit,
    onCategoryClick: (SettingsRootCategory) -> Unit = {},
    mainHazeState: HazeState? = null,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle(initialValue = emptyList())
    val searchResults = remember(searchQuery) {
        resolveSettingsSearchResults(query = searchQuery, maxResults = 20)
    }
    val windowSizeClass = LocalWindowSizeClass.current
    val bottomBarVisible = LocalBottomBarVisible.current
    val navigationTransitionRunning =
        LocalAnimatedVisibilityScope.current?.transition?.isRunning == true
    val rootEntranceStartWhen = shouldStartSettingsEntrance(
        entranceEnabled = true,
        navigationTransitionRunning = navigationTransitionRunning,
    )
    val bottomInset = resolveSettingsContentBottomPadding(
        navigationBarsBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        bottomBarVisible = bottomBarVisible,
        isBottomBarFloating = false,
        bottomBarLabelMode = 0,
        isTablet = windowSizeClass.isTablet,
    )

    SettingsPageScaffold(
        title = stringResource(R.string.settings_search_results_title),
        onBack = onBack,
        backContentDescription = stringResource(R.string.common_back),
        bottomContentPadding = bottomInset,
        scrollHost = SettingsPageScrollHost.External,
        header = {
            SettingsSearchBarSection(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = { viewModel.recordSearchQuery(searchQuery) },
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            EntranceGroup(startWhen = rootEntranceStartWhen) {
                SettingsRootCategoryEntranceSection {
                    if (searchQuery.isBlank()) {
                        SettingsSearchHistorySection(
                            history = searchHistory,
                            onQueryClick = {
                                searchQuery = it
                                viewModel.recordSearchQuery(it)
                            },
                            onDelete = viewModel::deleteSearchHistory,
                            onClear = viewModel::clearSearchHistory,
                        )
                    } else SettingsSearchResultsSection(
                        results = searchResults,
                        onResultClick = { result ->
                            viewModel.recordSearchQuery(searchQuery)
                            val category = resolveSettingsRootCategoryForSearchTarget(result.target)
                            if (isSceneSettingsSearchTarget(result.target) && category != null) {
                                onCategoryClick(category)
                            } else {
                                SettingsSearchFocusController.submit(result.target, result.focusId)
                                onSearchResultClick(result)
                            }
                        },
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
