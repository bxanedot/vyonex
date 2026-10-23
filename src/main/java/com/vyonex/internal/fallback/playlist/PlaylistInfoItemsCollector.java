// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.playlist;

import com.vyonex.internal.fallback.InfoItemsCollector;
import com.vyonex.internal.fallback.exceptions.ParsingException;

import java.util.function.Consumer;

public class PlaylistInfoItemsCollector
        extends InfoItemsCollector<PlaylistInfoItem, PlaylistInfoItemExtractor> {
    public PlaylistInfoItemsCollector(final int serviceId) {
        super(serviceId);
    }

    @Override
    public PlaylistInfoItem extract(final PlaylistInfoItemExtractor extractor)
            throws ParsingException {
        final PlaylistInfoItem item = new PlaylistInfoItem(
                getServiceId(), extractor.getUrl(), extractor.getName());

        copy(extractor::getUploaderName, item::setUploaderName);
        copy(extractor::getUploaderUrl, item::setUploaderUrl);
        copy(extractor::isUploaderVerified, item::setUploaderVerified);
        copy(extractor::getThumbnails, item::setThumbnails);
        copy(extractor::getStreamCount, item::setStreamCount);
        copy(extractor::getDescription, item::setDescription);
        copy(extractor::getPlaylistType, item::setPlaylistType);
        return item;
    }

    private <T> void copy(final Field<T> field, final Consumer<T> setter) {
        try {
            setter.accept(field.read());
        } catch (final Exception error) {
            addError(error);
        }
    }

    @FunctionalInterface
    private interface Field<T> {
        T read() throws Exception;
    }
}
