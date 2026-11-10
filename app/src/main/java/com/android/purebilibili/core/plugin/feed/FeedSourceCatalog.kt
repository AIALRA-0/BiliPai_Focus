package com.android.purebilibili.core.plugin.feed

import com.android.purebilibili.core.plugin.BuiltinPluginIds

import android.content.Context
import com.android.purebilibili.plugin.sdk.PluginCapability
import com.android.purebilibili.core.plugin.PluginManager
import com.android.purebilibili.core.plugin.js.BiliPaiJsPluginInstallStore
import com.android.purebilibili.feature.plugin.js.resolveBiliPaiJsInitialParamValues

fun isSubscriptionPluginOrFeedEnabled(
    context: Context,
    installedPlugins: List<com.android.purebilibili.core.plugin.PluginInfo> = emptyList(),
    isPluginPersistedEnabled: Boolean = false,
): Boolean {
    if (isPluginPersistedEnabled) return true

    val builtinPluginEnabled = installedPlugins.any {
        it.enabled && it.plugin.id == BuiltinPluginIds.SUBSCRIPTION_FEED
    } || PluginManager.isEnabled(BuiltinPluginIds.SUBSCRIPTION_FEED)
    if (builtinPluginEnabled) return true

    val scripts = runCatching {
        BiliPaiJsPluginInstallStore.createDefault(context).listInstalledPlugins()
    }.getOrDefault(emptyList())
    val jsFeedEnabled = scripts.any { installed ->
        installed.enabled &&
            PluginCapability.FEED_SOURCE in installed.grantedCapabilities &&
            PluginCapability.NETWORK in installed.grantedCapabilities
    }
    if (jsFeedEnabled) return true

    return loadEnabledFeedSources(context).isNotEmpty()
}

fun loadEnabledFeedSources(context: Context): List<FeedSource> {
    val builtin = if (PluginManager.isEnabled(BuiltinPluginIds.SUBSCRIPTION_FEED)) {
        SubscriptionFeedStore.listBlocking(context)
            .filter { it.enabled && isHttpFeedUrl(it.url) }
            .map {
                FeedSource(
                    id = "builtin:${it.id}",
                    title = it.title,
                    url = it.url,
                    group = it.group,
                )
            }
    } else {
        emptyList()
    }
    val scripts = runCatching {
        BiliPaiJsPluginInstallStore.createDefault(context).listInstalledPlugins()
    }.getOrDefault(emptyList())
    val fromJs = scripts
        .filter { installed ->
            installed.enabled &&
                PluginCapability.FEED_SOURCE in installed.grantedCapabilities &&
                PluginCapability.NETWORK in installed.grantedCapabilities
        }
        .flatMap { installed ->
            installed.manifest.modules
                .filter { it.kind.equals("feed", ignoreCase = true) }
                .mapNotNull { module ->
                    val values = resolveBiliPaiJsInitialParamValues(
                        params = module.params,
                        savedValues = readJsFeedParamValues(context, installed.manifest.id, module),
                    )
                    val url = values["url"].orEmpty().ifBlank {
                        module.params.firstOrNull { it.name == "url" }?.defaultValue.orEmpty()
                    }
                    if (!isHttpFeedUrl(url)) return@mapNotNull null
                    FeedSource(
                        id = "js:${installed.manifest.id}:${module.id.ifBlank { module.functionName }}",
                        title = module.title.ifBlank { installed.manifest.title },
                        url = url,
                    )
                }
        }
    return (builtin + fromJs).distinctBy { it.url }
}

private fun readJsFeedParamValues(
    context: Context,
    pluginId: String,
    module: com.android.purebilibili.core.plugin.js.BiliPaiJsModule,
): Map<String, String> {
    val prefs = context.getSharedPreferences("bilipai_js_plugin_params", Context.MODE_PRIVATE)
    val moduleId = module.id.ifBlank { module.functionName }
    return module.params.associate { param ->
        val key = com.android.purebilibili.feature.plugin.js.buildBiliPaiJsParamPreferenceKey(
            pluginId,
            moduleId,
            param.name,
        )
        param.name to prefs.getString(key, param.defaultValue).orEmpty()
    }
}
