// bxane
package com.vyonex.internal.fallback.utils;

public interface Logger {
    void debug(String tag, String message);
    void debug(String tag, String message, Throwable throwable);
    void warn(String tag, String message);
    void warn(String tag, String message, Throwable throwable);
    void error(String tag, String message);
    void error(String tag, String message, Throwable t);
}
