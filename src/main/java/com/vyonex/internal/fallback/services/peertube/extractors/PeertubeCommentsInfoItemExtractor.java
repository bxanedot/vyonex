// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
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

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.getAvatarsFromOwnerAccountOrVideoChannelObject;

public final class PeertubeCommentsInfoItemExtractor implements CommentsInfoItemExtractor {
    private static final String CHILDREN = "children";
    @Nullable private final String displayName;
    @Nullable private final String createdAt;
    @Nullable private final String text;
    @Nullable private final Number id;
    @Nullable private final Number threadId;
    @Nullable private final String accountName;
    @Nullable private final String accountHost;
    private final List<Image> avatars;
    private final int childCount;
    @Nullable private final Number totalReplies;
    private final long totalRepliesFromVideoAuthor;
    @Nullable private final byte[] childReplyBody;
    private final String commentsUrl;
    private final String baseUrl;
    private final boolean reply;

    public PeertubeCommentsInfoItemExtractor(final JsonObject item, final JsonArray children,
                                             final String commentsUrl, final String baseUrl,
                                             final boolean reply) {
        final JsonObject account = item.getObject("account");
        displayName = account.getString("displayName");
        createdAt = item.getString("createdAt");
        text = item.getString("text");
        id = item.getNumber("id");
        threadId = item.getNumber("threadId");
        accountName = account.getString("name");
        accountHost = account.getString("host");
        avatars = getAvatarsFromOwnerAccountOrVideoChannelObject(baseUrl, account);
        childCount = children == null || children.isEmpty() ? -1 : children.size();
        totalReplies = item.getNumber("totalReplies");
        totalRepliesFromVideoAuthor = item.getLong("totalRepliesFromVideoAuthor");
        childReplyBody = reply && childCount > 0 ? childReplyBody(children) : null;
        this.commentsUrl = commentsUrl;
        this.baseUrl = baseUrl;
        this.reply = reply;
    }

    @Override public String getUrl() throws ParsingException {
        return commentsUrl + "/" + getCommentId();
    }
    @Override public List<Image> getThumbnails() { return getUploaderAvatars(); }
    @Override public String getName() throws ParsingException {
        return required(displayName, "account.displayName");
    }
    @Override public String getTextualUploadDate() throws ParsingException {
        return required(createdAt, "createdAt");
    }
    @Override public DateWrapper getUploadDate() throws ParsingException {
        return DateWrapper.fromInstant(getTextualUploadDate());
    }
    @Override public Description getCommentText() throws ParsingException {
        return new Description(Jsoup.parse(required(text, "text")).text(), Description.PLAIN_TEXT);
    }
    @Override public String getCommentId() throws ParsingException {
        return required(id, "id").toString();
    }
    @Override public List<Image> getUploaderAvatars() { return avatars; }
    @Override public String getUploaderName() throws ParsingException {
        return required(accountName, "account.name") + "@" + required(accountHost, "account.host");
    }
    @Override public String getUploaderUrl() throws ParsingException {
        final String name = required(accountName, "account.name");
        final String host = required(accountHost, "account.host");
        return ServiceList.PeerTube.getChannelLHFactory()
                .fromId("accounts/" + name + "@" + host, baseUrl).getUrl();
    }
    @Override public Page getReplies() throws ParsingException {
        if (getReplyCount() == 0) return null;
        final String id = required(threadId, "threadId").toString();
        final String url = commentsUrl + "/" + id;
        if (childReplyBody != null) {
            return new Page(url, id, childReplyBody.clone());
        }
        return new Page(url, id);
    }
    @Override public int getReplyCount() throws ParsingException {
        if (childCount >= 0) return childCount;
        return required(totalReplies, "totalReplies").intValue();
    }
    @Override public boolean hasCreatorReply() {
        return totalRepliesFromVideoAuthor > 0;
    }

    private static String required(@Nullable final String value, final String field)
            throws ParsingException {
        if (value == null) throw new ParsingException("Missing string field: " + field);
        return value;
    }

    private static Number required(@Nullable final Number value, final String field)
            throws ParsingException {
        if (value == null) throw new ParsingException("Missing numeric field: " + field);
        return value;
    }

    private static byte[] childReplyBody(final JsonArray children) {
        final JsonObject body = new JsonObject();
        body.put(CHILDREN, children);
        return JsonWriter.string(body).getBytes(StandardCharsets.UTF_8);
    }
}
