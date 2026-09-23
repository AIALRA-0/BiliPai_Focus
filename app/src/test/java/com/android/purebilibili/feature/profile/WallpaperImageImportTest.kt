package com.android.purebilibili.feature.profile

import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class WallpaperImageImportTest {
    @Test
    fun importedWallpaperKeepsOriginalBytesAndDoesNotOverwritePreviousSelection() {
        val directory = Files.createTempDirectory("wallpaper-test").toFile()
        try {
            val bytes = byteArrayOf(1, 2, 3, 4)
            var closed = false
            val first = copyWallpaperImage(directory, {
                object : ByteArrayInputStream(bytes) {
                    override fun close() {
                        closed = true
                        super.close()
                    }
                }
            }, { it.readBytes().contentEquals(bytes) })
            val second = copyWallpaperImage(directory, { bytes.inputStream() }, { true })

            assertTrue(closed)
            assertTrue(first != second)
            assertContentEquals(bytes, first.readBytes())
            assertContentEquals(bytes, second.readBytes())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun unreadableEmptyAndInvalidSourcesLeaveNoPartialWallpaper() {
        val directory = Files.createTempDirectory("wallpaper-test").toFile()
        try {
            assertFailsWith<IOException> { copyWallpaperImage(directory, { null }, { true }) }
            assertFailsWith<IOException> {
                copyWallpaperImage(directory, { byteArrayOf().inputStream() }, { true })
            }
            assertFailsWith<IOException> {
                copyWallpaperImage(directory, { byteArrayOf(1).inputStream() }, { false })
            }
            assertFailsWith<SecurityException> {
                copyWallpaperImage(directory, { throw SecurityException("permission expired") }, { true })
            }
            assertEquals(0, directory.listFiles()!!.size)
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun interruptedReadClosesSourceAndRemovesPartialCopy() {
        val directory = Files.createTempDirectory("wallpaper-test").toFile()
        var closed = false
        try {
            assertFailsWith<IOException> {
                copyWallpaperImage(directory, {
                    object : ByteArrayInputStream(byteArrayOf(1, 2)) {
                        private var reads = 0
                        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                            if (reads++ > 0) throw IOException("provider disconnected")
                            return super.read(buffer, offset, 1)
                        }
                        override fun close() {
                            closed = true
                            super.close()
                        }
                    }
                }, { true })
            }
            assertTrue(closed)
            assertEquals(0, directory.listFiles()!!.size)
        } finally {
            directory.deleteRecursively()
        }
    }
}
