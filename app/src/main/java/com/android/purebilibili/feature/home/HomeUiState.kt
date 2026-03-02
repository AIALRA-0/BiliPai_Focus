// 文件路径: feature/home/HomeUiState.kt
package com.android.purebilibili.feature.home

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.data.model.response.LiveRoom

/**
 * 用户状态
 *  性能优化：@Immutable 告诉 Compose 此类不可变，减少不必要的重组
 */
@Immutable
data class UserState(
    val isLogin: Boolean = false,
    val face: String = "",
    val name: String = "",
    val mid: Long = 0,
    val level: Int = 0,
    val coin: Double = 0.0,
    val bcoin: Double = 0.0,
    val following: Int = 0,
    val follower: Int = 0,
    val dynamic: Int = 0,
    val isVip: Boolean = false,
    val vipLabel: String = "",
    //  [New] 顶部背景图
    val topPhoto: String = ""
)

/**
 * 首页分类枚举（含 Bilibili 分区 ID）
 */
enum class HomeCategory(val label: String, val tid: Int = 0) {
    RECOMMEND("推荐", 0),
    FOLLOW("关注", 0),    //  关注动态
    POPULAR("热门", 0),
    LIVE("直播", 0),
    ANIME("追番", 13),     // 番剧分区
    GAME("游戏", 4),       // 游戏分区
    KNOWLEDGE("知识", 36), // 知识分区
    TECH("科技", 188)      // 科技分区
}

/**
 * 直播子分类
 */
enum class LiveSubCategory(val label: String) {
    FOLLOWED("关注"),
    POPULAR("热门")
}

enum class PopularSubCategory(val label: String) {
    COMPREHENSIVE("综合热门"),
    RANKING("排行榜"),
    WEEKLY("每周必看"),
    PRECIOUS("入站必刷")
}

enum class TodayWatchMode(val label: String) {
    RELAX("今晚轻松看"),
    LEARN("深度学习看")
}

@Immutable
data class TodayUpRank(
    val mid: Long,
    val name: String,
    val score: Double,
    val watchCount: Int
)

@Immutable
data class TodayWatchPlan(
    val mode: TodayWatchMode = TodayWatchMode.RELAX,
    val upRanks: List<TodayUpRank> = emptyList(),
    val videoQueue: List<VideoItem> = emptyList(),
    val explanationByBvid: Map<String, String> = emptyMap(),
    val historySampleCount: Int = 0,
    val nightSignalUsed: Boolean = false,
    val generatedAt: Long = 0L
)

@Immutable
data class TodayWatchCardUiConfig(
    val showUpRank: Boolean = true,
    val showReasonHint: Boolean = true,
    val queuePreviewLimit: Int = 6,
    val enableWaterfallAnimation: Boolean = true,
    val waterfallExponent: Float = 1.38f
)

/**
 * 单个分类的内容状态 (新增)
 */
@Stable
data class CategoryContent(
    val videos: List<VideoItem> = emptyList(),
    val liveRooms: List<LiveRoom> = emptyList(),
    val followedLiveRooms: List<LiveRoom> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val pageIndex: Int = 1, //  保存分页索引
    val hasMore: Boolean = true //  保存是否还有更多数据
)

/**
 * 首页 UI 状态
 *  性能优化：@Stable 告诉 Compose 此类字段变化可被追踪，优化重组
 */
@Stable
data class HomeUiState(
    // 兼容旧字段便于迁移（将被 categoryStates 替代）
    val videos: List<VideoItem> = emptyList(),
    val liveRooms: List<LiveRoom> = emptyList(),  // 热门直播
    val followedLiveRooms: List<LiveRoom> = emptyList(),  // 🔴 [新增] 关注的主播直播
    val isLoading: Boolean = false,
    val error: String? = null,
    
    // 📺 [核心变更] 各分类独立状态缓存
    // 使用 Map 保存每个分类的数据，切换时直接读取
    val categoryStates: Map<HomeCategory, CategoryContent> = emptyMap(),
    
    val user: UserState = UserState(),
    val currentCategory: HomeCategory = HomeCategory.RECOMMEND,
    val liveSubCategory: LiveSubCategory = LiveSubCategory.FOLLOWED,
    val popularSubCategory: PopularSubCategory = PopularSubCategory.COMPREHENSIVE,
    val refreshKey: Long = 0L,
    val followingMids: Set<Long> = emptySet(),
    //  [新增] 标签页显示索引（独立于内容分类，用于特殊分类导航后保持标签位置）
    val displayedTabIndex: Int = 0,
    //  [彩蛋] 刷新成功后的趣味消息
    val refreshMessage: String? = null,
    //  [新增] 增量刷新新增条数（null 表示不展示）
    val refreshNewItemsCount: Int? = null,
    //  [新增] 新增条数提示触发键（用于一次性 UI 动效）
    val refreshNewItemsKey: Long = 0L,
    //  [新增] 新增条数提示已消费键（防止离开页面后重复弹出）
    val refreshNewItemsHandledKey: Long = 0L,
    //  [新增] 推荐流旧内容锚点（刷新前首条视频 bvid）
    val recommendOldContentAnchorBvid: String? = null,
    //  [新增] 推荐流中“旧内容起始”索引（用于插入分割线）
    val recommendOldContentStartIndex: Int? = null,
    //  [新增] 正在消散动画中的视频 BVIDs（动画完成后移除）
    val dissolvingVideos: Set<String> = emptySet(),
    //  [新增] 今日看什么推荐单
    val todayWatchMode: TodayWatchMode = TodayWatchMode.RELAX,
    val todayWatchPlan: TodayWatchPlan? = null,
    val todayWatchLoading: Boolean = false,
    val todayWatchError: String? = null,
    val todayWatchPluginEnabled: Boolean = false,
    val todayWatchCollapsed: Boolean = false,
    val todayWatchCardConfig: TodayWatchCardUiConfig = TodayWatchCardUiConfig(),
    //  [新增] 刷新撤销状态
    val undoAvailable: Boolean = false
)
