package com.trashpilot.app.core.emptyfolders

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

data class EmptyFolderScanProgress(val inspectedFolders: Int, val verifiedFolders: Int)
data class EmptyFolderScanResult(val folders: List<EmptyFolderItem>, val partialAccess: Boolean)

class EmptyFolderSafScanner(private val resolver: ContentResolver) {
    suspend fun scan(
        treeUri: Uri,
        onProgress: suspend (EmptyFolderItem?, EmptyFolderScanProgress) -> Unit = { _, _ -> }
    ): EmptyFolderScanResult = withContext(Dispatchers.IO) {
        val rootId = DocumentsContract.getTreeDocumentId(treeUri)
        val observations = mutableListOf<FolderObservation>()
        val metadata = mutableMapOf<String, FolderMetadata>()
        val pending = ArrayDeque(listOf(Pending(rootId, null, emptyList())))
        val visited = mutableSetOf<String>()
        var partial = false
        var inspected = 0
        while (pending.isNotEmpty()) {
            coroutineContext.ensureActive()
            val directory = pending.removeFirst()
            if (!visited.add(directory.id)) { partial = true; continue }
            val own = queryDocument(treeUri, directory.id)
            val name = own?.name.orEmpty()
            val path = directory.path + name.takeIf(String::isNotBlank).orEmpty()
            val protected = directory.id != rootId && isProtectedEmptyFolderPath(directory.id, rootId)
            var readable = false
            var hasFile = false
            if (!protected) {
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, directory.id)
                try {
                    val cursor = resolver.query(childrenUri, PROJECTION, null, null, null)
                    if (cursor != null) cursor.use {
                        readable = true
                        val id = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                        val childName = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                        val mime = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                        val modified = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                        val flags = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_FLAGS)
                        while (it.moveToNext()) {
                            coroutineContext.ensureActive()
                            val childId = it.getString(id)
                            if (it.getString(mime) == DocumentsContract.Document.MIME_TYPE_DIR) {
                                metadata[childId] = FolderMetadata(it.getString(childName).orEmpty(), if (it.isNull(modified)) 0 else it.getLong(modified), it.getInt(flags))
                                pending.addLast(Pending(childId, directory.id, path))
                            } else hasFile = true
                        }
                    } else partial = true
                } catch (_: SecurityException) { partial = true }
                catch (_: IllegalArgumentException) { partial = true }
            }
            val meta = metadata[directory.id] ?: own ?: FolderMetadata(name, 0, 0)
            metadata[directory.id] = meta
            observations += FolderObservation(directory.id, directory.parentId, hasFile, readable, protected)
            inspected++
            onProgress(null, EmptyFolderScanProgress(inspected, 0))
        }
        val verified = topLevelVerifiedEmptyIds(observations, rootId)
        val parentById = observations.associate { it.id to it.parentId }
        val folders = verified.mapNotNull { id ->
            val meta = metadata[id] ?: return@mapNotNull null
            val parentName = parentById[id]?.let { metadata[it]?.name }.orEmpty()
            val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, id).toString()
            EmptyFolderItem(uri, meta.name, parentName, buildRelativePath(id, rootId), meta.modified,
                meta.flags and DocumentsContract.Document.FLAG_SUPPORTS_DELETE != 0)
        }.sortedBy { it.relativePath.lowercase() }
        folders.forEachIndexed { index, folder -> onProgress(folder, EmptyFolderScanProgress(inspected, index + 1)) }
        EmptyFolderScanResult(folders, partial || observations.any { !it.readable && !it.protected })
    }

    private fun queryDocument(treeUri: Uri, id: String): FolderMetadata? = runCatching {
        resolver.query(DocumentsContract.buildDocumentUriUsingTree(treeUri, id), PROJECTION, null, null, null)?.use {
            if (!it.moveToFirst()) null else FolderMetadata(
                it.getString(it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)).orEmpty(),
                it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED).let { index -> if (it.isNull(index)) 0 else it.getLong(index) },
                it.getInt(it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_FLAGS))
            )
        }
    }.getOrNull()

    private fun buildRelativePath(id: String, rootId: String): String {
        val value = id.substringAfter(':', id).replace('\\', '/').trim('/')
        val root = rootId.substringAfter(':', rootId).replace('\\', '/').trim('/')
        return value.removePrefix(root).trim('/').ifBlank { value }
    }

    private data class Pending(val id: String, val parentId: String?, val path: List<String>)
    private data class FolderMetadata(val name: String, val modified: Long, val flags: Int)
    private companion object {
        val PROJECTION = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.COLUMN_LAST_MODIFIED, DocumentsContract.Document.COLUMN_FLAGS)
    }
}
