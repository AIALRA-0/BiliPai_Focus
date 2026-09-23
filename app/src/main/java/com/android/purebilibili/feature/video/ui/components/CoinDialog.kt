// File: feature/video/ui/components/CoinDialog.kt
package com.android.purebilibili.feature.video.ui.components
import com.android.purebilibili.core.ui.components.AppText

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.AppAlertDialog
import com.android.purebilibili.core.ui.components.AppButton
import com.android.purebilibili.core.ui.components.AppCheckbox
import com.android.purebilibili.core.ui.components.AppFilterChip
import com.android.purebilibili.core.ui.components.AppTextButton

/**
 * Coin Dialog Component
 * 
 * Dialog for giving coins to a video.
 * 
 * Requirement Reference: AC3.2 - Coin dialog in dedicated file
 */

/**
 * Coin Dialog
 */
@Composable
fun CoinDialog(
    visible: Boolean,
    currentCoinCount: Int,  // Already given coins 0/1/2
    userBalance: Double?,    // [New] Current user coin balance (null = loading)
    onDismiss: () -> Unit,
    onConfirm: (count: Int, alsoLike: Boolean) -> Unit
) {
    if (!visible) return
    
    var selectedCount by remember { mutableIntStateOf(1) }
    var alsoLike by remember { mutableStateOf(true) }
    
    val maxCoins = 2 - currentCoinCount  // Remaining coins that can be given
    
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Column {
                AppText("\u6295\u5e01", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                val balanceText = when {
                    userBalance == null -> "加载中..."
                    userBalance == -1.0 -> "加载失败"
                    userBalance == -2.0 -> "网络错误"
                    userBalance == -3.0 -> "未登录"
                    userBalance == -4.0 -> "Token丢失"
                    else -> "余额: $userBalance"
                }
                AppText(
                    balanceText, 
                    style = MaterialTheme.typography.labelMedium, 
                    color = if (userBalance != null && userBalance < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column {
                AppText(
                    "\u9009\u62e9\u6295\u5e01\u6570\u91cf",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Coin options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // 1 coin
                    AppFilterChip(
                        selected = selectedCount == 1,
                        onClick = { selectedCount = 1 },
                        label = { AppText("1 \u786c\u5e01") },
                        enabled = maxCoins >= 1
                    )
                    // 2 coins
                    AppFilterChip(
                        selected = selectedCount == 2,
                        onClick = { selectedCount = 2 },
                        label = { AppText("2 \u786c\u5e01") },
                        enabled = maxCoins >= 2
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Also like checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { alsoLike = !alsoLike },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppCheckbox(
                        checked = alsoLike,
                        onCheckedChange = { alsoLike = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    AppText("\u540c\u65f6\u70b9\u8d5e")
                }
            }
        },
        confirmButton = {
            AppButton(
                onClick = { onConfirm(selectedCount.coerceAtMost(maxCoins), alsoLike) },
                enabled = maxCoins > 0
            ) {
                AppText("\u6295\u5e01")
            }
        },
        dismissButton = {
            AppTextButton(onClick = onDismiss) {
                AppText("\u53d6\u6d88")
            }
        }
    )
}
