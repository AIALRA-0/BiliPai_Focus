package com.android.purebilibili.data.repository

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CommentReadRouteMemoTest {

    @Test
    fun `grpc is attempted while there is no recorded failure`() {
        val memo = CommentReadRouteMemo()
        assertFalse(memo.shouldSkipGrpcAttempt())
    }

    @Test
    fun `recorded grpc failure is skipped within the ttl and retried after it`() {
        var now = 0L
        val memo = CommentReadRouteMemo(failureTtlMs = 1_000L, nowMs = { now })

        memo.recordGrpcFailure()
        assertTrue(memo.shouldSkipGrpcAttempt())

        // TTL 内继续跳过，不产生额外请求。
        now = 999L
        assertTrue(memo.shouldSkipGrpcAttempt())

        // TTL 到期后自动重新给 gRPC 一次机会，网络恢复无需重启进程。
        now = 1_000L
        assertFalse(memo.shouldSkipGrpcAttempt())
        assertFalse(memo.shouldSkipGrpcAttempt())
    }

    @Test
    fun `grpc success clears the recorded failure`() {
        var now = 0L
        val memo = CommentReadRouteMemo(failureTtlMs = 10_000L, nowMs = { now })

        memo.recordGrpcFailure()
        assertTrue(memo.shouldSkipGrpcAttempt())

        memo.recordGrpcSuccess()
        assertFalse(memo.shouldSkipGrpcAttempt())

        // 成功后即便时间推进也不应再跳过。
        now = 5_000L
        assertFalse(memo.shouldSkipGrpcAttempt())
    }

    @Test
    fun `reset forgets the failure`() {
        val memo = CommentReadRouteMemo()
        memo.recordGrpcFailure()
        assertTrue(memo.shouldSkipGrpcAttempt())
        memo.reset()
        assertFalse(memo.shouldSkipGrpcAttempt())
    }
}
