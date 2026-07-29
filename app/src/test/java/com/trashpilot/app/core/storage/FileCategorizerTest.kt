package com.trashpilot.app.core.storage

import org.junit.Assert.assertEquals
import org.junit.Test

class FileCategorizerTest {

    @Test
    fun `categorizes supported mime types and apk extension`() {
        assertEquals(FileCategory.IMAGES, categorize("photo.jpg", "image/jpeg"))
        assertEquals(FileCategory.VIDEOS, categorize("clip.mp4", "video/mp4"))
        assertEquals(FileCategory.AUDIO, categorize("song.mp3", "audio/mpeg"))
        assertEquals(FileCategory.DOCUMENTS, categorize("notes.pdf", "application/pdf"))
        assertEquals(FileCategory.APK_FILES, categorize("release.apk", "application/octet-stream"))
    }

    @Test
    fun `download directory takes precedence over file type`() {
        assertEquals(
            FileCategory.DOWNLOADS,
            FileCategorizer.categorize(
                fileName = "photo.jpg",
                mimeType = "image/jpeg",
                pathSegments = listOf("Internal storage", "Download", "photo.jpg")
            )
        )
    }

    @Test
    fun `unknown files are other`() {
        assertEquals(FileCategory.OTHER, categorize("archive.bin", "application/octet-stream"))
    }

    private fun categorize(name: String, mimeType: String): FileCategory =
        FileCategorizer.categorize(name, mimeType, listOf("Internal storage", name))
}
