package com.android.purebilibili.core.store

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*

class AudioLibraryDatabaseTest {
    @Test fun playlistEditsPreserveOrderDeduplicateAndCascadeDelete() = runTest {
        val db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext,
            AudioLibraryDatabase::class.java).build()
        try {
            val dao = db.library()
            dao.savePlaylist(LocalPlaylist("one", "第一份", items = listOf(LocalPlaylistItem("A"))))
            dao.savePlaylist(LocalPlaylist("two", "第二份"))
            dao.addTracks("one", listOf(LocalPlaylistItem("A"), LocalPlaylistItem("B"), LocalPlaylistItem("B")))
            val rows = dao.playlists().first().map { it.toPlaylist() }
            assertEquals(listOf("two", "one"), rows.map { it.id })
            assertEquals(listOf("A", "B"), rows.last().items.map { it.bvid })
            dao.deletePlaylist("one")
            assertTrue(dao.tracks("one").isEmpty())
            dao.addTracks("missing", listOf(LocalPlaylistItem("C")))
            assertTrue(dao.tracks("missing").isEmpty())
        } finally { db.close() }
    }

    @Test fun historyUpdatesOneRowRetainsMetadataAndTrimsToThreeHundred() = runTest {
        val db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext,
            AudioLibraryDatabase::class.java).build()
        try {
            val dao = db.library()
            dao.record(PlayHistoryEntry("A", title = "保留标题", owner = "作者"))
            dao.record(PlayHistoryEntry("A", cid = 42L, lastPlayedAtMs = 20))
            val updated = dao.recent(300).first().single().entry
            assertEquals(2, updated.playCount)
            assertEquals("保留标题", updated.title)
            assertEquals(42L, updated.cid)
            repeat(301) { dao.record(PlayHistoryEntry("item$it")) }
            val recent = dao.recent(300).first()
            assertEquals(300, recent.size)
            assertEquals("item300", recent.first().entry.bvid)
            assertNull(dao.history("A"))
        } finally { db.close() }
    }

    @Test fun legacyImportPreservesCountsOrderAndDoesNotReimportDeletedData() = runTest {
        val db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext,
            AudioLibraryDatabase::class.java).build()
        try {
            val playlists = listOf(LocalPlaylist("new", "最近"), LocalPlaylist("old", "较早"))
            val history = listOf(PlayHistoryEntry("A", playCount = 7), PlayHistoryEntry("B", playCount = 2))
            AudioLibraryDatabase.importLegacy(db, playlists, history)
            val dao = db.library()
            assertEquals(listOf("new", "old"), dao.playlists().first().map { it.playlist.id })
            assertEquals(7, dao.recent(300).first().first().entry.playCount)
            dao.deletePlaylist("new")
            AudioLibraryDatabase.importLegacy(db, playlists, history)
            assertEquals(listOf("old"), dao.playlists().first().map { it.playlist.id })
            assertEquals(7, dao.recent(300).first().first().entry.playCount)
        } finally { db.close() }
    }

}
