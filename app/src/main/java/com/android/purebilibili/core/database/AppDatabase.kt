package com.android.purebilibili.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.android.purebilibili.core.database.dao.SearchHistoryDao
import com.android.purebilibili.core.database.entity.SearchHistory
import com.android.purebilibili.core.database.entity.BlockedUp
import com.android.purebilibili.core.database.dao.BlockedUpDao

@Database(entities = [SearchHistory::class, BlockedUp::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun blockedUpDao(): BlockedUpDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .addMigrations(MIGRATION_3_4)
                    //  数据库迁移：Schema 变更时清空旧数据
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE blocked_ups ADD COLUMN level INTEGER")
                db.execSQL("ALTER TABLE blocked_ups ADD COLUMN sign TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE blocked_ups ADD COLUMN vipLabel TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE blocked_ups ADD COLUMN officialTitle TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE blocked_ups ADD COLUMN follower INTEGER")
                db.execSQL("ALTER TABLE blocked_ups ADD COLUMN archiveCount INTEGER")
                db.execSQL("ALTER TABLE blocked_ups ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE blocked_ups ADD COLUMN lastSyncedAt INTEGER")
            }
        }
    }
}
