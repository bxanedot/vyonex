// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.primary.extraction

import com.vyonex.internal.primary.extraction.strategy.PoTokenProviderKind
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

class TokenProviderContractTest {
    @Test
    fun tokenContractsArePlatformNeutralAndDefaultToNoCapabilities() {
        assertEquals(emptySet(), TokenProviderCapabilities().providers)
        assertFalse(TokenProviderCapabilities().usesWebView)
        assertEquals(PoTokenProviderKind.IOS_ATTESTATION, PoTokenProviderKind.valueOf("IOS_ATTESTATION"))
    }

    @Test
    fun bearerCredentialValidatesFieldsAndRedactsSecrets() {
        val expiresAt = Clock.System.now() + 10.minutes
        val credential = TvBearerCredential(
            value = "private-bearer-secret",
            profileId = "TVHTML5",
            expiresAt = expiresAt,
            sessionGeneration = 3,
            visitorData = "private-visitor-secret",
        )

        assertContains(credential.toString(), "value=present")
        assertFalse(credential.toString().contains("private-bearer-secret"))
        assertFalse(credential.toString().contains("private-visitor-secret"))
        assertFailsWith<IllegalArgumentException> {
            TvBearerCredential("bad\nvalue", "TVHTML5", expiresAt, 3)
        }
        assertFailsWith<IllegalArgumentException> {
            TvBearerCredential("secret", "UNKNOWN", expiresAt, 3)
        }
        assertFailsWith<IllegalArgumentException> {
            TvBearerCredential("secret", "TVHTML5", expiresAt, -1)
        }
    }
}
