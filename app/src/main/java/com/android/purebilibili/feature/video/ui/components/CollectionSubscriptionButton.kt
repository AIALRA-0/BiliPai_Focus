package com.android.purebilibili.feature.video.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.data.repository.ActionRepository
import kotlinx.coroutines.launch

@Composable
internal fun CollectionSubscriptionButton(
    collectionId: Long,
    currentBvid: String,
    currentAid: Long,
    fontSize: TextUnit,
    contentPadding: PaddingValues = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cachedIsSubscribed by SettingsManager
        .isCollectionSubscribed(context, collectionId)
        .collectAsState(initial = false)
    var remoteIsSubscribed by remember(collectionId, currentBvid, currentAid) {
        mutableStateOf<Boolean?>(null)
    }
    var isUpdating by remember(collectionId) { mutableStateOf(false) }
    val isSubscribed = remoteIsSubscribed ?: cachedIsSubscribed

    LaunchedEffect(collectionId, currentBvid, currentAid) {
        if (collectionId <= 0L || currentBvid.isBlank()) return@LaunchedEffect
        ActionRepository
            .checkCollectionSubscriptionStatus(bvid = currentBvid, aid = currentAid)
            .onSuccess { subscribed ->
                remoteIsSubscribed = subscribed
                SettingsManager.setCollectionSubscription(context, collectionId, subscribed)
            }
    }

    TextButton(
        enabled = !isUpdating,
        onClick = {
            if (collectionId <= 0L) {
                Toast.makeText(context, "无法识别合集 ID", Toast.LENGTH_SHORT).show()
                return@TextButton
            }
            scope.launch {
                isUpdating = true
                try {
                    val targetSubscribed = !isSubscribed
                    ActionRepository
                        .setCollectionSubscription(
                            seasonId = collectionId,
                            subscribe = targetSubscribed
                        )
                        .onSuccess { subscribed ->
                            remoteIsSubscribed = subscribed
                            SettingsManager.setCollectionSubscription(context, collectionId, subscribed)
                            Toast.makeText(
                                context,
                                if (subscribed) "已订阅合集" else "已取消订阅合集",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .onFailure { error ->
                            Toast.makeText(
                                context,
                                error.message ?: "合集订阅操作失败",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                } finally {
                    isUpdating = false
                }
            }
        },
        contentPadding = contentPadding
    ) {
        Text(
            text = if (isSubscribed) "已订阅" else "订阅",
            color = if (isSubscribed) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            fontSize = fontSize,
            fontWeight = FontWeight.Medium
        )
    }
}
