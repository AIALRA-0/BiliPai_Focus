package com.android.purebilibili.core.plugin.js

import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BiliPaiJsNativeExampleTest {

    @Test
    fun tvLiveExampleUsesNativeBiliPaiJsApi() {
        val script = findRepoFile("examples/plugins/tv-live.bilipai.js").readText(Charsets.UTF_8)

        assertContains(script, "BiliPaiPlugin")
        assertContains(script, "BiliPai.http.get")
        assertContains(script, "BiliPai.storage.set")
        assertContains(script, "EXTERNAL_MEDIA_PLAYBACK")
        assertContains(script, "iconLibraryUrl")
        assertContains(script, "iconProxyTemplate")
        assertContains(script, "useGeneratedLogoFallback")
        assertContains(script, "preferGroupCategory")
        assertContains(script, "coverUrls")
        assertContains(script, "buildIconCandidates")
        assertContains(script, "parseIconLibrary")
        assertContains(script, "parsed.icons")
        assertContains(script, "{url}")
        assertContains(script, "parseExtInfAttributes")
        assertContains(script, "group-title")
        assertContains(script, "tvg-id")
        assertContains(script, "tvg-name")
        assertContains(script, "tvg-logo")
        assertContains(script, "#EXTGRP:")
        assertContains(script, "url-epg")
        assertContains(script, "x-tvg-url")
        assertContains(script, "guessCategoryFromGroup")
        assertContains(script, "isValidStreamUrl")
        assertContains(script, "udp")
        assertContains(script, "mms")
        assertFalse(script.contains("WidgetMetadata"))
        assertFalse(script.contains("Widget.http"))
        assertTrue(script.contains("function loadChannels"))
    }

    private fun findRepoFile(relativePath: String): File {
        var dir = File(System.getProperty("user.dir") ?: ".").absoluteFile
        repeat(6) {
            val candidate = File(dir, relativePath)
            if (candidate.exists()) return candidate
            dir = dir.parentFile ?: dir
        }
        throw IllegalStateException("找不到示例文件: $relativePath")
    }
}
