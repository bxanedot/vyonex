// bxane
package com.vyonex.internal.primary.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class Sha1ContractTest {
    @Test
    fun returnsLowercaseHexDigest() {
        assertEquals("da39a3ee5e6b4b0d3255bfef95601890afd80709", sha1(""))
        assertEquals("a9993e364706816aba3e25717850c26c9cd0d89d", sha1("abc"))
        assertEquals(
            "84983e441c3bd26ebaae4aa1f95129e5e54670f1",
            sha1("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq"),
        )
    }
}
