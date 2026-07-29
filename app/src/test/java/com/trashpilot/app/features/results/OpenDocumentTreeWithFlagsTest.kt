package com.trashpilot.app.features.scanner

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Test

class OpenDocumentTreeWithFlagsTest {
    @Test
    fun persistableFlags_keepReadAndWriteOnly() {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
            Intent.FLAG_ACTIVITY_NEW_TASK

        assertEquals(
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            persistableDocumentFlags(flags)
        )
    }

    @Test
    fun persistableFlags_doNotInventMissingGrant() {
        assertEquals(
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
            persistableDocumentFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        )
    }
}
