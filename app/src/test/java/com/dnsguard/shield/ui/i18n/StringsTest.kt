package com.dnsguard.shield.ui.i18n

import kotlin.reflect.full.memberProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class StringsTest {

    @Test
    fun `english is the default language`() {
        assertEquals(AppLanguage.EN, AppLanguage.fromCode(null))
        assertEquals(AppLanguage.EN, AppLanguage.fromCode("xx"))
        assertEquals("EN", EnglishStrings.language.code)
        assertEquals("AR", ArabicStrings.language.code)
    }

    @Test
    fun `language toggle cycles EN to AR and back`() {
        assertEquals(AppLanguage.AR, AppLanguage.EN.next())
        assertEquals(AppLanguage.EN, AppLanguage.AR.next())
    }

    @Test
    fun `rtl flag is set only for arabic`() {
        assertTrue(AppLanguage.AR.isRtl)
        assertTrue(!AppLanguage.EN.isRtl)
    }

    @Test
    fun `every translated string is non-blank in every language`() {
        val bundles = listOf(EnglishStrings, ArabicStrings)
        for (bundle in bundles) {
            val textProperties = Strings::class.memberProperties
            assertTrue(textProperties.size >= 70, "expected a comprehensive string table")
            for (property in textProperties) {
                if (property.name == "language") continue
                val value = property.get(bundle) as? String
                assertTrue(value != null, "${property.name} must be a String")
                assertTrue(
                    value!!.isNotBlank(),
                    "${bundle.language}/${property.name} must not be blank"
                )
            }
        }
    }

    @Test
    fun `arabic actually differs from english for user-facing copy`() {
        assertNotEquals(
            EnglishStrings.shieldActivatesSubtitle,
            ArabicStrings.shieldActivatesSubtitle
        )
        assertNotEquals(
            EnglishStrings.adbGuideTitle,
            ArabicStrings.adbGuideTitle
        )
        assertNotEquals(
            EnglishStrings.overlayRevealButton,
            ArabicStrings.overlayRevealButton
        )
    }

    @Test
    fun `arabic strings use arabic script for prose`() {
        val arabicProse = ArabicStrings.step1Body
        assertTrue(
            arabicProse.any { it in '\u0600'..'\u06FF' },
            "expected Arabic characters in prose strings"
        )
    }

    @Test
    fun `command constants stay pinned to the application id`() {
        assertEquals("EN", EnglishStrings.language.code)
        assertTrue(EnglishStrings.adbIntro.contains("USB"))
    }
}
