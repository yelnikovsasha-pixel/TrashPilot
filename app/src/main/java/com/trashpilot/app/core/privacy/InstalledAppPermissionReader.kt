package com.trashpilot.app.core.privacy

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

class InstalledAppPermissionReader(private val context: Context) {
    fun read(): PrivacySnapshot {
        val packageManager = context.packageManager
        val flags = PackageManager.GET_PERMISSIONS or PackageManager.GET_SERVICES
        val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledPackages(
                PackageManager.PackageInfoFlags.of(flags.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstalledPackages(flags)
        }
        return PrivacyPermissionAnalyzer.analyze(packages.mapNotNull { info ->
            val applicationInfo = info.applicationInfo ?: return@mapNotNull null
            val requested = buildSet {
                addAll(info.requestedPermissions.orEmpty())
                if (info.services.orEmpty().any {
                        it.permission == android.Manifest.permission.BIND_ACCESSIBILITY_SERVICE
                    }) {
                    add(android.Manifest.permission.BIND_ACCESSIBILITY_SERVICE)
                }
            }
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
