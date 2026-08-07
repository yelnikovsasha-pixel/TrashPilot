package com.trashpilot.app.core.emptyfolders

import org.junit.Assert.*
import org.junit.Test

class EmptyFolderModelsTest {
    @Test fun `empty tree has no deletable root`() {
        val nodes = listOf(FolderObservation("root", null, false, true))
        assertTrue(topLevelVerifiedEmptyIds(nodes, "root").isEmpty())
    }

    @Test fun `truly empty folder is verified`() {
        val nodes = listOf(FolderObservation("root", null, false, true), FolderObservation("empty", "root", false, true))
        assertEquals(setOf("empty"), topLevelVerifiedEmptyIds(nodes, "root"))
    }

    @Test fun `files and non-empty descendants exclude ancestors`() {
        val nodes = listOf(FolderObservation("root", null, false, true), FolderObservation("parent", "root", false, true), FolderObservation("child", "parent", true, true))
        val states = verifyFolders(nodes, "root")
        assertEquals(FolderVerification.CONTENT, states["parent"])
        assertTrue(topLevelVerifiedEmptyIds(nodes, "root").isEmpty())
    }

    @Test fun `nested empty subtree collapses to highest safe folder`() {
        val nodes = listOf(FolderObservation("root", null, false, true), FolderObservation("parent", "root", false, true), FolderObservation("child", "parent", false, true))
        assertEquals(setOf("parent"), topLevelVerifiedEmptyIds(nodes, "root"))
    }

    @Test fun `inaccessible and missing descendants remain unknown`() {
        val inaccessible = listOf(FolderObservation("root", null, false, true), FolderObservation("unknown", "root", false, false))
        assertEquals(FolderVerification.UNKNOWN, verifyFolders(inaccessible, "root")["root"])
        val missing = listOf(FolderObservation("root", null, false, true), FolderObservation("parent", "root", false, true), FolderObservation("ghost", "missing", false, true))
        assertNotEquals(FolderVerification.EMPTY, verifyFolders(missing, "root")["ghost"])
    }

    @Test fun `protected locations are excluded`() {
        assertTrue(isProtectedEmptyFolderPath("primary:DCIM", "primary:"))
        assertTrue(isProtectedEmptyFolderPath("primary:Android", "primary:"))
        assertTrue(isProtectedEmptyFolderPath("primary:Android/data/com.trashpilot.app/cache", "primary:Android/data"))
        assertFalse(isProtectedEmptyFolderPath("primary:Android/data/example/cache", "primary:Android/data"))
        assertFalse(isProtectedEmptyFolderPath("primary:UserFolder/empty", "primary:"))
    }

    @Test fun `search sorting selection and deletion accounting use exact folders`() {
        val alpha = item("Alpha", "Parent B", 20)
        val beta = item("Beta", "Parent A", 10)
        val unknown = item("Unknown", "Parent C", 0)
        val items = listOf(alpha, beta, unknown)
        assertEquals(listOf(beta), items.emptyFoldersView("parent a", EmptyFolderSort.NAME))
        assertEquals(listOf(beta, alpha, unknown), items.emptyFoldersView("", EmptyFolderSort.OLDEST))
        assertEquals(listOf(beta, alpha, unknown), items.emptyFoldersView("", EmptyFolderSort.PARENT))
        val result = accountEmptyFolderDeletion(items, setOf(alpha.uri))
        assertEquals(listOf(alpha), result.deleted)
        assertEquals(listOf(beta, unknown), result.failed)
        val readOnly = unknown.copy(canDelete = false)
        assertEquals(setOf(alpha.uri), toggleEmptyFolderSelection(emptySet(), alpha))
        assertTrue(toggleEmptyFolderSelection(emptySet(), readOnly).isEmpty())
        assertEquals(setOf(alpha.uri, beta.uri), selectAllDeletableFolders(emptySet(), listOf(alpha, beta, readOnly)))
    }

    @Test fun `large trees are evaluated without losing results`() {
        val nodes = buildList { add(FolderObservation("root", null, false, true)); repeat(10_000) { add(FolderObservation("folder-$it", "root", false, true)) } }
        assertEquals(10_000, topLevelVerifiedEmptyIds(nodes, "root").size)
    }

    private fun item(name: String, parent: String, modified: Long) = EmptyFolderItem("content://$name", name, parent, "$parent/$name", modified, true)
}
