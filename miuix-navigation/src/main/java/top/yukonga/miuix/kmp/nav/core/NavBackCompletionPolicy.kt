// Copyright 2026, BiliPai contributors
// SPDX-License-Identifier: Apache-2.0
package top.yukonga.miuix.kmp.nav.core

/** Per-session decisions for predictive-back terminal events. Discrete back is unaffected. */
interface NavBackCompletionPolicy {
    fun shouldCommit(progress: Float, peakProgress: Float, velocity: Float): Boolean

    /** Opt-in only: a floating card can land despite a platform cancellation on release. */
    fun shouldCommitOnCancel(progress: Float, peakProgress: Float): Boolean = false
}
