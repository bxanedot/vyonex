// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.stream;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Objects;

public class Description implements Serializable {
    public static final int HTML = 1;
    public static final int MARKDOWN = 2;
    public static final int PLAIN_TEXT = 3;
    public static final Description EMPTY_DESCRIPTION = new Description("", PLAIN_TEXT);

    private final String content;
    private final int type;

    public Description(@Nullable final String content, final int type) {
        this.content = Objects.requireNonNullElse(content, "");
        this.type = type;
    }

    public String getContent() { return content; }
    public int getType() { return type; }

    @Override
    public boolean equals(final Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        final Description that = (Description) other;
        return type == that.type && content.equals(that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content, type);
    }
}
