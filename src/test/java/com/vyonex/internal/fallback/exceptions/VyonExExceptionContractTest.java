// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VyonExExceptionContractTest {
    @Test
    void keepsExtractionMessagesAndCauses() {
        final IllegalStateException cause = new IllegalStateException("offline");
        final ExtractionException fromCause = new ExtractionException(cause);
        final ExtractionException withMessage = new ExtractionException("failed", cause);

        assertSame(cause, fromCause.getCause());
        assertSame(cause, withMessage.getCause());
        assertEquals("failed", withMessage.getMessage());
    }

    @Test
    void keepsParsingAndAdErrorsInTheExtractionHierarchy() {
        final IllegalArgumentException cause = new IllegalArgumentException("bad input");
        final ParsingException parsing = new ParsingException("invalid", cause);
        final FoundAdException ad = new FoundAdException("advertisement", cause);

        assertTrue(parsing instanceof ExtractionException);
        assertSame(cause, parsing.getCause());
        assertTrue(ad instanceof ParsingException);
        assertSame(cause, ad.getCause());
    }

    @Test
    void exposesCaptchaTargetUrl() {
        final ReCaptchaException captcha = new ReCaptchaException("captcha", "https://example.org");

        assertEquals("captcha", captcha.getMessage());
        assertEquals("https://example.org", captcha.getUrl());
    }

    @Test
    void preservesAccountTerminationReasonDefaults() {
        final AccountTerminatedException unknown = new AccountTerminatedException("terminated");
        final AccountTerminatedException violation = new AccountTerminatedException(
                "terminated", AccountTerminatedException.Reason.VIOLATION);
        final AccountTerminatedException caused = new AccountTerminatedException(
                "terminated", new IllegalStateException("probe"));

        assertEquals(AccountTerminatedException.Reason.UNKNOWN, unknown.getReason());
        assertEquals(AccountTerminatedException.Reason.VIOLATION, violation.getReason());
        assertEquals(AccountTerminatedException.Reason.UNKNOWN, caused.getReason());
    }
}
