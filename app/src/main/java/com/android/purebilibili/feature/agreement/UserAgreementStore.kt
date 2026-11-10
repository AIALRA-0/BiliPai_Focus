package com.android.purebilibili.feature.agreement

import android.content.Context

/** Gate persistence: an install may proceed only after agreeing to the current version. */
object UserAgreementStore {
    private const val PREFS_NAME = "user_agreement"
    private const val KEY_ACCEPTED_VERSION = "accepted_version"

    fun isAccepted(context: Context): Boolean = acceptedVersion(context) >= USER_AGREEMENT_VERSION

    fun acceptedVersion(context: Context): Int =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_ACCEPTED_VERSION, 0)

    fun accept(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_ACCEPTED_VERSION, USER_AGREEMENT_VERSION).apply()
    }

    fun revokeForTesting(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_ACCEPTED_VERSION).apply()
    }
}
