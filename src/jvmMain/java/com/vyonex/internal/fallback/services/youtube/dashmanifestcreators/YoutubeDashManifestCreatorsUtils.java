// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube.dashmanifestcreators;

import com.vyonex.internal.fallback.MediaFormat;
import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.services.youtube.DeliveryType;
import com.vyonex.internal.fallback.services.youtube.ItagItem;
import com.vyonex.internal.fallback.stream.AudioTrackType;
import com.vyonex.internal.fallback.utils.ManifestCreatorCache;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getAndroidUserAgent;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getIosUserAgent;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getOriginReferrerHeaders;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.getVisionOsUserAgent;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.isAndroidStreamingUrl;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.isIosStreamingUrl;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.isVisionOsStreamingUrl;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.isWebEmbeddedPlayerStreamingUrl;
import static com.vyonex.internal.fallback.services.youtube.YoutubeParsingHelper.isWebStreamingUrl;

public final class YoutubeDashManifestCreatorsUtils {
    public static final int MAXIMUM_REDIRECT_COUNT = 20;
    public static final String SQ_0 = "&sq=0";
    public static final String RN_0 = "&rn=0";
    public static final String ALR_YES = "&alr=yes";

    public static final String MPD = "MPD";
    public static final String PERIOD = "Period";
    public static final String ADAPTATION_SET = "AdaptationSet";
    public static final String ROLE = "Role";
    public static final String REPRESENTATION = "Representation";
    public static final String AUDIO_CHANNEL_CONFIGURATION = "AudioChannelConfiguration";
    public static final String SEGMENT_TEMPLATE = "SegmentTemplate";
    public static final String SEGMENT_TIMELINE = "SegmentTimeline";
    public static final String BASE_URL = "BaseURL";
    public static final String SEGMENT_BASE = "SegmentBase";
    public static final String INITIALIZATION = "Initialization";

    private YoutubeDashManifestCreatorsUtils() { }

    public static void setAttribute(final Element element, final Document document,
                                    final String name, final String value) {
        final Attr attribute = document.createAttribute(name);
        attribute.setValue(value);
        element.setAttributeNode(attribute);
    }

    @Nonnull
    public static Document generateDocumentAndDoCommonElementsGeneration(
            @Nonnull final ItagItem itag, final long durationMs) throws CreationException {
        final Document document = generateDocumentAndMpdElement(durationMs);
        generatePeriodElement(document);
        generateAdaptationSetElement(document, itag);
        generateRoleElement(document, itag);
        generateRepresentationElement(document, itag);
        if (itag.itagType == ItagItem.ItagType.AUDIO) {
            generateAudioChannelConfigurationElement(document, itag);
        }
        return document;
    }

    @Nonnull
    public static Document generateDocumentAndMpdElement(final long durationMs)
            throws CreationException {
        try {
            final Document document = newDocument();
            final Element mpd = document.createElement(MPD);
            document.appendChild(mpd);
            setAttribute(mpd, document, "xmlns:xsi",
                    "http://www.w3.org/2001/XMLSchema-instance");
            setAttribute(mpd, document, "xmlns", "urn:mpeg:DASH:schema:MPD:2011");
            setAttribute(mpd, document, "xsi:schemaLocation",
                    "urn:mpeg:DASH:schema:MPD:2011 DASH-MPD.xsd");
            setAttribute(mpd, document, "minBufferTime", "PT1.500S");
            setAttribute(mpd, document, "profiles", "urn:mpeg:dash:profile:full:2011");
            setAttribute(mpd, document, "type", "static");
            setAttribute(mpd, document, "mediaPresentationDuration",
                    String.format(Locale.ENGLISH, "PT%.3fS", durationMs / 1000.0));
            return document;
        } catch (final Exception e) {
            throw new CreationException("Could not create DASH document", e);
        }
    }

    public static void generatePeriodElement(@Nonnull final Document document)
            throws CreationException {
        try {
            document.getDocumentElement().appendChild(document.createElement(PERIOD));
        } catch (final DOMException e) {
            throw CreationException.couldNotAddElement(PERIOD, e);
        }
    }

    public static void generateAdaptationSetElement(@Nonnull final Document document,
                                                    @Nonnull final ItagItem itag)
            throws CreationException {
        final MediaFormat format = itag.getMediaFormat();
        if (format == null || format.getMimeType() == null || format.getMimeType().isBlank()) {
            throw CreationException.couldNotAddElement(ADAPTATION_SET,
                    "the media type is missing");
        }
        try {
            final Element adaptation = document.createElement(ADAPTATION_SET);
            setAttribute(adaptation, document, "id", "0");
            if (itag.itagType == ItagItem.ItagType.AUDIO && itag.getAudioLocale() != null) {
                final String language = itag.getAudioLocale().getLanguage();
                if (!language.isBlank()) setAttribute(adaptation, document, "lang", language);
            }
            setAttribute(adaptation, document, "mimeType", format.getMimeType());
            setAttribute(adaptation, document, "subsegmentAlignment", "true");
            document.getElementsByTagName(PERIOD).item(0).appendChild(adaptation);
        } catch (final DOMException e) {
            throw CreationException.couldNotAddElement(ADAPTATION_SET, e);
        }
    }

    public static void generateRoleElement(@Nonnull final Document document,
                                           @Nonnull final ItagItem itag)
            throws CreationException {
        try {
            final Element role = document.createElement(ROLE);
            setAttribute(role, document, "schemeIdUri", "urn:mpeg:DASH:role:2011");
            setAttribute(role, document, "value", roleValue(itag.getAudioTrackType()));
            document.getElementsByTagName(ADAPTATION_SET).item(0).appendChild(role);
        } catch (final DOMException e) {
            throw CreationException.couldNotAddElement(ROLE, e);
        }
    }

    private static String roleValue(@Nullable final AudioTrackType type) {
        if (type == null || type == AudioTrackType.ORIGINAL) return "main";
        if (type == AudioTrackType.DUBBED) return "dub";
        if (type == AudioTrackType.DESCRIPTIVE) return "description";
        return "alternate";
    }

    public static void generateRepresentationElement(@Nonnull final Document document,
                                                     @Nonnull final ItagItem itag)
            throws CreationException {
        if (itag.id <= 0) {
            throw CreationException.couldNotAddElement(REPRESENTATION,
                    "the itag id must be positive");
        }
        if (itag.getCodec() == null || itag.getCodec().isBlank()) {
            throw CreationException.couldNotAddElement(REPRESENTATION, "codec is missing");
        }
        if (itag.getBitrate() <= 0) {
            throw CreationException.couldNotAddElement(REPRESENTATION,
                    "the bitrate must be positive");
        }
        try {
            final Element representation = document.createElement(REPRESENTATION);
            setAttribute(representation, document, "id", String.valueOf(itag.id));
            setAttribute(representation, document, "codecs", itag.getCodec());
            setAttribute(representation, document, "startWithSAP", "1");
            setAttribute(representation, document, "maxPlayoutRate", "1");
            setAttribute(representation, document, "bandwidth", String.valueOf(itag.getBitrate()));
            if (itag.itagType == ItagItem.ItagType.VIDEO
                    || itag.itagType == ItagItem.ItagType.VIDEO_ONLY) {
                if (itag.getWidth() <= 0 && itag.getHeight() <= 0) {
                    throw CreationException.couldNotAddElement(REPRESENTATION,
                            "video dimensions are missing");
                }
                if (itag.getWidth() > 0) {
                    setAttribute(representation, document, "width",
                            String.valueOf(itag.getWidth()));
                }
                setAttribute(representation, document, "height", String.valueOf(itag.getHeight()));
                if (itag.getFps() > 0) {
                    setAttribute(representation, document, "frameRate",
                            String.valueOf(itag.getFps()));
                }
            }
            if (itag.itagType == ItagItem.ItagType.AUDIO && itag.getSampleRate() > 0) {
                setAttribute(representation, document, "audioSamplingRate",
                        String.valueOf(itag.getSampleRate()));
            }
            document.getElementsByTagName(ADAPTATION_SET).item(0).appendChild(representation);
        } catch (final DOMException e) {
            throw CreationException.couldNotAddElement(REPRESENTATION, e);
        }
    }

    public static void generateAudioChannelConfigurationElement(
            @Nonnull final Document document, @Nonnull final ItagItem itag)
            throws CreationException {
        if (itag.getAudioChannels() <= 0) {
            throw new CreationException("the number of audioChannels in the ItagItem is <= 0: "
                    + itag.getAudioChannels());
        }
        try {
            final Element configuration = document.createElement(AUDIO_CHANNEL_CONFIGURATION);
            setAttribute(configuration, document, "schemeIdUri",
                    "urn:mpeg:dash:23003:3:audio_channel_configuration:2011");
            setAttribute(configuration, document, "value", String.valueOf(itag.getAudioChannels()));
            document.getElementsByTagName(REPRESENTATION).item(0).appendChild(configuration);
        } catch (final DOMException e) {
            throw CreationException.couldNotAddElement(AUDIO_CHANNEL_CONFIGURATION, e);
        }
    }

    public static String buildAndCacheResult(@Nonnull final String originalUrl,
                                             @Nonnull final Document document,
                                             @Nonnull final ManifestCreatorCache<String, String> cache)
            throws CreationException {
        try {
            final String xml = documentToXml(document);
            cache.put(originalUrl, xml);
            return xml;
        } catch (final Exception e) {
            throw new CreationException("Could not serialize DASH document", e);
        }
    }

    public static void generateSegmentTemplateElement(@Nonnull final Document document,
                                                      @Nonnull final String baseUrl,
                                                      final DeliveryType deliveryType)
            throws CreationException {
        if (deliveryType != DeliveryType.OTF && deliveryType != DeliveryType.LIVE) {
            throw CreationException.couldNotAddElement(SEGMENT_TEMPLATE,
                    "invalid delivery type: " + deliveryType);
        }
        try {
            final Element template = document.createElement(SEGMENT_TEMPLATE);
            setAttribute(template, document, "startNumber",
                    deliveryType == DeliveryType.LIVE ? "0" : "1");
            setAttribute(template, document, "timescale", "1000");
            if (deliveryType == DeliveryType.OTF) {
                setAttribute(template, document, "initialization", baseUrl + SQ_0);
            }
            setAttribute(template, document, "media", baseUrl + "&sq=$Number$");
            document.getElementsByTagName(REPRESENTATION).item(0).appendChild(template);
        } catch (final DOMException e) {
            throw CreationException.couldNotAddElement(SEGMENT_TEMPLATE, e);
        }
    }

    public static void generateSegmentTimelineElement(@Nonnull final Document document)
            throws CreationException {
        try {
            document.getElementsByTagName(SEGMENT_TEMPLATE).item(0)
                    .appendChild(document.createElement(SEGMENT_TIMELINE));
        } catch (final DOMException e) {
            throw CreationException.couldNotAddElement(SEGMENT_TIMELINE, e);
        }
    }

    @Nonnull
    public static Response getInitializationResponse(@Nonnull final String streamUrl,
                                                     @Nonnull final ItagItem itag,
                                                     final DeliveryType deliveryType)
            throws CreationException {
        final boolean web = isWebStreamingUrl(streamUrl)
                || isWebEmbeddedPlayerStreamingUrl(streamUrl);
        String requestUrl = streamUrl + (web ? ALR_YES : "");
        if (deliveryType != DeliveryType.PROGRESSIVE) requestUrl += SQ_0;
        requestUrl += RN_0;

        final Downloader downloader = VyonEx.getDownloader();
        if (downloader == null) throw new CreationException("No downloader is configured");
        if (web && itag.getMediaFormat() != null
                && !itag.getMediaFormat().getMimeType().isBlank()) {
            return getWebResponse(downloader, requestUrl);
        }
        try {
            if (isAndroidStreamingUrl(requestUrl)) {
                return downloader.post(requestUrl,
                        Map.of("User-Agent", List.of(getAndroidUserAgent(null))), new byte[0]);
            }
            if (isIosStreamingUrl(requestUrl)) {
                return downloader.post(requestUrl,
                        Map.of("User-Agent", List.of(getIosUserAgent(null))), new byte[0]);
            }
            if (isVisionOsStreamingUrl(requestUrl)) {
                return downloader.post(requestUrl,
                        Map.of("User-Agent", List.of(getVisionOsUserAgent(null))), new byte[0]);
            }
            return downloader.get(requestUrl);
        } catch (final IOException | ExtractionException e) {
            throw new CreationException("Could not fetch stream initialization data", e);
        }
    }

    private static Response getWebResponse(final Downloader downloader, String url) {
        final Map<String, List<String>> headers = getOriginReferrerHeaders(
                "https://www.youtube.com");
        final byte[] body = {0x78, 0};
        for (int redirects = 0; ; redirects++) {
            final Response response;
            try {
                response = downloader.post(url, headers, body);
            } catch (final IOException | ExtractionException e) {
                throw new CreationException("Could not fetch web stream initialization data", e);
            }
            if (response.responseCode() != 200) {
                throw new CreationException("Could not get the initialization URL: HTTP response "
                        + response.responseCode());
            }
            final String contentType = response.getHeader("Content-Type");
            if (contentType == null) {
                throw new CreationException("The stream response has no Content-Type header");
            }
            if (!contentType.equals("text/plain")) return response;
            if (redirects >= MAXIMUM_REDIRECT_COUNT) {
                throw new CreationException("Too many web stream redirects");
            }
            url = response.responseBody();
            if (url.isBlank()) throw new CreationException("The stream redirect URL is empty");
        }
    }

    private static Document newDocument() throws Exception {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        } catch (final Exception ignored) {
            // Some platform XML providers do not support these restrictions.
        }
        return factory.newDocumentBuilder().newDocument();
    }

    private static String documentToXml(final Document document) throws Exception {
        final TransformerFactory factory = TransformerFactory.newInstance();
        try {
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        } catch (final Exception ignored) {
            // Some platform XML providers do not support these restrictions.
        }
        final var transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.VERSION, "1.0");
        transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
        transformer.setOutputProperty(OutputKeys.STANDALONE, "no");
        final StringWriter output = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(output));
        return output.toString();
    }
}
