// 文件路径: core/util/PermissionHelper.kt
package com.android.purebilibili.core.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
//  Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.android.purebilibili.core.theme.BiliPink

/**
 *  权限请求 Composable - 带说明对话框
 * 
 * 使用方式:
 * ```
 * val storagePermission = rememberPermissionState(
 *     permission = Manifest.permission.WRITE_EXTERNAL_STORAGE,
 *     rationaleTitle = "需要存储权限",
 *     rationaleMessage = "保存图片到相册需要存储权限",
 *     onPermissionResult = { granted -> if (granted) saveImage() }
 * )
 * 
 * Button(onClick = { storagePermission.request() }) { ... }
 * ```
 */
@Composable
fun rememberPermissionState(
    permission: String,
    rationaleTitle: String,
    rationaleMessage: String,
    rationaleIcon: androidx.compose.ui.graphics.vector.ImageVector = CupertinoIcons.Default.Checkmark,
    onPermissionResult: (Boolean) -> Unit = {}
): PermissionState {
    val context = LocalContext.current
    
    // 权限请求回调
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showRationale by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onPermissionResult(granted)
        if (!granted) {
            // 如果用户拒绝且不再询问，提示去设置中开启
            if (!shouldShowRationale(context, permission)) {
                showSettingsDialog = true
            }
        }
    }
    
    // 权限说明对话框
    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            icon = { Icon(rationaleIcon, contentDescription = null, tint = BiliPink) },
            title = { Text(rationaleTitle) },
            text = { Text(rationaleMessage) },
            confirmButton = {
                Button(
                    onClick = {
                        showRationale = false
                        permissionLauncher.launch(permission)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BiliPink)
                ) {
                    Text("授权")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRationale = false }) {
                    Text("取消")
                }
            }
        )
    }
    
    // 已永久拒绝，引导去设置
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            icon = { Icon(CupertinoIcons.Default.Checkmark, contentDescription = null, tint = BiliPink) },
            title = { Text("权限已关闭") },
            text = { Text("您已拒绝该权限；如需使用此功能，请在系统设置中手动开启权限") },
            confirmButton = {
                Button(
                    onClick = {
                        showSettingsDialog = false
                        openAppSettings(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BiliPink)
                ) {
                    Text("去设置")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
    
    return remember(permission) {
        PermissionState(
            permission = permission,
            checkPermission = { checkPermission(context, permission) },
            requestPermission = {
                when {
                    // 已授权
                    checkPermission(context, permission) -> {
                        onPermissionResult(true)
                    }
                    // 用户曾拒绝过 -> 显示说明对话框再请求
                    shouldShowRationale(context, permission) -> {
                        showRationale = true
                    }
                    // 首次请求或永久拒绝 -> 直接请求（系统处理）
                    else -> {
                        permissionLauncher.launch(permission)
                    }
                }
            }
        )
    }
}

/**
 * 权限状态封装
 */
class PermissionState(
    val permission: String,
    private val checkPermission: () -> Boolean,
    private val requestPermission: () -> Unit
) {
    /** 检查是否已授权 */
    val isGranted: Boolean get() = checkPermission()
    
    /** 请求权限（会显示说明对话框） */
    fun request() = requestPermission()
    
    /** 如果有权限则执行，否则请求权限 */
    fun launchWithPermission(block: () -> Unit) {
        if (isGranted) {
            block()
        } else {
            requestPermission()
        }
    }
}

/**
 * 存储权限快捷方法
 */
@Composable
fun rememberStoragePermissionState(
    onPermissionResult: (Boolean) -> Unit = {}
): PermissionState {
    return rememberPermissionState(
        permission = Manifest.permission.WRITE_EXTERNAL_STORAGE,
        rationaleTitle = "需要存储权限",
        rationaleMessage = "保存图片到相册需要访问设备存储空间；授权后即可将喜欢的图片保存到手机",
        onPermissionResult = onPermissionResult
    )
}

/**
 * 通知权限快捷方法
 */
@Composable
fun rememberNotificationPermissionState(
    onPermissionResult: (Boolean) -> Unit = {}
): PermissionState {
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.POST_NOTIFICATIONS
    } else {
        // Android 13 以下不需要运行时请求
        return remember {
            PermissionState(
                permission = "",
                checkPermission = { true },
                requestPermission = { onPermissionResult(true) }
            )
        }
    }
    
    return rememberPermissionState(
        permission = permission,
        rationaleTitle = "开启通知",
        rationaleMessage = "开启通知后，您可以在后台播放视频时通过通知栏控制播放",
        onPermissionResult = onPermissionResult
    )
}

// --- 工具函数 ---

private fun checkPermission(context: Context, permission: String): Boolean {
    // Android 10+ 使用 MediaStore，不需要存储权限
    if (permission == Manifest.permission.WRITE_EXTERNAL_STORAGE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        return true
    }
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

private fun shouldShowRationale(context: Context, permission: String): Boolean {
    // 🔧 [修复] 遍历 ContextWrapper 链安全获取 Activity
    // 在 Dialog 等环境中 Context 可能被多层包装
    var ctx: Context = context
    while (ctx is android.content.ContextWrapper) {
        if (ctx is android.app.Activity) {
            return androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(ctx, permission)
        }
        ctx = ctx.baseContext
    }
    return false
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

/**
 * 检查存储权限是否需要且已授予
 */
fun isStoragePermissionGranted(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        true // Android 10+ 不需要存储权限
    } else {
        ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
}
