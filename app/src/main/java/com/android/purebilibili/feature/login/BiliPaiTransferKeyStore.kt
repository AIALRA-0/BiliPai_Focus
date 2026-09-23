package com.android.purebilibili.feature.login

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore

/** Device identity for QR transfer; the private key never leaves Android Keystore. */
object BiliPaiTransferKeyStore {
    private const val STORE = "AndroidKeyStore"
    private const val ALIAS = "bilipai.qr.transfer.identity"

    fun getOrCreate(context: Context): KeyPair {
        val store = KeyStore.getInstance(STORE).apply { load(null) }
        if (!store.containsAlias(ALIAS)) {
            KeyPairGenerator.getInstance("RSA", STORE).apply {
                initialize(KeyGenParameterSpec.Builder(
                    ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT or
                        KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
                ).setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                    .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                    .build())
            }.generateKeyPair()
        }
        return KeyPair(store.getCertificate(ALIAS).publicKey, store.getKey(ALIAS, null) as java.security.PrivateKey)
    }
}
