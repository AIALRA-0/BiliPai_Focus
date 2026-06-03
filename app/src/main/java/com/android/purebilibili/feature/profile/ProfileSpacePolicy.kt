package com.android.purebilibili.feature.profile

import com.android.purebilibili.data.model.response.FavFolder
import com.android.purebilibili.data.model.response.FollowBangumiItem
import com.android.purebilibili.data.model.response.MemberAccountData
import com.android.purebilibili.data.model.response.SpaceAggregateArchiveItem
import com.android.purebilibili.data.model.response.SpaceAggregateData
import com.android.purebilibili.data.model.response.SpaceAggregateFavoriteItem
import com.android.purebilibili.data.model.response.SpaceDynamicItem
import com.android.purebilibili.data.model.response.SpaceDynamicRichText
import com.android.purebilibili.data.model.response.SpaceVideoItem
import com.android.purebilibili.feature.dynamic.DynamicDeleteAction
import com.android.purebilibili.feature.home.UserState

enum class ProfileSpaceMainTab(val title: String) {
    HOME("主页"),
    DYNAMIC("动态"),
    CONTRIBUTION("投稿"),
    FAVORITE("收藏"),
    BANGUMI("追番")
}

data class ProfileSpaceTabItem(
    val tab: ProfileSpaceMainTab,
    val title: String
)

enum class ProfileSpaceHomeSection {
    FAVORITES,
    BANGUMI,
    COIN_VIDEOS,
    LIKE_VIDEOS,
    CONTRIBUTIONS,
    SERVICES
}

data class ProfileSpaceUiState(
    val selectedTab: ProfileSpaceMainTab = ProfileSpaceMainTab.HOME,
    val isLoading: Boolean = false,
    val favoriteFolders: List<FavFolder> = emptyList(),
    val bangumiItems: List<FollowBangumiItem> = emptyList(),
    val contributionVideos: List<SpaceVideoItem> = emptyList(),
    val coinVideos: List<SpaceAggregateArchiveItem> = emptyList(),
    val likeVideos: List<SpaceAggregateArchiveItem> = emptyList(),
    val dynamicItems: List<SpaceDynamicItem> = emptyList(),
    val favoriteFolderCount: Int = 0,
    val bangumiCount: Int = 0,
    val contributionVideoCount: Int = 0,
    val coinVideoCount: Int = 0,
    val likeVideoCount: Int = 0,
    val message: String? = null,
    val signSaveMessage: String? = null,
    val isSavingSign: Boolean = false
)

data class ProfileEditableAccountState(
    val name: String = "",
    val birthday: String = "",
    val sex: String = "",
    val sign: String = "",
    val canEditName: Boolean = false,
    val canEditBirthday: Boolean = false,
    val canEditSex: Boolean = false
) {
    val normalizedSign: String = sign.trim()
    val canSubmitSign: Boolean = validateProfileSign(sign) == null
}

fun defaultProfileSpaceTabs(): List<ProfileSpaceTabItem> {
    return ProfileSpaceMainTab.entries.map { tab ->
        ProfileSpaceTabItem(tab = tab, title = tab.title)
    }
}

fun resolveProfileSpaceHomeSections(
    favoriteFolders: List<FavFolder>,
    bangumiItems: List<FollowBangumiItem>,
    coinVideos: List<SpaceAggregateArchiveItem>,
    likeVideos: List<SpaceAggregateArchiveItem>,
    contributionVideos: List<SpaceVideoItem>
): List<ProfileSpaceHomeSection> {
    return buildList {
        if (favoriteFolders.any { it.id > 0L && it.title.isNotBlank() }) add(ProfileSpaceHomeSection.FAVORITES)
        if (bangumiItems.any { it.seasonId > 0L && it.title.isNotBlank() }) add(ProfileSpaceHomeSection.BANGUMI)
        if (coinVideos.any { it.aid > 0L && it.title.isNotBlank() }) add(ProfileSpaceHomeSection.COIN_VIDEOS)
        if (likeVideos.any { it.aid > 0L && it.title.isNotBlank() }) add(ProfileSpaceHomeSection.LIKE_VIDEOS)
        if (contributionVideos.any { it.bvid.isNotBlank() || it.aid > 0L }) add(ProfileSpaceHomeSection.CONTRIBUTIONS)
        add(ProfileSpaceHomeSection.SERVICES)
    }
}

fun validateProfileSign(sign: String): String? {
    return if (sign.length > 70) "签名最多支持 70 个字符" else null
}

fun resolveProfileEditableAccountState(
    account: MemberAccountData?,
    user: UserState,
    aggregateSign: String = ""
): ProfileEditableAccountState {
    return ProfileEditableAccountState(
        name = account?.uname?.ifBlank { user.name } ?: user.name,
        birthday = account?.birthday.orEmpty(),
        sex = account?.sex.orEmpty(),
        sign = account?.sign?.ifBlank { aggregateSign }.orEmpty(),
        canEditName = false,
        canEditBirthday = false,
        canEditSex = false
    )
}

fun resolveProfileSpaceStateFromAggregate(
    aggregate: SpaceAggregateData?,
    favoriteFoldersFallback: List<FavFolder>,
    bangumiItems: List<FollowBangumiItem>,
    dynamicItems: List<SpaceDynamicItem>
): ProfileSpaceUiState {
    val aggregateFavoriteFolders = aggregate?.favourite2?.item.orEmpty().map(::mapProfileAggregateFavoriteFolder)
    val contributionVideos = aggregate?.archive?.item.orEmpty().map(::mapProfileAggregateVideoItem)
    return ProfileSpaceUiState(
        favoriteFolders = aggregateFavoriteFolders.ifEmpty { favoriteFoldersFallback },
        favoriteFolderCount = aggregate?.favourite2?.count ?: favoriteFoldersFallback.size,
        bangumiItems = bangumiItems,
        bangumiCount = bangumiItems.size,
        contributionVideos = contributionVideos,
        contributionVideoCount = aggregate?.archive?.count ?: contributionVideos.size,
        coinVideos = aggregate?.coinArchive?.item.orEmpty(),
        coinVideoCount = aggregate?.coinArchive?.count ?: aggregate?.coinArchive?.item.orEmpty().size,
        likeVideos = aggregate?.likeArchive?.item.orEmpty(),
        likeVideoCount = aggregate?.likeArchive?.count ?: aggregate?.likeArchive?.item.orEmpty().size,
        dynamicItems = dynamicItems.filter { it.visible }
    )
}

private fun mapProfileAggregateVideoItem(item: SpaceAggregateArchiveItem): SpaceVideoItem {
    return SpaceVideoItem(
        aid = item.aid,
        bvid = item.bvid,
        title = item.title,
        pic = item.cover,
        play = item.play,
        comment = item.reply,
        length = item.length,
        created = item.ctime,
        author = item.author,
        typename = item.tname
    )
}

private fun mapProfileAggregateFavoriteFolder(item: SpaceAggregateFavoriteItem): FavFolder {
    val resolvedId = item.mediaId.takeIf { it > 0L } ?: item.id.takeIf { it > 0L } ?: item.fid
    return FavFolder(
        id = resolvedId,
        fid = item.fid,
        mid = item.mid,
        title = item.title,
        cover = item.cover,
        media_count = item.media_count.takeIf { it > 0 } ?: item.count
    )
}

fun resolveProfileDynamicCover(item: SpaceDynamicItem): String {
    val major = item.modules.module_dynamic?.major
    return major?.archive?.cover
        ?.takeIf { it.isNotBlank() }
        ?: major?.draw?.items?.firstOrNull { it.src.isNotBlank() }?.src
        ?: major?.opus?.pics?.firstOrNull { it.src.isNotBlank() }?.src
        ?: major?.article?.covers?.firstOrNull { it.isNotBlank() }
        ?: ""
}

fun resolveProfileDynamicImageUrls(item: SpaceDynamicItem): List<String> {
    val major = item.modules.module_dynamic?.major
    return when {
        major?.draw?.items?.isNotEmpty() == true -> major.draw.items.mapNotNull { it.src.takeIf(String::isNotBlank) }
        major?.opus?.pics?.isNotEmpty() == true -> major.opus.pics.mapNotNull { it.src.takeIf(String::isNotBlank) }
        major?.article?.covers?.isNotEmpty() == true -> major.article.covers.mapNotNull { it.takeIf(String::isNotBlank) }
        else -> emptyList()
    }
}

fun resolveProfileDynamicText(item: SpaceDynamicItem): String {
    val dynamic = item.modules.module_dynamic
    val major = dynamic?.major
    return listOf(
        dynamic?.desc?.rich_text_nodes?.toProfileDynamicText(),
        dynamic?.desc?.text,
        major?.opus?.summary?.rich_text_nodes?.toProfileDynamicText(),
        major?.opus?.summary?.text,
        major?.article?.desc,
        major?.archive?.desc,
        major?.opus?.title,
        major?.article?.title,
        major?.archive?.title
    ).firstNotBlank()
}

fun resolveProfileDynamicAuthorName(item: SpaceDynamicItem): String {
    return item.modules.module_author?.name?.takeIf { it.isNotBlank() } ?: "动态"
}

fun resolveProfileDynamicPublishText(item: SpaceDynamicItem): String {
    return item.modules.module_author?.pub_time?.takeIf { it.isNotBlank() } ?: ""
}

fun resolveProfileDynamicActionText(label: String, count: Int): String {
    return if (count > 0) "$label ${formatProfileDynamicCount(count)}" else label
}

fun resolveProfileDynamicDeleteAction(item: SpaceDynamicItem): DynamicDeleteAction? {
    val deleteItem = item.modules.module_more
        ?.three_point_items
        ?.firstOrNull { it.type == "THREE_POINT_DELETE" }
        ?: return null
    val params = deleteItem.params
    val dynamicId = params?.dyn_id_str
        ?.takeIf { it.isNotBlank() }
        ?: item.id_str.takeIf { it.isNotBlank() }
        ?: return null

    return DynamicDeleteAction(
        dynamicId = dynamicId,
        dynType = params?.dyn_type?.takeIf { it > 0 },
        rid = params?.rid_str?.takeIf { it.isNotBlank() },
        label = deleteItem.label.takeIf { it.isNotBlank() } ?: "删除",
        title = deleteItem.modal?.title?.takeIf { it.isNotBlank() } ?: "要删除动态吗？",
        content = deleteItem.modal?.content?.takeIf { it.isNotBlank() }
            ?: "动态删除后将无法恢复，请谨慎操作",
        confirmText = deleteItem.modal?.confirm?.takeIf { it.isNotBlank() } ?: "删除",
        cancelText = deleteItem.modal?.cancel?.takeIf { it.isNotBlank() } ?: "取消"
    )
}

private fun List<SpaceDynamicRichText>.toProfileDynamicText(): String {
    return joinToString(separator = "") { node ->
        node.text
            .ifBlank { node.orig_text }
            .ifBlank { node.emoji?.text.orEmpty() }
    }.trim()
}

private fun List<String?>.firstNotBlank(): String {
    return firstOrNull { !it.isNullOrBlank() }?.orEmpty() ?: ""
}

private fun formatProfileDynamicCount(count: Int): String {
    return when {
        count >= 10000 -> {
            val wan = count / 10000.0
            val text = if (count % 10000 == 0) {
                (count / 10000).toString()
            } else {
                String.format(java.util.Locale.US, "%.1f", wan).trimEnd('0').trimEnd('.')
            }
            "${text}万"
        }
        else -> count.toString()
    }
}
