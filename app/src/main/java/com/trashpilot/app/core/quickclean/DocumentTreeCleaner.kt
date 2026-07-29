package com.trashpilot.app.core.quickclean

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DocumentTreeCleaner(
    private val contentResolver: ContentResolver
) {
    suspend fun clean(candidates: List<DisposableCandidate>): CleaningReport =
        withContext(Dispatchers.IO) {
            val deleted = DisposableCategory.entries.associateWith { 0 }.toMutableMap()
            val failures = mutableListOf<FailedCleanItem>()
            var reclaimedBytes = 0L

            candidates.forEach { candidate ->
                runCatching {
                    DocumentsContract.deleteDocument(
                        contentResolver,
                        Uri.parse(candidate.uri)
                    )
                }.fold(
                    onSuccess = { removed ->
                        if (removed) {
                            deleted[candidate.category] = deleted.getValue(candidate.category) + 1
                            reclaimedBytes += candidate.sizeBytes
                        } else {
                            failures += FailedCleanItem(candidate, null)
                        }
                    },
                    onFailure = { failures += FailedCleanItem(candidate, it.message) }
                )
            }

            CleaningReport(
                reclaimedBytes = reclaimedBytes,
                deletedByCategory = deleted,
                failedItems = failures
            )
        }
}
