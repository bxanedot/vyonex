// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube;

import com.vyonex.internal.json.JsonParser;
import com.vyonex.internal.json.JsonParserException;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YoutubeDescriptionHelperTest {
    @Test
    void escapesPlainTextAndPreservesParagraphSpacing() throws JsonParserException {
        final String html = render("{\"content\":\"<tag>&\\nB  C\"}");

        assertEquals("&lt;tag&gt;&amp;<br>B &nbsp;C", html);
        assertEquals(1, Jsoup.parseBodyFragment(html).select("br").size());
    }

    @Test
    void appliesFormattingAcrossOverlappingRangesAsValidHtml() throws JsonParserException {
        final String html = render("{\"content\":\"abcdef\",\"styleRuns\":["
                + "{\"startIndex\":0,\"length\":4,\"weightLabel\":\"FONT_WEIGHT_BOLD\"},"
                + "{\"startIndex\":2,\"length\":4,\"italic\":true}]}");
        final var body = Jsoup.parseBodyFragment(html).body();

        assertEquals("abcdef", body.text());
        assertEquals("abcd", body.selectFirst("b").text());
        assertEquals(java.util.List.of("cd", "ef"), body.select("i").eachText());
    }

    @Test
    void resolvesExternalLinkLabelsAndEscapesUnsafeContent() throws JsonParserException {
        final String html = render("{\"content\":\"  /short  \","
                + "\"commandRuns\":[{\"startIndex\":0,\"length\":10,"
                + "\"onTap\":{\"innertubeCommand\":{\"urlEndpoint\":{"
                + "\"url\":\"https://example.org/?a=1&b=2\"}}},"
                + "\"onTapOptions\":{\"accessibilityInfo\":{"
                + "\"accessibilityLabel\":\"Website Channel Link: example\"}}}]}");
        final var anchor = Jsoup.parseBodyFragment(html).selectFirst("a");
        assertNotNull(anchor);
        assertEquals("https://example.org/?a=1&b=2", anchor.attr("href"));
        assertEquals("Website: example", anchor.text());

        final String unsafe = render("{\"content\":\"<img src=x onerror=alert(1)>\"}");
        assertTrue(Jsoup.parseBodyFragment(unsafe).select("img").isEmpty());
    }

    @Test
    void ignoresMalformedRangesAndUnsafeLinkSchemes() throws JsonParserException {
        final String html = render("{\"content\":\"abc\",\"styleRuns\":["
                + "{\"startIndex\":2,\"length\":99,\"italic\":true},"
                + "{\"startIndex\":-1,\"length\":2,\"weightLabel\":\"BOLD\"}],"
                + "\"commandRuns\":[{\"startIndex\":0,\"length\":3,"
                + "\"onTap\":{\"innertubeCommand\":{\"urlEndpoint\":{"
                + "\"url\":\"javascript:alert(1)\"}}}},"
                + "{\"startIndex\":0,\"length\":3,\"onTap\":{\"innertubeCommand\":{"
                + "\"urlEndpoint\":{}}}}]}");
        final var body = Jsoup.parseBodyFragment(html).body();

        assertEquals("abc", body.text());
        assertNull(body.selectFirst("a"));
        assertEquals("c", body.selectFirst("i").text());
    }

    private static String render(final String json) throws JsonParserException {
        return YoutubeDescriptionHelper.attributedDescriptionToHtml(JsonParser.object().from(json));
    }
}
