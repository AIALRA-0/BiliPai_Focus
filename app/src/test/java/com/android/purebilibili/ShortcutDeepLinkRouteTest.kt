package com.android.purebilibili

import com.android.purebilibili.navigation.ScreenRoutes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ShortcutDeepLinkRouteTest {

    @Test
    fun resolveShortcutRoute_supportsReleaseSmokeEntries() {
        assertEquals(ScreenRoutes.Login.route, resolveShortcutRoute("login"))
        assertEquals(ScreenRoutes.PlaybackSettings.route, resolveShortcutRoute("playback"))
        assertEquals(ScreenRoutes.PluginsSettings.createRoute(), resolveShortcutRoute("plugins"))
        assertEquals(ScreenRoutes.WatchLater.route, resolveShortcutRoute("watch_later"))
    }

    @Test
    fun resolveShortcutRoute_returnsNullForUnknownHost() {
        assertNull(resolveShortcutRoute("unknown"))
    }

    @Test
    fun resolvePluginInstallDeepLink_parsesInstallUrl() {
        val request = resolvePluginInstallDeepLink("bilipai://plugin/install?url=https%3A%2F%2Fexample.com%2Fplugins%2Fsample.json")

        assertNotNull(request)
        assertEquals("https://example.com/plugins/sample.json", request.pluginUrl)
    }

    @Test
    fun resolvePluginInstallDeepLink_rejectsInvalidOrMissingUrl() {
        val missingUrl = "bilipai://plugin/install"
        val invalidScheme = "bilipai://plugin/install?url=ftp%3A%2F%2Fexample.com%2Fplugin.json"
        val invalidHost = "https://evil.example.com/plugin/install?url=https%3A%2F%2Fexample.com%2Fplugin.json"

        assertNull(resolvePluginInstallDeepLink(missingUrl))
        assertNull(resolvePluginInstallDeepLink(invalidScheme))
        assertNull(resolvePluginInstallDeepLink(invalidHost))
    }

    @Test
    fun resolvePluginInstallDeepLink_supportsHttpsInstallPageLinks() {
        val request = resolvePluginInstallDeepLink(
            "https://bilipai.app/plugin/install?url=https%3A%2F%2Fexample.com%2Fplugins%2Fsample.json"
        )

        assertNotNull(request)
        assertEquals("https://example.com/plugins/sample.json", request.pluginUrl)
    }
}
