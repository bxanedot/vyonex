// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.extraction.strategy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlaybackClientCatalogContractTest {
    @Test
    fun everyCatalogProfileUsesVyonExEvidence() {
        assertTrue(PlaybackClientCatalog.manifests.isNotEmpty())
        PlaybackClientCatalog.manifests.forEach { manifest ->
            assertTrue(
                manifest.evidence.isNotEmpty() && manifest.evidence.all { it.startsWith("VyonEx ") },
                "${manifest.id} must use VyonEx-owned evidence",
            )
        }
    }

    @Test
    fun everyAutomaticProfileHasVyonExEvidence() {
        assertTrue(PlaybackClientCatalog.automaticManifests.isNotEmpty())
        PlaybackClientCatalog.automaticManifests.forEach { manifest ->
            assertTrue(
                manifest.evidence.any { it.startsWith("VyonEx ") },
                "${manifest.id} must be backed by a VyonEx probe or benchmark",
            )
        }
    }

    @Test
    fun webProfilesMatchTheFirstPartyPageIdentitiesCapturedForTheirFallbacks() {
        val web = requireNotNull(PlaybackClientCatalog.findManifest("WEB"))
        assertEquals("WEB", web.client.clientName)
        assertEquals("1", web.client.clientId)
        assertEquals("2.20261022.01.00", web.client.clientVersion)
        assertTrue(web.evidence.single().startsWith("VyonEx capture of official YouTube"))

        val embedded = requireNotNull(PlaybackClientCatalog.findManifest("WEB_EMBEDDED_PLAYER"))
        assertEquals("WEB_EMBEDDED_PLAYER", embedded.client.clientName)
        assertEquals("56", embedded.client.clientId)
        assertTrue(embedded.evidence.any { it.startsWith("VyonEx capture of official YouTube") })
    }
}
