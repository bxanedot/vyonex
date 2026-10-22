// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.peertube.extractors;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.Page;
import com.vyonex.internal.fallback.ServiceList;
import com.vyonex.internal.fallback.comments.CommentsInfoItemExtractor;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.localization.DateWrapper;
import com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper;
import com.vyonex.internal.fallback.stream.Description;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonWriter;
import org.jsoup.Jsoup;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.getAvatarsFromOwnerAccountOrVideoChannelObject;
import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.requiredNumber;
import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.requiredString;

public final class PeertubeCommentsInfoItemExtractor implements CommentsInfoItemExtractor {
    private static final String CHILDREN = "children";
    private final JsonObject item;
    private final JsonArray children;
    private final String commentsUrl;
    private final String baseUrl;
    private final boolean reply;

    public PeertubeCommentsInfoItemExtractor(final JsonObject item, final JsonArray children,
                                             final String commentsUrl, final String baseUrl,
                                             final boolean reply) {
        this.item = item;
        this.children = children;
        this.commentsUrl = commentsUrl;
        this.baseUrl = baseUrl;
        this.reply = reply;
    }

    @Override public String getUrl() throws ParsingException {
        return commentsUrl + "/" + getCommentId();
    }
    @Override public List<Image> getThumbnails() { return getUploaderAvatars(); }
    @Override public String getName() throws ParsingException {
        return requiredString(item, "account.displayName");
    }
    @Override public String getTextualUploadDate() throws ParsingException {
        return requiredString(item, "createdAt");
    }
    @Override public DateWrapper getUploadDate() throws ParsingException {
        return DateWrapper.fromInstant(getTextualUploadDate());
    }
    @Override public Description getCommentText() throws ParsingException {
        final String html = requiredString(item, "text");
        return new Description(Jsoup.parse(html).text(), Description.PLAIN_TEXT);
    }
    @Override public String getCommentId() throws ParsingException {
        return requiredNumber(item, "id").toString();
    }
    @Override public List<Image> getUploaderAvatars() {
        return getAvatarsFromOwnerAccountOrVideoChannelObject(baseUrl, item.getObject("account"));
    }
    @Override public String getUploaderName() throws ParsingException {
        return requiredString(item, "account.name") + "@" + requiredString(item, "account.host");
    }
    @Override public String getUploaderUrl() throws ParsingException {
        final String name = requiredString(item, "account.name");
        final String host = requiredString(item, "account.host");
        return ServiceList.PeerTube.getChannelLHFactory()
                .fromId("accounts/" + name + "@" + host, baseUrl).getUrl();
    }
    @Override public Page getReplies() throws ParsingException {
        if (getReplyCount() == 0) return null;
        final String threadId = requiredNumber(item, "threadId").toString();
        final String url = commentsUrl + "/" + threadId;
        if (reply && children != null && !children.isEmpty()) {
            final JsonObject body = new JsonObject();
            body.put(CHILDREN, children);
            final byte[] content = JsonWriter.string(body).getBytes(StandardCharsets.UTF_8);
            return new Page(url, threadId, content);
        }
        return new Page(url, threadId);
    }
    @Override public int getReplyCount() throws ParsingException {
        if (children != null && !children.isEmpty()) return children.size();
        return requiredNumber(item, "totalReplies").intValue();
    }
    @Override public boolean hasCreatorReply() {
        return item.getLong("totalRepliesFromVideoAuthor") > 0;
    }
}
