// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.channel;

import com.vyonex.internal.fallback.InfoItemExtractor;
import com.vyonex.internal.fallback.exceptions.ParsingException;

public interface ChannelInfoItemExtractor extends InfoItemExtractor {
    String getDescription() throws ParsingException;
    long getSubscriberCount() throws ParsingException;
    long getStreamCount() throws ParsingException;
    boolean isVerified() throws ParsingException;
}
