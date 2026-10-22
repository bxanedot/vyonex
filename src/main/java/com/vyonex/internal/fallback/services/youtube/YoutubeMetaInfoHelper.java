// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube;

import com.vyonex.internal.fallback.MetaInfo;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.stream.Description;
import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public final class YoutubeMetaInfoHelper {
    private YoutubeMetaInfoHelper() { }

    public static List<MetaInfo> getMetaInfo(final JsonArray contents) throws ParsingException {
        final List<MetaInfo> results = new ArrayList<>();
        for (final Object sectionValue : contents) {
            if (!(sectionValue instanceof JsonObject)) continue;
            final JsonObject section = (JsonObject) sectionValue;
            if (!section.has("itemSectionRenderer")) continue;
            for (final Object contentValue : section.getObject("itemSectionRenderer")
                    .getArray("contents")) {
                if (!(contentValue instanceof JsonObject)) continue;
                final JsonObject content = (JsonObject) contentValue;
                if (content.has("infoPanelContentRenderer")) {
                    results.add(infoPanel(content.getObject("infoPanelContentRenderer")));
                }
                if (content.has("clarificationRenderer")) {
                    results.add(clarification(content.getObject("clarificationRenderer")));
                }
                if (content.has("emergencyOneboxRenderer")) {
                    results.addAll(emergency(content.getObject("emergencyOneboxRenderer")));
                }
            }
        }
        return results;
    }

    private static MetaInfo infoPanel(final JsonObject renderer) throws ParsingException {
        final MetaInfo result = new MetaInfo();
        final StringBuilder paragraphs = new StringBuilder();
        for (final Object value : renderer.getArray("paragraphs")) {
            if (!(value instanceof JsonObject)) continue;
            final String text = YoutubeParsingHelper.getTextFromObject((JsonObject) value);
            if (text == null) continue;
            if (paragraphs.length() > 0) paragraphs.append("<br>");
            paragraphs.append(text);
        }
        result.setContent(new Description(paragraphs.toString(), Description.HTML));

        if (renderer.has("sourceEndpoint")) {
            result.addUrl(requiredUrl(YoutubeParsingHelper.getUrlFromNavigationEndpoint(
                    renderer.getObject("sourceEndpoint")), "metadata source"));
            final JsonObject label = renderer.has("inlineSource")
                    ? renderer.getObject("inlineSource") : renderer.getObject("disclaimer");
            final String text = YoutubeParsingHelper.getTextFromObject(label);
            if (text == null || text.isEmpty()) {
                throw new ParsingException("Metadata source has no label");
            }
            result.addUrlText(text);
        }
        return result;
    }

    private static MetaInfo clarification(final JsonObject renderer) throws ParsingException {
        final String title = YoutubeParsingHelper.getTextFromObject(
                renderer.getObject("contentTitle"));
        final String text = YoutubeParsingHelper.getTextFromObject(renderer.getObject("text"));
        if (title == null || text == null) {
            throw new ParsingException("Clarification metadata is missing its title or text");
        }
        final MetaInfo result = new MetaInfo();
        result.setTitle(title);
        result.setContent(new Description(text, Description.PLAIN_TEXT));

        if (renderer.has("actionButton")) {
            final JsonObject button = renderer.getObject("actionButton")
                    .getObject("buttonRenderer");
            result.addUrl(requiredUrl(YoutubeParsingHelper.getUrlFromNavigationEndpoint(
                    button.getObject("command")), "metadata action"));
            final String label = YoutubeParsingHelper.getTextFromObject(
                    button.getObject("text"));
            if (label == null || label.isEmpty()) {
                throw new ParsingException("Metadata action has no label");
            }
            result.addUrlText(label);
        }

        if (renderer.has("secondaryEndpoint") && renderer.has("secondarySource")) {
            final String value = YoutubeParsingHelper.getUrlFromNavigationEndpoint(
                    renderer.getObject("secondaryEndpoint"));
            if (value != null && !YoutubeParsingHelper.isGoogleURL(value)) {
                result.addUrl(requiredUrl(value, "secondary metadata"));
                final String label = YoutubeParsingHelper.getTextFromObject(
                        renderer.getObject("secondarySource"));
                result.addUrlText(label == null ? value : label);
            }
        }
        return result;
    }

    private static List<MetaInfo> emergency(final JsonObject renderer) throws ParsingException {
        final List<JsonObject> items = new ArrayList<>();
        for (final Object value : renderer.values()) {
            if (value instanceof JsonObject) {
                final JsonObject container = (JsonObject) value;
                for (final Object child : container.values()) {
                    if (child instanceof JsonObject
                            && ((JsonObject) child).has("singleActionEmergencySupportRenderer")) {
                        items.add(((JsonObject) child)
                                .getObject("singleActionEmergencySupportRenderer"));
                    }
                }
                if (container.has("singleActionEmergencySupportRenderer")) {
                    items.add(container.getObject("singleActionEmergencySupportRenderer"));
                }
            }
        }
        if (items.isEmpty()) {
            throw new ParsingException("Emergency metadata contains no support entry");
        }

        final List<MetaInfo> results = new ArrayList<>(items.size());
        for (final JsonObject item : items) {
            final String title = requiredText(item.getObject("title"), "emergency title");
            final String details = requiredText(item.getObject("detailsText"), "emergency details");
            final String label = requiredText(item.getObject("navigationText"),
                    "emergency link label");
            final StringBuilder action = new StringBuilder();
            if (item.has("actionText")) {
                action.append('\n').append(requiredText(item.getObject("actionText"),
                        "emergency action"));
            } else if (item.has("contacts")) {
                for (final Object contactValue : item.getArray("contacts")) {
                    if (!(contactValue instanceof JsonObject)) continue;
                    action.append('\n').append(requiredText(
                            ((JsonObject) contactValue).getObject("actionText"),
                            "emergency contact"));
                }
            }

            final String destination = YoutubeParsingHelper.getUrlFromNavigationEndpoint(
                    item.getObject("navigationEndpoint"));
            final MetaInfo result = new MetaInfo();
            result.setTitle(title);
            result.setContent(new Description(details + action, Description.PLAIN_TEXT));
            result.addUrlText(label);
            result.addUrl(requiredUrl(httpsUrl(destination), "emergency destination"));
            results.add(result);
        }
        return results;
    }

    private static String requiredText(final JsonObject value, final String field)
            throws ParsingException {
        final String text = YoutubeParsingHelper.getTextFromObject(value);
        if (text == null) throw new ParsingException("Missing " + field);
        return text;
    }

    private static String httpsUrl(final String value) {
        return value != null && value.startsWith("http://")
                ? "https://" + value.substring("http://".length()) : value;
    }

    private static URL requiredUrl(final String value, final String field) throws ParsingException {
        if (value == null || value.isEmpty()) {
            throw new ParsingException("Missing " + field + " URL");
        }
        try {
            final String cached = YoutubeParsingHelper.extractCachedUrlIfNeeded(value);
            return new URL(cached);
        } catch (final MalformedURLException e) {
            throw new ParsingException("Invalid " + field + " URL", e);
        }
    }
}
