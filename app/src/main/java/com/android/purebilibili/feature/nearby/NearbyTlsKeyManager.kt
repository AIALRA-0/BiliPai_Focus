package com.android.purebilibili.feature.nearby

import java.net.Socket
import java.security.Principal
import java.security.PrivateKey
import java.security.cert.X509Certificate
import javax.net.ssl.SSLEngine
import javax.net.ssl.X509ExtendedKeyManager

/** 普通 Socket 与引擎连接必须选择同一设备身份，双方认证均适用。 */
internal class NearbyTlsKeyManager(
    private val identityAlias: String,
    private val identityKey: PrivateKey,
    private val identityChain: Array<X509Certificate>,
) : X509ExtendedKeyManager() {
    private fun supports(keyType: String?): Boolean = keyType == identityKey.algorithm ||
        keyType?.startsWith("${identityKey.algorithm}_") == true

    override fun getClientAliases(keyType: String?, issuers: Array<out Principal>?): Array<String>? =
        if (supports(keyType)) arrayOf(identityAlias) else null

    override fun getServerAliases(keyType: String?, issuers: Array<out Principal>?): Array<String>? =
        getClientAliases(keyType, issuers)

    override fun chooseClientAlias(
        keyType: Array<out String>?, issuers: Array<out Principal>?, socket: Socket?,
    ): String? = if (keyType?.any(::supports) == true) identityAlias else null

    override fun chooseServerAlias(
        keyType: String?, issuers: Array<out Principal>?, socket: Socket?,
    ): String? = if (supports(keyType)) identityAlias else null

    // 引擎连接不会自动转调 Socket 回调；省略覆盖会继承返回 null 的默认实现。
    override fun chooseEngineClientAlias(
        keyType: Array<out String>?, issuers: Array<out Principal>?, engine: SSLEngine?,
    ): String? = chooseClientAlias(keyType, issuers, null)

    override fun chooseEngineServerAlias(
        keyType: String?, issuers: Array<out Principal>?, engine: SSLEngine?,
    ): String? = chooseServerAlias(keyType, issuers, null)

    override fun getCertificateChain(alias: String?): Array<X509Certificate>? =
        if (alias == identityAlias) identityChain.copyOf() else null

    override fun getPrivateKey(alias: String?): PrivateKey? =
        if (alias == identityAlias) identityKey else null
}
