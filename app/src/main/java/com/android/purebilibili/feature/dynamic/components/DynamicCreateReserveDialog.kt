package com.android.purebilibili.feature.dynamic.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.android.purebilibili.core.ui.AppAlertDialog
import com.android.purebilibili.core.ui.AppDialogAction
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.components.AppFilterChip
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.AppTextButton
import com.android.purebilibili.core.ui.components.AppTextField
import com.android.purebilibili.data.model.response.DynamicCreatedReserve
import com.android.purebilibili.data.repository.DynamicCreateRepository
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun DynamicCreateReserveDialog(
    onDismiss: () -> Unit,
    onCreated: (DynamicCreatedReserve) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val initial = remember {
        Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, 20); set(Calendar.MINUTE, 0) }
    }
    var title by remember { mutableStateOf("") }
    var subType by remember { mutableIntStateOf(0) }
    var startAtMillis by remember { mutableLongStateOf(initial.timeInMillis) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }
    val startLabel = remember(startAtMillis) {
        val calendar = Calendar.getInstance().apply { timeInMillis = startAtMillis }
        "%d-%02d-%02d %02d:%02d".format(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH),
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE)
        )
    }

    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { AppText("添加直播预约") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small)
            ) {
                AppTextField(value = title, onValueChange = { title = it }, placeholder = "预约标题", singleLine = true)
                AppFilterChip(
                    selected = subType == 0,
                    onClick = { subType = 0 },
                    label = { AppText("公开直播") }
                )
                AppFilterChip(
                    selected = subType == 1,
                    onClick = { subType = 1 },
                    label = { AppText("大航海直播") }
                )
                AppTextButton(
                    onClick = {
                        val calendar = Calendar.getInstance().apply { timeInMillis = startAtMillis }
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        startAtMillis = Calendar.getInstance().apply {
                                            set(year, month, day, hour, minute, 0)
                                            set(Calendar.MILLISECOND, 0)
                                        }.timeInMillis
                                    },
                                    calendar.get(Calendar.HOUR_OF_DAY),
                                    calendar.get(Calendar.MINUTE),
                                    true
                                ).show()
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
                ) {
                    AppText("开播时间 $startLabel")
                }
                errorMessage?.let { AppText(it) }
            }
        },
        confirmButton = {
            AppDialogAction(
                onClick = {
                    if (submitting) return@AppDialogAction
                    submitting = true
                    errorMessage = null
                    scope.launch {
                        DynamicCreateRepository.createReserve(
                            title = title,
                            livePlanStartTimeSeconds = startAtMillis / 1000L,
                            subType = subType
                        ).fold(
                            onSuccess = { created ->
                                submitting = false
                                onCreated(created)
                            },
                            onFailure = { error ->
                                submitting = false
                                errorMessage = error.message ?: "创建失败"
                            }
                        )
                    }
                }
            ) {
                AppText(if (submitting) "创建中…" else "创建")
            }
        },
        dismissButton = {
            AppDialogAction(onClick = onDismiss) { AppText("取消") }
        }
    )
}