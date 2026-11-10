package com.android.purebilibili.feature.dynamic

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class DynamicCommentStructureTest {

    @Test
    fun `dynamic comments use the shared dialog host and keep the entry bar read only`() {
        val sheetSource = File(
            "src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicCommentSheet.kt"
        ).readText()
        val hostSource = File(
            "src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicCommentInputHost.kt"
        ).readText()
        val entryBarSource = File(
            "src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicCommentEntryBar.kt"
        ).readText()

        assertTrue(sheetSource.contains("DynamicCommentInputHost("))
        assertTrue(sheetSource.contains("DynamicCommentEntryBar("))
        assertTrue(hostSource.contains("CommentInputDialog("))
        assertTrue(hostSource.contains("draftKey = draftKey"))
        assertTrue(hostSource.contains("initialText = draft.text"))
        assertTrue(hostSource.contains("onDraftChange = { text, images, sync ->"))
        assertTrue(hostSource.contains("currentDrafts.sessionId == draftSessionId"))
        assertTrue(hostSource.contains("commentReplyTarget.value?.parentRpid == draftReplyRpid"))
        assertTrue(hostSource.contains("allowImageAttachment = replyTarget == null"))
        assertTrue(entryBarSource.contains("CommentEntryBarContent("))
        assertTrue(entryBarSource.contains("appActionClickable(role = Role.Button, onClick = onClick)"))
        assertTrue(!entryBarSource.contains("OutlinedTextField("))
    }

    @Test
    fun `dynamic comments expose sort mode state and reset pagination when it changes`() {
        val source = File("src/main/java/com/android/purebilibili/feature/dynamic/DynamicViewModel.kt")
            .readText()

        assertTrue(source.contains("val dynamicCommentSortMode: StateFlow<CommentSortMode>"))
        assertTrue(source.contains("fun setDynamicCommentSortMode(mode: CommentSortMode)"))
        assertTrue(source.contains("nextPage = 1"))
        assertTrue(source.contains("commentGrpcNextOffset = null"))
        assertTrue(source.contains("loadCommentsForDynamic(item)"))
    }

    @Test
    fun `dynamic sub replies use server sorting and cursor paging`() {
        val source = File("src/main/java/com/android/purebilibili/feature/dynamic/DynamicViewModel.kt")
            .readText()

        assertTrue(source.contains("paginationOffset = request.offset"))
        assertTrue(source.contains("CommentRepository.getMobileSortedSubCommentsForSubject("))
        assertTrue(source.contains("mode = sortMode.apiMode"))
        assertTrue(source.contains("grpcNextOffset = data.grpcNextOffset"))
    }

    @Test
    fun `dynamic detail reloads comments for each screen entry`() {
        val source = File("src/main/java/com/android/purebilibili/feature/dynamic/DynamicDetailScreen.kt")
            .readText()

        assertTrue(source.contains("interactionViewModel.openCommentSheet("))
        assertTrue(!source.contains("hasAutoOpenedComments"))
        assertTrue(!source.contains("rememberSaveable(\n        dynamicId"))
    }

    @Test
    fun `dynamic detail pins the comment composer outside the scrolling list`() {
        val source = File("src/main/java/com/android/purebilibili/feature/dynamic/DynamicDetailScreen.kt")
            .readText()
        val commentContent = source
            .substringAfter("val commentContent:")
            .substringBefore("val commentComposer:")

        assertTrue(source.contains("val commentComposer: @Composable (Modifier) -> Unit = { modifier ->"))
        assertTrue(source.contains("DynamicCommentEntryBar("))
        assertTrue(source.contains("shouldLoadMoreDynamicDetailComments("))
        assertTrue(source.contains("LaunchedEffect(detailListState, commentListState, useSplitLayout)"))
        assertTrue(commentContent.contains("dynamicInlineCommentItems("))
        assertTrue(!commentContent.contains("DynamicCommentEntryBar("))
        assertTrue(!source.contains("item(key = \"dynamic_detail_comment_composer\")"))
        assertTrue(source.contains(".align(Alignment.BottomCenter)"))
    }

    @Test
    fun `dynamic detail comment composer becomes a liquid dock when enabled`() {
        val source = File("src/main/java/com/android/purebilibili/feature/dynamic/DynamicDetailScreen.kt")
            .readText()
        val entryBarSource = File(
            "src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicCommentEntryBar.kt"
        ).readText()

        assertTrue(entryBarSource.contains("BottomBarMatchedReusableLiquidDock("))
        assertTrue(entryBarSource.contains("reuseEnabled = true"))
        assertTrue(entryBarSource.contains("drawShellLens = true"))
        assertTrue(entryBarSource.contains("backdrop = backdrop"))
        assertTrue(entryBarSource.contains("CommentEntryBarContent("))
        assertTrue(entryBarSource.contains(".height(44.dp)"))
        assertTrue(entryBarSource.contains(".height(48.dp)"))
        assertTrue(source.contains("shouldUseFloatingLiquidBottomInputBar("))
        assertTrue(source.contains("resolveBottomInputBarContentBottomPadding("))
        assertTrue(source.contains("val detailCommentSource = if (liquidGlassEnabled) rememberChromeBackdropSource() else null"))
        assertTrue(source.contains("val detailCommentBackdrop = detailCommentSource?.backdrop"))
        assertTrue(source.contains("bottom = commentContentBottomPadding"))
        assertTrue(source.contains("top = paddingValues.calculateTopPadding()"))
        assertTrue(source.contains(".widthIn(max = 360.dp)"))
        assertTrue(!source.contains(".weight(1f)"))
    }

    @Test
    fun `dynamic comment reload cancels and ignores the previous request`() {
        val source = File("src/main/java/com/android/purebilibili/feature/dynamic/DynamicViewModel.kt")
            .readText()

        assertTrue(source.contains("private var commentLoadJob: Job? = null"))
        assertTrue(source.contains("val requestId = ++commentLoadRequestId"))
        assertTrue(source.contains("commentLoadJob?.cancel()"))
        assertTrue(source.contains("if (requestId != commentLoadRequestId) return@launchAccount"))
    }

    @Test
    fun `dynamic comment mentions forward user navigation through every comment surface`() {
        val sheetSource = File(
            "src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicCommentSheet.kt"
        ).readText()
        val subReplySource = File(
            "src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicSubReplyPreviewHost.kt"
        ).readText()
        val screenSource = File(
            "src/main/java/com/android/purebilibili/feature/dynamic/DynamicScreen.kt"
        ).readText()
        val detailSource = File(
            "src/main/java/com/android/purebilibili/feature/dynamic/DynamicDetailScreen.kt"
        ).readText()

        assertTrue(sheetSource.contains("content = reply.content"))
        assertTrue(sheetSource.contains("content = subReply.content"))
        assertTrue(sheetSource.contains("SubReplyDetailContent("))
        assertTrue(sheetSource.contains("onUserClick = onUserClick"))
        assertTrue(sheetSource.contains("onAvatarClick = { mid -> mid.toLongOrNull()?.let(onUserClick) }"))
        assertTrue(subReplySource.contains("onAvatarClick = { mid -> mid.toLongOrNull()?.let(onUserClick) }"))
        assertTrue(subReplySource.contains("DynamicEmoteCatalog.ensureLoaded()"))
        assertTrue(subReplySource.contains("emoteMap = emoteMap"))
        assertTrue(!subReplySource.contains("emoteMap = emptyMap()"))
        assertTrue(screenSource.contains("DynamicCommentOverlayHost("))
        assertTrue(screenSource.contains("onUserClick = onUserClick"))
        assertTrue(detailSource.contains("dynamicInlineCommentItems("))
        assertTrue(detailSource.contains("onUserClick = onUserClick"))
    }

    @Test
    fun `dynamic comment fan decoration is top-end overlay not inline in name row`() {
        val sheetSource = File(
            "src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicCommentSheet.kt"
        ).readText()
        val commentItem = sheetSource
            .substringAfter("private fun CommentItem(")
            .substringBefore("private fun formatTime(")

        assertTrue(commentItem.contains("FanGroupDecorationBadge("))
        assertTrue(commentItem.contains("Alignment.TopEnd"))
        assertTrue(commentItem.contains("decorationEndReserve"))
        // Must not keep badge inline after username/time in the name Row.
        val nameRow = commentItem
            .substringAfter("// 用户名 + 时间")
            .substringBefore("// 评论内容")
        assertTrue(!nameRow.contains("FanGroupDecorationBadge("))
    }

    @Test
    fun `dynamic comment avatar and name open user space`() {
        val sheetSource = File(
            "src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicCommentSheet.kt"
        ).readText()
        val commentItem = sheetSource
            .substringAfter("private fun CommentItem(")
            .substringBefore("private fun formatTime(")

        assertTrue(commentItem.contains("ReplyMemberAvatar("))
        assertTrue(commentItem.contains("onClick = memberMid?.let"))
        assertTrue(commentItem.contains("onUserClick(memberMid)"))
        assertTrue(commentItem.contains("onUserClick(mid)"))
    }

    @Test
    fun `dynamic comments expose like reply and thread tap interactions`() {
        val sheetSource = File(
            "src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicCommentSheet.kt"
        ).readText()
        val viewModelSource = File(
            "src/main/java/com/android/purebilibili/feature/dynamic/DynamicViewModel.kt"
        ).readText()
        val commentItem = sheetSource
            .substringAfter("private fun CommentItem(")
            .substringBefore("private fun formatTime(")

        assertTrue(sheetSource.contains("resolveDynamicCommentCountLabel("))
        assertTrue(sheetSource.contains("resolveDynamicCommentEmptyLabel()"))
        assertTrue(commentItem.contains("shouldOpenDynamicCommentThreadOnTap(reply)"))
        assertTrue(commentItem.contains("onReply(reply)"))
        assertTrue(commentItem.contains("onLike(reply)"))
        assertTrue(viewModelSource.contains("fun likeComment(rpid: Long"))
        assertTrue(viewModelSource.contains("fun startCommentReply("))
        assertTrue(viewModelSource.contains("root = replyTarget?.rootRpid ?: 0L"))
    }

    @Test
    fun `dynamic comments share video comment typography tokens`() {
        val sheetSource = File(
            "src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicCommentSheet.kt"
        ).readText()
        val commentItem = sheetSource
            .substringAfter("private fun CommentItem(")
            .substringBefore("private fun formatTime(")

        assertTrue(commentItem.contains("VideoCommentTypographyTokens.author"))
        assertTrue(commentItem.contains("VideoCommentTypographyTokens.metadata"))
        assertTrue(commentItem.contains("VideoCommentTypographyTokens.body"))
        assertTrue(commentItem.contains("VideoCommentTypographyTokens.action"))
        assertTrue(commentItem.contains("VideoCommentTypographyTokens.subReply"))
        assertTrue(!commentItem.contains("MaterialTheme.typography.labelSmall.fontSize"))
    }

    @Test
    fun `feed and space comment buttons open dynamic detail`() {
        val screenSource = File(
            "src/main/java/com/android/purebilibili/feature/dynamic/DynamicScreen.kt"
        ).readText()
        val spaceSource = File(
            "src/main/java/com/android/purebilibili/feature/space/SpaceScreen.kt"
        ).readText()
        val topicSource = File(
            "src/main/java/com/android/purebilibili/feature/search/TopicDetailScreen.kt"
        ).readText()

        assertTrue(screenSource.contains("onCommentClick = onDynamicDetailClick"))
        assertTrue(!screenSource.contains("onCommentClick = { viewModel.openCommentSheet(it) }"))
        assertTrue(spaceSource.contains("onCommentClick = { onDynamicDetailClick(dynamic.id_str) }"))
        assertTrue(topicSource.contains("onCommentClick = onDynamicDetailClick"))
    }
}
