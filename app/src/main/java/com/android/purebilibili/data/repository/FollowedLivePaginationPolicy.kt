package com.android.purebilibili.data.repository

/** 接口可能限制实际页大小；不能用请求的 page_size 判断短页结束。 */
internal fun hasMoreFollowedLivePages(page: Int, returnedRoomCount: Int, totalPages: Int): Boolean =
    returnedRoomCount > 0 && (totalPages <= 0 || page < totalPages)
