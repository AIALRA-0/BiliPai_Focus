package com.android.purebilibili.core.plugin

import com.android.purebilibili.plugin.sdk.PluginCapabilityManifest

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

/** Host-only presentation and settings for a built-in SDK plugin. */
interface BuiltinPlugin : com.android.purebilibili.plugin.sdk.Plugin {
    /** 唯一标识符，如 "sponsorblock" */
    override val id: String
    
    /** 显示名称，如 "空降助手" */
    override val name: String
    
    /** 插件描述 */
    val description: String
    
    /** 版本号，如 "1.0.0" */
    override val version: String
    
    /**  插件作者 */
    val author: String
        get() = "Unknown"
    
    /** 插件图标 (可选) */
    val icon: ImageVector?
        get() = null
    
    /**  是否暂不可用 (用于标识功能尚未完成) */
    val unavailable: Boolean
        get() = false
    
    /**  不可用原因描述 */
    val unavailableReason: String
        get() = "功能开发中"

    /** 插件能力声明，用于安装前授权和插件中心展示 */
    override val capabilityManifest: PluginCapabilityManifest
        get() = PluginCapabilityManifest(
            pluginId = id,
            displayName = name,
            version = version,
            apiVersion = 1,
            entryClassName = this::class.java.name,
            capabilities = emptySet()
        )
    
    /**
     * 插件配置界面 (可选)
     * 返回 null 表示无配置项
     */
    @Composable
    fun SettingsContent(): Unit = Unit

    /**
     * Modifier-aware settings entry point used by the built-in plugin detail screen.
     *
     * The no-argument entry point stays in place for older and experimental plugins.
     * Implementations can override this overload when their root layout needs to consume
     * the caller-owned detail-page constraints directly.
     */
    @Composable
    fun SettingsContent(modifier: Modifier) {
        Box(modifier = modifier) {
            SettingsContent()
        }
    }
}
