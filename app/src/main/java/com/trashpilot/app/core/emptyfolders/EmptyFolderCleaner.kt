package com.trashpilot.app.core.emptyfolders

import android.content.ContentResolver
import android.provider.DocumentsContract
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

class EmptyFolderCleaner(private val resolver: ContentResolver) {
    suspend fun delete(folders: List<EmptyFolderItem>): EmptyFolderDeletionResult = withContext(Dispatchers.IO) {
        val deleted = mutableListOf<EmptyFolderItem>()
        val failed = mutableListOf<EmptyFolderItem>()
        folders.forEach { folder ->
            coroutineContext.ensureActive()
            val uri = folder.uri.toUri()
            val removed = runCatching { DocumentsContract.deleteDocument(resolver, uri) }.getOrDefault(false)
            val stillExists = if (!removed) true else runCatching {
                resolver.query(uri, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID), null, null, null)
                    ?.use { it.moveToFirst() } ?: false
            }.getOrDefault(false)
            if (removed && !stillExists) deleted += folder else failed += folder
        }
        EmptyFolderDeletionResult(deleted, failed)
    }
}
