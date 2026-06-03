package com.android.purebilibili.core.store

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PlayerSettingsMappingPolicyTest {

    @Test
    fun `setHwDecode keeps PlayerSettingsCache in sync`() {
        val source = File("src/main/java/com/android/purebilibili/core/store/SettingsManager.kt")
            .takeIf { it.exists() }
            ?: File("app/src/main/java/com/android/purebilibili/core/store/SettingsManager.kt")

        val text = source.readText()
        val setHwDecodeBody = Regex(
            pattern = """suspend fun setHwDecode\(context: Context, value: Boolean\) \{([\s\S]*?)\n    \}""",
            options = setOf(RegexOption.MULTILINE)
        ).find(text)?.groupValues?.get(1).orEmpty()

        assertTrue(
            "setHwDecode must update PlayerSettingsCache so player creation reads the latest hardware decoder setting",
            setHwDecodeBody.contains("PlayerSettingsCache.setHwDecodeEnabled(context, value)")
        )
    }
}
