package com.android.purebilibili.data.repository

/**
 * 评论主列表读取路由的短期记忆。
 *
 * 评论首页读取是「先 gRPC MainList、失败再 REST」的串行降级链。当 gRPC 在某条链路上
 * 稳定不可用时，每个视频、每次翻页都会先白等一次 gRPC 失败往返（200~800ms）再走 REST
 * ——这段等待正好落在首屏骨架期，是整个评论区最贵的一段延迟。
 *
 * 这里只记住「最近一次 gRPC 尝试失败」，在 TTL 内直接跳过 gRPC 尝试：
 * - 只跳过「刚刚失败过」的尝试，一旦 gRPC 成功立刻清除记忆，不会把可用链路误判为不可用；
 * - TTL 到期自动重新尝试 gRPC，网络恢复后无需重启进程；
 * - 跳过 gRPC 只会退回 REST——本来就是失败时的兜底路径，不会让请求变多、也不会增加并发。
 *
 * 刻意**不**把 gRPC 与 REST 并发发出：两条链路返回的是同一份评论，并发只会把请求数
 * 翻倍、抬高瞬时 CPU 负载，而首屏墙上时间几乎不变（REST 仍然要等网络往返）。
 */
internal class CommentReadRouteMemo(
    private val failureTtlMs: Long = 5 * 60 * 1000L,
    private val nowMs: () -> Long = System::currentTimeMillis,
) {

    private var grpcFailedAtMs: Long? = null

    /** 最近一次 gRPC 尝试失败且仍在 TTL 内 → 本次直接跳过 gRPC，走 REST。 */
    @Synchronized
    internal fun shouldSkipGrpcAttempt(): Boolean {
        val failedAt = grpcFailedAtMs ?: return false
        if (nowMs() - failedAt >= failureTtlMs) {
            // TTL 到期：忘记失败，重新给 gRPC 一次机会。
            grpcFailedAtMs = null
            return false
        }
        return true
    }

    @Synchronized
    internal fun recordGrpcSuccess() {
        grpcFailedAtMs = null
    }

    @Synchronized
    internal fun recordGrpcFailure() {
        grpcFailedAtMs = nowMs()
    }

    @Synchronized
    internal fun reset() {
        grpcFailedAtMs = null
    }
}

/** 进程内共享实例：链路可用性是链路/账号级别的属性，不需要按视频区分。 */
internal val commentReadRouteMemo = CommentReadRouteMemo()
