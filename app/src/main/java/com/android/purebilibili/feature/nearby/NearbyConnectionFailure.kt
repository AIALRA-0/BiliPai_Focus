package com.android.purebilibili.feature.nearby

import java.io.EOFException
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import javax.net.ssl.SSLException

internal enum class NearbyConnectionStage { PREPARE, CONNECT, HANDSHAKE, CONFIRM }

/** 只向界面输出已知分类，不回显异常文本、配对码、令牌或证书内容。 */
internal class NearbyConnectionFailure(val hint: String, cause: Throwable? = null) : IOException(hint, cause)

internal fun nearbyConnectionHint(error: Throwable, stage: NearbyConnectionStage): String {
    val chain = generateSequence(error) { it.cause }.take(8).toList()
    chain.filterIsInstance<NearbyConnectionFailure>().firstOrNull()?.let { return it.hint }
    return when {
        chain.any { it is SecurityException } -> "局域网或附近设备权限未允许，请检查应用权限后重试"
        chain.any { it is SSLException } -> "加密握手失败：请在对端重新生成配对码，或解除旧配对后重试"
        chain.any { it is NoRouteToHostException } -> "设备地址不可达：手机与模拟器请先由手机生成配对码，再在模拟器发起连接"
        chain.any { it is ConnectException } -> "连接被拒绝：请确认对端保持设备接力开启，地址和端口可访问"
        chain.any { it is SocketTimeoutException } -> when (stage) {
            NearbyConnectionStage.CONNECT -> "连接地址超时：请检查两端网络是否互通；手机通常不能直连模拟器默认地址"
            NearbyConnectionStage.HANDSHAKE -> "加密握手超时：请检查对端是否仍开启接收"
            else -> "等待对端确认超时：请保持对端前台，并确认配对或接续播放请求"
        }
        chain.any { it is EOFException } -> "对端关闭了连接：请保持对端设备接力开启，重新连接后重试"
        error is IllegalArgumentException && stage == NearbyConnectionStage.PREPARE ->
            "配对码格式不正确：请使用另一台设备复制的完整配对码"
        else -> when (stage) {
            NearbyConnectionStage.PREPARE -> "无法准备设备身份或地址：请确认两端已开启接力并连接局域网"
            NearbyConnectionStage.CONNECT -> "无法连接对端地址：请检查网络、接收开关和端口"
            NearbyConnectionStage.HANDSHAKE -> "无法建立加密连接：请重新配对设备"
            NearbyConnectionStage.CONFIRM -> "对端未完成配对确认或播放确认，请查看对端提示"
        }
    }
}
