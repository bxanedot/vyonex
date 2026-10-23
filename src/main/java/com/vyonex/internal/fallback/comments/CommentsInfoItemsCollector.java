// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.comments;

import com.vyonex.internal.fallback.InfoItemsCollector;
import com.vyonex.internal.fallback.exceptions.ParsingException;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class CommentsInfoItemsCollector
        extends InfoItemsCollector<CommentsInfoItem, CommentsInfoItemExtractor> {
    public CommentsInfoItemsCollector(final int serviceId) {
        super(serviceId);
    }

    @Override
    public CommentsInfoItem extract(final CommentsInfoItemExtractor extractor)
            throws ParsingException {
        final CommentsInfoItem item = new CommentsInfoItem(
                getServiceId(), extractor.getUrl(), extractor.getName());

        copy(extractor::getCommentId, item::setCommentId);
        copy(extractor::getCommentText, item::setCommentText);
        copy(extractor::getUploaderName, item::setUploaderName);
        copy(extractor::getUploaderAvatars, item::setUploaderAvatars);
        copy(extractor::getUploaderUrl, item::setUploaderUrl);
        copy(extractor::getTextualUploadDate, item::setTextualUploadDate);
        copy(extractor::getUploadDate, item::setUploadDate);
        copy(extractor::getLikeCount, item::setLikeCount);
        copy(extractor::getTextualLikeCount, item::setTextualLikeCount);
        copy(extractor::getThumbnails, item::setThumbnails);
        copy(extractor::isHeartedByUploader, item::setHeartedByUploader);
        copy(extractor::isPinned, item::setPinned);
        copy(extractor::getStreamPosition, item::setStreamPosition);
        copy(extractor::getReplyCount, item::setReplyCount);
        copy(extractor::getReplies, item::setReplies);
        copy(extractor::isChannelOwner, item::setChannelOwner);
        copy(extractor::hasCreatorReply, item::setCreatorReply);
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

    @Override
    public void commit(final CommentsInfoItemExtractor extractor) {
        try {
            addItem(extract(extractor));
        } catch (final Exception error) {
            addError(error);
        }
    }

    public List<CommentsInfoItem> getCommentsInfoItemList() {
        return new ArrayList<>(getItems());
    }
}
