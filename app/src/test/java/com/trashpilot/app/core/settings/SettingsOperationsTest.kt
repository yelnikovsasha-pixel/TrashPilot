package com.trashpilot.app.core.settings

import com.trashpilot.app.core.cache.clearCacheRoots
import com.trashpilot.app.core.trashdna.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class SettingsOperationsTest {
    @Test fun ownCacheClearingRemovesOnlyChildrenAndReportsActualBytes() {
        val root = createTempDirectory("trashpilot-cache-test-").toFile()
        val nested = File(root, "nested").apply { mkdirs() }
        File(nested, "cache.bin").writeBytes(ByteArray(128) { 1 })
        try {
            assertEquals(128, clearCacheRoots(listOf(root)))
            assertTrue(root.exists())
            assertTrue(root.listFiles().orEmpty().isEmpty())
        } finally { root.deleteRecursively() }
    }

    @Test fun reportsClearUsesReportScopeWhileTrashDnaResetPreservesRows() = runBlocking {
        val dao = FakeDao(mutableListOf(session(TrashDnaSessionType.SCAN), session(TrashDnaSessionType.PRIVACY_REVIEW)))
        val repository = TrashDnaRepository(dao)
        repository.resetTrashDnaHistory(99)
        assertEquals(2, dao.sessions.size)
        assertEquals(99L, dao.state?.resetAtMillis)
        repository.clearReportHistory()
        assertEquals(listOf(TrashDnaSessionType.PRIVACY_REVIEW), dao.sessions.map { it.sessionType })
    }

    private fun session(type: String) = TrashDnaSessionEntity(
        sessionType = type, timestampMillis = 1, scannedFolderName = "", reclaimableBytes = 0,
        reclaimedBytes = 0, result = TrashDnaResult.ANALYZED, temporaryBytes = 0, cacheBytes = 0,
        emptyFolderCount = 0, apkLeftoverBytes = 0, logBytes = 0
    )

    private class FakeDao(val sessions: MutableList<TrashDnaSessionEntity>) : TrashDnaDao {
        var state: TrashDnaStateEntity? = null
        override suspend fun insert(session: TrashDnaSessionEntity) { sessions += session }
        override suspend fun loadAll() = sessions.toList()
        override suspend fun loadResetAtMillis() = state?.resetAtMillis
        override suspend fun saveState(state: TrashDnaStateEntity) { this.state = state }
        override suspend fun clearAll() { sessions.clear() }
        override suspend fun clearReportHistory() { sessions.removeAll { it.sessionType in setOf(TrashDnaSessionType.SCAN, TrashDnaSessionType.CLEANUP, TrashDnaSessionType.CACHE_SCAN) } }
        override suspend fun insertAll(sessions: List<TrashDnaSessionEntity>) { this.sessions += sessions }
    }
}
