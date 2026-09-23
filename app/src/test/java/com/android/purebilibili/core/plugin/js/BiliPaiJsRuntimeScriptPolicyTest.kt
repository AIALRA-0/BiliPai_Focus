package com.android.purebilibili.core.plugin.js

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class BiliPaiJsRuntimeScriptPolicyTest {

    @Test
    fun previewExpressionRequiresNativeBiliPaiPluginManifest() {
        val expression = buildBiliPaiJsPreviewExpression()

        assertContains(expression, "BiliPaiPlugin")
        assertContains(expression, "未找到 BiliPaiPlugin")
    }

    @Test
    fun executionScriptExposesNativeBiliPaiApiOnly() {
        val script = buildBiliPaiJsExecutionScript(
            callId = "call",
            pluginScript = "globalThis.BiliPaiPlugin = {};",
            expression = "return globalThis.BiliPaiPlugin;"
        )

        assertContains(script, "window.BiliPai")
        assertContains(script, "BiliPaiHttpNative.get")
        assertContains(script, "BiliPaiStorageNative.set")
        assertFalse(script.contains("window.Widget"))
    }

    @Test
    fun moduleExpressionCanCallNativePluginFunction() {
        val expression = buildBiliPaiJsModuleExpression(
            functionName = "loadChannels",
            paramsJson = "{}"
        )

        assertContains(expression, "plugin[functionName]")
        assertContains(expression, "loadChannels")
    }
}
