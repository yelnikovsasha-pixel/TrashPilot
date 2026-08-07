package com.trashpilot.app.core.about

import android.content.Context
import android.os.Build

data class AboutVersionInfo(
    val versionName: String?,
    val versionCode: Long
)

enum class AboutDestination {
    PRIVACY_POLICY,
    OPEN_SOURCE_LICENSES,
    APP_PERMISSIONS,
    INTRODUCTION
}

enum class AboutDestinationAvailability { AVAILABLE, UNAVAILABLE }

enum class AboutNavigationAction {
    OPEN_ABOUT,
    BACK_TO_SETTINGS,
    OPEN_PERMISSIONS,
    VIEW_INTRODUCTION
}

enum class AboutNavigationTarget {
    ABOUT,
    SETTINGS_OVERVIEW,
    SETTINGS_PERMISSIONS,
    INTRODUCTION
}

fun aboutVersionInfo(versionName: String?, versionCode: Long): AboutVersionInfo =
    AboutVersionInfo(versionName?.trim()?.takeIf(String::isNotEmpty), versionCode.coerceAtLeast(0L))

fun aboutDestinationAvailability(
    destination: AboutDestination,
    privacyPolicyAvailable: Boolean
): AboutDestinationAvailability = when (destination) {
    AboutDestination.PRIVACY_POLICY -> if (privacyPolicyAvailable) {
        AboutDestinationAvailability.AVAILABLE
    } else {
        AboutDestinationAvailability.UNAVAILABLE
    }
    AboutDestination.OPEN_SOURCE_LICENSES,
    AboutDestination.APP_PERMISSIONS,
    AboutDestination.INTRODUCTION -> AboutDestinationAvailability.AVAILABLE
}

fun aboutNavigationTarget(action: AboutNavigationAction): AboutNavigationTarget = when (action) {
    AboutNavigationAction.OPEN_ABOUT -> AboutNavigationTarget.ABOUT
    AboutNavigationAction.BACK_TO_SETTINGS -> AboutNavigationTarget.SETTINGS_OVERVIEW
    AboutNavigationAction.OPEN_PERMISSIONS -> AboutNavigationTarget.SETTINGS_PERMISSIONS
    AboutNavigationAction.VIEW_INTRODUCTION -> AboutNavigationTarget.INTRODUCTION
}

class AboutInfoProvider(private val context: Context) {
    fun load(): AboutVersionInfo {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
        return aboutVersionInfo(info.versionName, versionCode)
    }
}
