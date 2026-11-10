package com.android.purebilibili.core.store

import android.content.Context
import androidx.room.*
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

@Entity(tableName = "playlists")
internal data class PlaylistRecord(
    @PrimaryKey val id: String,
    val name: String,
    val coverUrl: String,
    val source: String,
    val createdAtMs: Long,
    val position: Long,
)

@Entity(
    tableName = "playlist_tracks", primaryKeys = ["playlistId", "position"],
    foreignKeys = [ForeignKey(entity = PlaylistRecord::class, parentColumns = ["id"],
        childColumns = ["playlistId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("playlistId")],
)
internal data class PlaylistTrackRecord(
    val playlistId: String,
    val position: Int,
    @Embedded val item: LocalPlaylistItem,
)

internal data class PlaylistWithTracks(
    @Embedded val playlist: PlaylistRecord,
    @Relation(parentColumn = "id", entityColumn = "playlistId") val tracks: List<PlaylistTrackRecord>,
) {
    fun toPlaylist() = LocalPlaylist(playlist.id, playlist.name, playlist.coverUrl, playlist.source,
        playlist.createdAtMs, tracks.sortedBy { it.position }.map { it.item })
}

@Entity(tableName = "audio_history")
internal data class AudioHistoryRecord(
    @PrimaryKey val id: String,
    @Embedded val entry: PlayHistoryEntry,
    val position: Long,
)

@Entity(tableName = "library_migrations")
internal data class LibraryMigration(@PrimaryKey val name: String)

@Dao
internal abstract class AudioLibraryDao {
    @Transaction @Query("SELECT * FROM playlists ORDER BY position DESC")
    abstract fun playlists(): Flow<List<PlaylistWithTracks>>
    @Query("SELECT COALESCE(MAX(position), 0) + 1 FROM playlists")
    abstract suspend fun nextPlaylistPosition(): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun putPlaylist(row: PlaylistRecord)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun putTracks(rows: List<PlaylistTrackRecord>)
    @Query("DELETE FROM playlist_tracks WHERE playlistId = :id")
    abstract suspend fun clearTracks(id: String)
    @Query("DELETE FROM playlists WHERE id = :id")
    abstract suspend fun deletePlaylist(id: String)
    @Query("SELECT EXISTS(SELECT 1 FROM playlists WHERE id = :id)")
    abstract suspend fun hasPlaylist(id: String): Boolean
    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :id ORDER BY position")
    abstract suspend fun tracks(id: String): List<PlaylistTrackRecord>
    @Query("SELECT * FROM audio_history ORDER BY position DESC LIMIT :limit")
    abstract fun recent(limit: Int): Flow<List<AudioHistoryRecord>>
    @Query("SELECT * FROM audio_history WHERE id = :id")
    abstract suspend fun history(id: String): AudioHistoryRecord?
    @Query("SELECT COALESCE(MAX(position), 0) + 1 FROM audio_history")
    abstract suspend fun nextHistoryPosition(): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun putHistory(row: AudioHistoryRecord)
    @Query("DELETE FROM audio_history WHERE id NOT IN (SELECT id FROM audio_history ORDER BY position DESC LIMIT 300)")
    abstract suspend fun trimHistory()
    @Query("SELECT EXISTS(SELECT 1 FROM library_migrations WHERE name = :name)")
    abstract suspend fun migrated(name: String): Boolean
    @Insert abstract suspend fun markMigrated(row: LibraryMigration)

    @Transaction
    open suspend fun savePlaylist(playlist: LocalPlaylist) {
        putPlaylist(PlaylistRecord(playlist.id, playlist.name, playlist.coverUrl, playlist.source,
            playlist.createdAtMs, nextPlaylistPosition()))
        clearTracks(playlist.id)
        putTracks(playlist.items.mapIndexed { index, item -> PlaylistTrackRecord(playlist.id, index, item) })
    }

    @Transaction
    open suspend fun addTracks(id: String, items: List<LocalPlaylistItem>) {
        if (!hasPlaylist(id)) return
        val previous = tracks(id)
        val known = previous.mapTo(hashSetOf()) { it.item.bvid }
        val added = items.filter { known.add(it.bvid) }
        val firstPosition = (previous.maxOfOrNull { it.position } ?: -1) + 1
        putTracks(added.mapIndexed { index, item -> PlaylistTrackRecord(id, firstPosition + index, item) })
    }

    @Transaction
    open suspend fun record(entry: PlayHistoryEntry) {
        val old = history(entry.bvid)?.entry
        val next = if (old == null) entry else entry.copy(
            title = entry.title.ifBlank { old.title }, cover = entry.cover.ifBlank { old.cover },
            owner = entry.owner.ifBlank { old.owner }, playCount = old.playCount + 1,
        )
        putHistory(AudioHistoryRecord(entry.bvid, next, nextHistoryPosition()))
        trimHistory()
    }
}

@Database(entities = [PlaylistRecord::class, PlaylistTrackRecord::class,
    AudioHistoryRecord::class, LibraryMigration::class], version = 1, exportSchema = false)
internal abstract class AudioLibraryDatabase : RoomDatabase() {
    abstract fun library(): AudioLibraryDao

    companion object {
        @Volatile private var instance: AudioLibraryDatabase? = null
        private val json = Json { ignoreUnknownKeys = true }

        fun get(context: Context): AudioLibraryDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext,
                AudioLibraryDatabase::class.java, "audio_library.db").build().also { instance = it }
        }

        private inline fun <reified T> decodeLegacy(raw: String?): List<T> {
            if (raw.isNullOrBlank()) return emptyList()
            return try { json.decodeFromString<List<T>>(raw) }
            catch (failure: IllegalArgumentException) {
                com.android.purebilibili.core.util.Logger.w("AudioLibrary", "旧媒体列表无法解析，保留原始偏好数据", failure)
                emptyList()
            }
        }

        internal suspend fun importLegacy(
            db: AudioLibraryDatabase,
            playlists: List<LocalPlaylist>,
            history: List<PlayHistoryEntry>,
        ) {
            val dao = db.library()
            db.withTransaction {
                if (!dao.migrated("preferences_v1")) {
                    playlists.asReversed().forEach { dao.savePlaylist(it) }
                    history.take(300).asReversed().forEach {
                        dao.putHistory(AudioHistoryRecord(it.bvid, it, dao.nextHistoryPosition()))
                    }
                    dao.markMigrated(LibraryMigration("preferences_v1"))
                }
            }
        }

        /** Marker and imported rows commit together; interruption never imports a partial library. */
        suspend fun ready(context: Context): AudioLibraryDatabase {
            val db = get(context)
            val dao = db.library()
            if (dao.migrated("preferences_v1")) return db
            val prefs = context.settingsDataStore.data.first()
            val playlists = decodeLegacy<LocalPlaylist>(prefs[stringPreferencesKey("local_playlists_v1")])
            val history = decodeLegacy<PlayHistoryEntry>(prefs[stringPreferencesKey("audio_play_history_v1")])
            importLegacy(db, playlists, history)
            // Keep the old keys intact for recovery; Room is the source for all later operations.
            return db
        }
    }
}
