// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.url

import kotlin.test.Test
import kotlin.test.assertEquals

class FormUrlEncoderTest {
    @Test
    fun preservesFormQueryEncoding() {
        assertEquals("art+%26+caf%C3%A9", FormUrlEncoder.encode("art & café"))
        assertEquals("a%7E%21*%27%28%29+%2B%26", FormUrlEncoder.encode("a~!*'() +&"))
        assertEquals("", FormUrlEncoder.encode(""))
    }
}
