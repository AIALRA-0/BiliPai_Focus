package com.android.purebilibili.core.plugin

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStoreFile
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.store.settingsDataStore
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
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
class SponsorBlockEnabledStoreTest {
    @Test
    fun settingsKeyWinsConflictingMirrorAndUnifiedToggleSurvivesRegistryRecreation() = runTest {
        val directory = Files.createTempDirectory("sponsor-enabled-store").toFile()
        val context = object : ContextWrapper(RuntimeEnvironment.getApplication()) {
            override fun getApplicationContext(): Context = this
            override fun getFilesDir(): File = directory
        }
        val settingsKey = booleanPreferencesKey("sponsor_block_enabled")
        context.settingsDataStore.edit { it[settingsKey] = true }
        val mirrorJob = SupervisorJob()
        val mirror = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(mirrorJob + Dispatchers.IO),
            produceFile = { context.preferencesDataStoreFile("plugin_prefs") }
        )
        try {
            mirror.edit { it[booleanPreferencesKey("plugin_enabled_sponsor_block")] = false }
            fun registry() = BuiltinPluginRegistry(
                readEnabled = { PluginStore.isEnabled(context, it) },
                writeEnabled = { id, enabled -> PluginStore.setEnabled(context, id, enabled) },
                onFailure = { _, error -> throw AssertionError(error) },
                onEnabledChanged = { _, _ -> }
            )
            fun plugin() = object : BuiltinPlugin {
                override val id = BuiltinPluginIds.SPONSOR_BLOCK
                override val name = "Sponsor"
                override val version = "1"
                override val description = ""
            }
            val first = registry()
            first.register(plugin())
            assertTrue(first.pluginsFlow.value.single().enabled)
            assertEquals(false, mirror.data.first()[booleanPreferencesKey("plugin_enabled_sponsor_block")])
            first.setEnabled(BuiltinPluginIds.SPONSOR_BLOCK, false)
            assertFalse(first.pluginsFlow.value.single().enabled)
            assertFalse(SettingsManager.getSponsorBlockEnabled(context).first())
            assertEquals(false, context.settingsDataStore.data.first()[settingsKey])
            assertEquals(
                JsonPrimitive(false),
                SettingsManager.exportShareableSettingsSnapshot(context)["sponsor_block_enabled"]
            )
            val restarted = registry()
            restarted.register(plugin())
            assertFalse(restarted.pluginsFlow.value.single().enabled)
        } finally {
            mirrorJob.cancelAndJoin()
        }
    }
}
