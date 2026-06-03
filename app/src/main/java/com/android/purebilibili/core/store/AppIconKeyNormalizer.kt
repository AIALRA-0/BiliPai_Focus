package com.android.purebilibili.core.store

const val APP_ICON_COMPONENT_PACKAGE_NAME = "com.android.purebilibili"
const val APP_ICON_COMPAT_ALIAS_CLASS_NAME = "$APP_ICON_COMPONENT_PACKAGE_NAME.MainActivityAlias3D"

const val DEFAULT_APP_ICON_KEY = "icon_3d"

private val CANONICAL_APP_ICON_KEYS = setOf(
    "icon_3d",
    "icon_bilipai",
    "icon_bilipai_pink",
    "icon_bilipai_white",
    "icon_bilipai_monet",
    "icon_anime",
    "icon_flat",
    "icon_telegram_blue",
    "icon_telegram_dark",
    "Yuki",
    "Headphone"
)

private val LAUNCHER_ALIAS_SUFFIX_BY_KEY = mapOf(
    "icon_3d" to "MainActivityAlias3DLauncher",
    "icon_bilipai" to "MainActivityAliasBiliPai",
    "icon_bilipai_pink" to "MainActivityAliasBiliPaiPink",
    "icon_bilipai_white" to "MainActivityAliasBiliPaiWhite",
    "icon_bilipai_monet" to "MainActivityAliasBiliPaiMonet",
    "icon_anime" to "MainActivityAliasAnime",
    "icon_flat" to "MainActivityAliasFlat",
    "icon_telegram_blue" to "MainActivityAliasTelegramBlue",
    "icon_telegram_dark" to "MainActivityAliasDark",
    "Yuki" to "MainActivityAliasYuki",
    "Headphone" to "MainActivityAliasHeadphone"
)

private val NO_ICON_LAUNCHER_ALIAS_SUFFIX_BY_KEY = mapOf(
    "icon_3d" to "MainActivityAlias3DNoIcon",
    "icon_bilipai" to "MainActivityAliasBiliPaiNoIcon",
    "icon_bilipai_pink" to "MainActivityAliasBiliPaiPinkNoIcon",
    "icon_bilipai_white" to "MainActivityAliasBiliPaiWhiteNoIcon",
    "icon_bilipai_monet" to "MainActivityAliasBiliPaiMonetNoIcon",
    "icon_anime" to "MainActivityAliasAnimeNoIcon",
    "icon_flat" to "MainActivityAliasFlatNoIcon",
    "icon_telegram_blue" to "MainActivityAliasTelegramBlueNoIcon",
    "icon_telegram_dark" to "MainActivityAliasDarkNoIcon",
    "Yuki" to "MainActivityAliasYukiNoIcon",
    "Headphone" to "MainActivityAliasHeadphoneNoIcon"
)

fun normalizeAppIconKey(rawKey: String?): String {
    val key = rawKey?.trim().orEmpty()
    if (key.isEmpty()) return DEFAULT_APP_ICON_KEY

    return when (key) {
        "default", "3D" -> "icon_3d"
        "BiliPai", "bilipai", "Icon BiliPai" -> "icon_bilipai"
        "BiliPai Pink", "BiliPai 粉", "bilipai_pink" -> "icon_bilipai_pink"
        "BiliPai White", "BiliPai 白", "bilipai_white" -> "icon_bilipai_white"
        "BiliPai Monet", "BiliPai 莫奈", "bilipai_monet" -> "icon_bilipai_monet"
        "Anime" -> "icon_anime"
        "Flat" -> "icon_flat"
        "Telegram Blue" -> "icon_telegram_blue"
        "Dark", "Telegram Dark" -> "icon_telegram_dark"
        "icon_headphone" -> "Headphone"
        else -> if (CANONICAL_APP_ICON_KEYS.contains(key)) key else DEFAULT_APP_ICON_KEY
    }
}

fun resolveAppIconLauncherAlias(
    packageName: String,
    rawKey: String?,
    splashIconVisible: Boolean = true
): String {
    val normalizedKey = normalizeAppIconKey(rawKey)
    val aliasMap = if (splashIconVisible) {
        LAUNCHER_ALIAS_SUFFIX_BY_KEY
    } else {
        NO_ICON_LAUNCHER_ALIAS_SUFFIX_BY_KEY
    }
    val aliasSuffix = aliasMap[normalizedKey]
        ?: aliasMap.getValue(DEFAULT_APP_ICON_KEY)
    return "$APP_ICON_COMPONENT_PACKAGE_NAME.$aliasSuffix"
}

fun allManagedAppIconLauncherAliases(packageName: String): Set<String> {
    return (LAUNCHER_ALIAS_SUFFIX_BY_KEY.values + NO_ICON_LAUNCHER_ALIAS_SUFFIX_BY_KEY.values)
        .map { aliasSuffix -> "$APP_ICON_COMPONENT_PACKAGE_NAME.$aliasSuffix" }
        .plus(APP_ICON_COMPAT_ALIAS_CLASS_NAME)
        .toSet()
}
