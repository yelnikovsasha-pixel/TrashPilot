package com.trashpilot.app.core.downloads

import com.trashpilot.app.core.storage.FileCategory
import com.trashpilot.app.core.storage.ScannedFile

enum class DownloadType { IMAGES, VIDEOS, AUDIO, DOCUMENTS, ARCHIVES, APK, OTHER }
enum class DownloadSort { LARGEST, SMALLEST, NEWEST, OLDEST, NAME }

data class DownloadItem(val file: ScannedFile, val type: DownloadType)

data class DownloadSummary(val fileCount: Int, val totalBytes: Long, val bytesByType: Map<DownloadType, Long>)

data class DownloadDeletionAccounting(
    val deleted: List<DownloadItem>,
    val failed: List<DownloadItem>
) {
    val reclaimedBytes: Long = deleted.sumOf { it.file.sizeBytes }
}

fun ScannedFile.toDownloadItem(): DownloadItem? {
    val segments = relativePath.replace('\\', '/').split('/').filter(String::isNotBlank)
    if (category != FileCategory.DOWNLOADS && segments.none { it.equals("download", true) || it.equals("downloads", true) }) return null
    val extension = name.substringAfterLast('.', "").lowercase()
    val type = when {
        extension == "apk" -> DownloadType.APK
        extension in ARCHIVE_EXTENSIONS -> DownloadType.ARCHIVES
        extension in IMAGE_EXTENSIONS -> DownloadType.IMAGES
        extension in VIDEO_EXTENSIONS -> DownloadType.VIDEOS
        extension in AUDIO_EXTENSIONS -> DownloadType.AUDIO
        extension in DOCUMENT_EXTENSIONS -> DownloadType.DOCUMENTS
        else -> DownloadType.OTHER
    }
    return DownloadItem(this, type)
}

fun List<DownloadItem>.downloadsView(query: String, type: DownloadType?, sort: DownloadSort): List<DownloadItem> {
    val comparator = when (sort) {
        DownloadSort.LARGEST -> compareByDescending<DownloadItem> { it.file.sizeBytes }
        DownloadSort.SMALLEST -> compareBy { it.file.sizeBytes }
        DownloadSort.NEWEST -> compareByDescending { it.file.lastModifiedMillis }
        DownloadSort.OLDEST -> compareBy { it.file.lastModifiedMillis.takeIf { value -> value > 0 } ?: Long.MAX_VALUE }
        DownloadSort.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { item: DownloadItem -> item.file.name }
    }.thenBy { it.file.uri }
    return asSequence()
        .filter { query.isBlank() || it.file.name.contains(query, ignoreCase = true) }
        .filter { type == null || it.type == type }
        .sortedWith(comparator)
        .toList()
}

fun List<DownloadItem>.downloadSummary() = DownloadSummary(
    fileCount = size,
    totalBytes = sumOf { it.file.sizeBytes },
    bytesByType = DownloadType.entries.associateWith { type -> filter { it.type == type }.sumOf { it.file.sizeBytes } }
)

fun selectedDownloadBytes(items: List<DownloadItem>, selectedUris: Set<String>): Long =
    items.asSequence().filter { it.file.uri in selectedUris }.sumOf { it.file.sizeBytes }

fun accountDownloadDeletion(
    requested: List<DownloadItem>,
    successfullyDeletedUris: Set<String>
): DownloadDeletionAccounting = DownloadDeletionAccounting(
    deleted = requested.filter { it.file.uri in successfullyDeletedUris },
    failed = requested.filterNot { it.file.uri in successfullyDeletedUris }
)

private val IMAGE_EXTENSIONS = setOf("avif", "bmp", "gif", "heic", "heif", "jpeg", "jpg", "png", "svg", "webp")
private val VIDEO_EXTENSIONS = setOf("3gp", "avi", "m4v", "mkv", "mov", "mp4", "mpeg", "mpg", "webm")
private val AUDIO_EXTENSIONS = setOf("aac", "flac", "m4a", "mp3", "ogg", "opus", "wav", "wma")
private val DOCUMENT_EXTENSIONS = setOf("csv", "doc", "docx", "epub", "html", "md", "odt", "pdf", "ppt", "pptx", "rtf", "txt", "xls", "xlsx", "xml")
private val ARCHIVE_EXTENSIONS = setOf("7z", "bz2", "gz", "rar", "tar", "tgz", "xz", "zip")
