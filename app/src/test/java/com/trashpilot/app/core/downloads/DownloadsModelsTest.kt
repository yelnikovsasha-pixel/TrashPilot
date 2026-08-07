package com.trashpilot.app.core.downloads

import com.trashpilot.app.core.storage.FileCategory
import com.trashpilot.app.core.storage.ScannedFile
import org.junit.Assert.*
import org.junit.Test

class DownloadsModelsTest {
    @Test fun `empty downloads have exact zero summary`() = assertEquals(DownloadSummary(0, 0, DownloadType.entries.associateWith { 0L }), emptyList<DownloadItem>().downloadSummary())

    @Test fun `categories use real extensions`() {
        assertEquals(DownloadType.IMAGES, file("photo.JPG").toDownloadItem()?.type)
        assertEquals(DownloadType.VIDEOS, file("clip.mp4").toDownloadItem()?.type)
        assertEquals(DownloadType.AUDIO, file("song.flac").toDownloadItem()?.type)
        assertEquals(DownloadType.DOCUMENTS, file("paper.pdf").toDownloadItem()?.type)
        assertEquals(DownloadType.ARCHIVES, file("backup.zip").toDownloadItem()?.type)
        assertEquals(DownloadType.APK, file("app.apk").toDownloadItem()?.type)
        assertEquals(DownloadType.OTHER, file("blob.bin").toDownloadItem()?.type)
    }

    @Test fun `non-download and stale missing path entries are excluded`() {
        assertNull(file("photo.jpg", path = "Pictures/photo.jpg", category = FileCategory.IMAGES).toDownloadItem())
    }

    @Test fun `sorting filtering and search are deterministic`() {
        val old = file("old.pdf", 10, 10).toDownloadItem()!!
        val newer = file("new.pdf", 20, 20).toDownloadItem()!!
        val unknown = file("unknown.pdf", 30, 0).toDownloadItem()!!
        val items = listOf(unknown, newer, old)
        assertEquals(listOf(old, newer, unknown), items.downloadsView("", null, DownloadSort.OLDEST))
        assertEquals(listOf(newer), items.downloadsView("new", DownloadType.DOCUMENTS, DownloadSort.NAME))
        assertEquals(listOf(old, newer, unknown), items.downloadsView("", null, DownloadSort.SMALLEST))
    }

    @Test fun `selection size and partial deletion count only successes`() {
        val one = file("one.zip", 11).toDownloadItem()!!
        val two = file("two.zip", 22).toDownloadItem()!!
        assertEquals(33, selectedDownloadBytes(listOf(one, two), setOf(one.file.uri, two.file.uri)))
        val partial = accountDownloadDeletion(listOf(one, two), setOf(two.file.uri))
        assertEquals(22, partial.reclaimedBytes)
        assertEquals(listOf(two), partial.deleted)
        assertEquals(listOf(one), partial.failed)
    }

    @Test fun `large collections stay complete`() {
        val items = (1..10_000).map { file("file-$it.bin", it.toLong()).toDownloadItem()!! }
        assertEquals(10_000, items.downloadsView("", null, DownloadSort.LARGEST).size)
        assertEquals(10_000L, items.downloadsView("", null, DownloadSort.LARGEST).first().file.sizeBytes)
    }

    private fun file(name: String, size: Long = 1, modified: Long = 1, path: String = "Download/$name", category: FileCategory = FileCategory.DOWNLOADS) =
        ScannedFile(name, size, modified, "content://test/$name/$size", category, path)
}
