// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.peertube;

import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.exceptions.ReCaptchaException;
import com.vyonex.internal.json.JsonObject;

import java.io.IOException;

public class PeertubeInstance {
    public static final PeertubeInstance DEFAULT_INSTANCE =
            new PeertubeInstance("https://framatube.org", "FramaTube");

    private final String url;
    private String name;

    public PeertubeInstance(final String url) {
        this(url, "PeerTube");
    }

    public PeertubeInstance(final String url, final String name) {
        this.url = url;
        this.name = name;
    }

    public String getUrl() { return url; }
    public String getName() { return name; }

    public void fetchInstanceMetaData() throws Exception {
        final Downloader downloader = VyonEx.getDownloader();
        if (downloader == null) throw new Exception("VyonEx has not been initialized");

        final Response response;
        try {
            response = downloader.get(url + "/api/v1/config");
        } catch (final ReCaptchaException | IOException error) {
            throw new Exception("Unable to load PeerTube instance metadata from " + url, error);
        }
        try {
            final JsonObject config = PeertubeParsingHelper.readApiObject(response,
                    "instance configuration");
            final String discoveredName = config.getObject("instance").getString("name");
            if (discoveredName == null || discoveredName.isBlank()) {
                throw new ParsingException("PeerTube configuration has no instance name");
            }
            name = discoveredName;
        } catch (final ExtractionException error) {
            throw new Exception("Unable to parse PeerTube instance metadata", error);
        }
    }
}
