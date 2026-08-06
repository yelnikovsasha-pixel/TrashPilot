package com.trashpilot.app.core.cache

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.coroutineContext

class OwnCacheCleaner(private val context: Context) {
    suspend fun clean(): CacheCleaningReport = withContext(Dispatchers.IO) {
        val roots = buildList {
            add(context.cacheDir)
            add(context.codeCacheDir)
            context.externalCacheDirs.filterNotNull().forEach(::add)
        }.distinctBy(File::getAbsolutePath)
        val before = roots.sumOf(::sizeOf)
        roots.forEach { root ->
            coroutineContext.ensureActive()
            root.listFiles()?.forEach { it.deleteRecursively() }
        }
        val after = roots.sumOf(::sizeOf)
        val cleaned = (before - after).coerceAtLeast(0)
        CacheCleaningReport(cleaned, if (cleaned > 0) 1 else 0)
    }

    private fun sizeOf(file: File): Long {
        if (!file.exists()) return 0
        if (file.isFile) return file.length()
        return file.listFiles()?.sumOf(::sizeOf) ?: 0
    }
}
