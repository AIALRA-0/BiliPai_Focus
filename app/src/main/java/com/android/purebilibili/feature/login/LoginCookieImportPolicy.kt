package com.android.purebilibili.feature.login

internal data class ImportedLoginCookies(
    val sessData: String,
    val csrf: String?,
    val buvid3: String?,
    val dedeUserId: String?,
    val values: Map<String, String>
) {
    fun toCookieHeader(): String = values.entries.joinToString(separator = "; ") { (name, value) ->
        "$name=$value"
    }
}

internal fun parseLoginCookieHeader(rawCookieHeader: String): ImportedLoginCookies? {
    val values = rawCookieHeader
        .lineSequence()
        .flatMap { line ->
            line.trim()
                .removePrefix("Cookie:")
                .removePrefix("cookie:")
                .removePrefix("Set-Cookie:")
                .removePrefix("set-cookie:")
                .split(';')
                .asSequence()
        }
        .map { it.trim() }
        .mapNotNull { segment ->
            val separator = segment.indexOf('=')
            if (separator <= 0) null else {
                segment.substring(0, separator).trim() to
                    segment.substring(separator + 1).trim().removeSurrounding("\"")
            }
        }
        .toMap()

    val sessData = values["SESSDATA"].orEmpty()
    if (sessData.isBlank()) return null

    return ImportedLoginCookies(
        sessData = sessData,
        csrf = values["bili_jct"],
        buvid3 = values["buvid3"],
        dedeUserId = values["DedeUserID"],
        values = values
    )
}
