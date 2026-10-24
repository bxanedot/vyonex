// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.channel;

import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.Info;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.VyonEx;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;

public class ChannelInfo extends Info {
    private String parentChannelName;
    private String parentChannelUrl;
    private String feedUrl;
    private long subscriberCount = ChannelExtractor.UNKNOWN_SUBSCRIBER_COUNT;
    private String description;
    private String[] donationLinks;
    @Nonnull private List<Image> avatars = List.of();
    @Nonnull private List<Image> banners = List.of();
    @Nonnull private List<Image> parentChannelAvatars = List.of();
    private boolean verified;
    @Nonnull private List<ListLinkHandler> tabs = List.of();
    @Nonnull private List<String> tags = List.of();

    public ChannelInfo(final int serviceId, final String id, final String url,
                       final String originalUrl, final String name) {
        super(serviceId, id, url, originalUrl, name);
    }

    public static ChannelInfo getInfo(final String url) throws IOException, ExtractionException {
        return getInfo(VyonEx.getServiceByUrl(url), url);
    }

    public static ChannelInfo getInfo(final StreamingService service, final String url)
            throws IOException, ExtractionException {
        final ChannelExtractor extractor = service.getChannelExtractor(url);
        extractor.fetchPage();
        return getInfo(extractor);
    }

    public static ChannelInfo getInfo(final ChannelExtractor extractor)
            throws IOException, ExtractionException {
        final ChannelInfo result = new ChannelInfo(extractor.getServiceId(), extractor.getId(),
                extractor.getUrl(), extractor.getOriginalUrl(), extractor.getName());
        try { result.setAvatars(extractor.getAvatars()); } catch (final Exception e) { result.addError(e); }
        try { result.setBanners(extractor.getBanners()); } catch (final Exception e) { result.addError(e); }
        try { result.setFeedUrl(extractor.getFeedUrl()); } catch (final Exception e) { result.addError(e); }
        try { result.setSubscriberCount(extractor.getSubscriberCount()); } catch (final Exception e) { result.addError(e); }
        try { result.setDescription(extractor.getDescription()); } catch (final Exception e) { result.addError(e); }
        try { result.setParentChannelName(extractor.getParentChannelName()); } catch (final Exception e) { result.addError(e); }
        try { result.setParentChannelUrl(extractor.getParentChannelUrl()); } catch (final Exception e) { result.addError(e); }
        try { result.setParentChannelAvatars(extractor.getParentChannelAvatars()); } catch (final Exception e) { result.addError(e); }
        try { result.setVerified(extractor.isVerified()); } catch (final Exception e) { result.addError(e); }
        try { result.setTabs(extractor.getTabs()); } catch (final Exception e) { result.addError(e); }
        try { result.setTags(extractor.getTags()); } catch (final Exception e) { result.addError(e); }
        return result;
    }

    public String getParentChannelName() { return parentChannelName; }
    public void setParentChannelName(final String value) { parentChannelName = value; }
    public String getParentChannelUrl() { return parentChannelUrl; }
    public void setParentChannelUrl(final String value) { parentChannelUrl = value; }
    @Nonnull public List<Image> getParentChannelAvatars() { return parentChannelAvatars; }
    public void setParentChannelAvatars(@Nonnull final List<Image> value) { parentChannelAvatars = value; }
    @Nonnull public List<Image> getAvatars() { return avatars; }
    public void setAvatars(@Nonnull final List<Image> value) { avatars = value; }
    @Nonnull public List<Image> getBanners() { return banners; }
    public void setBanners(@Nonnull final List<Image> value) { banners = value; }
    public String getFeedUrl() { return feedUrl; }
    public void setFeedUrl(final String value) { feedUrl = value; }
    public long getSubscriberCount() { return subscriberCount; }
    public void setSubscriberCount(final long value) { subscriberCount = value; }
    public String getDescription() { return description; }
    public void setDescription(final String value) { description = value; }
    public String[] getDonationLinks() { return donationLinks; }
    public void setDonationLinks(final String[] value) { donationLinks = value; }
    public boolean isVerified() { return verified; }
    public void setVerified(final boolean value) { verified = value; }
    @Nonnull public List<ListLinkHandler> getTabs() { return tabs; }
    public void setTabs(@Nonnull final List<ListLinkHandler> value) { tabs = value; }
    @Nonnull public List<String> getTags() { return tags; }
    public void setTags(@Nonnull final List<String> value) { tags = value; }
}
