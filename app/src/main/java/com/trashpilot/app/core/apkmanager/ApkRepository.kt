package com.trashpilot.app.core.apkmanager

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import com.trashpilot.app.core.storage.ScannedFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.coroutineContext

class ApkMetadataParser(private val context: Context) {
    suspend fun parse(file: ScannedFile): ApkMetadata? = withContext(Dispatchers.IO) {
        if (!file.isApkInstaller()) return@withContext null
        val temporary = File.createTempFile("apk-metadata-", ".apk", context.cacheDir)
        try {
            context.contentResolver.openInputStream(file.uri.toUri())?.use { input ->
                temporary.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        coroutineContext.ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                    }
                }
            } ?: return@withContext null
            val packageManager = context.packageManager
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageArchiveInfo(temporary.absolutePath, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageArchiveInfo(temporary.absolutePath, 0)
            } ?: return@withContext null
            val applicationInfo = packageInfo.applicationInfo
            applicationInfo?.sourceDir = temporary.absolutePath
            applicationInfo?.publicSourceDir = temporary.absolutePath
            ApkMetadata(
                packageName = packageInfo.packageName,
                versionName = packageInfo.versionName,
                appLabel = applicationInfo?.let { runCatching { it.loadLabel(packageManager).toString().takeIf(String::isNotBlank) }.getOrNull() },
                icon = applicationInfo?.let { runCatching { it.loadIcon(packageManager).toBitmap(96, 96) }.getOrNull() }
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        } finally {
            temporary.delete()
        }
    }
}

class ApkRepository(private val parser: ApkMetadataParser) {
    suspend fun inspect(file: ScannedFile): ApkFileItem? =
        file.takeIf(ScannedFile::isApkInstaller)?.let { ApkFileItem(it, parser.parse(it)) }
}
