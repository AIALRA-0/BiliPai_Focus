package com.android.purebilibili.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_ups")
data class BlockedUp(
    @PrimaryKey
    val mid: Long,
    val name: String,
    val face: String,
    val blockedAt: Long = System.currentTimeMillis(),
    val level: Int? = null,
    val sign: String = "",
    val vipLabel: String = "",
    val officialTitle: String = "",
    val follower: Long? = null,
    val archiveCount: Int? = null,
    val isDeleted: Boolean = false,
    val lastSyncedAt: Long? = null
)
