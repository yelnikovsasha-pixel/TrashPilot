package com.trashpilot.app.core.cache

import org.junit.Assert.assertEquals
import org.junit.Test

class CacheModelsTest {
    private val apps = listOf(
        CacheApp("z.pkg", "Zulu", 20, 100),
        CacheApp("a.pkg", "Alpha", 50, 50),
        CacheApp("unknown.pkg", "Unknown", null, 200)
    )

    @Test fun `snapshot totals only cache sizes Android exposed`() {
        val snapshot = CacheSnapshot(1, apps)
        assertEquals(70, snapshot.totalCacheBytes)
        assertEquals(2, snapshot.measurableAppCount)
    }

    @Test fun `largest sorting places unavailable values last`() {
        assertEquals(
            listOf("Alpha", "Zulu", "Unknown"),
            apps.filteredAndSorted("", CacheSort.LARGEST).map(CacheApp::label)
        )
    }

    @Test fun `search matches labels and package names without case`() {
        assertEquals(
            listOf("Zulu"),
            apps.filteredAndSorted("Z.PKG", CacheSort.APP_NAME).map(CacheApp::label)
        )
    }

    @Test fun `recent sorting uses real package update time`() {
        assertEquals(
            listOf("Unknown", "Zulu", "Alpha"),
            apps.filteredAndSorted("", CacheSort.RECENTLY_UPDATED).map(CacheApp::label)
        )
    }
}
