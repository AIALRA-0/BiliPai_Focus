package com.android.purebilibili.core.plugin.json

import android.app.Application
import android.content.Context
import java.io.File
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE, application = Application::class)
class JsonPluginStorageConcurrencyTest {
    @Test fun concurrentRestoreAndEditKeepNewRulesAndSkipDamagedFiles() = runTest {
        val context = RuntimeEnvironment.getApplication() as Context
        val directory = File(context.filesDir, "json_plugins").apply { mkdirs() }
        File(directory, "restore_test.json").writeText(
            """{"id":"restore_test","name":"旧规则","type":"feed","rules":[{"field":"title","op":"contains","value":"旧词","action":"hide"}]}"""
        )
        File(directory, "damaged.json").writeText("{broken")
        val edited = JsonRulePlugin(
            id = "restore_test", name = "已编辑", type = "feed",
            rules = listOf(Rule(field = "title", op = "contains", value = JsonPrimitive("新词"), action = "hide")),
        )
        listOf(
            async { JsonPluginManager.initialize(context) },
            async { JsonPluginManager.initialize(context) },
            async {
                JsonPluginManager.initialize(context)
                JsonPluginManager.updatePlugin(edited)
                JsonPluginManager.setEnabled(edited.id, false)
            },
        ).awaitAll()
        val restored = JsonPluginManager.plugins.value.single { it.plugin.id == edited.id }
        assertEquals(edited, restored.plugin)
        assertFalse(restored.enabled)
        assertTrue(File(directory, "restore_test.json").readText().contains("新词"))
        assertFalse(JsonPluginManager.plugins.value.any { it.plugin.id == "damaged" })
        JsonPluginManager.removePlugin(edited.id)
        assertFalse(File(directory, "restore_test.json").exists())
    }
}
