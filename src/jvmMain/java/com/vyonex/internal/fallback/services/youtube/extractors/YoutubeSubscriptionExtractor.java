// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.services.youtube.YoutubeService;
import com.vyonex.internal.fallback.subscription.SubscriptionExtractor;
import com.vyonex.internal.fallback.subscription.SubscriptionItem;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonParser;
import com.vyonex.internal.json.JsonParserException;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.vyonex.internal.fallback.subscription.SubscriptionExtractor.ContentSource.INPUT_STREAM;

/** Imports YouTube channel subscriptions from exported JSON, CSV, or ZIP data. */
public class YoutubeSubscriptionExtractor extends SubscriptionExtractor {
    private static final String CHANNEL_URL = "https://www.youtube.com/channel/";

    public YoutubeSubscriptionExtractor(final YoutubeService youtubeService) {
        super(youtubeService, Collections.singletonList(INPUT_STREAM));
    }

    @Override public String getRelatedUrl() {
        return "https://takeout.google.com/takeout/custom/youtube";
    }

    @Override
    public List<SubscriptionItem> fromInputStream(@Nonnull final InputStream content)
            throws ExtractionException {
        return fromJsonInputStream(content);
    }

    @Override
    public List<SubscriptionItem> fromInputStream(@Nonnull final InputStream content,
                                                  @Nonnull final String contentType)
            throws ExtractionException {
        final String type = Objects.requireNonNull(contentType, "content type is required")
                .trim().toLowerCase(Locale.ROOT);
        return switch (type) {
            case "json", "application/json" -> fromJsonInputStream(content);
            case "csv", "text/csv", "text/comma-separated-values" -> fromCsvInputStream(content);
            case "zip", "application/zip" -> fromZipInputStream(content);
            default -> throw new InvalidSourceException("Unsupported content type: " + contentType);
        };
    }

    public List<SubscriptionItem> fromJsonInputStream(@Nonnull final InputStream content)
            throws ExtractionException {
        final JsonArray entries;
        try {
            entries = JsonParser.array().from(content);
        } catch (final JsonParserException error) {
            throw new InvalidSourceException("Invalid JSON input", error);
        }

        final List<SubscriptionItem> subscriptions = new ArrayList<>();
        boolean invalidEntry = false;
        for (final Object value : entries) {
            if (!(value instanceof JsonObject entry)) {
                invalidEntry = true;
                continue;
            }
            final JsonObject snippet = entry.getObject("snippet");
            final String id = snippet.getObject("resourceId").getString("channelId");
            if (id == null || id.length() != 24) {
                invalidEntry = true;
                continue;
            }
            subscriptions.add(new SubscriptionItem(service.getServiceId(), CHANNEL_URL + id,
                    snippet.getString("title", "")));
        }

        if (invalidEntry && subscriptions.isEmpty()) {
            throw new InvalidSourceException("No valid channel IDs were found");
        }
        return List.copyOf(subscriptions);
    }

    public List<SubscriptionItem> fromZipInputStream(@Nonnull final InputStream content)
            throws ExtractionException {
        try (ZipInputStream zip = new ZipInputStream(content, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.getName().toLowerCase(Locale.ROOT).endsWith(".csv")) continue;

                final byte[] csvBytes = zip.readAllBytes();
                try {
                    final List<SubscriptionItem> subscriptions = fromCsvInputStream(
                            new ByteArrayInputStream(csvBytes));
                    if (!subscriptions.isEmpty()) return subscriptions;
                } catch (final ExtractionException ignored) {
                    // Continue through the archive; unrelated CSV exports are common.
                }
            }
        } catch (final IOException error) {
            throw new InvalidSourceException("Could not read subscription archive", error);
        }
        throw new InvalidSourceException("Archive contains no usable subscriptions CSV");
    }

    public List<SubscriptionItem> fromCsvInputStream(@Nonnull final InputStream content)
            throws ExtractionException {
        final List<SubscriptionItem> subscriptions = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(content,
                StandardCharsets.UTF_8))) {
            reader.readLine(); // The first record is the localized export header.
            String line;
            while ((line = reader.readLine()) != null) {
                final List<String> columns = parseCsvLine(line);
                if (columns == null || columns.size() < 3) continue;
                String channelUrl = columns.get(1).trim();
                if (channelUrl.startsWith("http://")) {
                    channelUrl = "https://" + channelUrl.substring("http://".length());
                }
                if (!channelUrl.startsWith(CHANNEL_URL)) continue;
                subscriptions.add(new SubscriptionItem(service.getServiceId(), channelUrl,
                        columns.get(2)));
            }
        } catch (final IOException error) {
            throw new InvalidSourceException("Could not read subscriptions CSV", error);
        }
        return List.copyOf(subscriptions);
    }

    // shortcut: CSV records with embedded newlines are unsupported; add a record reader if Takeout emits them.
    private static List<String> parseCsvLine(final String line) {
        final List<String> columns = new ArrayList<>();
        final StringBuilder value = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            final char character = line.charAt(index);
            if (character == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    value.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (character == ',' && !quoted) {
                columns.add(value.toString());
                value.setLength(0);
            } else {
                value.append(character);
            }
        }
        if (quoted) return null;
        columns.add(value.toString());
        return columns;
    }
}
