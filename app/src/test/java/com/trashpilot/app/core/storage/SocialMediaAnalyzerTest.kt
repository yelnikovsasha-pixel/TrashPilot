package com.trashpilot.app.core.storage

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialMediaAnalyzerTest {
    @Test
    fun `detects supported media folders without matching file names`() {
        assertTrue(
            SocialMediaAnalyzer.isSupportedSocialMedia(
                file("Android/media/com.whatsapp/WhatsApp/Media/photo.jpg")
            )
        )
        assertTrue(
            SocialMediaAnalyzer.isSupportedSocialMedia(
                file("Telegram/Telegram Images/photo.jpg")
            )
        )
        assertFalse(
            SocialMediaAnalyzer.isSupportedSocialMedia(
                file("Pictures/facebook-memory.jpg")
            )
        )
    }

    @Test
    fun `excludes non media files in supported folders`() {
        assertFalse(
            SocialMediaAnalyzer.isSupportedSocialMedia(
                file(
                    path = "Android/media/com.whatsapp/backup.db",
                    category = FileCategory.DOCUMENTS
                )
            )
        )
    }

    @Test
    fun `groups only applications with real accessible files`() {
        val whatsappPhoto = file(
            path = "Android/media/com.whatsapp/WhatsApp/Media/photo.jpg",
            size = 20
        )
        val whatsappDownload = file(
            path = "Download/WhatsApp/document.pdf",
            category = FileCategory.DOWNLOADS,
            size = 30
        )
        val telegramVideo = file(
            path = "Telegram/Telegram Video/clip.mp4",
            category = FileCategory.VIDEOS,
            size = 40
        )

        val groups = SocialMediaAnalyzer.groups(
            listOf(whatsappPhoto, whatsappDownload, telegramVideo)
        )

        assertEquals(listOf("WhatsApp", "Telegram"), groups.map { it.applicationName })
        assertEquals(50, groups.first().totalBytes)
        assertEquals(2, groups.first().files.size)
    }

    private fun file(
        path: String,
        category: FileCategory = FileCategory.IMAGES,
        size: Long = 10
    ) = ScannedFile(
        name = path.substringAfterLast('/'),
        sizeBytes = size,
        lastModifiedMillis = 0,
        uri = "content://test/${path.hashCode()}",
        category = category,
        relativePath = path
    )
}
