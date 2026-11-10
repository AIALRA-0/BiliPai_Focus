package com.android.purebilibili.core.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal enum class ScopeOwner {
    APP,
    LIFECYCLE
}

internal enum class LaunchThread {
    MAIN,
    IO
}

internal data class AsyncLaunchPolicy(
    val scopeOwner: ScopeOwner,
    val thread: LaunchThread
)

internal fun resolveHomePreloadLaunchPolicy(): AsyncLaunchPolicy {
    return AsyncLaunchPolicy(scopeOwner = ScopeOwner.APP, thread = LaunchThread.IO)
}

internal fun resolveLocalProxyWarmupLaunchPolicy(): AsyncLaunchPolicy {
    return AsyncLaunchPolicy(scopeOwner = ScopeOwner.APP, thread = LaunchThread.IO)
}

internal fun resolveShortLinkLaunchPolicy(): AsyncLaunchPolicy {
    return AsyncLaunchPolicy(scopeOwner = ScopeOwner.LIFECYCLE, thread = LaunchThread.MAIN)
}

internal object AppScope {
    private val appJob = SupervisorJob()

    val ioScope: CoroutineScope = CoroutineScope(appJob + Dispatchers.IO)

    /** 主线程应用级作用域：承载与界面生命周期无关的短时编排，如前台安定窗口计时。 */
    val mainScope: CoroutineScope = CoroutineScope(appJob + Dispatchers.Main.immediate)
}
