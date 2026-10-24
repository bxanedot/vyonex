// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.channel;

import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.downloader.Request;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.services.youtube.YoutubeService;
import com.vyonex.internal.fallback.services.youtube.linkHandler.YoutubeChannelLinkHandlerFactory;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChannelInfoContractTest {
    @Test
    void collectsChannelDetailsAndKeepsOptionalFailures() throws Exception {
        final Downloader previous = VyonEx.getDownloader();
        VyonEx.init(new Downloader() {
            @Override
            public Response execute(final Request request) throws IOException {
                throw new AssertionError("channel info should not fetch a page");
            }
        });
        final ChannelInfo info;
        try {
            info = ChannelInfo.getInfo(new ExampleChannelExtractor());
        } finally {
            VyonEx.init(previous);
        }

        assertEquals(77, info.getServiceId());
        assertEquals("channel/UC123", info.getId());
        assertEquals("Creator", info.getName());
        assertEquals("https://www.youtube.com/channel/UC123", info.getUrl());
        assertEquals("https://www.youtube.com/channel/UC123", info.getOriginalUrl());
        assertEquals("https://img.example/avatar.jpg", info.getAvatars().get(0).getUrl());
        assertEquals("https://img.example/banner.jpg", info.getBanners().get(0).getUrl());
        assertEquals("https://media.example/feed.xml", info.getFeedUrl());
        assertEquals(101, info.getSubscriberCount());
        assertEquals("Parent", info.getParentChannelName());
        assertEquals("https://media.example/parent", info.getParentChannelUrl());
        assertEquals("https://img.example/parent.jpg",
                info.getParentChannelAvatars().get(0).getUrl());
        assertTrue(info.isVerified());
        assertTrue(info.getTabs().isEmpty());
        assertTrue(info.getTags().isEmpty());
        assertEquals(1, info.getErrors().size());
        assertEquals("description unavailable", info.getErrors().get(0).getMessage());
    }

    private static Image image(final String file) {
        return new Image("https://img.example/" + file, 90, 90, Image.ResolutionLevel.LOW);
    }

    private static final class ExampleChannelExtractor extends ChannelExtractor {
        private ExampleChannelExtractor() throws ParsingException {
            super(new YoutubeService(77), YoutubeChannelLinkHandlerFactory.getInstance()
                    .fromUrl("https://www.youtube.com/channel/UC123"));
        }

        @Override public void onFetchPage(final Downloader downloader) { }
        @Override public String getName() { return "Creator"; }
        @Override public List<Image> getAvatars() { return List.of(image("avatar.jpg")); }
        @Override public List<Image> getBanners() { return List.of(image("banner.jpg")); }
        @Override public String getFeedUrl() { return "https://media.example/feed.xml"; }
        @Override public long getSubscriberCount() { return 101; }
        @Override public String getDescription() throws ParsingException {
            throw new ParsingException("description unavailable");
        }
        @Override public String getParentChannelName() { return "Parent"; }
        @Override public String getParentChannelUrl() { return "https://media.example/parent"; }
        @Override public List<Image> getParentChannelAvatars() {
            return List.of(image("parent.jpg"));
        }
        @Override public boolean isVerified() { return true; }
        @Override public List<ListLinkHandler> getTabs() { return List.of(); }
    }
}
