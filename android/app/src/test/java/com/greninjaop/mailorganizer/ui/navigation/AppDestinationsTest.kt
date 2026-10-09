package com.greninjaop.mailorganizer.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the navigation foundation (phase-01 §25): every planned
 * destination is a registered, distinct route with a title and an owning
 * future phase, so later phases add screens without a rewrite.
 */
class AppDestinationsTest {

    @Test
    fun `all ten destinations are registered and distinct`() {
        assertEquals(10, AppDestinations.all.size)
        assertEquals(10, AppDestinations.all.toSet().size)
    }

    @Test
    fun `primary and secondary groups cover every route exactly once`() {
        assertEquals(5, AppDestinations.primary.size)
        assertEquals(5, AppDestinations.secondary.size)
        assertEquals(
            AppDestinations.all.toSet(),
            (AppDestinations.primary + AppDestinations.secondary).toSet(),
        )
        assertTrue(AppDestinations.primary.intersect(AppDestinations.secondary.toSet()).isEmpty())
    }

    @Test
    fun `home is a primary destination and the graph start`() {
        assertTrue(AppDestinations.primary.contains(AppDestinations.HOME))
    }

    @Test
    fun `every route has a title and an owning phase`() {
        AppDestinations.all.forEach { route ->
            assertTrue(
                "route '$route' has no title",
                AppDestinations.titleFor(route).isNotBlank(),
            )
            assertTrue(
                "route '$route' has no owning phase",
                AppDestinations.phaseFor(route).isNotBlank(),
            )
        }
    }
}
