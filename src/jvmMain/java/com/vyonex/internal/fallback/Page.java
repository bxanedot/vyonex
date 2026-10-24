// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class Page implements Serializable {
    private final String url;
    private final String id;
    private final List<String> ids;
    private final Map<String, String> cookies;
    @Nullable private final byte[] body;

    public Page(final String url, final String id, final List<String> ids,
                final Map<String, String> cookies, @Nullable final byte[] body) {
        this.url = url;
        this.id = id;
        this.ids = ids;
        this.cookies = cookies;
        this.body = body;
    }

    public Page(final String url) { this(url, null, null, null, null); }
    public Page(final String url, final String id) { this(url, id, null, null, null); }
    public Page(final String url, final String id, final byte[] body) {
        this(url, id, null, null, body);
    }
    public Page(final String url, final byte[] body) { this(url, null, null, null, body); }
    public Page(final String url, final Map<String, String> cookies) {
        this(url, null, null, cookies, null);
    }
    public Page(final List<String> ids) { this(null, null, ids, null, null); }
    public Page(final List<String> ids, final Map<String, String> cookies) {
        this(null, null, ids, cookies, null);
    }

    public String getUrl() { return url; }
    public String getId() { return id; }
    public List<String> getIds() { return ids; }
    public Map<String, String> getCookies() { return cookies; }
    @Nullable public byte[] getBody() { return body; }

    public static boolean isValid(@Nullable final Page page) {
        return page != null && ((page.url != null && !page.url.isEmpty())
                || (page.ids != null && !page.ids.isEmpty()));
    }
}
