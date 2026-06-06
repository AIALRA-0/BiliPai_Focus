package com.android.purebilibili.feature.login

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse

class LoginSensitiveLoggingPolicyTest {

    @Test
    fun loginDebugLogsDoNotPrintSessionOrVerificationSecrets() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/login/LoginViewModel.kt")
        val forbiddenSnippets = listOf(
            "Key: \$qrcodeKey",
            "gt=\${response.data.geetest?.gt}",
            "validate=\$validate",
            "+\$countryCode \$phone",
            "captchaKey=\${currentCaptchaKey}",
            "phone=\$currentPhone",
            "phone=\$phone",
            "code=\$code",
            "SESSDATA=\$sessData",
            "bili_jct=\${biliJct.isNotEmpty()}",
            "authCode=\${tvAuthCode.take(10)}",
            "获取 TV 二维码失败: code=\${response.code}, msg=\${response.message}",
            "TV 轮询状态: code=\${response.code}"
        )

        forbiddenSnippets.forEach { snippet ->
            assertFalse(
                source.contains(snippet),
                "Login logs must not print sensitive runtime values: $snippet"
            )
        }
    }

    @Test
    fun captchaLogsDoNotPrintVerificationSecrets() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/login/CaptchaManager.kt")
        val forbiddenSnippets = listOf(
            "gt=\$gt",
            "challenge=\$challenge",
            "validate=\$validate",
            "challenge=\$newChallenge",
            "Captcha failed via JS: \$error"
        )

        forbiddenSnippets.forEach { snippet ->
            assertFalse(
                source.contains(snippet),
                "Captcha logs must not print sensitive runtime values: $snippet"
            )
        }
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        val sourceFile = listOf(
            File(path),
            File(normalizedPath)
        ).firstOrNull { it.exists() }
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText()
    }
}
