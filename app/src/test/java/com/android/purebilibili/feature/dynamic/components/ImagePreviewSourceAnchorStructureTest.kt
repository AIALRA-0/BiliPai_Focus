package com.android.purebilibili.feature.dynamic.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 守护「全局图片查看统一入口」：每个 ImagePreviewDialog 调用点都必须携带
 * sourceRect（落位/回位动画的锚点）；评论链路显式传 Field 圆角，
 * SpaceScreen 头像用圆形源、hero 封面用 0 圆角。
 */
class ImagePreviewSourceAnchorStructureTest {

    private fun readMainSource(relativePath: String): String {
        return listOf(
            File("app/src/main/java/com/android/purebilibili/$relativePath"),
            File("src/main/java/com/android/purebilibili/$relativePath")
        ).first { it.exists() }.readText().replace("\r\n", "\n")
    }

    private fun substringCount(source: String, substring: String): Int =
        source.split(substring).size - 1

    private val callSiteFiles = listOf(
        "feature/home/subscription/SubscriptionFeedPage.kt",
        "feature/video/ui/components/VideoCommentSheetHost.kt",
        "feature/video/screen/VideoContentSection.kt",
        "feature/video/screen/TabletVideoLayout.kt",
        "feature/video/screen/TabletCinemaLayout.kt",
        "feature/dynamic/DynamicDetailScreen.kt",
        "feature/dynamic/components/DynamicCommentSheet.kt",
        "feature/dynamic/components/DynamicSubReplyPreviewHost.kt",
        "feature/dynamic/components/ForwardedContent.kt",
        "feature/dynamic/components/DynamicCard.kt",
        "feature/bangumi/ui/player/BangumiPlayerContent.kt",
        "feature/comment/CommentDetailScreen.kt",
        "feature/profile/ProfileScreen.kt",
        "feature/article/ArticleDetailScreen.kt",
        "feature/space/SpaceScreen.kt",
    )

    @Test
    fun everyImagePreviewDialogCallSiteCarriesSourceAnchor() {
        callSiteFiles.forEach { path ->
            val source = readMainSource(path)
            assertTrue(
                source.contains("ImagePreviewDialog("),
                "$path should still be a call site"
            )
            assertTrue(
                source.contains("sourceRect = "),
                "$path must pass sourceRect for the open/return morph"
            )
        }
    }

    @Test
    fun previouslyAnchorlessEntriesNowPassRects() {
        assertEquals(
            1,
            substringCount(
                readMainSource("feature/bangumi/ui/player/BangumiPlayerContent.kt"),
                "sourceRect = previewSourceRect,"
            ),
            "bangumi should forward the rect already carried by onImagePreview"
        )
        assertTrue(
            readMainSource("feature/home/subscription/SubscriptionFeedPage.kt")
                .contains("sourceRect = previewSourceRect,"),
            "subscription article images should capture their own bounds"
        )
        val space = readMainSource("feature/space/SpaceScreen.kt")
        assertEquals(
            2,
            substringCount(space, "sourceRect = topPhotoSourceRect,") +
                substringCount(space, "sourceRect = avatarSourceRect,")
        )
    }

    @Test
    fun commentChainCallSitesUseRealContainerCorner() {
        listOf(
            "feature/video/screen/VideoContentSection.kt",
            "feature/video/screen/TabletVideoLayout.kt",
            "feature/video/screen/TabletCinemaLayout.kt",
            "feature/video/ui/components/VideoCommentSheetHost.kt",
            "feature/dynamic/DynamicDetailScreen.kt",
            "feature/dynamic/components/DynamicCommentSheet.kt",
            "feature/dynamic/components/DynamicSubReplyPreviewHost.kt",
            "feature/comment/CommentDetailScreen.kt",
        ).forEach { path ->
            val source = readMainSource(path)
            assertTrue(
                source.contains("AppShapes.containerCornerDp(ContainerLevel.Field)"),
                "$path should derive source corner from the real thumbnail shape"
            )
        }
    }

    @Test
    fun spaceScreenUsesCircularAvatarAnchorAndFullBleedHeroCorner() {
        val source = readMainSource("feature/space/SpaceScreen.kt")
        assertTrue(source.contains("imagePreviewSourceBounds(avatarRect)"))
        assertTrue(source.contains("imagePreviewSourceBounds(topPhotoRect)"))
        assertTrue(source.contains("sourceCornerRadiusDp = avatarCornerDp"))
        assertTrue(source.contains("sourceCornerRadiusDp = 0f"))
    }

    @Test
    fun profileCoverUsesChipShapeCornerInsteadOfMagicNumber() {
        val source = readMainSource("feature/profile/ProfileScreen.kt")
        assertTrue(source.contains("AppShapes.containerCornerDp(ContainerLevel.Chip)"))
    }

    @Test
    fun commentPicturesConstructsAnchorsWithRealThumbnailCorners() {
        val source = readMainSource("feature/video/ui/components/ReplyComponents.kt")
        assertTrue(source.contains("onImageClick: (List<String>, Int, ImagePreviewSourceAnchor?) -> Unit"))
        assertTrue(source.contains("ImagePreviewSourceAnchor(it, singleImageCornerDp)"))
        assertTrue(source.contains("ImagePreviewSourceAnchor(it, gridImageCornerDp)"))
        assertTrue(source.contains("AppShapes.containerCornerDp(ContainerLevel.Card)"))
    }

    @Test
    fun bangumiPreviewKeepsRectAndTextFromCallback() {
        val source = readMainSource("feature/bangumi/ui/player/BangumiPlayerContent.kt")
        assertTrue(source.contains("previewSourceRect = rect"))
        assertTrue(source.contains("previewTextContent = textContent"))
    }
}
