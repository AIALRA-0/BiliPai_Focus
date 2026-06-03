package com.android.purebilibili.core.util

import java.net.URLDecoder
import java.nio.charset.Charset

internal fun decodeUrlComponentCompat(
    value: String,
    charset: Charset = Charsets.UTF_8
): String {
    return URLDecoder.decode(value, charset.name())
}
