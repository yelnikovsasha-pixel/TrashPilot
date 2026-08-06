package com.trashpilot.app.core.hiddenfiles

import com.trashpilot.app.core.largefiles.LargeFileItem
import com.trashpilot.app.core.largefiles.LargeFileSort
import com.trashpilot.app.core.largefiles.LargeFileType
import com.trashpilot.app.core.largefiles.toLargeFileItem
import com.trashpilot.app.core.storage.ScannedFile

fun isProtectedStoragePath(pathSegments: List<String>): Boolean {
    val relative = pathSegments.drop(1).map(String::trim)
    if (relative.isEmpty()) return false
    if (relative.first().equals("data", true) || relative.first().equals("system", true)) return true
    return relative.size >= 2 && relative[0].equals("Android", true) &&
        (relative[1].equals("data", true) || relative[1].equals("obb", true))
}

fun isProtectedTreeDocumentId(documentId: String): Boolean {
    val path = documentId.substringAfter(':', documentId).replace('\\', '/').trim('/')
    val segments = path.split('/').filter(String::isNotBlank)
    if (segments.isEmpty()) return false
    if (segments.first().equals("data", true) || segments.first().equals("system", true)) return true
    return segments.size >= 2 && segments[0].equals("Android", true) &&
        (segments[1].equals("data", true) || segments[1].equals("obb", true))
}

fun ScannedFile.isHiddenUserFile(): Boolean {
    val segments = relativePath.replace('\\', '/').split('/').filter(String::isNotBlank)
    return !isProtectedStoragePath(segments) && segments.any {
        it.length > 1 && it.startsWith('.') && it != "." && it != ".."
    }
}

fun hiddenUserFiles(files: List<ScannedFile>): List<ScannedFile> {
    val hiddenMediaDirectories = files.asSequence()
        .filter { it.name.equals(".nomedia", ignoreCase = true) && it.isHiddenUserFile() }
        .map { it.relativePath.replace('\\', '/').substringBeforeLast('/', "") }
        .filter(String::isNotBlank)
        .toSet()
    return files.filter { file ->
        file.isHiddenUserFile() || hiddenMediaDirectories.any { directory ->
            val path = file.relativePath.replace('\\', '/')
            path.startsWith("$directory/")
        }
    }
}

fun List<LargeFileItem>.hiddenFilesView(
    filter: LargeFileType?,
    search: String,
    sort: LargeFileSort
): List<LargeFileItem> {
    val eligible = asSequence()
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

fun ScannedFile.toHiddenFileItem(): LargeFileItem = toLargeFileItem()
