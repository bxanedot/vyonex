// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.fallback.exceptions.AccountTerminatedException;
import com.vyonex.internal.fallback.exceptions.AgeRestrictedContentException;
import com.vyonex.internal.fallback.exceptions.ContentNotAvailableException;
import com.vyonex.internal.fallback.exceptions.GeographicRestrictionException;
import com.vyonex.internal.fallback.exceptions.PaidContentException;
import com.vyonex.internal.fallback.exceptions.PrivateContentException;
import com.vyonex.internal.fallback.exceptions.SignInConfirmNotBotException;
import com.vyonex.internal.fallback.exceptions.YoutubeMusicPremiumContentException;
import com.vyonex.internal.json.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class YoutubePlayabilityStatusTest {
    @Test
    void acceptsMissingAndSuccessfulStatus() throws Exception {
        assertDoesNotThrow(() -> YoutubeStreamExtractor.checkPlayabilityStatus(
                JsonParser.object().from("{}")));
        assertDoesNotThrow(() -> YoutubeStreamExtractor.checkPlayabilityStatus(
                JsonParser.object().from("{\"status\":\"OK\"}")));
    }

    @Test
    void mapsLoginFailures() throws Exception {
        assertThrows(AgeRestrictedContentException.class,
                () -> check("LOGIN_REQUIRED", "Inappropriate for some users"));
        assertThrows(PrivateContentException.class,
                () -> check("login_required", "This video is private"));
        assertThrows(SignInConfirmNotBotException.class,
                () -> check("login_required", "Confirm you are not a bot"));
    }

    @Test
    void mapsPlaybackFailures() throws Exception {
        assertThrows(YoutubeMusicPremiumContentException.class,
                () -> check("unplayable", "Music Premium required"));
        assertThrows(PaidContentException.class,
                () -> check("error", "Payment required"));
        assertThrows(PaidContentException.class,
                () -> check("unplayable", "Members only"));
        assertThrows(GeographicRestrictionException.class,
                () -> check("unplayable", "Unavailable in this country"));
        assertThrows(AccountTerminatedException.class,
                () -> check("error", "Account terminated"));
        assertThrows(ContentNotAvailableException.class,
                () -> check("unplayable", "Unknown failure"));
    }

    private static void check(final String status, final String reason) throws Exception {
        YoutubeStreamExtractor.checkPlayabilityStatus(JsonParser.object().from(
                "{\"status\":\"" + status + "\",\"reason\":\"" + reason + "\"}"));
    }
}
