package com.android.purebilibili.data.model.response

enum class BangumiIndexFilterGroupKey {
    ORDER,
    SEASON_VERSION,
    STYLE,
    PRODUCER,
    SPOKEN_LANGUAGE,
    COPYRIGHT,
    SEASON_MONTH,
    YEAR,
    SEASON_STATUS
}

data class BangumiIndexFilterOption(
    val label: String,
    val order: Int? = null,
    val sortDirection: Int? = null,
    val styleId: Int? = null,
    val producerId: Int? = null,
    val year: String? = null,
    val seasonStatus: String? = null,
    val seasonVersion: Int? = null,
    val spokenLanguageType: Int? = null,
    val copyright: String? = null,
    val seasonMonth: Int? = null
)

data class BangumiIndexFilterGroup(
    val key: BangumiIndexFilterGroupKey,
    val title: String,
    val options: List<BangumiIndexFilterOption>
)

data class BangumiIndexRequestFilter(
    val order: Int,
    val sortDirection: Int,
    val area: Int,
    val isFinish: Int,
    val year: String,
    val releaseDate: String,
    val styleId: Int,
    val producerId: Int,
    val seasonStatus: String,
    val seasonVersion: Int,
    val spokenLanguageType: Int,
    val copyright: String,
    val seasonMonth: Int
)

fun buildBangumiIndexRequestFilter(
    filter: BangumiFilter,
    seasonType: Int
): BangumiIndexRequestFilter {
    return BangumiIndexRequestFilter(
        order = filter.order,
        sortDirection = filter.sortDirection,
        area = filter.area,
        isFinish = filter.isFinish,
        year = filter.toApiYear(seasonType),
        releaseDate = filter.toApiReleaseDate(seasonType),
        styleId = filter.styleId,
        producerId = filter.producerId,
        seasonStatus = filter.seasonStatus,
        seasonVersion = filter.seasonVersion,
        spokenLanguageType = filter.spokenLanguageType,
        copyright = filter.copyright,
        seasonMonth = filter.seasonMonth
    )
}

fun resolveBangumiSearchTypeForSeasonType(seasonType: Int): SearchType {
    return when (seasonType) {
        BangumiType.ANIME.value, BangumiType.GUOCHUANG.value -> SearchType.BANGUMI
        else -> SearchType.MEDIA_FT
    }
}

fun resolveBangumiSearchPlaceholder(seasonType: Int): String {
    return when (resolveBangumiSearchTypeForSeasonType(seasonType)) {
        SearchType.MEDIA_FT -> "输入关键词搜索影视"
        else -> "输入关键词搜索番剧"
    }
}

fun resolveBangumiIndexFilterGroups(
    seasonType: Int,
    currentYear: Int
): List<BangumiIndexFilterGroup> {
    val groups = mutableListOf<BangumiIndexFilterGroup>()
    groups += BangumiIndexFilterGroup(
        key = BangumiIndexFilterGroupKey.ORDER,
        title = "综合排序",
        options = listOf(
            BangumiIndexFilterOption(label = "综合排序", order = 3, sortDirection = 0),
            BangumiIndexFilterOption(label = "最多播放", order = 2, sortDirection = 0),
            BangumiIndexFilterOption(label = "最近更新", order = 0, sortDirection = 0),
            BangumiIndexFilterOption(label = "最高评分", order = 4, sortDirection = 0)
        )
    )

    groups += BangumiIndexFilterGroup(
        key = BangumiIndexFilterGroupKey.STYLE,
        title = "全部风格",
        options = resolveBangumiStyleOptions(seasonType)
    )

    groups += BangumiIndexFilterGroup(
        key = BangumiIndexFilterGroupKey.SEASON_VERSION,
        title = "全部类型",
        options = SEASON_VERSION_OPTIONS
    )

    groups += BangumiIndexFilterGroup(
        key = BangumiIndexFilterGroupKey.SPOKEN_LANGUAGE,
        title = "全部配音",
        options = SPOKEN_LANGUAGE_OPTIONS
    )

    groups += BangumiIndexFilterGroup(
        key = BangumiIndexFilterGroupKey.COPYRIGHT,
        title = "全部版权",
        options = COPYRIGHT_OPTIONS
    )

    if (seasonType == BangumiType.ANIME.value || seasonType == BangumiType.GUOCHUANG.value) {
        groups += BangumiIndexFilterGroup(
            key = BangumiIndexFilterGroupKey.SEASON_MONTH,
            title = "全部季度",
            options = SEASON_MONTH_OPTIONS
        )
    }

    if (seasonType == BangumiType.DOCUMENTARY.value) {
        groups += BangumiIndexFilterGroup(
            key = BangumiIndexFilterGroupKey.PRODUCER,
            title = "全部出品",
            options = DOCUMENTARY_PRODUCER_OPTIONS
        )
    }

    groups += BangumiIndexFilterGroup(
        key = BangumiIndexFilterGroupKey.YEAR,
        title = "全部年份",
        options = resolveBangumiYearOptions(currentYear)
    )

    if (seasonType == BangumiType.MOVIE.value ||
        seasonType == BangumiType.DOCUMENTARY.value ||
        seasonType == BangumiType.TV_SHOW.value ||
        seasonType == BangumiType.VARIETY.value
    ) {
        groups += BangumiIndexFilterGroup(
            key = BangumiIndexFilterGroupKey.SEASON_STATUS,
            title = "付费类型",
            options = SEASON_STATUS_OPTIONS
        )
    }

    return groups
}

fun applyBangumiIndexFilterOption(
    filter: BangumiFilter,
    option: BangumiIndexFilterOption
): BangumiFilter {
    return filter.copy(
        order = option.order ?: filter.order,
        sortDirection = option.sortDirection ?: filter.sortDirection,
        styleId = option.styleId ?: filter.styleId,
        producerId = option.producerId ?: filter.producerId,
        year = option.year ?: filter.year,
        seasonStatus = option.seasonStatus ?: filter.seasonStatus,
        seasonVersion = option.seasonVersion ?: filter.seasonVersion,
        spokenLanguageType = option.spokenLanguageType ?: filter.spokenLanguageType,
        copyright = option.copyright ?: filter.copyright,
        seasonMonth = option.seasonMonth ?: filter.seasonMonth
    )
}

fun resolveBangumiIndexSelectedOption(
    filter: BangumiFilter,
    group: BangumiIndexFilterGroup
): BangumiIndexFilterOption {
    return group.options.firstOrNull { option ->
        when (group.key) {
            BangumiIndexFilterGroupKey.ORDER -> option.order == filter.order &&
                option.sortDirection == filter.sortDirection
            BangumiIndexFilterGroupKey.STYLE -> option.styleId == filter.styleId
            BangumiIndexFilterGroupKey.PRODUCER -> option.producerId == filter.producerId
            BangumiIndexFilterGroupKey.YEAR -> option.year == filter.year
            BangumiIndexFilterGroupKey.SEASON_STATUS -> option.seasonStatus == filter.seasonStatus
            BangumiIndexFilterGroupKey.SEASON_VERSION -> option.seasonVersion == filter.seasonVersion
            BangumiIndexFilterGroupKey.SPOKEN_LANGUAGE -> option.spokenLanguageType == filter.spokenLanguageType
            BangumiIndexFilterGroupKey.COPYRIGHT -> option.copyright == filter.copyright
            BangumiIndexFilterGroupKey.SEASON_MONTH -> option.seasonMonth == filter.seasonMonth
        }
    } ?: group.options.first()
}

fun resolveBangumiIndexFilterKey(
    seasonType: Int,
    filter: BangumiFilter
): String {
    return listOf(
        seasonType,
        filter.area,
        filter.styleId,
        filter.producerId,
        filter.year,
        filter.seasonStatus,
        filter.order,
        filter.sortDirection,
        filter.seasonVersion,
        filter.spokenLanguageType,
        filter.copyright,
        filter.seasonMonth
    ).joinToString("|")
}

private fun resolveBangumiStyleOptions(seasonType: Int): List<BangumiIndexFilterOption> {
    return when (seasonType) {
        BangumiType.DOCUMENTARY.value -> DOCUMENTARY_STYLE_OPTIONS
        else -> listOf(BangumiIndexFilterOption(label = "全部风格", styleId = -1))
    }
}

private fun resolveBangumiYearOptions(currentYear: Int): List<BangumiIndexFilterOption> {
    val safeCurrentYear = currentYear.coerceAtLeast(1980)
    val recentYears = (safeCurrentYear downTo 2016).map { year ->
        BangumiIndexFilterOption(
            label = year.toString(),
            year = "[$year,${year + 1})"
        )
    }
    return listOf(BangumiIndexFilterOption(label = "全部年份", year = "-1")) +
        recentYears +
        listOf(
            BangumiIndexFilterOption(label = "2015-2010", year = "[2010,2016)"),
            BangumiIndexFilterOption(label = "2009-2005", year = "[2005,2010)"),
            BangumiIndexFilterOption(label = "2004-2000", year = "[2000,2005)"),
            BangumiIndexFilterOption(label = "90年代", year = "[1990,2000)"),
            BangumiIndexFilterOption(label = "80年代", year = "[1980,1990)"),
            BangumiIndexFilterOption(label = "更早", year = "[,1980)")
        )
}

private val DOCUMENTARY_STYLE_OPTIONS = listOf(
    BangumiIndexFilterOption(label = "全部风格", styleId = -1),
    BangumiIndexFilterOption(label = "历史", styleId = 25),
    BangumiIndexFilterOption(label = "美食", styleId = 39),
    BangumiIndexFilterOption(label = "人文", styleId = 19),
    BangumiIndexFilterOption(label = "科技", styleId = 27),
    BangumiIndexFilterOption(label = "探险", styleId = 29),
    BangumiIndexFilterOption(label = "宇宙", styleId = 1201),
    BangumiIndexFilterOption(label = "萌宠", styleId = 1202),
    BangumiIndexFilterOption(label = "社会", styleId = 1203),
    BangumiIndexFilterOption(label = "动物", styleId = 989),
    BangumiIndexFilterOption(label = "自然", styleId = 34),
    BangumiIndexFilterOption(label = "医疗", styleId = 1204),
    BangumiIndexFilterOption(label = "军事", styleId = 988),
    BangumiIndexFilterOption(label = "灾难", styleId = 1205),
    BangumiIndexFilterOption(label = "旅行", styleId = 31)
)

private val DOCUMENTARY_PRODUCER_OPTIONS = listOf(
    BangumiIndexFilterOption(label = "全部出品", producerId = -1),
    BangumiIndexFilterOption(label = "央视", producerId = 4),
    BangumiIndexFilterOption(label = "BBC", producerId = 1),
    BangumiIndexFilterOption(label = "探索频道", producerId = 7),
    BangumiIndexFilterOption(label = "NHK", producerId = 2),
    BangumiIndexFilterOption(label = "历史频道", producerId = 6),
    BangumiIndexFilterOption(label = "卫视", producerId = 8),
    BangumiIndexFilterOption(label = "自制", producerId = 9),
    BangumiIndexFilterOption(label = "ITV", producerId = 5),
    BangumiIndexFilterOption(label = "SKY", producerId = 3),
    BangumiIndexFilterOption(label = "ZDF", producerId = 10),
    BangumiIndexFilterOption(label = "合作机构", producerId = 11),
    BangumiIndexFilterOption(label = "国内其他", producerId = 12),
    BangumiIndexFilterOption(label = "国外其他", producerId = 13)
)

private val SEASON_STATUS_OPTIONS = listOf(
    BangumiIndexFilterOption(label = "全部", seasonStatus = "-1"),
    BangumiIndexFilterOption(label = "免费", seasonStatus = "1"),
    BangumiIndexFilterOption(label = "付费", seasonStatus = "2,6"),
    BangumiIndexFilterOption(label = "大会员", seasonStatus = "4,6")
)

private val SEASON_VERSION_OPTIONS = listOf(
    BangumiIndexFilterOption(label = "全部类型", seasonVersion = -1),
    BangumiIndexFilterOption(label = "正片", seasonVersion = 1),
    BangumiIndexFilterOption(label = "电影", seasonVersion = 2),
    BangumiIndexFilterOption(label = "其他", seasonVersion = 3)
)

private val SPOKEN_LANGUAGE_OPTIONS = listOf(
    BangumiIndexFilterOption(label = "全部配音", spokenLanguageType = -1),
    BangumiIndexFilterOption(label = "原声", spokenLanguageType = 1),
    BangumiIndexFilterOption(label = "中文配音", spokenLanguageType = 2)
)

private val COPYRIGHT_OPTIONS = listOf(
    BangumiIndexFilterOption(label = "全部版权", copyright = "-1"),
    BangumiIndexFilterOption(label = "独家", copyright = "3"),
    BangumiIndexFilterOption(label = "其他", copyright = "1,2,4")
)

private val SEASON_MONTH_OPTIONS = listOf(
    BangumiIndexFilterOption(label = "全部季度", seasonMonth = -1),
    BangumiIndexFilterOption(label = "一月", seasonMonth = 1),
    BangumiIndexFilterOption(label = "四月", seasonMonth = 4),
    BangumiIndexFilterOption(label = "七月", seasonMonth = 7),
    BangumiIndexFilterOption(label = "十月", seasonMonth = 10)
)
