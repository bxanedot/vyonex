// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.cipher

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class QuickJsEngineTest {
    @Test
    fun safelyPassesStringInputAndCreatesThePlayerEnvironment() = runTest {
        val engine = QuickJsEngine()
        val input = "quote \" slash \\ newline\n control\u0001"
        try {
            engine.initialize()
            engine.setupYoutubeGlobals()
            engine.execute("function echo(value) { return value; }")

            assertEquals(input, engine.callFunction("echo", input))
            assertEquals(
                "https://www.youtube.com/watch?v=vyonex-probe",
                engine.evaluate("location.href", 128),
            )
        } finally {
            engine.dispose()
        }
    }
}
