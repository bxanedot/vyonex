// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.utils;

import java.io.Serializable;
import java.util.Objects;

public class Pair<F extends Serializable, S extends Serializable> implements Serializable {
    private F firstObject;
    private S secondObject;

    public Pair(final F first, final S second) {
        firstObject = first;
        secondObject = second;
    }

    public void setFirst(final F first) { firstObject = first; }

    public void setSecond(final S second) { secondObject = second; }

    public F getFirst() { return firstObject; }

    public S getSecond() { return secondObject; }

    @Override
    public String toString() { return "{" + firstObject + ", " + secondObject + "}"; }

    @Override
    public boolean equals(final Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        final Pair<?, ?> that = (Pair<?, ?>) other;
        return Objects.equals(firstObject, that.firstObject)
                && Objects.equals(secondObject, that.secondObject);
    }

    @Override
    public int hashCode() { return Objects.hash(firstObject, secondObject); }
}
