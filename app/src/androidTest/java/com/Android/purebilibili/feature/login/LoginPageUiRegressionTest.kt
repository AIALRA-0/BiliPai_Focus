package com.Android.purebilibili.feature.login

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.purebilibili.feature.login.LoginMethod
import com.android.purebilibili.feature.login.LoginPage
import com.android.purebilibili.feature.login.LoginState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginPageUiRegressionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun compactLoginPage_keepsEveryLoginMethodReachable() {
        composeTestRule.setContent {
            var selectedMethod by remember { mutableStateOf(LoginMethod.TV_QR) }
            MaterialTheme {
                Box(modifier = Modifier.size(width = 320.dp, height = 360.dp)) {
                    LoginPage(
                        state = LoginState.PhoneIdle,
                        selectedMethod = selectedMethod,
                        onMethodSelected = { selectedMethod = it },
                        onClose = {},
                        onRefreshQr = {},
                        onRequestSms = { _, _ -> },
                        onSubmitSms = {},
                        onRequestPassword = { _, _ -> },
                        onImportCookie = {},
                        onContinueWithStandardSession = {},
                        onAuthorizeHighQuality = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("密码登录").performClick()
        composeTestRule.onNodeWithText("验证并登录").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("短信登录").performClick()
        composeTestRule.onNodeWithText("获取验证码").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Cookie 导入").performClick()
        composeTestRule.onNodeWithText("验证并导入").performScrollTo().assertIsDisplayed()
    }
}
