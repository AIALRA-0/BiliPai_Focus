package com.android.purebilibili.feature.dynamic.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import com.android.purebilibili.core.ui.AppAlertDialog
import com.android.purebilibili.core.ui.AppChromeSizeTokens
import com.android.purebilibili.core.ui.AppDialogAction
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.AppTextButton
import com.android.purebilibili.core.ui.components.AppTextField
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppIconButton
import com.android.purebilibili.core.ui.components.AppNativeSegmentedControl
import com.android.purebilibili.core.ui.components.AppSegmentOption
import com.android.purebilibili.core.store.HomeSettings
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.util.PickMultipleGalleryVisualMedia
import com.android.purebilibili.core.ui.rememberAppDeleteIcon
import com.android.purebilibili.feature.home.components.BottomBarMatchedReusableLiquidDock
import com.android.purebilibili.feature.home.components.BottomBarLiquidSegmentedControl
import com.android.purebilibili.feature.home.components.resolveFloatingDockGeometryScale
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import com.android.purebilibili.data.model.response.DynamicCreatedReserve
import com.android.purebilibili.data.model.response.DynamicCreatedVote
import com.android.purebilibili.data.model.response.DynamicPublishDraft
import com.android.purebilibili.data.model.response.DynamicPublishMention
import com.android.purebilibili.data.model.response.DynamicPublishTopic
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun DynamicPublishComposer(
    initialDraft: DynamicPublishDraft,
    isEditing: Boolean,
    submitting: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSubmit: (DynamicPublishDraft) -> Unit
) {
    val context = LocalContext.current
    val homeSettings by SettingsManager.getHomeSettings(context)
        .collectAsStateWithLifecycle(initialValue = HomeSettings())
    val liquidGlassEnabled = homeSettings.androidNativeLiquidGlassEnabled
    var text by remember(initialDraft) { mutableStateOf(initialDraft.text) }
    var title by remember(initialDraft) { mutableStateOf(initialDraft.title) }
    var imageUris by remember(initialDraft) { mutableStateOf(initialDraft.imageUris) }
    var vote by remember(initialDraft) {
        mutableStateOf(
            initialDraft.voteId.takeIf { it > 0L }?.let {
                DynamicCreatedVote(it, initialDraft.voteTitle.ifBlank { "投票" })
            }
        )
    }
    var reserve by remember(initialDraft) {
        mutableStateOf(
            initialDraft.reserveId.takeIf { it > 0L }?.let {
                DynamicCreatedReserve(it, "预约")
            }
        )
    }
    var mentions by remember(initialDraft) { mutableStateOf(initialDraft.mentions) }
    var emotes by remember(initialDraft) { mutableStateOf(initialDraft.emotes) }
    var topic by remember(initialDraft) { mutableStateOf(initialDraft.topic) }
    var privatePublish by remember(initialDraft) { mutableStateOf(initialDraft.private) }
    var showVoteDialog by remember { mutableStateOf(false) }
    var showReserveDialog by remember { mutableStateOf(false) }
    var showMentionDialog by remember { mutableStateOf(false) }
    var showTopicDialog by remember { mutableStateOf(false) }
    var showEmoteDialog by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(
        PickMultipleGalleryVisualMedia(maxItems = 9)
    ) { uris ->
        if (uris.isNotEmpty()) {
            imageUris = (imageUris + uris.map { it.toString() }).distinct().take(9)
        }
    }

    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { AppText(if (isEditing) "编辑动态" else "发布动态") },
        text = {
            val publishChromeBackdrop = if (liquidGlassEnabled) rememberLayerBackdrop() else null
            val visibilityLabels = remember { listOf("公开", "仅自己可见") }
            val visibilityOptions = remember {
                listOf(
                    AppSegmentOption(false, "公开"),
                    AppSegmentOption(true, "仅自己可见"),
                )
            }
            Box(modifier = Modifier.fillMaxWidth()) {
                if (liquidGlassEnabled) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .then(
                                if (publishChromeBackdrop != null) {
                                    Modifier.layerBackdrop(publishChromeBackdrop)
                                } else {
                                    Modifier
                                }
                            )
                            .background(AppSurfaceTokens.background())
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small)
                ) {
                    AppTextField(
                        value = title,
                        onValueChange = { if (it.length <= 20) title = it },
                        placeholder = "标题，选填 20 字",
                        singleLine = true
                    )
                    AppTextField(
                        value = text,
                        onValueChange = { text = it },
                        placeholder = "说点什么吧…",
                        singleLine = false,
                        minLines = 4
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small)) {
                            items(imageUris, key = { it }) { uri ->
                                Box {
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(AppShapes.container(ContainerLevel.Chip)),
                                        contentScale = ContentScale.Crop
                                    )
                                    AppIconButton(
                                        onClick = { imageUris = imageUris - uri },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(AppChromeSizeTokens.MinimumTouchTarget),
                                    ) {
                                        AppIcon(
                                            imageVector = rememberAppDeleteIcon(),
                                            contentDescription = "移除图片",
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            }
                    }
                    BottomBarMatchedReusableLiquidDock(
                        shape = AppShapes.container(ContainerLevel.Pill),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(AppSpacingTokens.TripleExtraLarge),
                        backdrop = publishChromeBackdrop,
                        reuseEnabled = liquidGlassEnabled,
                        drawShellLens = true,
                        shellLensIntensity = resolveFloatingDockGeometryScale(
                            AppSpacingTokens.TripleExtraLarge.value
                        ),
                    ) { liquidChromeActive ->
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = if (liquidChromeActive) AppSpacingTokens.ExtraSmall else AppSpacingTokens.None,
                                ),
                            horizontalArrangement = Arrangement.spacedBy(AppSpacingTokens.ExtraSmall),
                        ) {
                            item {
                                AppTextButton(onClick = { showTopicDialog = true }) {
                                    AppText(topic?.name?.let { "#$it#" } ?: "话题")
                                }
                            }
                            item {
                                AppTextButton(onClick = { showMentionDialog = true }) { AppText("@用户") }
                            }
                            item {
                                AppTextButton(onClick = { showEmoteDialog = true }) { AppText("表情") }
                            }
                            item {
                                AppTextButton(
                                    onClick = {
                                        picker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                ) { AppText(if (imageUris.isEmpty()) "图片" else "图片 ${imageUris.size}/9") }
                            }
                            item {
                                AppTextButton(onClick = { showVoteDialog = true }) {
                                    AppText(vote?.title?.let { "投票：$it" } ?: "投票")
                                }
                            }
                            item {
                                AppTextButton(onClick = { showReserveDialog = true }) {
                                    AppText(reserve?.title?.let { "预约：$it" } ?: "预约")
                                }
                            }
                        }
                    }
                    if (liquidGlassEnabled) {
                        BottomBarLiquidSegmentedControl(
                            items = visibilityLabels,
                            selectedIndex = if (privatePublish) 1 else 0,
                            onSelected = { index -> privatePublish = index == 1 },
                            itemWidth = 88.dp,
                            height = AppChromeSizeTokens.BottomBarMatchedSegmentedControlHeightDp.dp,
                            indicatorHeight = AppChromeSizeTokens.BottomBarMatchedSegmentedIndicatorHeightDp.dp,
                            labelFontSize = 13.sp,
                            miuixBackdrop = publishChromeBackdrop,
                            liquidGlassEffectsEnabled = true,
                            dragSelectionEnabled = visibilityLabels.size > 1,
                            tapPressRefractionEnabled = true,
                        )
                    } else {
                        AppNativeSegmentedControl(
                            options = visibilityOptions,
                            selectedValue = privatePublish,
                            modifier = Modifier.fillMaxWidth(),
                            onSelectionChange = { privatePublish = it },
                        )
                    }
                errorMessage?.let { AppText(it) }
                }
            }
        },
        confirmButton = {
            AppDialogAction(
                onClick = {
                    if (submitting) return@AppDialogAction
                    onSubmit(
                        DynamicPublishDraft(
                            text = text,
                            title = title,
                            imageUris = imageUris,
                            voteId = vote?.voteId ?: 0L,
                            voteTitle = vote?.title.orEmpty(),
                            reserveId = reserve?.reserveId ?: 0L,
                            private = privatePublish,
                            mentions = mentions,
                            emotes = emotes,
                            topic = topic,
                            existingImages = initialDraft.existingImages.filter { it.img_src in imageUris },
                        )
                    )
                }
            ) {
                AppText(
                    when {
                        submitting -> "发布中…"
                        isEditing -> "保存"
                        else -> "发布"
                    }
                )
            }
        },
        dismissButton = {
            AppDialogAction(onClick = onDismiss) { AppText("取消") }
        }
    )

    if (showVoteDialog) {
        DynamicCreateVoteDialog(
            onDismiss = { showVoteDialog = false },
            onCreated = { created ->
                vote = created
                showVoteDialog = false
            }
        )
    }
    if (showReserveDialog) {
        DynamicCreateReserveDialog(
            onDismiss = { showReserveDialog = false },
            onCreated = { created ->
                reserve = created
                showReserveDialog = false
            }
        )
    }
    if (showMentionDialog) {
        DynamicMentionPickerDialog(
            onDismiss = { showMentionDialog = false },
            onSelected = { mention ->
                mentions = (mentions + mention).distinctBy { it.uid }
                text = appendDynamicComposerToken(text, "@${mention.name} ")
                showMentionDialog = false
            },
        )
    }
    if (showTopicDialog) {
        DynamicTopicPickerDialog(
            onDismiss = { showTopicDialog = false },
            onSelected = { selectedTopic ->
                topic = selectedTopic
                showTopicDialog = false
            },
        )
    }
    if (showEmoteDialog) {
        DynamicEmotePickerDialog(
            onDismiss = { showEmoteDialog = false },
            onSelected = { emote ->
                emotes = (emotes + emote).distinct()
                text = appendDynamicComposerToken(text, emote)
                showEmoteDialog = false
            },
        )
    }
}

internal fun appendDynamicComposerToken(text: String, token: String): String {
    if (token.isBlank()) return text
    return when {
        text.isBlank() -> token
        text.last().isWhitespace() || token.first().isWhitespace() -> text + token
        else -> "$text $token"
    }
}
