// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.extraction.strategy

import com.vyonex.internal.primary.extraction.ContentHints
import com.vyonex.internal.primary.models.PoTokenBinding
import com.vyonex.internal.primary.models.YouTubeClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContentAwareFallbackStrategyContractTest {
    @Test
    fun manualOverrideWinsAndReportsConstraintsItBypasses() {
        val manifest =
            profile(
                id = "OVERRIDE",
                mode = ClientSelectionMode.PROBE_ONLY,
                transports = setOf(PlaybackTransport.HLS),
                content = ClientContentCapabilities(kids = CapabilitySupport.UNSUPPORTED),
            )
        val result = strategy(manifest).selectClients(
            ClientSelectionRequest(
                hints = ContentHints(isKidsContent = true, wantVideo = true, playbackClientOverrideId = manifest.id),
                authenticated = false,
            ),
        )

        assertEquals(listOf(manifest.id), result.candidates.map { it.manifest?.id })
        assertTrue(result.candidates.single().reasons.contains("ignored: transport cannot satisfy video request"))
    }

    @Test
    fun rejectsProfilesMissingAuthRuntimeTokenAndFastPathRequirements() {
        val requiredToken =
            PoTokenRule(
                requirement = PoTokenRequirement.REQUIRED,
                binding = PoTokenBinding.VIDEO_ID,
                providers = setOf(PoTokenProviderKind.WEBPAGE_ATTESTATION),
            )
        val manifest =
            profile(
                id = "RESTRICTED",
                authentication = AuthenticationPolicy.REQUIRED,
                request =
                    ClientRequestCapabilities(
                        signatureTimestamp = true,
                        signatureCipher = JavaScriptRequirement.REQUIRED,
                        nTransform = JavaScriptRequirement.REQUIRED,
                        webView = WebViewRequirement.FULL_PLAYER,
                    ),
                poTokens = ClientPoTokenCapabilities(player = requiredToken, gvs = requiredToken),
            )
        val result =
            strategy(manifest).selectClients(
                ClientSelectionRequest(
                    hints = ContentHints(),
                    authenticated = false,
                    javaScriptRuntimeAvailable = false,
                    fastPathOnly = true,
                ),
            )

        assertTrue(result.candidates.isEmpty())
        val reasons = result.rejected.single().reasons
        assertTrue(reasons.contains("login required"))
        assertTrue(reasons.contains("signature transform unavailable"))
        assertTrue(reasons.contains("n-transform unavailable"))
        assertTrue(reasons.contains("full WebView runtime unavailable"))
        assertTrue(reasons.contains("player+GVS PO-token provider unavailable"))
        assertTrue(reasons.contains("watch config excluded from fast path"))
        assertTrue(reasons.contains("token generation excluded from fast path"))
    }

    @Test
    fun requiresContentSupportAndHonorsTransportPreference() {
        val unsupported =
            profile(
                id = "NO_KIDS",
                content = ClientContentCapabilities(kids = CapabilitySupport.UNSUPPORTED),
            )
        val filtered =
            strategy(unsupported).selectClients(
                ClientSelectionRequest(ContentHints(isKidsContent = true), authenticated = false),
            )
        assertTrue(filtered.candidates.isEmpty())
        assertEquals(listOf("content type unsupported"), filtered.rejected.single().reasons)

        val direct = profile(id = "DIRECT", priority = 50, transports = setOf(PlaybackTransport.DIRECT))
        val hls = profile(id = "HLS", priority = 50, transports = setOf(PlaybackTransport.HLS))
        val selected =
            strategy(direct, hls).selectClients(
                ClientSelectionRequest(
                    hints = ContentHints(),
                    authenticated = false,
                    transportPreference = PlaybackTransportPreference.DIRECT,
                ),
            )
        assertEquals("DIRECT", selected.candidates.first().manifest?.id)
    }

    @Test
    fun appliesHealthAndOnlyAcceptsACompatibleTokenProvider() {
        val requiredToken =
            PoTokenRule(
                requirement = PoTokenRequirement.REQUIRED,
                binding = PoTokenBinding.VIDEO_ID,
                providers = setOf(PoTokenProviderKind.WEBPAGE_ATTESTATION),
            )
        val tokenClient =
            profile(
                id = "TOKEN_CLIENT",
                poTokens = ClientPoTokenCapabilities(player = requiredToken),
            )
        val hls = profile(id = "HEALTHY_HLS", priority = 50, transports = setOf(PlaybackTransport.HLS))
        val direct = profile(id = "DIRECT", priority = 50, transports = setOf(PlaybackTransport.DIRECT))
        val strategy =
            ContentAwareFallbackStrategy(
                healthMonitor = object : ClientHealthMonitor {
                    override fun scoreAdjustment(clientId: String, scope: ClientHealthScope?) =
                        if (clientId == "HEALTHY_HLS") 100 else 0
                },
                catalog = TestCatalog(listOf(tokenClient, hls, direct)),
            )

        val missingProvider =
            strategy.selectClients(ClientSelectionRequest(ContentHints(), authenticated = false))
        assertTrue(missingProvider.rejected.any { it.manifest.id == tokenClient.id })

        val withProvider =
            strategy.selectClients(
                ClientSelectionRequest(
                    hints = ContentHints(),
                    authenticated = false,
                    availablePoTokenProviders = setOf(PoTokenProviderKind.WEBPAGE_ATTESTATION),
                ),
            )
        assertTrue(withProvider.candidates.any { it.manifest?.id == tokenClient.id })
        assertEquals("HEALTHY_HLS", withProvider.candidates.first().manifest?.id)
        assertTrue(withProvider.candidates.first().reasons.contains("runtime-health=+100"))
    }

    @Test
    fun premiumBypassesOnlyEligibleRequiredTokensAndProfileExclusions() {
        val requiredToken =
            PoTokenRule(
                requirement = PoTokenRequirement.REQUIRED,
                binding = PoTokenBinding.VIDEO_ID,
                providers = setOf(PoTokenProviderKind.WEBPAGE_ATTESTATION),
                premiumMayBypass = true,
            )
        val eligible =
            profile(
                id = "PREMIUM",
                authentication = AuthenticationPolicy.OPTIONAL,
                poTokens = ClientPoTokenCapabilities(gvs = requiredToken),
            )
        val premiumRequest = ClientSelectionRequest(ContentHints(), authenticated = true, premium = true)
        val strategy = strategy(eligible)
        assertEquals(listOf("PREMIUM"), strategy.selectClients(premiumRequest).candidates.map { it.manifest?.id })
        assertTrue(
            strategy.selectClients(premiumRequest.copy(excludedClients = setOf("PREMIUM_NO_TOKEN"))).candidates.isEmpty(),
        )

        val ineligible =
            profile(
                id = "NO_LOGIN",
                poTokens = ClientPoTokenCapabilities(gvs = requiredToken),
            )
        val blocked = strategy(ineligible).selectClients(premiumRequest)
        assertTrue(blocked.candidates.isEmpty())
        assertTrue(blocked.rejected.single().reasons.contains("GVS PO-token provider unavailable"))
    }

    private fun strategy(vararg manifests: PlaybackClientManifest) =
        ContentAwareFallbackStrategy(catalog = TestCatalog(manifests.toList()))

    private fun profile(
        id: String,
        priority: Int = 50,
        mode: ClientSelectionMode = ClientSelectionMode.AUTOMATIC,
        authentication: AuthenticationPolicy = AuthenticationPolicy.UNSUPPORTED,
        transports: Set<PlaybackTransport> = setOf(PlaybackTransport.DIRECT),
        request: ClientRequestCapabilities = ClientRequestCapabilities(),
        poTokens: ClientPoTokenCapabilities = ClientPoTokenCapabilities(),
        content: ClientContentCapabilities = ClientContentCapabilities(),
    ) =
        PlaybackClientManifest(
            id = id,
            displayName = id,
            client =
                YouTubeClient(
                    clientName = id,
                    clientVersion = "1",
                    clientId = "1",
                    userAgent = "test",
                    loginSupported = authentication != AuthenticationPolicy.UNSUPPORTED,
                    loginRequired = authentication == AuthenticationPolicy.REQUIRED,
                    useSignatureTimestamp = request.signatureTimestamp,
                    isEmbedded = request.embedded,
                    useSabr = PlaybackTransport.SABR in transports,
                ),
            lifecycle = ClientLifecycle.STABLE,
            selectionMode = mode,
            authentication = authentication,
            transports = transports,
            poTokens = poTokens,
            request = request,
            content = content,
            priority = priority,
            evidence = setOf("VyonEx test profile"),
        )

    private class TestCatalog(private val profiles: List<PlaybackClientManifest>) : PlaybackClientCatalogView {
        override val automaticManifests = profiles.filter(PlaybackClientManifest::isAutomatic)

        override fun find(id: String) =
            profiles.firstOrNull { it.id == id }?.let {
                PlaybackClientOption(it.id, it.displayName, it.client, it)
            }

        override fun profileIds(
            manifest: PlaybackClientManifest,
            usedPoToken: Boolean,
        ) = setOf("${manifest.id}_NO_TOKEN", "${manifest.id}_WITH_TOKEN")
    }
}
