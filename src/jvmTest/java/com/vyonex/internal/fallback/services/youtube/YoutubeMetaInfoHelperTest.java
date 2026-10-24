// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.stream.Description;
import com.vyonex.internal.json.JsonParser;
import com.vyonex.internal.json.JsonParserException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class YoutubeMetaInfoHelperTest {
    @Test
    void extractsInfoPanelsClarificationsAndEmergencyContacts()
            throws JsonParserException, ParsingException {
        final var results = YoutubeMetaInfoHelper.getMetaInfo(JsonParser.array().from(
                "[{\"itemSectionRenderer\":{\"contents\":["
                        + "{\"infoPanelContentRenderer\":{\"paragraphs\":["
                        + "{\"simpleText\":\"Fact\"},{\"simpleText\":\"More\"}],"
                        + "\"sourceEndpoint\":{\"urlEndpoint\":{\"url\":\"https://source.example\"}},"
                        + "\"inlineSource\":{\"simpleText\":\"Source\"}}},"
                        + "{\"clarificationRenderer\":{\"contentTitle\":{\"simpleText\":\"Notice\"},"
                        + "\"text\":{\"simpleText\":\"Details\"},"
                        + "\"actionButton\":{\"buttonRenderer\":{\"command\":{\"urlEndpoint\":"
                        + "{\"url\":\"https://action.example\"}},\"text\":{\"simpleText\":\"Open\"}}},"
                        + "\"secondaryEndpoint\":{\"urlEndpoint\":{\"url\":"
                        + "\"https://www.google.com/search?q=notice\"}},"
                        + "\"secondarySource\":{\"simpleText\":\"Search\"}}},"
                        + "{\"emergencyOneboxRenderer\":{\"support\":"
                        + "{\"singleActionEmergencySupportRenderer\":{\"title\":"
                        + "{\"simpleText\":\"Help\"},\"detailsText\":{\"simpleText\":\"Info\"},"
                        + "\"contacts\":[{\"actionText\":{\"simpleText\":\"Call\"}},"
                        + "{\"actionText\":{\"simpleText\":\"Text\"}}],"
                        + "\"navigationText\":{\"simpleText\":\"Organization\"},"
                        + "\"navigationEndpoint\":{\"urlEndpoint\":{\"url\":"
                        + "\"http://help.example\"}}}}}}]}}]"));

        assertEquals(3, results.size());
        assertEquals("Fact<br>More", results.get(0).getContent().getContent());
        assertEquals(Description.HTML, results.get(0).getContent().getType());
        assertEquals("https://source.example", results.get(0).getUrls().get(0).toString());
        assertEquals("Source", results.get(0).getUrlTexts().get(0));

        assertEquals("Notice", results.get(1).getTitle());
        assertEquals("Details", results.get(1).getContent().getContent());
        assertEquals(Description.PLAIN_TEXT, results.get(1).getContent().getType());
        assertEquals(1, results.get(1).getUrls().size());
        assertEquals("https://action.example", results.get(1).getUrls().get(0).toString());

        assertEquals("Help", results.get(2).getTitle());
        assertEquals("Info\nCall\nText", results.get(2).getContent().getContent());
        assertEquals("https://help.example", results.get(2).getUrls().get(0).toString());
        assertEquals("Organization", results.get(2).getUrlTexts().get(0));
    }

    @Test
    void rejectsAnEmergencyEntryWithoutTextOrDestination() throws JsonParserException {
        final var contents = JsonParser.array().from(
                "[{\"itemSectionRenderer\":{\"contents\":[{\"emergencyOneboxRenderer\":{"
                        + "\"support\":{\"singleActionEmergencySupportRenderer\":{}}}}]}}]");

        assertThrows(ParsingException.class, () -> YoutubeMetaInfoHelper.getMetaInfo(contents));
    }
}
