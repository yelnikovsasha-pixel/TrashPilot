package com.trashpilot.app.core.largefiles

import com.trashpilot.app.core.storage.FileCategory
import com.trashpilot.app.core.storage.ScannedFile

enum class LargeFileType { VIDEOS, IMAGES, DOCUMENTS, AUDIO, ARCHIVES, APK, OTHER }
enum class LargeFileSort { LARGEST, SMALLEST, NEWEST, OLDEST, FILE_NAME }

enum class LargeFileThreshold(val bytes: Long) {
    MB_50(50L * 1024 * 1024),
    MB_100(100L * 1024 * 1024),
    MB_250(250L * 1024 * 1024),
    MB_500(500L * 1024 * 1024),
    GB_1(1024L * 1024 * 1024)
}

data class LargeFileItem(val file: ScannedFile, val type: LargeFileType) {
    val folderName: String = file.relativePath.replace('\\', '/').split('/')
        .filter(String::isNotBlank).dropLast(1).lastOrNull().orEmpty()
}

fun ScannedFile.toLargeFileItem(): LargeFileItem {
    val extension = name.substringAfterLast('.', "").lowercase()
    val type = when {
        extension in ARCHIVE_EXTENSIONS -> LargeFileType.ARCHIVES
        category == FileCategory.VIDEOS -> LargeFileType.VIDEOS
        category == FileCategory.IMAGES -> LargeFileType.IMAGES
        category == FileCategory.DOCUMENTS -> LargeFileType.DOCUMENTS
        category == FileCategory.AUDIO -> LargeFileType.AUDIO
        category == FileCategory.APK_FILES || extension == "apk" -> LargeFileType.APK
        else -> LargeFileType.OTHER
    }
    return LargeFileItem(this, type)
}

fun List<LargeFileItem>.largeFilesView(
    threshold: LargeFileThreshold,
    filter: LargeFileType?,
    search: String,
    sort: LargeFileSort
): List<LargeFileItem> {
    val eligible = asSequence()
        .filter { it.file.sizeBytes >= threshold.bytes }
        .filter { filter == null || it.type == filter }
        .filter { search.isBlank() || it.file.name.contains(search, ignoreCase = true) }
    val comparator = when (sort) {
        LargeFileSort.LARGEST -> compareByDescending<LargeFileItem> { it.file.sizeBytes }
        LargeFileSort.SMALLEST -> compareBy { it.file.sizeBytes }
        LargeFileSort.NEWEST -> compareByDescending { it.file.lastModifiedMillis }
        LargeFileSort.OLDEST -> compareBy { it.file.lastModifiedMillis.takeIf { time -> time > 0 } ?: Long.MAX_VALUE }
        LargeFileSort.FILE_NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.file.name }
    }
    return eligible.sortedWith(comparator.thenBy { it.file.uri }).toList()
}

private val ARCHIVE_EXTENSIONS = setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz", "tgz")
