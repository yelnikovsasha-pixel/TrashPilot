package com.trashpilot.app.features.scanner

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract

data class SelectedDocumentTree(
    val uri: Uri,
    val persistableFlags: Int
)

internal fun persistableDocumentFlags(intentFlags: Int): Int =
    intentFlags and (
        Intent.FLAG_GRANT_READ_URI_PERMISSION or
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )

class OpenDocumentTreeWithFlags : ActivityResultContract<Uri?, SelectedDocumentTree?>() {
    override fun createIntent(context: Context, input: Uri?): Intent =
        Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
            )
            input?.let { putExtra("android.provider.extra.INITIAL_URI", it) }
        }

    override fun parseResult(resultCode: Int, intent: Intent?): SelectedDocumentTree? {
        if (resultCode != Activity.RESULT_OK) return null
        val uri = intent?.data ?: return null
        val returnedFlags = persistableDocumentFlags(intent.flags)
        return SelectedDocumentTree(
            uri = uri,
            persistableFlags = returnedFlags
        )
    }
}
