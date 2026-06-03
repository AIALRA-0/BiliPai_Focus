package com.android.purebilibili.feature.cast

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.plugin.CastPluginApi
import com.android.purebilibili.core.plugin.CastPluginRoute
import com.android.purebilibili.core.plugin.PluginManager
import kotlinx.coroutines.flow.combine
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private data class PluginRouteEntry(
    val plugin: CastPluginApi,
    val route: CastPluginRoute
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceListDialog(
    onDismissRequest: () -> Unit,
    onPluginCastDeviceSelected: (CastPluginApi, CastPluginRoute) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current

    // Generic cast plugin routes
    val allPlugins by PluginManager.pluginsFlow.collectAsStateWithLifecycle()
    val castPlugins = remember(allPlugins) {
        allPlugins.filter { it.enabled }.mapNotNull { it.plugin as? CastPluginApi }
    }

    val pluginRouteEntries by produceState(emptyList<PluginRouteEntry>(), castPlugins) {
        if (castPlugins.isEmpty()) {
            value = emptyList()
            return@produceState
        }
        combine(castPlugins.map { it.routes }) { arrays ->
            arrays.flatMapIndexed { index, routes ->
                routes.map { PluginRouteEntry(castPlugins[index], it) }
            }
        }.collect { value = it }
    }

    val isDiscovering by produceState(false, castPlugins) {
        if (castPlugins.isEmpty()) {
            value = false
            return@produceState
        }
        combine(castPlugins.map { it.isDiscovering }) { states ->
            states.any { it }
        }.collect { value = it }
    }

    DisposableEffect(castPlugins) {
        castPlugins.forEach { it.startRouteDiscovery(context) }
        onDispose {
            castPlugins.forEach { it.stopRouteDiscovery() }
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = { Icon(Icons.Rounded.Cast, null) },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("选择投屏设备")
                Spacer(Modifier.weight(1f))
                IconButton(
                    enabled = !isDiscovering,
                    onClick = {
                        if (!isDiscovering) {
                            castPlugins.forEach { it.refreshRouteDiscovery(context) }
                        }
                    }
                ) {
                    Icon(Icons.Rounded.Refresh, "刷新")
                }
            }
        },
        text = {
            val hasDevices = pluginRouteEntries.isNotEmpty()

            if (!hasDevices && !isDiscovering) {
                Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("未找到设备", style = MaterialTheme.typography.bodyMedium)
                        Text("请确保在同一 WiFi 下", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else if (!hasDevices && isDiscovering) {
                Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("搜索设备中...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                    items(pluginRouteEntries, key = { "${it.plugin.id}:${it.route.routeId}" }) { entry ->
                        val plugin = entry.plugin
                        val route = entry.route
                        ListItem(
                            headlineContent = { Text(route.name) },
                            supportingContent = { Text(route.description ?: plugin.name) },
                            leadingContent = { Icon(route.icon ?: plugin.icon ?: Icons.Rounded.Cast, null) },
                            modifier = Modifier
                                .clickable { onPluginCastDeviceSelected(plugin, route) }
                                .fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        com.android.purebilibili.core.util.LogCollector.exportAndShare(context)
                    }
                ) {
                    Text("导出日志")
                }

                TextButton(onClick = onDismissRequest) {
                    Text("取消")
                }
            }
        }
    )
}
