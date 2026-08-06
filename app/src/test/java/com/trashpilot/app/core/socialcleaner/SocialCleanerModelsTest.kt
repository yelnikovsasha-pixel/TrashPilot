package com.trashpilot.app.core.socialcleaner

import com.trashpilot.app.core.storage.FileCategory
import com.trashpilot.app.core.storage.ScannedFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SocialCleanerModelsTest {
    private val telegram = InstalledSocialApp(SUPPORTED_SOCIAL_APPS.first { it.name == "Telegram" })

    @Test fun `only installed application paths are matched`() {
        val file = file("Storage/Telegram/Telegram Images/photo.jpg", FileCategory.IMAGES)
        assertEquals("Telegram", socialMediaItem(file, listOf(telegram))?.app?.definition?.name)
        assertNull(socialMediaItem(file, emptyList()))
    }

    @Test fun `voice notes gifs stickers and downloads use real metadata`() {
        assertEquals(SocialMediaType.VOICE_NOTES, socialMediaItem(file("Storage/WhatsApp/Media/WhatsApp Voice Notes/note.opus", FileCategory.AUDIO), listOf(whatsApp()))?.type)
        assertEquals(SocialMediaType.GIFS, socialMediaItem(file("Storage/WhatsApp/Media/clip.gif", FileCategory.IMAGES), listOf(whatsApp()))?.type)
        assertEquals(SocialMediaType.STICKERS, socialMediaItem(file("Storage/WhatsApp/Media/WhatsApp Stickers/a.webp", FileCategory.IMAGES), listOf(whatsApp()))?.type)
        assertEquals(SocialMediaType.DOWNLOADS, socialMediaItem(file("Storage/Telegram/Downloads/file.pdf", FileCategory.DOCUMENTS), listOf(telegram))?.type)
    }

    @Test fun `search filters and sorting preserve real values`() {
        val older = socialMediaItem(file("Storage/Telegram/Images/holiday-old.jpg", FileCategory.IMAGES, 10, 10), listOf(telegram))!!
        val newer = socialMediaItem(file("Storage/Telegram/Images/holiday-new.jpg", FileCategory.IMAGES, 20, 20), listOf(telegram))!!
        val result = listOf(older, newer).socialMediaView("holiday", SocialMediaType.IMAGES, telegram.definition.packageName, SocialMediaSort.LARGEST)
        assertEquals(listOf(newer.file.uri, older.file.uri), result.map { it.file.uri })
    }

    @Test fun `MediaStore owner package overrides ambiguous folder names`() {
        val ownedByOtherApp = file("Storage/Telegram/photo.jpg", FileCategory.IMAGES).copy(ownerPackageName = "other.package")
        assertNull(socialMediaItem(ownedByOtherApp, listOf(telegram)))
        val ownedByTelegram = file("Storage/Pictures/photo.jpg", FileCategory.IMAGES).copy(ownerPackageName = telegram.definition.packageName)
        assertEquals("Telegram", socialMediaItem(ownedByTelegram, listOf(telegram))?.app?.definition?.name)
    }

    private fun whatsApp() = InstalledSocialApp(SUPPORTED_SOCIAL_APPS.first { it.name == "WhatsApp" })
    private fun file(path: String, category: FileCategory, size: Long = 1, modified: Long = 0): ScannedFile {
        val name = path.substringAfterLast('/')
        return ScannedFile(name, size, modified, "content://test/$name/$size", category, path)
    }
}
