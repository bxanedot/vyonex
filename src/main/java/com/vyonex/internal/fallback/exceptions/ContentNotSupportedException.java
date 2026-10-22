// bxane
package com.vyonex.internal.fallback.exceptions;

public class ContentNotSupportedException extends ParsingException {
    public ContentNotSupportedException(final String message) {
        super(message);
    }

    public ContentNotSupportedException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
