package com.android.purebilibili.feature.video.ui.overlay

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import com.android.purebilibili.core.plugin.BuiltinPluginIds
import com.android.purebilibili.core.plugin.PluginStore
import com.android.purebilibili.data.model.response.SponsorCategory
import com.android.purebilibili.feature.plugin.SponsorBlockConfig
import com.android.purebilibili.feature.plugin.SponsorBlockSegmentBehavior
import com.android.purebilibili.feature.plugin.loadSponsorBlockConfig
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE, application = Application::class)
class PiliSponsorMenuSessionTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        val directory = Files.createTempDirectory("sponsor-menu-config").toFile()
        context = object : ContextWrapper(RuntimeEnvironment.getApplication()) {
            override fun getApplicationContext(): Context = this
            override fun getFilesDir(): File = directory
        }
    }

    @Test
    fun storedCommunityServerAndIdentityAreNotReplacedWithDefaults() = runTest {
        PluginStore.setConfigJson(context, BuiltinPluginIds.SPONSOR_BLOCK, """{
            "serverBaseUrl":"https://community.example/api",
            "userId":"existing-persistent-community-user",
            "unrecognizedFutureOption":true
        }""")
        val config = loadSponsorBlockConfig(context)
        assertEquals("https://community.example/api", config.serverBaseUrl)
        assertEquals("existing-persistent-community-user", config.userId)
    }

    @Test
    fun dialogUsesActualPerCategoryBehaviorAndColors() = runTest {
        PluginStore.setConfigJson(context, BuiltinPluginIds.SPONSOR_BLOCK, """{
            "userId":"existing-user",
            "categoryBehaviorRaw":{"sponsor":"MARKER_ONLY","intro":"SKIP_ONCE"},
            "categoryColorHex":{"sponsor":"#123abc"}
        }""")
        val config = loadSponsorBlockConfig(context)
        assertEquals(SponsorBlockSegmentBehavior.MARKER_ONLY, config.behaviorFor(SponsorCategory.SPONSOR))
        assertEquals(SponsorBlockSegmentBehavior.SKIP_ONCE, config.behaviorFor(SponsorCategory.INTRO))
        assertEquals("#123ABC", config.categoryColorHex[SponsorCategory.SPONSOR])
    }

    @Test
    fun legacyPluginSettingsUseTheSameMigrationAsPlaybackSession() = runTest {
        PluginStore.setConfigJson(context, BuiltinPluginIds.SPONSOR_BLOCK, """{
            "userId":"existing-user", "autoSkip":false, "skipSponsor":false
        }""")
        val config = loadSponsorBlockConfig(context)
        assertEquals(SponsorBlockSegmentBehavior.DISABLED, config.behaviorFor(SponsorCategory.SPONSOR))
        assertEquals(SponsorBlockSegmentBehavior.MANUAL, config.behaviorFor(SponsorCategory.INTRO))
    }

    @Test
    fun newConfigurationGetsAPersistedIdentityThroughSharedNormalization() = runTest {
        PluginStore.setConfigJson(context, BuiltinPluginIds.SPONSOR_BLOCK, "{}")
        val config = loadSponsorBlockConfig(context)
        assertTrue(config.userId.isNotBlank())
        val persisted = Json.decodeFromString<SponsorBlockConfig>(
            PluginStore.getConfigJson(context, BuiltinPluginIds.SPONSOR_BLOCK)!!,
        )
        assertEquals(config.userId, persisted.userId)
    }

    @Test
    fun blankStoredIdentityReusesCurrentNonemptyIdentity() = runTest {
        PluginStore.setConfigJson(context, BuiltinPluginIds.SPONSOR_BLOCK, """{"userId":"  "}""")
        val config = loadSponsorBlockConfig(context, SponsorBlockConfig(userId = "current-user"))
        assertEquals("current-user", config.userId)
        assertEquals(config, loadSponsorBlockConfig(context))
    }

    @Test
    fun concurrentFirstConfigLoadsShareThePersistedNonemptyIdentity() = runTest {
        PluginStore.setConfigJson(context, BuiltinPluginIds.SPONSOR_BLOCK, "{}")
        val start = CompletableDeferred<Unit>()
        val loads = listOf("playback-user", "settings-user").map { currentId ->
            async(Dispatchers.IO) {
                start.await()
                loadSponsorBlockConfig(context, SponsorBlockConfig(userId = currentId))
            }
        }
        start.complete(Unit)
        val configs = loads.awaitAll()
        val persisted = Json.decodeFromString<SponsorBlockConfig>(
            PluginStore.getConfigJson(context, BuiltinPluginIds.SPONSOR_BLOCK)!!,
        )
        assertTrue(persisted.userId.isNotBlank())
        assertTrue(configs.all { it.userId == persisted.userId })
    }

    @Test
    fun malformedStoredConfigurationDoesNotSilentlyResetServerOrIdentity() = runTest {
        PluginStore.setConfigJson(context, BuiltinPluginIds.SPONSOR_BLOCK, "{not-json}")
        assertFailsWith<SerializationException> { loadSponsorBlockConfig(context) }
        assertEquals("{not-json}", PluginStore.getConfigJson(context, BuiltinPluginIds.SPONSOR_BLOCK))
    }
}
