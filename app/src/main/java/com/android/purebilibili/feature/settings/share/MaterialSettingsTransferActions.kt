package com.android.purebilibili.feature.settings.share

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.components.AppButton
import com.android.purebilibili.core.ui.components.AppOutlinedButton
import androidx.compose.material3.Text

@Composable
internal fun MaterialSettingsTransferActions(
    enabled: Boolean,
    onExport: () -> Unit,
    onShare: () -> Unit,
    onImport: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 独立按钮在窄分屏与大字号下按需增高，四个操作共用原有回调和忙碌状态。
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AppButton(onClick = onExport, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text("导出到文件")
        }
        AppOutlinedButton(onClick = onShare, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text("分享导出文件")
        }
        AppOutlinedButton(onClick = onImport, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text("从文件导入")
        }
        AppOutlinedButton(onClick = onSave, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text("保存当前配置")
        }
    }
}
