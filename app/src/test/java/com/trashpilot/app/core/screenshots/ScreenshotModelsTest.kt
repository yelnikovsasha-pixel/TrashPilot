package com.trashpilot.app.core.screenshots

import org.junit.Assert.*
import org.junit.Test

class ScreenshotModelsTest {
    @Test fun `empty media store produces empty view`() = assertTrue(emptyList<ScreenshotItem>().screenshotsView("", ScreenshotSort.NEWEST).isEmpty())

    @Test fun `known screenshot paths and OEM variants classify`() {
        assertTrue(isConfidentScreenshotPath("Pictures/Screenshots/", "Screenshots", "Screenshot_1.png"))
        assertTrue(isConfidentScreenshotPath("DCIM/Screenshots/", "Screenshots", "2026-01-01.png"))
        assertTrue(isConfidentScreenshotPath("Pictures/ScreenCapture/", "ScreenCapture", "capture.webp"))
        assertTrue(isConfidentScreenshotPath("MIUI/screen shots/", "screen shots", "shot.jpg"))
    }

    @Test fun `ordinary photos and filename-only guesses are excluded`() {
        assertFalse(isConfidentScreenshotPath("DCIM/Camera/", "Camera", "Screenshot_1.png"))
        assertFalse(isConfidentScreenshotPath("Pictures/", "Pictures", "photo.jpg"))
        assertFalse(isConfidentScreenshotPath("Pictures/Screenshots/", "Screenshots", "archive.zip"))
    }

    @Test fun `sorting search and unknown timestamps are deterministic`() {
        val old = item("old.png", 10, 10)
        val newer = item("new.png", 20, 20)
        val unknown = item("unknown.png", 30, 0)
        val items = listOf(unknown, newer, old)
        assertEquals(listOf(old, newer, unknown), items.screenshotsView("", ScreenshotSort.OLDEST))
        assertEquals(listOf(unknown, newer, old), items.screenshotsView("", ScreenshotSort.LARGEST))
        assertEquals(listOf(newer), items.screenshotsView("new", ScreenshotSort.NEWEST))
    }

    @Test fun `time groups derive only from timestamps`() {
        val now = 10L * DAY
        assertEquals(ScreenshotGroup.TODAY, screenshotGroup(now - 1, now))
        assertEquals(ScreenshotGroup.THIS_WEEK, screenshotGroup(now - 2 * DAY, now))
        assertEquals(ScreenshotGroup.OLDER, screenshotGroup(now - 8 * DAY, now))
        assertEquals(ScreenshotGroup.DATE_UNAVAILABLE, screenshotGroup(0, now))
    }

    @Test fun `selection size and partial deletion count exact records`() {
        val one = item("one.png", 11, 1)
        val two = item("two.png", 22, 2)
        val selected = toggleScreenshotSelection(emptySet(), one.uri)
        assertEquals(setOf(one.uri), selected)
        assertEquals(11, selectedScreenshotBytes(listOf(one, two), selected))
        val partial = accountScreenshotDeletion(listOf(one, two), setOf(two.uri))
        assertEquals(22, partial.reclaimedBytes)
        assertEquals(listOf(two), partial.deleted)
        assertEquals(listOf(one), partial.failed)
    }

    @Test fun `stale entries remain failed when no deletion is verified`() {
        val stale = item("gone.png", 99, 1)
        assertEquals(listOf(stale), accountScreenshotDeletion(listOf(stale), emptySet()).failed)
    }

    @Test fun `large collections preserve every real item`() {
        val items = (1..10_000).map { item("shot-$it.png", it.toLong(), it.toLong()) }
        assertEquals(10_000, items.screenshotsView("", ScreenshotSort.NEWEST).size)
        assertEquals(10_000L, items.screenshotsView("", ScreenshotSort.NEWEST).first().timestampMillis)
    }

    private fun item(name: String, size: Long, time: Long) = ScreenshotItem("content://test/$name", name, size, time, "Screenshots", "Pictures/Screenshots/", 1080, 2400)
    private companion object { const val DAY = 24L * 60L * 60L * 1_000L }
}
