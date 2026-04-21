package com.android.purebilibili.data.repository

import com.android.purebilibili.data.model.response.ReplyPicture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CommentRepositoryImagePayloadTest {

    @Test
    fun `buildPicturesPayload returns null when no pictures`() {
        val payload = CommentRepository.buildPicturesPayload(emptyList())
        assertNull(payload)
    }

    @Test
    fun `buildPicturesPayload serializes expected fields`() {
        val payload = CommentRepository.buildPicturesPayload(
            listOf(
                ReplyPicture(
                    imgSrc = "https://i0.hdslb.com/bfs/new_dyn/test.png",
                    imgWidth = 1080,
                    imgHeight = 720,
                    imgSize = 321.5f
                )
            )
        )

        requireNotNull(payload)
        assertTrue(payload.contains("\"img_src\":\"https://i0.hdslb.com/bfs/new_dyn/test.png\""))
        assertTrue(payload.contains("\"img_width\":1080"))
        assertTrue(payload.contains("\"img_height\":720"))
        assertTrue(payload.contains("\"img_size\":321.5"))
    }

    @Test
    fun `resolveSyncToDynamicField only sends flag when enabled`() {
        assertNull(CommentRepository.resolveSyncToDynamicField(false))
        assertEquals(1, CommentRepository.resolveSyncToDynamicField(true))
    }

    @Test
    fun `resolveReplyTopActionField maps current state to api action`() {
        assertEquals(1, CommentRepository.resolveReplyTopActionField(isCurrentlyTop = false))
        assertEquals(0, CommentRepository.resolveReplyTopActionField(isCurrentlyTop = true))
    }

    @Test
    fun `shouldTryGrpcMainList supports hot and time modes with offset paging`() {
        assertTrue(CommentRepository.shouldTryGrpcMainList(page = 1, mode = 3, paginationOffset = null))
        assertTrue(CommentRepository.shouldTryGrpcMainList(page = 2, mode = 3, paginationOffset = "next"))
        assertTrue(CommentRepository.shouldTryGrpcMainList(page = 1, mode = 2, paginationOffset = null))
        assertNull(CommentRepository.resolveSyncToDynamicField(false))
        assertTrue(!CommentRepository.shouldTryGrpcMainList(page = 2, mode = 3, paginationOffset = null))
        assertTrue(!CommentRepository.shouldTryGrpcMainList(page = 1, mode = 4, paginationOffset = null))
        assertTrue(CommentRepository.shouldTryGrpcPagedRequest(page = 1, paginationOffset = null))
        assertTrue(CommentRepository.shouldTryGrpcPagedRequest(page = 2, paginationOffset = "offset"))
        assertTrue(!CommentRepository.shouldTryGrpcPagedRequest(page = 2, paginationOffset = null))
    }
}
