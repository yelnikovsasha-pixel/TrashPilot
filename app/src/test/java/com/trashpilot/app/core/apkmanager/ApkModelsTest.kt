package com.trashpilot.app.core.apkmanager

import com.trashpilot.app.core.storage.FileCategory
import com.trashpilot.app.core.storage.ScannedFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApkModelsTest {
    @Test fun `APK detection uses the real filename extension`() {
        assertTrue(file("Installer.APK", 1).isApkInstaller())
        assertFalse(file("Installer.apk.zip", 1).isApkInstaller())
    }

    @Test fun `parent folder contains no full path`() {
        val item = ApkFileItem(file("app.apk", 1, path = "Storage/Downloads/app.apk"), null)
        assertEquals("Downloads", item.parentFolder)
    }

    @Test fun `view searches metadata and filters unreadable files`() {
        val readable = ApkFileItem(file("release.apk", 10), ApkMetadata("com.example", "1.0", "Example", null))
        val unreadable = ApkFileItem(file("unknown.apk", 20), null)
        assertEquals(listOf(readable), listOf(readable, unreadable).apkView("Example", ApkSort.LARGEST, ApkFilter.VALID))
        assertEquals(listOf(unreadable), listOf(readable, unreadable).apkView("", ApkSort.LARGEST, ApkFilter.UNREADABLE))
    }

    @Test fun `sorting uses exact sizes and keeps unknown oldest dates last`() {
        val unknown = ApkFileItem(file("unknown.apk", 30, modified = 0), null)
        val old = ApkFileItem(file("old.apk", 10, modified = 10), null)
        val newer = ApkFileItem(file("new.apk", 20, modified = 20), null)
        assertEquals(listOf(old, newer, unknown), listOf(unknown, newer, old).apkView("", ApkSort.OLDEST, ApkFilter.ALL))
    }

    private fun file(name: String, size: Long, modified: Long = 0, path: String = "Downloads/$name") =
        ScannedFile(name, size, modified, "content://test/$name/$size", FileCategory.APK_FILES, path)
}
