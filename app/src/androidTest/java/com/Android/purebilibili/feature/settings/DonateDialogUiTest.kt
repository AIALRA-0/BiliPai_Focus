package com.android.purebilibili.feature.settings

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.purebilibili.R
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle
import com.android.purebilibili.core.ui.AppThemeConfig
import com.android.purebilibili.core.ui.LocalAppThemeConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import top.yukonga.miuix.kmp.theme.MiuixTheme

@RunWith(AndroidJUnit4::class)
class DonateDialogUiTest {
    @get:Rule
    val rule = createComposeRule()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private fun text(id: Int) = context.getString(id)

    @Test
    fun savingNeverOpensWechatUntilSuccessfulAndPreviewDoesNotDismiss() {
        val saving = mutableStateOf(false)
        val saved = mutableStateOf(false)
        var saves = 0
        var opens = 0
        var previews = 0
        var dismisses = 0
        rule.setContent {
            MaterialTheme {
                MiuixTheme {
                    DonationCardContent(
                        wide = false,
                        saving = saving.value,
                        saved = saved.value,
                        error = null,
                        onSave = { saves++; saving.value = true },
                        onOpenWechat = { opens++ },
                        onEnlarge = { previews++ },
                        onDismiss = { dismisses++ },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        rule.onNodeWithContentDescription(text(R.string.settings_donate_qr_description))
            .performScrollTo().performClick()
        rule.runOnIdle { assertEquals(1, previews); assertEquals(0, dismisses) }
        rule.onNodeWithText(text(R.string.settings_donate_open_wechat)).assertDoesNotExist()
        rule.onNodeWithText(text(R.string.settings_donate_save)).performScrollTo().performClick()
        rule.onNodeWithText(text(R.string.settings_donate_saving)).assertIsNotEnabled()
        rule.runOnIdle {
            assertEquals(1, saves)
            assertEquals(0, opens)
            saving.value = false
            saved.value = true
        }
        rule.onNodeWithText(text(R.string.settings_donate_open_wechat)).performScrollTo().performClick()
        rule.runOnIdle { assertEquals(1, opens); assertEquals(0, dismisses) }
        rule.onNodeWithText(text(R.string.settings_donate_save_again)).performScrollTo()
    }

    @Test
    fun failedSaveExplainsRetryWithoutShowingWechatAction() {
        var retries = 0
        rule.setContent {
            MaterialTheme {
                MiuixTheme {
                    DonationCardContent(
                        wide = false,
                        saving = false,
                        saved = false,
                        error = R.string.settings_donate_save_failed,
                        onSave = { retries++ },
                        onOpenWechat = { error("No successful save yet") },
                        onEnlarge = {},
                        onDismiss = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        rule.onNodeWithText(text(R.string.settings_donate_save_failed)).performScrollTo()
        rule.onNodeWithText(text(R.string.settings_donate_open_wechat)).assertDoesNotExist()
        rule.onNodeWithText(text(R.string.settings_donate_save)).performScrollTo().performClick()
        rule.runOnIdle { assertEquals(1, retries) }
    }

    @Test
    fun originalQrKeepsWhiteBackgroundInLightDarkAndBothNativeStyles() {
        val dark = mutableStateOf(false)
        val style = mutableStateOf(AppUiStyle.MATERIAL3)
        val glass = mutableStateOf(false)
        rule.setContent {
            MaterialTheme(colorScheme = if (dark.value) darkColorScheme() else lightColorScheme()) {
                MiuixTheme {
                    CompositionLocalProvider(
                        LocalAppUiStyle provides style.value,
                        LocalAppThemeConfig provides AppThemeConfig(liquidGlassEnabled = glass.value),
                    ) {
                        Box(Modifier.fillMaxSize()) {
                            DonationCardContent(false, false, false, null, {}, {}, {}, {})
                        }
                    }
                }
            }
        }
        for (mode in listOf(false, true)) for (uiStyle in AppUiStyle.entries) for (useGlass in listOf(false, true)) {
            rule.runOnIdle { dark.value = mode; style.value = uiStyle; glass.value = useGlass }
            val pixels = rule.onNodeWithContentDescription(text(R.string.settings_donate_qr_description))
                .performScrollTo().captureToImage().toPixelMap()
            val quietZone = pixels[pixels.width / 2, pixels.height / 10]
            assertTrue(quietZone.red > 0.95f && quietZone.green > 0.95f && quietZone.blue > 0.95f)
            rule.onNodeWithText(text(R.string.settings_donate_open_wechat)).assertDoesNotExist()
        }
    }
}
