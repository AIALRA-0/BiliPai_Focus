package com.android.purebilibili.feature.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.purebilibili.R
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.ui.AdaptiveSplitLayout
import com.android.purebilibili.core.ui.rememberAppBackIcon
import com.android.purebilibili.core.ui.rememberAppCollectionIcon
import com.android.purebilibili.core.ui.rememberAppInfoIcon
import com.android.purebilibili.core.ui.rememberAppLockIcon
import com.android.purebilibili.core.ui.rememberAppSettingsIcon
import dev.chrisbanes.haze.HazeState
import com.android.purebilibili.core.theme.iOSBlue
import com.android.purebilibili.core.theme.iOSGreen
import com.android.purebilibili.core.theme.iOSOrange
import com.android.purebilibili.core.theme.iOSPink
import com.android.purebilibili.core.theme.iOSPurple
import com.android.purebilibili.core.theme.iOSTeal
import kotlinx.coroutines.launch

enum class SettingsCategory(
    val titleResId: Int,
    val color: Color
) {
    GENERAL(R.string.settings_section_general, iOSPink),
    PRIVACY(R.string.settings_section_privacy, iOSPurple),
    STORAGE(R.string.settings_section_storage, iOSBlue),
    DEVELOPER(R.string.settings_section_developer, iOSTeal),
    ABOUT(R.string.settings_section_about, iOSOrange)
}

@Composable
fun TabletSettingsLayout(
    // Callbacks
    onBack: () -> Unit,
    onAppearanceClick: () -> Unit,
    onPlaybackClick: () -> Unit,
    onPermissionClick: () -> Unit,
    onPluginsClick: () -> Unit,
    onExportLogsClick: () -> Unit,
    onLicenseClick: () -> Unit,
    onDisclaimerClick: () -> Unit,
    onGithubClick: () -> Unit,
    onVerificationClick: () -> Unit,
    onBuildSourceClick: () -> Unit,
    onBuildFingerprintClick: () -> Unit,
    onCheckUpdateClick: () -> Unit,
    onViewReleaseNotesClick: () -> Unit,
    onVersionClick: () -> Unit,
    onReplayOnboardingClick: () -> Unit,
    onTelegramClick: () -> Unit,
    onTwitterClick: () -> Unit,
    onSettingsShareClick: () -> Unit,
    onWebDavBackupClick: () -> Unit,
    onDownloadPathClick: () -> Unit,
    onClearCacheClick: () -> Unit,
    onDonateClick: () -> Unit,
    onTipsClick: () -> Unit, // [Feature]
    onOpenLinksClick: () -> Unit, // [New]
    onBlockedListClick: () -> Unit, // [New]
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchResults: List<SettingsSearchResult>,
    onSearchResultClick: (SettingsSearchResult) -> Unit,
    
    // Logic Callbacks
    onPrivacyModeChange: (Boolean) -> Unit,
    onCrashTrackingChange: (Boolean) -> Unit,
    onAnalyticsChange: (Boolean) -> Unit,
    onEasterEggChange: (Boolean) -> Unit,
    onAutoCheckUpdateChange: (Boolean) -> Unit,
    
    // State
    privacyModeEnabled: Boolean,
    customDownloadPath: String?,
    cacheSize: String,
    crashTrackingEnabled: Boolean,
    analyticsEnabled: Boolean,
    pluginCount: Int,
    versionName: String,
    versionClickCount: Int,
    versionClickThreshold: Int,
    easterEggEnabled: Boolean,
    updateStatusText: String,
    isCheckingUpdate: Boolean,
    autoCheckUpdateEnabled: Boolean,
    verificationLabel: String,
    verificationSubtitle: String,
    buildSourceValue: String,
    buildSourceSubtitle: String,
    buildFingerprintValue: String,
    buildFingerprintCopyValue: String,
    buildFingerprintSubtitle: String,
    feedApiType: SettingsManager.FeedApiType,
    onFeedApiTypeChange: (SettingsManager.FeedApiType) -> Unit,
    incrementalTimelineRefreshEnabled: Boolean,
    onIncrementalTimelineRefreshChange: (Boolean) -> Unit,
    homeRefreshCount: Int,
    onHomeRefreshCountChange: (Int) -> Unit,
    
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(SettingsCategory.GENERAL) }
    val coroutineScope = rememberCoroutineScope()
    var pendingLanguageRestart by remember { mutableStateOf<AppLanguage?>(null) }
    val uiPreset = com.android.purebilibili.core.theme.LocalUiPreset.current
    val configuration = LocalConfiguration.current
    val restartDialogTitle = stringResource(R.string.app_language_restart_dialog_title)
    val restartDialogMessage = stringResource(R.string.app_language_restart_dialog_message)
    val restartDialogConfirm = stringResource(R.string.app_language_restart_dialog_confirm)
    val layoutPolicy = remember(configuration.screenWidthDp) {
        resolveSettingsTabletLayoutPolicy(
            widthDp = configuration.screenWidthDp
        )
    }
    
    // Internal navigation state for the right pane
    var activeDetail by remember { mutableStateOf<SettingsDetail?>(null) }
    
    // State from ViewModel (Need to access SettingsViewModel or pass state?)
    // The original TabletSettingsLayout receives primitive types. 
    // But the new *Content composables require ViewModel or State.
    // Ideally we should pass ViewModel to TabletSettingsLayout or hoist EVERYTHING.
    // Given the props list is long, passing ViewModel might be cleaner but let's see.
    // ThemeSettingsContent needs viewModel. AppearanceSettingsContent needs viewModel.
    // I should add viewModel parameter to TabletSettingsLayout.
    val viewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val context = androidx.compose.ui.platform.LocalContext.current
    val state by viewModel.state.collectAsState()
    val generalIcon = rememberAppSettingsIcon()
    val privacyIcon = rememberAppLockIcon()
    val storageIcon = rememberAppCollectionIcon()
    val developerIcon = rememberSettingsEntryVisual(SettingsSearchTarget.PLUGINS, uiPreset).icon
    val aboutIcon = rememberAppInfoIcon()

    AdaptiveSplitLayout(
        modifier = modifier,
        primaryRatio = layoutPolicy.primaryRatio,
        primaryContent = {
            // Master List
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(layoutPolicy.masterPanePaddingDp.dp)
            ) {
                // Back Button Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(bottom = 16.dp, start = 4.dp)
                        .clickable(onClick = onBack)
                        .padding(4.dp)
                ) {
                    Icon(
                        rememberAppBackIcon(),
                        contentDescription = "返回", 
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "返回首页", 
                        style = MaterialTheme.typography.bodyLarge, 
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "设置",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp, start = 8.dp)
                )
                SettingsSearchBarSection(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange
                )

                SettingsCategory.entries.forEach { category ->
                    val isSelected = category == selectedCategory
                    val categoryIcon = when (category) {
                        SettingsCategory.GENERAL -> generalIcon
                        SettingsCategory.PRIVACY -> privacyIcon
                        SettingsCategory.STORAGE -> storageIcon
                        SettingsCategory.DEVELOPER -> developerIcon ?: generalIcon
                        SettingsCategory.ABOUT -> aboutIcon
                    }
                    NavigationDrawerItem(
                        label = { Text(stringResource(category.titleResId)) },
                        selected = isSelected,
                        onClick = { 
                            selectedCategory = category 
                            activeDetail = null // Reset detail when category changes
                        },
                        icon = { 
                            Icon(
                                categoryIcon,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else category.color
                            ) 
                        },
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .testTag("settings_category_${category.name}")
                            .focusable(),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            unselectedContainerColor = Color.Transparent,
                            selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }
            }
        },
        secondaryContent = {
            // Detail Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(layoutPolicy.detailPanePaddingDp.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .testTag("settings_detail_panel")
                ) {
                // If we have an active detail, show it. Otherwise show Category Root.
                val detail = activeDetail
                if (searchQuery.isNotBlank()) {
                    Column(modifier = Modifier.widthIn(max = layoutPolicy.detailMaxWidthDp.dp)) {
                        SettingsSearchResultsSection(
                            results = searchResults,
                            onResultClick = { target ->
                                activeDetail = null
                                onSearchResultClick(target)
                            }
                        )
                    }
                } else if (detail != null) {
                    // Sub-page Content
                    Column(modifier = Modifier.widthIn(max = layoutPolicy.detailMaxWidthDp.dp)) {
                        // Header with Back Button
                        Row(
                            verticalAlignment = Alignment.CenterVertically, 
                            modifier = Modifier
                                .padding(bottom = 16.dp)
                                .clickable { 
                                    // if in Appearance Sub-pages, go back to Appearance? 
                                    // Or just simple stack: Root -> Appearance -> Theme
                                    // Let's implement simple logic: if in Theme/Icon/Anim, go back to Appearance.
                                    // if in Appearance, go back to Null.
                                    if (detail == SettingsDetail.ICONS || detail == SettingsDetail.ANIMATION) {
                                        activeDetail = SettingsDetail.APPEARANCE
                                    } else {
                                        activeDetail = null
                                    }
                                }
                                .padding(8.dp)
                        ) {
                            Icon(rememberAppBackIcon(), null, tint = MaterialTheme.colorScheme.primary)
                            Text("返回", color = MaterialTheme.colorScheme.primary)
                        }
                        
                        when (detail) {
                            SettingsDetail.APPEARANCE -> AppearanceSettingsContent(
                                state = state,
                                viewModel = viewModel,
                                context = context,
                                onNavigateToBottomBarSettings = { activeDetail = SettingsDetail.BOTTOM_BAR },
                                onNavigateToIconSettings = { activeDetail = SettingsDetail.ICONS },
                                onNavigateToAnimationSettings = { activeDetail = SettingsDetail.ANIMATION },
                                onAppLanguageChange = { language ->
                                    if (shouldPromptAppRestartForLanguageChange(state.appLanguage, language)) {
                                        pendingLanguageRestart = language
                                    }
                                }
                            )
                            SettingsDetail.ICONS -> {
                                // Need to recreate the data here or reuse helper?
                                // IconSettingsContent needs `iconGroups`. 
                                // I need to reconstruct them here or move them to a shared place.
                                // For now, I will duplicate or create a helper if possible.
                                // Since I can't easily move them to a separate file without another tool call, 
                                // and I want to proceed, I will redefine them here briefly or just pass empty if I can't access.
                                // Wait, I defined them inside `IconSettingsScreen` file but at top level.
                                // check imports.
                                IconSettingsContent(
                                    state = state,
                                    viewModel = viewModel,
                                    context = context,
                                    iconGroups = com.android.purebilibili.feature.settings.getIconGroups() // Need a way to get this
                                )
                            }
                            SettingsDetail.ANIMATION -> AnimationSettingsContent(
                                state = state,
                                viewModel = viewModel
                            )
                            SettingsDetail.PLAYBACK -> PlaybackSettingsContent(
                                state = state,
                                viewModel = viewModel
                            )
                            SettingsDetail.BOTTOM_BAR -> BottomBarSettingsContent(
                                modifier = Modifier
                            )
                            SettingsDetail.PERMISSION -> PermissionSettingsContent(
                                modifier = Modifier
                            )
                            SettingsDetail.BLOCKED_LIST -> {
                                // [New] Blocked List Content for Tablet
                                val repository = remember { com.android.purebilibili.data.repository.BlockedUpRepository(context) }
                                val blockedUps by repository.getAllBlockedUps().collectAsState(initial = emptyList())
                                // Pass scope for unblocking
                                val scope = rememberCoroutineScope()
                                BlockedListContent(
                                    blockedUps = blockedUps,
                                    onUnblock = { mid ->
                                        scope.launch { repository.unblockUp(mid) }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            SettingsDetail.PLUGINS -> {
                                // Need to manage editing state locally for the tablet view
                                var editingPlugin by remember { mutableStateOf<com.android.purebilibili.core.plugin.json.JsonRulePlugin?>(null) }
                                
                                val plugins by com.android.purebilibili.core.plugin.PluginManager.pluginsFlow.collectAsState()
                                val jsonPlugins by com.android.purebilibili.core.plugin.json.JsonPluginManager.plugins.collectAsState()
                                
                                if (editingPlugin != null) {
                                    // Show Editor
                                    // We need to manage state for the editor
                                    val plugin = editingPlugin!!
                                    var name by remember(plugin) { mutableStateOf(plugin.name) }
                                    var description by remember(plugin) { mutableStateOf(plugin.description) }
                                    var rules by remember(plugin) { mutableStateOf(plugin.rules) }
                                    
                                    Column {
                                        // Custom Header for Editor
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically, 
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically, 
                                                modifier = Modifier.clickable { editingPlugin = null }.padding(8.dp)
                                            ) {
                                                Icon(rememberAppBackIcon(), null, tint = MaterialTheme.colorScheme.primary)
                                                Text("返回插件列表", color = MaterialTheme.colorScheme.primary)
                                            }
                                            
                                            // Save Button
                                            IconButton(onClick = {
                                                val updated = plugin.copy(
                                                    name = name,
                                                    description = description,
                                                    rules = rules
                                                )
                                                com.android.purebilibili.core.plugin.json.JsonPluginManager.updatePlugin(updated)
                                                editingPlugin = null
                                            }) {
                                                Icon(Icons.Filled.CheckCircle, contentDescription = "保存", tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                        
                                        JsonPluginEditorContent(
                                            modifier = Modifier.fillMaxSize(),
                                            name = name,
                                            onNameChange = { newName: String -> name = newName },
                                            description = description,
                                            onDescriptionChange = { newDesc: String -> description = newDesc },
                                            rules = rules,
                                            onRulesChange = { newRules: List<com.android.purebilibili.core.plugin.json.Rule> -> rules = newRules },
                                            pluginType = plugin.type
                                        )
                                    }
                                } else {
                                    // Show List
                                    PluginsContent(
                                        modifier = Modifier,
                                        plugins = plugins,
                                        jsonPlugins = jsonPlugins,
                                        onEditJsonPlugin = { editingPlugin = it }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Category Root
                    AnimatedContent(
                        targetState = selectedCategory,
                        transitionSpec = {
                            (slideInVertically { height -> height } + fadeIn()).togetherWith(
                                slideOutVertically { height -> -height } + fadeOut())
                        },
                        label = "SettingsDetailTransition"
                    ) { category ->
                        Column(modifier = Modifier.widthIn(max = layoutPolicy.rootPanelMaxWidthDp.dp)) {
                            Text(
                                text = stringResource(category.titleResId),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 24.dp, start = 16.dp)
                            )
                            
                            when (category) {
                                SettingsCategory.GENERAL -> {
                                    GeneralSection(
                                        onAppearanceClick = { activeDetail = SettingsDetail.APPEARANCE },
                                        onPlaybackClick = { activeDetail = SettingsDetail.PLAYBACK },
                                        onBottomBarClick = { activeDetail = SettingsDetail.BOTTOM_BAR }
                                    )
                                }
                                SettingsCategory.PRIVACY -> PrivacySection(
                                    privacyModeEnabled = privacyModeEnabled,
                                    onPrivacyModeChange = onPrivacyModeChange,
                                    onPermissionClick = { activeDetail = SettingsDetail.PERMISSION },
                                    onBlockedListClick = { activeDetail = SettingsDetail.BLOCKED_LIST } // [New]
                                )
                                SettingsCategory.STORAGE -> {
                                    FeedApiSection(
                                        feedApiType = feedApiType,
                                        onFeedApiTypeChange = onFeedApiTypeChange,
                                        incrementalTimelineRefreshEnabled = incrementalTimelineRefreshEnabled,
                                        onIncrementalTimelineRefreshChange = onIncrementalTimelineRefreshChange,
                                        homeRefreshCount = homeRefreshCount,
                                        onHomeRefreshCountChange = onHomeRefreshCountChange
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    DataStorageSection(
                                        customDownloadPath = customDownloadPath,
                                        cacheSize = cacheSize,
                                        onSettingsShareClick = onSettingsShareClick,
                                        onWebDavBackupClick = onWebDavBackupClick,
                                        onDownloadPathClick = onDownloadPathClick,
                                        onClearCacheClick = onClearCacheClick
                                    )
                                }
                                SettingsCategory.DEVELOPER -> DeveloperSection(
                                    crashTrackingEnabled = crashTrackingEnabled,
                                    analyticsEnabled = analyticsEnabled,
                                    pluginCount = pluginCount,
                                    onCrashTrackingChange = onCrashTrackingChange,
                                    onAnalyticsChange = onAnalyticsChange,
                                    onPluginsClick = { activeDetail = SettingsDetail.PLUGINS },
                                    onExportLogsClick = onExportLogsClick
                                )
                                SettingsCategory.ABOUT -> {
                                    ReleaseChannelPinnedCard(
                                        onGithubClick = onGithubClick,
                                        onTelegramClick = onTelegramClick,
                                        onDisclaimerClick = onDisclaimerClick
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    FollowAuthorSection(
                                        onTelegramClick = onTelegramClick,
                                        onTwitterClick = onTwitterClick,
                                        onDonateClick = onDonateClick
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    AboutSection(
                                        versionName = versionName,
                                        easterEggEnabled = easterEggEnabled,
                                        onDisclaimerClick = onDisclaimerClick,
                                        onLicenseClick = onLicenseClick,
                                        onGithubClick = onGithubClick,
                                        onVerificationClick = onVerificationClick,
                                        onBuildSourceClick = onBuildSourceClick,
                                        onBuildFingerprintClick = onBuildFingerprintClick,
                                        onCheckUpdateClick = onCheckUpdateClick,
                                        onViewReleaseNotesClick = onViewReleaseNotesClick,
                                        autoCheckUpdateEnabled = autoCheckUpdateEnabled,
                                        onAutoCheckUpdateChange = onAutoCheckUpdateChange,
                                        onVersionClick = onVersionClick,
                                        onReplayOnboardingClick = onReplayOnboardingClick,
                                        onEasterEggChange = onEasterEggChange,
                                        updateStatusText = updateStatusText,
                                        isCheckingUpdate = isCheckingUpdate,
                                        verificationLabel = verificationLabel,
                                        verificationSubtitle = verificationSubtitle,
                                        buildSourceValue = buildSourceValue,
                                        buildSourceSubtitle = buildSourceSubtitle,
                                        buildFingerprintValue = buildFingerprintValue,
                                        buildFingerprintCopyValue = buildFingerprintCopyValue,
                                        buildFingerprintSubtitle = buildFingerprintSubtitle,
                                        versionClickCount = versionClickCount,
                                        versionClickThreshold = versionClickThreshold
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    SupportToolsSection(
                                        onTipsClick = onTipsClick,
                                        onOpenLinksClick = onOpenLinksClick
                                    )
                }
            }
        }
    }

    pendingLanguageRestart?.let { pendingLanguage ->
        AlertDialog(
            onDismissRequest = { pendingLanguageRestart = null },
            title = { Text(restartDialogTitle) },
            text = { Text(restartDialogMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingLanguageRestart = null
                        coroutineScope.launch {
                            persistAndApplyAppLanguageBeforeRestart(
                                appLanguage = pendingLanguage,
                                persist = { SettingsManager.setAppLanguage(context, it) },
                                restart = { restartApp(context) }
                            )
                        }
                    }
                ) {
                    Text(restartDialogConfirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingLanguageRestart = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}
                }
            }
        }
    )
}

enum class SettingsDetail {
    APPEARANCE, ICONS, ANIMATION, PLAYBACK, BOTTOM_BAR, PERMISSION, PLUGINS, BLOCKED_LIST // [New]
}
