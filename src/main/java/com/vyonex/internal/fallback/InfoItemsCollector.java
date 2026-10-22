// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback;

import com.vyonex.internal.fallback.exceptions.FoundAdException;
import com.vyonex.internal.fallback.exceptions.ParsingException;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public abstract class InfoItemsCollector<I extends InfoItem, E extends InfoItemExtractor>
        implements Collector<I, E> {
    private final List<I> items = new ArrayList<>();
    private final List<Throwable> errors = new ArrayList<>();
    private final int serviceId;
    @Nullable private final Comparator<I> comparator;

    public InfoItemsCollector(final int serviceId) {
        this(serviceId, null);
    }

    public InfoItemsCollector(final int serviceId, @Nullable final Comparator<I> comparator) {
        this.serviceId = serviceId;
        this.comparator = comparator;
    }

    @Override
    public List<I> getItems() {
        if (comparator != null) items.sort(comparator);
        return Collections.unmodifiableList(items);
    }

    @Override
    public List<Throwable> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    @Override
    public void reset() {
        items.clear();
        errors.clear();
    }

    protected void addError(final Exception error) { errors.add(error); }
    protected void addItem(final I item) { items.add(item); }
    public int getServiceId() { return serviceId; }

    @Override
    public void commit(final E extractor) {
        try {
            addItem(extract(extractor));
        } catch (final FoundAdException ignored) {
        } catch (final ParsingException error) {
            addError(error);
        }
    }
}
