package com.android.purebilibili.feature.login

import com.android.purebilibili.plugin.sdk.PluginCapability
import com.android.purebilibili.core.plugin.js.BiliPaiJsPluginManifest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BiliPaiTransferPayloadTest {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = false }

    @Test
    fun payload_jsonRoundTripsAllContentTypes() {
        val payload = BiliPaiTransferPayload(
            session = BiliPaiSessionBundle(mid = 42, sessData = "session", csrf = "csrf"),
            settingsJson = "{\"schemaVersion\":1}",
            jsonRules = BiliPaiTransferredJsonRules(
                sharedPrefsXml = "<map/>",
                files = listOf(BiliPaiTransferredFile("rule.json", "{}")),
            ),
            jsPlugins = listOf(
                BiliPaiTransferredJsPlugin(
                    manifest = BiliPaiJsPluginManifest(id = "p1", title = "插件"),
                    script = "console.log(1)",
                    grantedCapabilities = setOf(PluginCapability.NETWORK),
                    enabled = true,
                )
            ),
            skins = listOf(BiliPaiTransferredPackage("皮肤", "AAEC")),
            kotlinPlugins = listOf(BiliPaiTransferredPackage("外部插件", "AAED")),
        )
        val decoded = json.decodeFromString(
            BiliPaiTransferPayload.serializer(), json.encodeToString(BiliPaiTransferPayload.serializer(), payload))
        assertEquals(payload, decoded)
        assertEquals(BILIPAI_TRANSFER_PAYLOAD_VERSION, decoded.version)
    }

    @Test
    fun unknownPayloadVersion_isFlaggedByConstantMismatch() {
        val decoded = json.decodeFromString(
            BiliPaiTransferPayload.serializer(),
            """{"version":99,"session":{"mid":1,"sessData":"s"}}""",
        )
        assertTrue(decoded.version != BILIPAI_TRANSFER_PAYLOAD_VERSION)
    }

    @Test
    fun emptySelection_meansSessionOnlyPayload() {
        assertTrue(BiliPaiTransferContentSelection().isNothingSelected())
        assertTrue(!BiliPaiTransferContentSelection(settings = true).isNothingSelected())
        assertTrue(!BiliPaiTransferContentSelection(jsPluginIds = setOf("p1")).isNothingSelected())
    }
}
