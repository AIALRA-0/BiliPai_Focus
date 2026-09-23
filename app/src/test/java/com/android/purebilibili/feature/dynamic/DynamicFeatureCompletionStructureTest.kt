package com.android.purebilibili.feature.dynamic

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DynamicFeatureCompletionStructureTest {
    @Test
    fun listAndDetailShareTheManagementDispatcher() {
        val list = source("feature/dynamic/DynamicScreen.kt")
        val detail = source("feature/dynamic/DynamicDetailScreen.kt")

        assertTrue(list.contains("dispatchDynamicManageAction("))
        assertTrue(detail.contains("dispatchDynamicManageAction("))
    }

    @Test
    fun richEditUsesNewFeedProtocolAndKeepsRichControlsVisible() {
        val api = source("core/network/ApiClient.kt")
        val composer = source("feature/dynamic/components/DynamicPublishComposer.kt")
        val repository = source("data/repository/DynamicCreateRepository.kt")

        assertTrue(api.contains("x/dynamic/feed/edit/dyn"))
        assertFalse(api.contains("dynamic_svr/v1/dynamic_svr/modify"))
        assertFalse(composer.contains("if (!isEditing)"))
        assertTrue(repository.contains("existingImages.firstOrNull"))
        assertTrue(repository.contains("editFeedDynamic(query = query, body = request)"))
    }

    @Test
    fun nativeCardsAndInteractionActionsRemainWired() {
        val card = source("feature/dynamic/components/DynamicCard.kt")
        val share = source("feature/dynamic/components/DynamicShareToMessageDialog.kt")
        val shareGrpc = source("data/repository/MessageShareGrpcRepository.kt")
        val viewModel = source("feature/dynamic/DynamicViewModel.kt")
        val api = source("core/network/ApiClient.kt")
        val navigation = source("navigation/AppNavigation.kt")
        val topic = source("feature/search/TopicDetailScreen.kt")

        assertTrue(card.contains("content?.major?.music"))
        assertTrue(card.contains("content?.major?.medialist"))
        assertTrue(card.contains("content?.major?.courses"))
        assertTrue(card.contains("content?.major?.subscription_new"))
        assertTrue(card.contains("DynamicReserveAction("))
        assertTrue(card.contains("label = \"保存动态\""))
        assertTrue(card.contains("label = \"分享至消息\""))
        assertTrue(card.contains("label = \"检查动态\""))
        assertTrue(api.contains("x/dynamic/feed/reserve/click"))
        assertTrue(api.contains("val guestDynamicApi: DynamicApi"))
        assertTrue(navigation.contains("onCollectionClick = { mediaId, ownerMid, title, url ->"))
        assertTrue(navigation.contains("type = \"favorite\""))
        assertTrue(topic.contains("onMusicClick = onMusicClick"))
        assertTrue(topic.contains("onCollectionClick = onCollectionClick"))
        assertTrue(topic.contains("onCourseClick = onCourseClick"))
        assertTrue(share.contains("MessageShareGrpcRepository.sendDynamicShare("))
        assertTrue(share.contains("MessageShareGrpcRepository.getShareTargets"))
        assertTrue(share.contains("AppModalBottomSheet"))
        assertTrue(shareGrpc.contains("ImInterface/ShareList"))
        assertTrue(shareGrpc.contains("ImInterface/SendMsg"))
        assertTrue(shareGrpc.contains("ProtoWire.int32(5, 7)"))
        assertTrue(viewModel.contains("KEY_NOT_INTERESTED_DYNAMIC_IDS"))
        assertTrue(viewModel.contains("_likeOverrides.value"))
        assertTrue(viewModel.contains("NetworkModule.guestDynamicApi.getDynamicDetail"))
    }

    private fun source(relativePath: String): String {
        val path = "src/main/java/com/android/purebilibili/$relativePath"
        return listOf(File(path), File("app/$path")).first(File::exists).readText()
    }
}
