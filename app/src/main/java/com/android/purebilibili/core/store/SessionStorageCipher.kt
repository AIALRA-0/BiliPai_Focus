package com.android.purebilibili.core.store

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** AES-GCM encryption for locally persisted login sessions. */
object SessionStorageCipher {
    private const val STORE = "AndroidKeyStore"
    private const val ALIAS = "bilipai.session.storage"
    private const val PREFIX = "v1:"

    @Volatile
    private var cachedKey: SecretKey? = null

    fun encrypt(value: String): String {
        if (value.isEmpty()) return value
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            // Android Keystore requires a fresh system-generated IV for GCM encryption.
            init(Cipher.ENCRYPT_MODE, key())
        }
        return PREFIX + b64(cipher.iv) + ":" + b64(cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8)))
    }

    fun decrypt(value: String): String {
        if (!value.startsWith(PREFIX)) return value // one-time compatibility with pre-encryption data
        return runCatching {
            val parts = value.removePrefix(PREFIX).split(":", limit = 2)
            require(parts.size == 2)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, unb64(parts[0])))
            }
            String(cipher.doFinal(unb64(parts[1])), StandardCharsets.UTF_8)
        }.getOrElse { "" }
    }

    private fun key(): SecretKey {
        cachedKey?.let { return it }
        return synchronized(this) {
            cachedKey?.let { return@synchronized it }

            var store = KeyStore.getInstance(STORE).apply { load(null) }
            if (!store.containsAlias(ALIAS)) {
                KeyGenerator.getInstance("AES", STORE).apply {
                    init(
                        KeyGenParameterSpec.Builder(
                            ALIAS,
                            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                        )
                            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                            .build(),
                    )
                }.generateKey()
                // Reload after generation so this lookup observes the newly-created alias.
                store = KeyStore.getInstance(STORE).apply { load(null) }
            }
            (store.getEntry(ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
                .also { cachedKey = it }
        }
    }

    private fun b64(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP or Base64.URL_SAFE)
    private fun unb64(value: String): ByteArray = Base64.decode(value, Base64.NO_WRAP or Base64.URL_SAFE)
}
