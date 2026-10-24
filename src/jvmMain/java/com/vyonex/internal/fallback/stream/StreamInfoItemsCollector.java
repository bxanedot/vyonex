// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.stream;

import com.vyonex.internal.fallback.InfoItemsCollector;
import com.vyonex.internal.fallback.exceptions.FoundAdException;
import com.vyonex.internal.fallback.exceptions.ParsingException;

import java.util.Comparator;

public class StreamInfoItemsCollector
        extends InfoItemsCollector<StreamInfoItem, StreamInfoItemExtractor> {

    public StreamInfoItemsCollector(final int serviceId) {
        super(serviceId);
    }

    public StreamInfoItemsCollector(final int serviceId,
                                    final Comparator<StreamInfoItem> comparator) {
        super(serviceId, comparator);
    }

    @Override
    public StreamInfoItem extract(final StreamInfoItemExtractor extractor) throws ParsingException {
        if (extractor.isAd()) throw new FoundAdException("Found ad");
        final StreamInfoItem item = new StreamInfoItem(getServiceId(), extractor.getUrl(),
                extractor.getName(), extractor.getStreamType());
        try { item.setDuration(extractor.getDuration()); } catch (final Exception e) { addError(e); }
        try { item.setUploaderName(extractor.getUploaderName()); }
        catch (final Exception e) { addError(e); }
        try { item.setTextualUploadDate(extractor.getTextualUploadDate()); }
        catch (final Exception e) { addError(e); }
        try { item.setUploadDate(extractor.getUploadDate()); }
        catch (final ParsingException e) { addError(e); }
        try { item.setViewCount(extractor.getViewCount()); } catch (final Exception e) { addError(e); }
        try { item.setThumbnails(extractor.getThumbnails()); } catch (final Exception e) { addError(e); }
        try { item.setUploaderUrl(extractor.getUploaderUrl()); }
        catch (final Exception e) { addError(e); }
        try { item.setUploaderAvatars(extractor.getUploaderAvatars()); }
        catch (final Exception e) { addError(e); }
        try { item.setUploaderVerified(extractor.isUploaderVerified()); }
        catch (final Exception e) { addError(e); }
        try { item.setShortDescription(extractor.getShortDescription()); }
        catch (final Exception e) { addError(e); }
        try { item.setShortFormContent(extractor.isShortFormContent()); }
        catch (final Exception e) { addError(e); }
        try { item.setContentAvailability(extractor.getContentAvailability()); }
        catch (final Exception e) { addError(e); }
        return item;
    }

    @Override
    public void commit(final StreamInfoItemExtractor extractor) {
        try {
            addItem(extract(extractor));
        } catch (final FoundAdException ignored) {
        } catch (final Exception e) {
            addError(e);
        }
    }
}
