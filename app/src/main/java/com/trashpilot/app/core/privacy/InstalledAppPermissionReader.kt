package com.trashpilot.app.core.privacy

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

class InstalledAppPermissionReader(private val context: Context) {
    fun read(): PrivacySnapshot {
        val packageManager = context.packageManager
        val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledPackages(
                PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        }
        return PrivacyPermissionAnalyzer.analyze(packages.mapNotNull { info ->
            val applicationInfo = info.applicationInfo ?: return@mapNotNull null
            val requested = info.requestedPermissions.orEmpty()
            RawInstalledApp(
                label = applicationInfo.loadLabel(packageManager).toString()
                    .ifBlank { info.packageName },
                packageName = info.packageName,
                requestedPermissions = requested.toSet(),
                grantedPermissions = requested.filterTo(mutableSetOf()) { permission ->
                    packageManager.checkPermission(permission, info.packageName) ==
                        PackageManager.PERMISSION_GRANTED
                }
            )
        })
    }
}

