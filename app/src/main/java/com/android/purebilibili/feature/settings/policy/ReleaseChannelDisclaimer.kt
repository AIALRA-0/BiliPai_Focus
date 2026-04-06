package com.android.purebilibili.feature.settings

import com.android.purebilibili.BuildConfig
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

const val OFFICIAL_GITHUB_URL = "https://github.com/jay3-yy/BiliPai/"
val FOCUS_GITHUB_URL: String
    get() = BuildConfig.FOCUS_REPOSITORY_URL
const val OFFICIAL_TELEGRAM_GROUP_URL = "https://t.me/BiliPaii"
const val OFFICIAL_TELEGRAM_CHANNEL_URL = "https://t.me/BiliPai"
const val RELEASE_DISCLAIMER_ACK_KEY = "release_disclaimer_ack_v1"

@Composable
fun ReleaseChannelDisclaimerDialog(
    onDismiss: () -> Unit,
    onOpenGithub: () -> Unit,
    onOpenFocusGithub: () -> Unit,
    onOpenTelegramGroup: () -> Unit,
    onOpenTelegramChannel: () -> Unit,
    title: String = "免责声明"
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = "本应用仅用于学习与交流\n\n" +
                    "官方原版发布渠道：GitHub、Telegram 交流群与 Telegram 频道\n" +
                    "Focus 定制版发布渠道：Focus GitHub\n" +
                    "Focus 版本只会跟随 Focus 分支检查与接收更新，不会接收官方原版更新推送\n" +
                    "除上述渠道外，不存在任何其他发布途径\n\n" +
                    "请勿安装来源不明的安装包，以避免账号与设备安全风险"
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("我已知晓")
            }
        },
        dismissButton = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextButton(onClick = onOpenGithub) { Text("官方 GitHub") }
                TextButton(onClick = onOpenFocusGithub) { Text("Focus GitHub") }
                TextButton(onClick = onOpenTelegramGroup) { Text("Telegram 交流群") }
                TextButton(onClick = onOpenTelegramChannel) { Text("Telegram 频道") }
            }
        }
    )
}
