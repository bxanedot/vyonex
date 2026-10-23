// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ExtractorLoggerTest {
    private final RecordingLogger logger = new RecordingLogger();

    @AfterEach
    void clearLogger() {
        ExtractorLogger.setLogger(null);
    }

    @Test
    void formatsArgumentsEscapesBracesAndKeepsMissingPlaceholders() {
        ExtractorLogger.setLogger(logger);

        ExtractorLogger.d("test", "{{{Name}}}: {} / {}", "item", 7);

        assertEquals(List.of("DEBUG:test:{item}: 7 / {}"), logger.messages);
    }

    @Test
    void forwardsLevelsAndCauses() {
        final IllegalStateException cause = new IllegalStateException("failure");
        ExtractorLogger.setLogger(logger);

        ExtractorLogger.w("test", "warning", cause);
        ExtractorLogger.e("test", cause, "failed {}", "request");

        assertEquals(List.of("WARN:test:warning", "ERROR:test:failed request"), logger.messages);
        assertSame(cause, logger.causes.get(0));
        assertSame(cause, logger.causes.get(1));
    }

    private static final class RecordingLogger implements Logger {
        private final List<String> messages = new ArrayList<>();
        private final List<Throwable> causes = new ArrayList<>();

        @Override public void debug(String tag, String message) { messages.add("DEBUG:" + tag + ":" + message); }
        @Override public void debug(String tag, String message, Throwable throwable) { debug(tag, message); causes.add(throwable); }
        @Override public void warn(String tag, String message) { messages.add("WARN:" + tag + ":" + message); }
        @Override public void warn(String tag, String message, Throwable throwable) { warn(tag, message); causes.add(throwable); }
        @Override public void error(String tag, String message) { messages.add("ERROR:" + tag + ":" + message); }
        @Override public void error(String tag, String message, Throwable throwable) { error(tag, message); causes.add(throwable); }
    }
}
