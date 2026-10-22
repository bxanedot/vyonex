// bxane
package com.vyonex.internal.fallback.exceptions;

public class PaidContentException extends ContentNotAvailableException {
    public PaidContentException(final String message) {
        super(message);
    }

    public PaidContentException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
