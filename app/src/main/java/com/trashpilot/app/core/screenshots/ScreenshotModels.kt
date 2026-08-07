package com.trashpilot.app.core.screenshots

enum class ScreenshotSort { NEWEST, OLDEST, LARGEST, SMALLEST }
enum class ScreenshotGroup { TODAY, THIS_WEEK, OLDER, DATE_UNAVAILABLE }

data class ScreenshotItem(
    val uri: String,
    val name: String,
    val sizeBytes: Long,
    val timestampMillis: Long,
    val folderName: String,
    val relativePath: String,
    val width: Int?,
    val height: Int?
)

data class ScreenshotDeletionAccounting(
    val deleted: List<ScreenshotItem>,
    val failed: List<ScreenshotItem>
) {
    val reclaimedBytes: Long = deleted.sumOf(ScreenshotItem::sizeBytes)
}

fun isConfidentScreenshotPath(relativePath: String, bucketName: String?, displayName: String): Boolean {
    val rawSegments = relativePath.replace('\\', '/').split('/').filter(String::isNotBlank)
    val segments = if (rawSegments.lastOrNull()?.equals(displayName, true) == true) rawSegments.dropLast(1) else rawSegments
    val folderSignals = (segments + bucketName.orEmpty()).map { it.trim().lowercase() }
    val recognizedFolder = folderSignals.any { segment ->
        segment in SCREENSHOT_DIRECTORIES || SCREENSHOT_DIRECTORY_PATTERNS.any(segment::contains)
    }
    if (!recognizedFolder) return false
    val extension = displayName.substringAfterLast('.', "").lowercase()
    return extension.isBlank() || extension in IMAGE_EXTENSIONS
}

fun List<ScreenshotItem>.screenshotsView(query: String, sort: ScreenshotSort): List<ScreenshotItem> {
    val comparator = when (sort) {
        ScreenshotSort.NEWEST -> compareByDescending<ScreenshotItem> { it.timestampMillis }
        ScreenshotSort.OLDEST -> compareBy { it.timestampMillis.takeIf { value -> value > 0 } ?: Long.MAX_VALUE }
        ScreenshotSort.LARGEST -> compareByDescending { it.sizeBytes }
        ScreenshotSort.SMALLEST -> compareBy { it.sizeBytes }
    }
    return asSequence()
        .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
        .sortedWith(comparator.thenBy { it.uri })
        .toList()
}

fun screenshotGroup(timestampMillis: Long, nowMillis: Long): ScreenshotGroup {
    if (timestampMillis <= 0) return ScreenshotGroup.DATE_UNAVAILABLE
    val age = (nowMillis - timestampMillis).coerceAtLeast(0)
    return when {
        age < DAY_MILLIS -> ScreenshotGroup.TODAY
        age < WEEK_MILLIS -> ScreenshotGroup.THIS_WEEK
        else -> ScreenshotGroup.OLDER
    }
}

fun selectedScreenshotBytes(items: List<ScreenshotItem>, selectedUris: Set<String>): Long =
    items.asSequence().filter { it.uri in selectedUris }.sumOf(ScreenshotItem::sizeBytes)

fun toggleScreenshotSelection(selected: Set<String>, uri: String): Set<String> =
    if (uri in selected) selected - uri else selected + uri

fun accountScreenshotDeletion(requested: List<ScreenshotItem>, deletedUris: Set<String>) =
    ScreenshotDeletionAccounting(
        deleted = requested.filter { it.uri in deletedUris },
        failed = requested.filterNot { it.uri in deletedUris }
    )

private val SCREENSHOT_DIRECTORIES = setOf("screenshots", "screenshot", "screen shots", "screen_shots")
private val SCREENSHOT_DIRECTORY_PATTERNS = setOf("screenshot", "screen shot", "screencapture", "screen capture", "screen_capture")
private val IMAGE_EXTENSIONS = setOf("avif", "bmp", "gif", "heic", "heif", "jpeg", "jpg", "png", "webp")
private const val DAY_MILLIS = 24L * 60L * 60L * 1_000L
private const val WEEK_MILLIS = 7L * DAY_MILLIS
