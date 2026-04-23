package com.android.purebilibili.core.store

const val APP_ICON_COMPONENT_PACKAGE_NAME = "com.android.purebilibili"
const val APP_ICON_COMPAT_ALIAS_CLASS_NAME = "$APP_ICON_COMPONENT_PACKAGE_NAME.MainActivityAlias3D"

const val DEFAULT_APP_ICON_KEY = "icon_3d"

private val CANONICAL_APP_ICON_KEYS = setOf(
    "icon_3d",
    "icon_bilipai",
    "icon_blue",
    "icon_neon",
    "icon_anime",
    "icon_flat",
    "icon_telegram_blue",
    "icon_telegram_green",
    "icon_telegram_pink",
    "icon_telegram_purple",
    "icon_telegram_dark",
    "icon_telegram_blue_coin",
    "Yuki",
    "Headphone"
)

private val LAUNCHER_ALIAS_SUFFIX_BY_KEY = mapOf(
    "icon_3d" to "MainActivityAlias3DLauncher",
    "icon_bilipai" to "MainActivityAliasBiliPai",
    "icon_blue" to "MainActivityAliasBlue",
    "icon_neon" to "MainActivityAliasNeon",
    "icon_anime" to "MainActivityAliasAnime",
    "icon_flat" to "MainActivityAliasFlat",
    "icon_telegram_blue" to "MainActivityAliasTelegramBlue",
    "icon_telegram_green" to "MainActivityAliasGreen",
    "icon_telegram_pink" to "MainActivityAliasPink",
    "icon_telegram_purple" to "MainActivityAliasPurple",
    "icon_telegram_dark" to "MainActivityAliasDark",
    "icon_telegram_blue_coin" to "MainActivityAliasTelegramBlueCoin",
    "Yuki" to "MainActivityAliasYuki",
    "Headphone" to "MainActivityAliasHeadphone"
)

fun normalizeAppIconKey(rawKey: String?): String {
    val key = rawKey?.trim().orEmpty()
    if (key.isEmpty()) return DEFAULT_APP_ICON_KEY

    return when (key) {
        "default", "3D" -> "icon_3d"
        "BiliPai", "bilipai", "Icon BiliPai" -> "icon_bilipai"
        "Anime" -> "icon_anime"
        "Blue" -> "icon_blue"
        "Flat" -> "icon_flat"
        "Neon" -> "icon_neon"
        "Telegram Blue" -> "icon_telegram_blue"
        "Green", "Telegram Green" -> "icon_telegram_green"
        "Pink", "Telegram Pink" -> "icon_telegram_pink"
        "Purple", "Telegram Purple" -> "icon_telegram_purple"
        "Dark", "Telegram Dark" -> "icon_telegram_dark"
        "Telegram Blue Coin", "Blue Coin" -> "icon_telegram_blue_coin"
        "icon_headphone" -> "Headphone"
        else -> if (CANONICAL_APP_ICON_KEYS.contains(key)) key else DEFAULT_APP_ICON_KEY
    }
}

fun resolveAppIconLauncherAlias(packageName: String, rawKey: String?): String {
    val normalizedKey = normalizeAppIconKey(rawKey)
    val aliasSuffix = LAUNCHER_ALIAS_SUFFIX_BY_KEY[normalizedKey]
        ?: LAUNCHER_ALIAS_SUFFIX_BY_KEY.getValue(DEFAULT_APP_ICON_KEY)
    return "$APP_ICON_COMPONENT_PACKAGE_NAME.$aliasSuffix"
}

fun allManagedAppIconLauncherAliases(packageName: String): Set<String> {
    return LAUNCHER_ALIAS_SUFFIX_BY_KEY.values
        .map { aliasSuffix -> "$APP_ICON_COMPONENT_PACKAGE_NAME.$aliasSuffix" }
        .toSet()
}
