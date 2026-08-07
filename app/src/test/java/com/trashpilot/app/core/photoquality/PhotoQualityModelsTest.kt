package com.trashpilot.app.core.photoquality

import org.junit.Assert.*
import org.junit.Test

class PhotoQualityModelsTest {
    private val thresholds = PhotoQualityThresholds()

    @Test fun noImagesProducesEmptyView() = assertTrue(emptyList<PhotoQualityItem>().photoQualityView(PhotoQualityFilter.ALL, PhotoQualitySort.LARGEST).isEmpty())

    @Test fun lowResolutionUsesRealDimensions() {
        assertEquals(setOf(PhotoReason.LOW_RESOLUTION), classifyPhoto(640, 480, null, thresholds))
        assertTrue(classifyPhoto(1920, 1080, null, thresholds).isEmpty())
    }

    @Test fun resolutionBoundaryIsNotFlagged() = assertFalse(PhotoReason.LOW_RESOLUTION in classifyPhoto(1000, 1000, null, thresholds))

    @Test fun laplacianVarianceSeparatesFlatAndEdgedSamples() {
        val flat = measureLuminance(IntArray(25) { 128 }, 5, 5, thresholds)!!
        val checker = measureLuminance(IntArray(25) { if ((it / 5 + it % 5) % 2 == 0) 0 else 255 }, 5, 5, thresholds)!!
        assertTrue(flat.laplacianVariance < thresholds.blurVarianceMaximum)
        assertTrue(checker.laplacianVariance > thresholds.blurVarianceMaximum)
    }

    @Test fun darkAndBrightMetricsUseMeasuredDistribution() {
        val dark = measureLuminance(IntArray(100) { 20 }, 10, 10, thresholds)!!
        val bright = measureLuminance(IntArray(100) { 240 }, 10, 10, thresholds)!!
        assertTrue(PhotoReason.VERY_DARK in classifyPhoto(2000, 1500, dark, thresholds))
        assertTrue(PhotoReason.VERY_BRIGHT in classifyPhoto(2000, 1500, bright, thresholds))
    }

    @Test fun thresholdBoundariesAreConservative() {
        val exact = PhotoMetrics(thresholds.darkMeanMaximum, thresholds.darkPixelFractionMinimum, 0.0, thresholds.blurVarianceMaximum)
        assertFalse(PhotoReason.VERY_DARK in classifyPhoto(2000, 1500, exact, thresholds))
        assertFalse(PhotoReason.POSSIBLY_BLURRY in classifyPhoto(2000, 1500, exact, thresholds))
    }

    @Test fun multipleMeasuredReasonsAreRetained() {
        val metrics = PhotoMetrics(20.0, .95, 0.0, 10.0)
        assertEquals(setOf(PhotoReason.LOW_RESOLUTION, PhotoReason.POSSIBLY_BLURRY, PhotoReason.VERY_DARK), classifyPhoto(640, 480, metrics, thresholds))
    }

    @Test fun filteringSortingAndSelectionUseOnlyRealItems() {
        val items = listOf(item("a", 10, 100, setOf(PhotoReason.VERY_DARK)), item("b", 30, 50, setOf(PhotoReason.LOW_RESOLUTION)))
        assertEquals("a", items.photoQualityView(PhotoQualityFilter.VERY_DARK, PhotoQualitySort.LARGEST).single().uri)
        assertEquals(listOf("b", "a"), items.photoQualityView(PhotoQualityFilter.ALL, PhotoQualitySort.LARGEST).map { it.uri })
        assertEquals(30, selectedPhotoBytes(items, setOf("b")))
        assertEquals(setOf("a"), togglePhotoSelection(emptySet(), "a"))
    }

    @Test fun partialDeletionCountsOnlyConfirmedMissingUris() {
        val items = listOf(item("a", 10), item("b", 30), item("stale", 7))
        val report = accountPhotoDeletion(items, setOf("a", "stale"))
        assertEquals(17, report.reclaimedBytes)
        assertEquals(listOf("b"), report.failed.map { it.uri })
    }

    @Test fun corruptMetricInputIsIgnoredAndLargeCollectionsRemainDeterministic() {
        assertNull(measureLuminance(IntArray(2), 10, 10, thresholds))
        val items = (0 until 10_000).map { item(it.toString(), it.toLong(), it.toLong()) }
        assertEquals("9999", items.photoQualityView(PhotoQualityFilter.ALL, PhotoQualitySort.LARGEST).first().uri)
    }

    private fun item(uri: String, size: Long, date: Long = 0, reasons: Set<PhotoReason> = setOf(PhotoReason.LOW_RESOLUTION)) =
        PhotoQualityItem(uri, uri, size, date, "Pictures", "Pictures/", 640, 480, reasons, null)
}
