package com.trashpilot.app.core.settings

enum class PermissionAccessState { GRANTED, LIMITED, NOT_GRANTED, MANAGED_BY_ANDROID, UNSUPPORTED }
enum class SettingsResetAction { REPORTS_HISTORY, TRASH_DNA, OWN_CACHE }
enum class SettingsDestination { APPEARANCE, LANGUAGE, PRIVACY_PERMISSIONS, DATA_HISTORY, INTRODUCTION, ABOUT }

fun mediaAccessState(fullAccess: Boolean, selectedPhotosAccess: Boolean = false): PermissionAccessState = when {
    fullAccess -> PermissionAccessState.GRANTED
    selectedPhotosAccess -> PermissionAccessState.LIMITED
    else -> PermissionAccessState.NOT_GRANTED
}

fun usageAccessState(supported: Boolean, granted: Boolean): PermissionAccessState = when {
    !supported -> PermissionAccessState.UNSUPPORTED
    granted -> PermissionAccessState.GRANTED
    else -> PermissionAccessState.NOT_GRANTED
}

fun safAccessState(persistedGrantCount: Int): PermissionAccessState =
    if (persistedGrantCount > 0) PermissionAccessState.GRANTED else PermissionAccessState.MANAGED_BY_ANDROID

fun canOpenExternalSettings(hasHandler: Boolean): Boolean = hasHandler

fun resetActionRequiresConfirmation(action: SettingsResetAction): Boolean = when (action) {
    SettingsResetAction.REPORTS_HISTORY, SettingsResetAction.TRASH_DNA, SettingsResetAction.OWN_CACHE -> true
}

fun destinationForSettingsRow(destination: SettingsDestination): SettingsDestination = destination
