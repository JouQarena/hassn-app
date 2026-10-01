package com.dnsguard.shield.core

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RedditTogglePolicyTest {
    @Test fun `all supported reddit labels match`() {
        for (label in listOf(
            "NSFW", "18+", "Mature", "Over 18",
            "محتوى للبالغين", "إظهار محتوى"
        )) assertTrue(RedditTogglePolicy.matchesLabel("  Show $label content "), label)
    }

    @Test fun `non sensitive label is ignored`() {
        assertFalse(RedditTogglePolicy.matchesLabel("Enable dark mode"))
        assertFalse(RedditTogglePolicy.matchesLabel(null))
    }

    @Test fun `checked switch is turned off but unchecked is never clicked`() {
        assertTrue(RedditTogglePolicy.shouldTurnOff(checkable = true, checked = true))
        assertFalse(RedditTogglePolicy.shouldTurnOff(checkable = true, checked = false))
        assertFalse(RedditTogglePolicy.shouldTurnOff(checkable = false, checked = true))
    }
}
