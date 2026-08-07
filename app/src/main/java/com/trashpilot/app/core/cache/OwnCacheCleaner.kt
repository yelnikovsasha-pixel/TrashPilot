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
        val cleaned = clearCacheRoots(roots) { coroutineContext.ensureActive() }
        CacheCleaningReport(cleaned, if (cleaned > 0) 1 else 0)
    }
}

internal fun clearCacheRoots(roots: List<File>, cancellationCheck: () -> Unit = {}): Long {
    val before = roots.sumOf(::cacheTreeSize)
    roots.forEach { root ->
        cancellationCheck()
        root.listFiles()?.forEach { it.deleteRecursively() }
    }
    return (before - roots.sumOf(::cacheTreeSize)).coerceAtLeast(0)
}

private fun cacheTreeSize(file: File): Long = when {
    !file.exists() -> 0
    file.isFile -> file.length()
    else -> file.listFiles()?.sumOf(::cacheTreeSize) ?: 0
}
