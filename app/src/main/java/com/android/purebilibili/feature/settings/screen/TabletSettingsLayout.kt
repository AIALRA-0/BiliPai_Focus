package com.android.purebilibili.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.globalWallpaperAwareBackground
import com.android.purebilibili.core.ui.rememberAppBackIcon
import com.android.purebilibili.core.ui.rememberAppSettingsIcon
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun TabletSettingsLayout(
    // Callbacks
    onBack: () -> Unit,
    onAppearanceClick: () -> Unit,
    onAnimationClick: () -> Unit,
    onPlaybackClick: () -> Unit,
    onFocusSettingsClick: () -> Unit,
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
    onImageSavePathClick: () -> Unit,
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
    onPrivacyContentAuthenticationChange: (Boolean) -> Unit,
    onCrashTrackingChange: (Boolean) -> Unit,
    onAnalyticsChange: (Boolean) -> Unit,
    onEasterEggChange: (Boolean) -> Unit,
    onAutoCheckUpdateChange: (Boolean) -> Unit,
    
    // State
    privacyModeEnabled: Boolean,
    privacyContentAuthenticationEnabled: Boolean,
    customDownloadPath: String?,
    customImageSavePath: String?,
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
    dynamicImagePreviewTextVisible: Boolean,
    onDynamicImagePreviewTextVisibleChange: (Boolean) -> Unit,
    dynamicVisibleTabIds: Set<String>,
    onDynamicTabVisibilityChange: (String) -> Unit,
    homeRefreshCount: Int,
    onHomeRefreshCountChange: (Int) -> Unit,
    
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(SettingsRootCategory.INTERFACE_THEME) }
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
    val state by viewModel.state.collectAsStateWithLifecycle()
    val fallbackCategoryIcon = rememberAppSettingsIcon()
    val categoryOrder = remember { resolveTabletSettingsRootCategoryOrder() }
    val rootCategoryActions = SettingsRootCategoryActions(
        onAppearanceClick = { activeDetail = SettingsDetail.APPEARANCE },
        onAnimationClick = { activeDetail = SettingsDetail.ANIMATION },
        onPlaybackClick = { activeDetail = SettingsDetail.PLAYBACK },
        onFocusSettingsClick = onFocusSettingsClick,
        onBottomBarClick = { activeDetail = SettingsDetail.BOTTOM_BAR },
        onPermissionClick = { activeDetail = SettingsDetail.PERMISSION },
        onBlockedListClick = { activeDetail = SettingsDetail.BLOCKED_LIST },
        onPluginsClick = { activeDetail = SettingsDetail.PLUGINS },
        onExportLogsClick = onExportLogsClick,
        onSettingsShareClick = onSettingsShareClick,
        onWebDavBackupClick = onWebDavBackupClick,
        onDownloadPathClick = onDownloadPathClick,
        onImageSavePathClick = onImageSavePathClick,
        onClearCacheClick = onClearCacheClick,
        onGithubClick = onGithubClick,
        onTelegramClick = onTelegramClick,
        onTwitterClick = onTwitterClick,
        onDonateClick = onDonateClick,
        onDisclaimerClick = onDisclaimerClick,
        onLicenseClick = onLicenseClick,
        onVerificationClick = onVerificationClick,
        onBuildSourceClick = onBuildSourceClick,
        onBuildFingerprintClick = onBuildFingerprintClick,
        onCheckUpdateClick = onCheckUpdateClick,
        onViewReleaseNotesClick = onViewReleaseNotesClick,
        onVersionClick = onVersionClick,
        onReplayOnboardingClick = onReplayOnboardingClick,
        onTipsClick = onTipsClick,
        onOpenLinksClick = onOpenLinksClick,
        onPrivacyModeChange = onPrivacyModeChange,
        onPrivacyContentAuthenticationChange = onPrivacyContentAuthenticationChange,
        onCrashTrackingChange = onCrashTrackingChange,
        onAnalyticsChange = onAnalyticsChange,
        onEasterEggChange = onEasterEggChange,
        onAutoCheckUpdateChange = onAutoCheckUpdateChange,
        onFeedApiTypeChange = onFeedApiTypeChange,
        onIncrementalTimelineRefreshChange = onIncrementalTimelineRefreshChange,
        onDynamicImagePreviewTextVisibleChange = onDynamicImagePreviewTextVisibleChange,
        onDynamicTabVisibilityChange = onDynamicTabVisibilityChange,
        onHomeRefreshCountChange = onHomeRefreshCountChange
    )
    val rootCategoryState = SettingsRootCategoryState(
        privacyModeEnabled = privacyModeEnabled,
        privacyContentAuthenticationEnabled = privacyContentAuthenticationEnabled,
        crashTrackingEnabled = crashTrackingEnabled,
        analyticsEnabled = analyticsEnabled,
        pluginCount = pluginCount,
        customDownloadPath = customDownloadPath,
        customImageSavePath = customImageSavePath,
        cacheSize = cacheSize,
        versionName = versionName,
        easterEggEnabled = easterEggEnabled,
        updateStatusText = updateStatusText,
        isCheckingUpdate = isCheckingUpdate,
        autoCheckUpdateEnabled = autoCheckUpdateEnabled,
        verificationLabel = verificationLabel,
        verificationSubtitle = verificationSubtitle,
        buildSourceValue = buildSourceValue,
        buildSourceSubtitle = buildSourceSubtitle,
        buildFingerprintValue = buildFingerprintValue,
        buildFingerprintCopyValue = buildFingerprintCopyValue,
        buildFingerprintSubtitle = buildFingerprintSubtitle,
        versionClickCount = versionClickCount,
        versionClickThreshold = versionClickThreshold,
        feedApiType = feedApiType,
        incrementalTimelineRefreshEnabled = incrementalTimelineRefreshEnabled,
        dynamicImagePreviewTextVisible = dynamicImagePreviewTextVisible,
        dynamicVisibleTabIds = dynamicVisibleTabIds,
        homeRefreshCount = homeRefreshCount
    )

    AdaptiveSplitLayout(
        modifier = modifier,
        primaryRatio = layoutPolicy.primaryRatio,
        primaryContent = {
            // Master List
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppSurfaceTokens.cardContainer())
                    .padding(horizontal = layoutPolicy.masterPanePaddingDp.dp)
                    .statusBarsPadding()
            ) {
                // Back Button Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .clickable(onClick = onBack)
                        .padding(8.dp)
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

                categoryOrder.forEach { category ->
                    val isSelected = category == selectedCategory
                    val categoryVisual = rememberSettingsEntryVisual(category.searchTarget, uiPreset)
                    val categoryIcon = categoryVisual.icon ?: fallbackCategoryIcon
                    NavigationDrawerItem(
                        label = { Text(category.title) },
                        selected = isSelected,
                        onClick = { 
                            selectedCategory = category 
                            activeDetail = null // Reset detail when category changes
                        },
                        icon = { 
                            Icon(
                                categoryIcon,
                                contentDescription = null,
                                tint = if (isSelected) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    categoryVisual.iconTint
                                }
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
                    .globalWallpaperAwareBackground()
                    .padding(horizontal = layoutPolicy.detailPanePaddingDp.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .testTag("settings_detail_panel")
                ) {
                // If we have an active detail, show it. Otherwise show Category Root.
                val detail = activeDetail
                if (searchQuery.isNotBlank()) {
                    Column(modifier = Modifier
                        .widthIn(max = layoutPolicy.detailMaxWidthDp.dp)
                        .statusBarsPadding()
                    ) {
                        SettingsSearchResultsSection(
                            results = searchResults,
                            onResultClick = resultClick@{ target ->
                                resolveSettingsSceneDetailFocus(target.target)?.let { detailFocus ->
                                    resolveSettingsRootCategoryForSearchTarget(target.target)?.let { category ->
                                        selectedCategory = category
                                    }
                                    activeDetail = when (detailFocus.target) {
                                        SettingsSearchTarget.APPEARANCE -> SettingsDetail.APPEARANCE
                                        SettingsSearchTarget.ANIMATION -> SettingsDetail.ANIMATION
                                        SettingsSearchTarget.PLAYBACK -> SettingsDetail.PLAYBACK
                                        SettingsSearchTarget.BOTTOM_BAR -> SettingsDetail.BOTTOM_BAR
                                        else -> null
                                    }
                                    SettingsSearchFocusController.submit(detailFocus.target, detailFocus.focusId)
                                    onSearchQueryChange("")
                                    return@resultClick
                                }
                                if (isSceneSettingsSearchTarget(target.target)) {
                                    resolveSettingsRootCategoryForSearchTarget(target.target)?.let { category ->
                                        selectedCategory = category
                                        activeDetail = null
                                    }
                                }
                                activeDetail = null
                                onSearchResultClick(target)
                            }
                        )
                    }
                } else if (detail != null) {
                    // Sub-page Content
                    Column(modifier = Modifier
                        .widthIn(max = layoutPolicy.detailMaxWidthDp.dp)
                        .statusBarsPadding()
                    ) {
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
                                val fileService = remember { BlockedListFileService(context.applicationContext) }
                                val syncRepository = remember { com.android.purebilibili.data.repository.BilibiliBlockedListSyncRepository(repository) }
                                val blockedUps by repository.getAllBlockedUps().collectAsStateWithLifecycle(initialValue = emptyList())
                                val latestBlockedUps by rememberUpdatedState(blockedUps)
                                // Pass scope for unblocking
                                val scope = rememberCoroutineScope()
                                var syncingBlockedList by remember { mutableStateOf(false) }
                                var refreshingProfiles by remember { mutableStateOf(false) }
                                var blockedListSyncMessage by remember { mutableStateOf<String?>(null) }
                                val exportJsonLauncher = rememberLauncherForActivityResult(
                                    contract = ActivityResultContracts.CreateDocument("application/json")
                                ) { uri ->
                                    if (uri != null) {
                                        scope.launch {
                                            blockedListSyncMessage = "正在导出黑名单 JSON..."
                                            blockedListSyncMessage = fileService.exportJsonToUri(uri, latestBlockedUps).fold(
                                                onSuccess = { "已导出 ${latestBlockedUps.size} 个黑名单用户到 JSON 文件" },
                                                onFailure = { it.message ?: "导出黑名单 JSON 失败" }
                                            )
                                        }
                                    }
                                }
                                val importJsonLauncher = rememberLauncherForActivityResult(
                                    contract = ActivityResultContracts.OpenDocument()
                                ) { uri ->
                                    if (uri != null) {
                                        scope.launch {
                                            blockedListSyncMessage = "正在导入黑名单 JSON..."
                                            val text = fileService.readImportText(uri).getOrElse {
                                                blockedListSyncMessage = it.message ?: "读取黑名单 JSON 失败"
                                                return@launch
                                            }
                                            val items = com.android.purebilibili.data.repository.parseBlockedUpShareText(text)
                                            blockedListSyncMessage = repository.importBlockedUps(items).message
                                        }
                                    }
                                }
                                BlockedListContent(
                                    blockedUps = blockedUps,
                                    syncingBlockedList = syncingBlockedList,
                                    refreshingProfiles = refreshingProfiles,
                                    blockedListSyncMessage = blockedListSyncMessage,
                                    onSyncBlockedList = {
                                        if (!syncingBlockedList) {
                                            scope.launch {
                                                syncingBlockedList = true
                                                blockedListSyncMessage = "正在同步 B站黑名单..."
                                                val result = syncRepository.importFromBilibili()
                                                blockedListSyncMessage = result.fold(
                                                    onSuccess = { it.message },
                                                    onFailure = { it.message ?: "同步 B站黑名单失败" }
                                                )
                                                syncingBlockedList = false
                                            }
                                        }
                                    },
                                    onRefreshProfiles = {
                                        if (!refreshingProfiles) {
                                            scope.launch {
                                                refreshingProfiles = true
                                                blockedListSyncMessage = "正在刷新黑名单用户资料..."
                                                blockedListSyncMessage = repository.refreshBlockedUpProfiles().message
                                                refreshingProfiles = false
                                            }
                                        }
                                    },
                                    onShareBlockedList = {
                                        com.android.purebilibili.core.util.ShareUtils.shareText(
                                            context = context,
                                            subject = "BiliPai 黑名单",
                                            text = com.android.purebilibili.data.repository.buildBlockedUpShareText(blockedUps),
                                            chooserTitle = "分享黑名单"
                                        )
                                    },
                                    onExportBlockedListJson = {
                                        exportJsonLauncher.launch(buildBlockedListJsonFileName())
                                    },
                                    onImportBlockedListJsonRequest = {
                                        importJsonLauncher.launch(arrayOf("application/json", "text/plain", "text/*", "application/octet-stream"))
                                    },
                                    onImportBlockedList = { text ->
                                        scope.launch {
                                            val items = com.android.purebilibili.data.repository.parseBlockedUpShareText(text)
                                            blockedListSyncMessage = repository.importBlockedUps(items).message
                                        }
                                    },
                                    onUnblock = { mid ->
                                        scope.launch {
                                            blockedListSyncMessage = repository.unblockUpWithBilibiliSync(mid).message
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            SettingsDetail.PLUGINS -> {
                                // Need to manage editing state locally for the tablet view
                                var editingPlugin by remember { mutableStateOf<com.android.purebilibili.core.plugin.json.JsonRulePlugin?>(null) }
                                
                                val plugins by com.android.purebilibili.core.plugin.PluginManager.pluginsFlow.collectAsStateWithLifecycle()
                                val jsonPlugins by com.android.purebilibili.core.plugin.json.JsonPluginManager.plugins.collectAsStateWithLifecycle()
                                
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
                        Column(modifier = Modifier
                            .widthIn(max = layoutPolicy.rootPanelMaxWidthDp.dp)
                            .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = category.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .padding(bottom = 24.dp, start = 16.dp)
                                    .padding(top = layoutPolicy.detailPanePaddingDp.dp)
                                    .statusBarsPadding()
                            )
                            
                            SettingsRootCategoryContent(
                                category = category,
                                actions = rootCategoryActions,
                                state = rootCategoryState
                            )
                            Spacer(modifier = Modifier
                                .windowInsetsBottomHeight(WindowInsets.navigationBars)
                            )
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
