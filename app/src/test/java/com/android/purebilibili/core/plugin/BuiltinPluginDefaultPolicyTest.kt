package com.android.purebilibili.core.plugin

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BuiltinPluginDefaultPolicyTest {

    @Test
    fun castPluginsAreEnabledByDefault() {
        assertTrue(resolvePluginDefaultEnabled("dlna_cast"))
        assertTrue(resolvePluginDefaultEnabled("google_cast"))
    }

    @Test
    fun optionalPluginsStayDisabledByDefault() {
        assertFalse(resolvePluginDefaultEnabled("sponsor_block"))
        assertFalse(resolvePluginDefaultEnabled("ad_filter"))
        assertFalse(resolvePluginDefaultEnabled("unknown_plugin"))
    }
}
