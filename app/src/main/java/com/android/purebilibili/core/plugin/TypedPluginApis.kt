package com.android.purebilibili.core.plugin

interface PlayerPluginApi : PlayerPlugin {
    override val capabilityManifest: PluginCapabilityManifest
}

interface DanmakuPluginApi : DanmakuPlugin {
    override val capabilityManifest: PluginCapabilityManifest
}
