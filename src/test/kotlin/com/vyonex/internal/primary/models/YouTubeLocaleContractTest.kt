// bxane
package com.vyonex.internal.primary.models

import java.util.Locale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class YouTubeLocaleContractTest {
    @Test
    fun acceptLanguageUsesRegionAndLanguageFallback() {
        assertEquals("en-US,en;q=0.9", YouTubeLocale(gl = "US", hl = "en").acceptLanguageHeader())
        assertEquals("de-AT,de;q=0.9", YouTubeLocale(gl = "AT", hl = "de").acceptLanguageHeader())
        assertEquals("pt-BR,pt;q=0.9", YouTubeLocale(gl = "BR", hl = "pt_BR").acceptLanguageHeader())
    }

    @Test
    fun acceptLanguageKeepsAnExplicitLanguageRegion() {
        assertEquals("fr-CA,fr;q=0.9", YouTubeLocale(gl = "FR", hl = "fr-CA").acceptLanguageHeader())
        assertEquals("en", YouTubeLocale(gl = "", hl = "en").acceptLanguageHeader())
    }

    @Test
    fun missingLanguageProducesNoHeader() {
        assertEquals("", YouTubeLocale(gl = "US", hl = "").acceptLanguageHeader())
    }

    @Test
    fun systemLocaleUsesLanguageTagAndUppercaseRegion() {
        assertEquals(YouTubeLocale(gl = "CH", hl = "fr-CH"), systemYouTubeLocale(Locale.forLanguageTag("fr-CH")))
    }
}
