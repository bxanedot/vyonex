// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.primary.extraction.strategy

import com.vyonex.internal.primary.extraction.ContentHints
import com.vyonex.internal.primary.models.YouTubeClient

class ContentAwareFallbackStrategy(
    private val healthMonitor: ClientHealthMonitor = ClientHealthMonitor.NONE,
    private val catalog: PlaybackClientCatalogView = PlaybackClientCatalog,
) : ClientFallbackStrategy {
    override fun resolveClients(hints: ContentHints): List<YouTubeClient> =
        selectClients(
            ClientSelectionRequest(
                hints = hints,
                authenticated = false,
                premium = hints.premium,
                javaScriptRuntimeAvailable = false,
                webViewAvailable = false,
            ),
        ).candidates.map(SelectedClient::client)

    override fun selectClients(request: ClientSelectionRequest): ClientSelectionResult =
        selectClients(request, premiumHighQuality = false)

    internal fun selectClients(
        request: ClientSelectionRequest,
        premiumHighQuality: Boolean,
    ): ClientSelectionResult {
        val rejected = mutableListOf<RejectedClient>()
        request.hints.playbackClientOverrideId?.let(catalog::find)?.let { option ->
            val ignored = rejectionReasons(option.manifest, request, checkContentSupport = false)
            if (!option.manifest.isExcluded(request)) {
                return ClientSelectionResult(
                    candidates =
                        listOf(
                            SelectedClient(
                                option.client,
                                option.manifest,
                                Int.MAX_VALUE,
                                listOf("manual override") + ignored.map { "ignored: $it" },
                            ),
                        ),
                )
            }
            rejected += RejectedClient(option.manifest, ignored.ifEmpty { listOf("manual override recently failed") })
        }

        val candidates = mutableListOf<SelectedClient>()
        catalog.automaticManifests.forEach { manifest ->
            if (manifest.isExcluded(request)) return@forEach
            val rejectedForProfile = rejectionReasons(manifest, request)
            if (rejectedForProfile.isNotEmpty()) {
                rejected += RejectedClient(manifest, rejectedForProfile)
            } else {
                val (score, reasons) = score(manifest, request)
                candidates += SelectedClient(manifest.client, manifest, score, reasons)
            }
        }

        val ordering =
            compareBy<SelectedClient> { selected ->
                when (request.transportPreference) {
                    PlaybackTransportPreference.AUTO ->
                        when {
                            selected.client.useSabr -> 2
                            request.hints.isKidsContent == true && selected.client == YouTubeClient.WEB_KIDS -> 0
                            else -> 1
                        }

                    PlaybackTransportPreference.SABR -> if (selected.client.useSabr) 0 else 1
                    else -> 0
                }
            }.thenBy { selected ->
                if (
                    request.authenticated &&
                    ((premiumHighQuality && request.premium) || request.hints.wantVideo) &&
                    request.hints.isKidsContent != true &&
                    selected.manifest?.authentication == AuthenticationPolicy.UNSUPPORTED
                ) {
                    1
                } else {
                    0
                }
            }.thenByDescending(SelectedClient::score)
                .thenBy { it.manifest?.id }

        return ClientSelectionResult(
            candidates = candidates.sortedWith(ordering),
            rejected = rejected.distinctBy { it.manifest.id }.sortedBy { it.manifest.id },
        )
    }

    private fun rejectionReasons(
        manifest: PlaybackClientManifest,
        request: ClientSelectionRequest,
        checkContentSupport: Boolean = true,
    ): List<String> = buildList {
        if (manifest.lifecycle == ClientLifecycle.BROKEN) add("client marked broken")
        if (manifest.authentication == AuthenticationPolicy.REQUIRED && !request.authenticated) add("login required")
        if (request.hints.isUploaded == true && !request.authenticated) add("uploads require login")
        if (checkContentSupport && manifest.content.supportFor(request.hints) == CapabilitySupport.UNSUPPORTED) {
            add("content type unsupported")
        }
        if (request.hints.wantVideo && manifest.transports.none { it == PlaybackTransport.DIRECT || it == PlaybackTransport.SABR }) {
            add("transport cannot satisfy video request")
        }
        if (!request.javaScriptRuntimeAvailable && manifest.request.signatureCipher == JavaScriptRequirement.REQUIRED) {
            add("signature transform unavailable")
        }
        if (!request.javaScriptRuntimeAvailable && manifest.request.nTransform == JavaScriptRequirement.REQUIRED) {
            add("n-transform unavailable")
        }
        if (manifest.request.webView == WebViewRequirement.FULL_PLAYER && !request.webViewAvailable) {
            add("full WebView runtime unavailable")
        }

        val premium = manifest.hasUsablePremiumEntitlement(request)
        val tokenRules = listOf("player" to manifest.poTokens.player, "GVS" to manifest.poTokens.gvs)
        val requiredTokens = tokenRules.filter { (_, rule) -> rule.requiredFor(premium) }
        val missingProviders = requiredTokens.filter { (_, rule) -> rule.providers.none { it in request.availablePoTokenProviders } }
        if (missingProviders.isNotEmpty()) {
            add("${missingProviders.joinToString("+") { it.first }} PO-token provider unavailable")
        }
        if (request.fastPathOnly) {
            if (manifest.request.signatureTimestamp) add("watch config excluded from fast path")
            if (requiredTokens.isNotEmpty()) add("token generation excluded from fast path")
            if (manifest.request.signatureCipher == JavaScriptRequirement.REQUIRED) {
                add("signature transform excluded from fast path")
            }
            if (manifest.request.nTransform == JavaScriptRequirement.REQUIRED) add("n-transform excluded from fast path")
        }
    }

    private fun PlaybackClientManifest.isExcluded(request: ClientSelectionRequest): Boolean {
        val premium = hasUsablePremiumEntitlement(request)
        if (client.clientName in request.excludedClients || id in request.excludedClients) return true
        val excludedWithoutToken = catalog.profileIds(this, usedPoToken = false).any { it in request.excludedClients }
        val excludedWithToken = catalog.profileIds(this, usedPoToken = true).any { it in request.excludedClients }
        val rules = listOf(poTokens.player, poTokens.gvs)
        val canUseTokens = rules.any { rule -> rule.requirement != PoTokenRequirement.NONE && rule.providers.any { it in request.availablePoTokenProviders } }
        val requiresTokens = rules.any { it.requiredFor(premium) }
        return (excludedWithoutToken && excludedWithToken) || (!canUseTokens && excludedWithoutToken) || (requiresTokens && excludedWithToken)
    }

    private fun PlaybackClientManifest.hasUsablePremiumEntitlement(request: ClientSelectionRequest): Boolean =
        request.premium && request.authenticated && client.loginSupported

    private fun PoTokenRule.requiredFor(premiumEntitlement: Boolean): Boolean =
        requirement == PoTokenRequirement.REQUIRED && !(premiumEntitlement && premiumMayBypass)

    private fun score(
        manifest: PlaybackClientManifest,
        request: ClientSelectionRequest,
    ): Pair<Int, List<String>> {
        var score = manifest.priority
        val reasons = mutableListOf("base=${manifest.priority}")
        fun adjust(delta: Int, reason: String) {
            score += delta
            reasons += "$reason=${if (delta >= 0) "+" else ""}$delta"
        }

        when (manifest.content.supportFor(request.hints)) {
            CapabilitySupport.SUPPORTED -> adjust(25, "content")
            CapabilitySupport.LIMITED -> adjust(-12, "content-limited")
            CapabilitySupport.UNKNOWN -> adjust(-4, "content-unknown")
            CapabilitySupport.UNSUPPORTED -> Unit
        }
        val restricted =
            request.hints.isExplicit == true || request.hints.isKidsContent == true || request.hints.isAgeRestricted == true ||
                request.hints.isUploaded == true
        if (restricted && manifest.authentication == AuthenticationPolicy.OPTIONAL) {
            adjust(if (request.authenticated) 18 else -18, if (request.authenticated) "authenticated" else "signed-out")
        }
        if (manifest.authentication == AuthenticationPolicy.REQUIRED) adjust(8, "auth-specialist")

        when (request.transportPreference) {
            PlaybackTransportPreference.AUTO -> if (PlaybackTransport.DIRECT in manifest.transports) adjust(10, "direct-fast-path")
            PlaybackTransportPreference.DIRECT -> adjust(if (PlaybackTransport.DIRECT in manifest.transports) 30 else -30, "direct-preference")
            PlaybackTransportPreference.SABR -> adjust(if (PlaybackTransport.SABR in manifest.transports) 30 else -30, "sabr-preference")
            PlaybackTransportPreference.HLS -> adjust(if (PlaybackTransport.HLS in manifest.transports) 30 else -30, "hls-preference")
        }

        if (request.hints.wantVideo) {
            when {
                manifest.client.clientName in setOf("WEB_REMIX", "WEB_CREATOR") -> adjust(-40, "web-remix-or-creator-video-demoted")
                listOf("TVHTML5", "VISIONOS", "ANDROID_VR", "IOS").any { manifest.client.clientName.startsWith(it) } ->
                    adjust(35, "video-dedicated-client-preferred")
            }
        }
        when (manifest.lifecycle) {
            ClientLifecycle.STABLE -> Unit
            ClientLifecycle.CANARY -> adjust(-15, "canary")
            ClientLifecycle.EXPERIMENTAL -> adjust(-10, "experimental")
            ClientLifecycle.UNRELEASED -> adjust(-5, "unreleased")
            ClientLifecycle.DEPRECATED -> adjust(-20, "deprecated")
            ClientLifecycle.BROKEN -> adjust(-100, "broken")
        }

        val rules = listOf(manifest.poTokens.player, manifest.poTokens.gvs)
        val required = rules.count { it.requirement == PoTokenRequirement.REQUIRED }
        val optional = rules.count { it.requirement == PoTokenRequirement.OPTIONAL }
        if (required > 0) adjust(-12 * required, "required-token")
        if (optional > 0) adjust(-4 * optional, "optional-token")
        if (manifest.request.nTransform == JavaScriptRequirement.RESPONSE_DEPENDENT) adjust(-4, "possible-n-transform")
        if (manifest.request.signatureCipher == JavaScriptRequirement.RESPONSE_DEPENDENT) adjust(-3, "possible-cipher")
        when (manifest.request.webView) {
            WebViewRequirement.NONE -> Unit
            WebViewRequirement.OPTIONAL_TOKEN_MINTING -> adjust(-2, "optional-web-runtime")
            WebViewRequirement.REQUIRED_TOKEN_MINTING -> adjust(-8, "token-web-runtime")
            WebViewRequirement.FULL_PLAYER -> adjust(-25, "full-webview")
        }
        val health = healthMonitor.scoreAdjustment(manifest.id, ClientHealthScope.from(request.hints, request.authenticated))
        if (health != 0) adjust(health, "runtime-health")
        return score to reasons
    }
}
