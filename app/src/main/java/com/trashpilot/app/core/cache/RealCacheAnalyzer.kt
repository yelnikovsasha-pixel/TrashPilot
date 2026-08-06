package com.trashpilot.app.core.cache

import android.app.AppOpsManager
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Process
import android.os.storage.StorageManager
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

enum class CacheCapability { AVAILABLE, USAGE_ACCESS_REQUIRED, UNSUPPORTED_ANDROID_VERSION }

class RealCacheAnalyzer(private val context: Context) {
    fun capability(): CacheCapability {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return CacheCapability.UNSUPPORTED_ANDROID_VERSION
        val appOps = context.getSystemService(AppOpsManager::class.java)
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return if (mode == AppOpsManager.MODE_ALLOWED) {
            CacheCapability.AVAILABLE
        } else {
            CacheCapability.USAGE_ACCESS_REQUIRED
        }
    }

    suspend fun scan(
        onProgress: suspend (CacheScanProgress) -> Unit = {}
    ): CacheSnapshot {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            scanOnAndroidO(onProgress)
        } else {
            error("Android does not expose cache statistics before API 26")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun scanOnAndroidO(
        onProgress: suspend (CacheScanProgress) -> Unit
    ): CacheSnapshot = withContext(Dispatchers.IO) {
        check(capability() == CacheCapability.AVAILABLE)
        val packageManager = context.packageManager
        val installed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledApplications(
                android.content.pm.PackageManager.ApplicationInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstalledApplications(0)
        }
        val apps = installed.filter { it.flags and ApplicationInfo.FLAG_INSTALLED != 0 }
        val statsManager = context.getSystemService(StorageStatsManager::class.java)
        val user = Process.myUserHandle()
        onProgress(CacheScanProgress(0, apps.size))
        val results = ArrayList<CacheApp>(apps.size)
        apps.forEachIndexed { index, app ->
            coroutineContext.ensureActive()
            val bytes = runCatching {
                statsManager.queryStatsForPackage(StorageManager.UUID_DEFAULT, app.packageName, user).cacheBytes
            }.getOrNull()
            results += CacheApp(
                packageName = app.packageName,
                label = packageManager.getApplicationLabel(app).toString(),
                cacheBytes = bytes,
                lastUpdatedMillis = runCatching {
                    packageManager.getPackageInfo(app.packageName, 0).lastUpdateTime
                }.getOrDefault(0)
            )
            onProgress(CacheScanProgress(index + 1, apps.size))
        }
        CacheSnapshot(System.currentTimeMillis(), results)
    }
}
