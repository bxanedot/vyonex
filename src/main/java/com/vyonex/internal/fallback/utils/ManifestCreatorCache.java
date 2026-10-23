// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ManifestCreatorCache<K extends Serializable, V extends Serializable>
        implements Serializable {
    public static final int DEFAULT_MAXIMUM_SIZE = Integer.MAX_VALUE;
    public static final double DEFAULT_CLEAR_FACTOR = 0.75;

    private final ConcurrentHashMap<K, Pair<Integer, V>> concurrentHashMap =
            new ConcurrentHashMap<>();
    private int maximumSize = DEFAULT_MAXIMUM_SIZE;
    private double clearFactor = DEFAULT_CLEAR_FACTOR;

    public boolean containsKey(final K key) {
        return concurrentHashMap.containsKey(key);
    }

    @Nullable
    public Pair<Integer, V> get(final K key) {
        return concurrentHashMap.get(key);
    }

    @Nullable
    public synchronized V put(final K key, final V value) {
        if (!concurrentHashMap.containsKey(key) && concurrentHashMap.size() == maximumSize) {
            keepNewestEntries(Math.max(1, (int) Math.round(maximumSize * clearFactor)));
        }
        final Pair<Integer, V> previous = concurrentHashMap.put(key,
                new Pair<>(concurrentHashMap.size(), value));
        return previous == null ? null : previous.getSecond();
    }

    public synchronized void clear() {
        concurrentHashMap.clear();
    }

    public synchronized void reset() {
        clear();
        clearFactor = DEFAULT_CLEAR_FACTOR;
        maximumSize = DEFAULT_MAXIMUM_SIZE;
    }

    public int size() {
        return concurrentHashMap.size();
    }

    public synchronized long getMaximumSize() {
        return maximumSize;
    }

    public synchronized void setMaximumSize(final int maximumSize) {
        if (maximumSize <= 0) throw new IllegalArgumentException("Invalid maximum size");
        if (maximumSize < this.maximumSize && !concurrentHashMap.isEmpty()) {
            keepNewestEntries(Math.max(1, (int) Math.round(maximumSize * clearFactor)));
        }
        this.maximumSize = maximumSize;
    }

    public synchronized void resetMaximumSize() {
        maximumSize = DEFAULT_MAXIMUM_SIZE;
    }

    public synchronized double getClearFactor() {
        return clearFactor;
    }

    public synchronized void setClearFactor(final double clearFactor) {
        if (clearFactor <= 0 || clearFactor >= 1) {
            throw new IllegalArgumentException("Invalid clear factor");
        }
        this.clearFactor = clearFactor;
    }

    public synchronized void resetClearFactor() {
        clearFactor = DEFAULT_CLEAR_FACTOR;
    }

    @Nonnull
    @Override
    public String toString() {
        return "ManifestCreatorCache[clearFactor=" + clearFactor + ", maximumSize=" + maximumSize
                + ", concurrentHashMap=" + concurrentHashMap + "]";
    }

    private void keepNewestEntries(final int newLimit) {
        final int difference = concurrentHashMap.size() - newLimit;
        final ArrayList<Map.Entry<K, Pair<Integer, V>>> remove = new ArrayList<>();
        for (final Map.Entry<K, Pair<Integer, V>> entry : concurrentHashMap.entrySet()) {
            final Pair<Integer, V> value = entry.getValue();
            if (value.getFirst() < difference) remove.add(entry);
            else value.setFirst(value.getFirst() - difference);
        }
        for (final Map.Entry<K, Pair<Integer, V>> entry : remove) {
            concurrentHashMap.remove(entry.getKey(), entry.getValue());
        }
    }
}
