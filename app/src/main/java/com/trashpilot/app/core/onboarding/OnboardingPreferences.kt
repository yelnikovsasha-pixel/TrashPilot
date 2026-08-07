package com.trashpilot.app.core.onboarding

import android.annotation.SuppressLint
import android.content.Context

interface OnboardingStateStore {
    fun isCompleted(): Boolean
    fun complete(): Boolean
}

class OnboardingPreferences(context: Context) : OnboardingStateStore {
    private val preferences = context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    override fun isCompleted(): Boolean = preferences.getInt(KEY_COMPLETED_VERSION, 0) >= CURRENT_VERSION

    @SuppressLint("UseKtx")
    override fun complete(): Boolean = preferences.edit().putInt(KEY_COMPLETED_VERSION, CURRENT_VERSION).commit()

    /** Test/development hook. Deliberately not exposed by production UI. */
    @SuppressLint("UseKtx")
    fun resetForDevelopment(): Boolean = preferences.edit().remove(KEY_COMPLETED_VERSION).commit()

    companion object {
        const val CURRENT_VERSION = 1
        private const val FILE_NAME = "trashpilot-onboarding"
        private const val KEY_COMPLETED_VERSION = "completed_version"
    }
}

enum class StartupDestination { ONBOARDING, HOME }

fun startupDestination(isOnboardingCompleted: Boolean): StartupDestination =
    if (isOnboardingCompleted) StartupDestination.HOME else StartupDestination.ONBOARDING

data class OnboardingFlowState(val step: Int = 0) {
    init { require(step in 0..2) }
    val isFinalStep: Boolean get() = step == 2
    fun continueFlow(): OnboardingFlowState = copy(step = (step + 1).coerceAtMost(2))
    fun back(): OnboardingFlowState = copy(step = (step - 1).coerceAtLeast(0))
}

fun completeOnboarding(store: OnboardingStateStore): StartupDestination =
    if (store.complete()) StartupDestination.HOME else StartupDestination.ONBOARDING
