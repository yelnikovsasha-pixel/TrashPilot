package com.trashpilot.app.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InformationArchitectureTest {
    @Test fun topLevelNavigationRemainsMinimalAndStable() {
        assertEquals(
            listOf("home", "privacy", "reports", "settings"),
            TopLevelDestination.entries.map(TopLevelDestination::route)
        )
    }

    @Test fun everyReviewFeatureHasExactlyOneGroupParent() {
        assertEquals(2, reviewFeatures(ReviewGroup.APPS).size)
        assertEquals(3, reviewFeatures(ReviewGroup.PHOTOS).size)
        assertEquals(5, reviewFeatures(ReviewGroup.FILES).size)
        assertEquals(ReviewFeature.entries.size, ReviewGroup.entries.sumOf { reviewFeatures(it).size })
    }

    @Test fun quickCleanRequiresARealScanSession() {
        assertEquals("scanner", homeActionRoute(HomeAction.QUICK_CLEAN, hasScan = false))
        assertEquals("quick-clean", homeActionRoute(HomeAction.QUICK_CLEAN, hasScan = true))
    }

    @Test fun bottomNavigationSelectsLogicalParentsForDeepDestinations() {
        ReviewFeature.entries.forEach { assertEquals(TopLevelDestination.HOME, topLevelParent(it.route)) }
        assertEquals(TopLevelDestination.HOME, topLevelParent("results"))
        assertEquals(TopLevelDestination.SETTINGS, topLevelParent("about"))
        assertEquals(TopLevelDestination.SETTINGS, topLevelParent("introduction"))
        assertEquals(TopLevelDestination.PRIVACY, topLevelParent("privacy"))
    }

    @Test fun allDeclaredFeatureRoutesResolveAndUnknownRoutesDoNot() {
        ReviewFeature.entries.forEach { assertNotNull(reviewFeatureForRoute(it.route)) }
        assertNull(reviewFeatureForRoute("missing"))
        assertTrue(ReviewFeature.entries.map(ReviewFeature::route).toSet().size == ReviewFeature.entries.size)
    }
}
