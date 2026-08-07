package com.trashpilot.app.core.photoquality

enum class PhotoReason { LOW_RESOLUTION, POSSIBLY_BLURRY, VERY_DARK, VERY_BRIGHT }
enum class PhotoQualityFilter { ALL, LOW_RESOLUTION, POSSIBLY_BLURRY, VERY_DARK, VERY_BRIGHT }
enum class PhotoQualitySort { LARGEST, SMALLEST, NEWEST, OLDEST }

data class PhotoQualityThresholds(
    val minimumWidth: Int = 720,
    val minimumHeight: Int = 720,
    val minimumPixels: Long = 1_000_000,
    val blurVarianceMaximum: Double = 45.0,
    val darkMeanMaximum: Double = 35.0,
    val darkPixelMaximum: Int = 55,
    val darkPixelFractionMinimum: Double = 0.85,
    val brightMeanMinimum: Double = 225.0,
    val brightPixelMinimum: Int = 210,
    val brightPixelFractionMinimum: Double = 0.85,
    val analysisMaximumSide: Int = 256
)

data class PhotoMetrics(
    val meanLuminance: Double,
    val darkPixelFraction: Double,
    val brightPixelFraction: Double,
    val laplacianVariance: Double
)

data class PhotoQualityItem(
    val uri: String,
    val name: String,
    val sizeBytes: Long,
    val timestampMillis: Long,
    val folderName: String,
    val relativePath: String,
    val width: Int,
    val height: Int,
    val reasons: Set<PhotoReason>,
    val metrics: PhotoMetrics?
)

data class PhotoDeletionAccounting(val deleted: List<PhotoQualityItem>, val failed: List<PhotoQualityItem>) {
    val reclaimedBytes = deleted.sumOf(PhotoQualityItem::sizeBytes)
}

fun classifyPhoto(
    width: Int,
    height: Int,
    metrics: PhotoMetrics?,
    thresholds: PhotoQualityThresholds = PhotoQualityThresholds()
): Set<PhotoReason> = buildSet {
    if (width > 0 && height > 0 &&
        (width.toLong() * height < thresholds.minimumPixels ||
            minOf(width, height) < minOf(thresholds.minimumWidth, thresholds.minimumHeight))) {
        add(PhotoReason.LOW_RESOLUTION)
    }
    metrics ?: return@buildSet
    if (metrics.laplacianVariance < thresholds.blurVarianceMaximum) add(PhotoReason.POSSIBLY_BLURRY)
    if (metrics.meanLuminance < thresholds.darkMeanMaximum && metrics.darkPixelFraction >= thresholds.darkPixelFractionMinimum) add(PhotoReason.VERY_DARK)
    if (metrics.meanLuminance > thresholds.brightMeanMinimum && metrics.brightPixelFraction >= thresholds.brightPixelFractionMinimum) add(PhotoReason.VERY_BRIGHT)
}

fun List<PhotoQualityItem>.photoQualityView(filter: PhotoQualityFilter, sort: PhotoQualitySort): List<PhotoQualityItem> {
    val reason = when (filter) {
        PhotoQualityFilter.ALL -> null
        PhotoQualityFilter.LOW_RESOLUTION -> PhotoReason.LOW_RESOLUTION
        PhotoQualityFilter.POSSIBLY_BLURRY -> PhotoReason.POSSIBLY_BLURRY
        PhotoQualityFilter.VERY_DARK -> PhotoReason.VERY_DARK
        PhotoQualityFilter.VERY_BRIGHT -> PhotoReason.VERY_BRIGHT
    }
    val comparator = when (sort) {
        PhotoQualitySort.LARGEST -> compareByDescending<PhotoQualityItem> { it.sizeBytes }
        PhotoQualitySort.SMALLEST -> compareBy { it.sizeBytes }
        PhotoQualitySort.NEWEST -> compareByDescending { it.timestampMillis }
        PhotoQualitySort.OLDEST -> compareBy { it.timestampMillis.takeIf { value -> value > 0 } ?: Long.MAX_VALUE }
    }
    return asSequence().filter { reason == null || reason in it.reasons }.sortedWith(comparator.thenBy { it.uri }).toList()
}

fun selectedPhotoBytes(items: List<PhotoQualityItem>, selectedUris: Set<String>) =
    items.asSequence().filter { it.uri in selectedUris }.sumOf(PhotoQualityItem::sizeBytes)

fun togglePhotoSelection(selected: Set<String>, uri: String): Set<String> =
    if (uri in selected) selected - uri else selected + uri

fun accountPhotoDeletion(requested: List<PhotoQualityItem>, deletedUris: Set<String>) =
    PhotoDeletionAccounting(requested.filter { it.uri in deletedUris }, requested.filterNot { it.uri in deletedUris })
