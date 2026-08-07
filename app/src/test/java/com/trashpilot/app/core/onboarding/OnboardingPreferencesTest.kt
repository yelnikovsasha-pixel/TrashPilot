package com.trashpilot.app.core.onboarding

import org.junit.Assert.*
import org.junit.Test

class OnboardingPreferencesTest {
    @Test fun firstLaunchOpensOnboarding() {
        assertEquals(StartupDestination.ONBOARDING, startupDestination(false))
    }

    @Test fun completedLaunchOpensHome() {
        assertEquals(StartupDestination.HOME, startupDestination(true))
    }

    @Test fun completionPersistsInStoreAndRoutesHome() {
        val store = FakeStore()
        assertEquals(StartupDestination.HOME, completeOnboarding(store))
        assertTrue(store.isCompleted())
        assertEquals(StartupDestination.HOME, startupDestination(store.isCompleted()))
    }

    @Test fun failedPersistenceDoesNotLeaveOnboarding() {
        val store = FakeStore(canWrite = false)
        assertEquals(StartupDestination.ONBOARDING, completeOnboarding(store))
        assertFalse(store.isCompleted())
    }

    @Test fun finalStepIsDeterministicAndDoesNotAdvancePastEnd() {
        val final = OnboardingFlowState().continueFlow().continueFlow()
        assertTrue(final.isFinalStep)
        assertEquals(final, final.continueFlow())
    }

    @Test fun backRestoresPriorStepWithoutChangingCompletion() {
        val store = FakeStore()
        assertEquals(1, OnboardingFlowState(2).back().step)
        assertFalse(store.isCompleted())
    }

    @Test fun reconstructedFlowStateRestoresCurrentStep() {
        assertEquals(1, OnboardingFlowState(step = 1).step)
    }

    private class FakeStore(private val canWrite: Boolean = true) : OnboardingStateStore {
        private var completed = false
        override fun isCompleted() = completed
        override fun complete(): Boolean {
            if (canWrite) completed = true
            return canWrite
        }
    }
}
