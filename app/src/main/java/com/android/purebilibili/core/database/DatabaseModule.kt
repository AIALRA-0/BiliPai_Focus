package com.android.purebilibili.core.database

import android.content.Context

object DatabaseModule {
    fun getDatabase(context: Context): AppDatabase = AppDatabase.getDatabase(context)
}
