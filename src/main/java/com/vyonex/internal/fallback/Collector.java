// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback;

import com.vyonex.internal.fallback.exceptions.ParsingException;

import java.util.List;

public interface Collector<I, E> {
    void commit(E extractor);
    I extract(E extractor) throws ParsingException;
    List<I> getItems();
    List<Throwable> getErrors();
    void reset();
}
