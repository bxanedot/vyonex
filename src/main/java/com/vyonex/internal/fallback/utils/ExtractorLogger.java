// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.utils;

public final class ExtractorLogger {
    private static volatile Logger logger;

    private ExtractorLogger() { }

    public static void setLogger(final Logger customLogger) {
        logger = customLogger;
    }

    public enum Level { DEBUG, WARN, ERROR }

    private static void log(final Level level, final String tag, final String message,
                            final Throwable cause) {
        final Logger target = logger;
        if (target == null) return;
        switch (level) {
            case DEBUG -> {
                if (cause == null) target.debug(tag, message);
                else target.debug(tag, message, cause);
            }
            case WARN -> {
                if (cause == null) target.warn(tag, message);
                else target.warn(tag, message, cause);
            }
            case ERROR -> {
                if (cause == null) target.error(tag, message);
                else target.error(tag, message, cause);
            }
        }
    }

    private static void logFormat(final Level level, final String tag, final Throwable cause,
                                  final String template, final Object... arguments) {
        if (logger != null) log(level, tag, format(template, arguments), cause);
    }

    public static void d(final String tag, final String message) {
        log(Level.DEBUG, tag, message, null);
    }

    public static void d(final String tag, final String message, final Throwable cause) {
        log(Level.DEBUG, tag, message, cause);
    }

    public static void d(final String tag, final String template, final Object... arguments) {
        logFormat(Level.DEBUG, tag, null, template, arguments);
    }

    public static void d(final String tag, final Throwable cause, final String template,
                         final Object... arguments) {
        logFormat(Level.DEBUG, tag, cause, template, arguments);
    }

    public static void w(final String tag, final String message) {
        log(Level.WARN, tag, message, null);
    }

    public static void w(final String tag, final String message, final Throwable cause) {
        log(Level.WARN, tag, message, cause);
    }

    public static void w(final String tag, final String template, final Object... arguments) {
        logFormat(Level.WARN, tag, null, template, arguments);
    }

    public static void w(final String tag, final Throwable cause, final String template,
                         final Object... arguments) {
        logFormat(Level.WARN, tag, cause, template, arguments);
    }

    public static void e(final String tag, final String message) {
        log(Level.ERROR, tag, message, null);
    }

    public static void e(final String tag, final String message, final Throwable cause) {
        log(Level.ERROR, tag, message, cause);
    }

    public static void e(final String tag, final String template, final Object... arguments) {
        logFormat(Level.ERROR, tag, null, template, arguments);
    }

    public static void e(final String tag, final Throwable cause, final String template,
                         final Object... arguments) {
        logFormat(Level.ERROR, tag, cause, template, arguments);
    }

    private static String format(final String template, final Object... arguments) {
        if (template == null || arguments == null || arguments.length == 0) return template;
        final StringBuilder result = new StringBuilder(template.length() + arguments.length * 8);
        int argument = 0;
        for (int cursor = 0; cursor < template.length();) {
            final char current = template.charAt(cursor);
            if ((current == '{' || current == '}') && cursor + 1 < template.length()
                    && template.charAt(cursor + 1) == current) {
                result.append(current);
                cursor += 2;
            } else if (current == '{') {
                final int close = template.indexOf('}', cursor + 1);
                if (close < 0) {
                    result.append(template, cursor, template.length());
                    break;
                }
                if (argument < arguments.length) result.append(arguments[argument++]);
                else result.append(template, cursor, close + 1);
                cursor = close + 1;
            } else {
                result.append(current);
                cursor++;
            }
        }
        return result.toString();
    }

    public static final class ConsoleLogger implements Logger {
        @Override
        public void debug(final String tag, final String message) {
            System.out.println("[DEBUG][" + tag + "] " + message);
        }

        @Override
        public void debug(final String tag, final String message, final Throwable cause) {
            debug(tag, message);
            cause.printStackTrace(System.err);
        }

        @Override
        public void warn(final String tag, final String message) {
            System.out.println("[WARN ][" + tag + "] " + message);
        }

        @Override
        public void warn(final String tag, final String message, final Throwable cause) {
            warn(tag, message);
            cause.printStackTrace(System.err);
        }

        @Override
        public void error(final String tag, final String message) {
            System.err.println("[ERROR][" + tag + "] " + message);
        }

        @Override
        public void error(final String tag, final String message, final Throwable cause) {
            error(tag, message);
            cause.printStackTrace(System.err);
        }
    }
}
