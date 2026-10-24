// bxane
package com.vyonex.internal.primary.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CookieUtilsContractTest {
    @Test
    fun sanitizesHeaderPrefixAndLineWhitespace() {
        assertEquals("SID=one; SAPISID=two", sanitizeCookieString("  Cookie: SID=one;\r\n SAPISID=two\t "))
    }

    @Test
    fun parsesCookiePairsWithoutTruncatingValues() {
        assertEquals(
            mapOf("SID" to "one=two", "SAPISID" to "three"),
            parseCookieString("SID=one=two; ; SAPISID=three"),
        )
    }

    @Test
    fun rejectsOtherControlCharacters() {
        assertFailsWith<IllegalArgumentException> { sanitizeCookieString("SID=one\u0000two") }
    }

    @Test
    fun decodesQueryComponentsUsingFormSpaceRules() {
        assertEquals("a b=c", decodeQueryComponent("a+b%3Dc"))
    }
}
