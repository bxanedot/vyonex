// bxane
package com.vyonex.internal.fallback.services.peertube.extractors;

import com.vyonex.internal.json.JsonArray;
import com.vyonex.internal.json.JsonObject;
import com.vyonex.internal.json.JsonParser;
import com.vyonex.internal.json.JsonParserException;
import com.vyonex.internal.fallback.Image;
import com.vyonex.internal.fallback.StreamingService;
import com.vyonex.internal.fallback.channel.ChannelExtractor;
import com.vyonex.internal.fallback.channel.tabs.ChannelTabs;
import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.downloader.Response;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.vyonex.internal.fallback.exceptions.ReCaptchaException;
import com.vyonex.internal.fallback.linkhandler.ListLinkHandler;
import com.vyonex.internal.fallback.services.peertube.linkHandler.PeertubeChannelLinkHandlerFactory;
import com.vyonex.internal.fallback.services.peertube.linkHandler.PeertubeChannelTabLinkHandlerFactory;
import com.vyonex.internal.fallback.utils.JsonUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;

import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.getAvatarsFromOwnerAccountOrVideoChannelObject;
import static com.vyonex.internal.fallback.services.peertube.PeertubeParsingHelper.getBannersFromAccountOrVideoChannelObject;

public class PeertubeAccountExtractor extends ChannelExtractor {
    private JsonObject json;
    private final String baseUrl;
    private static final String ACCOUNTS = "accounts/";

    public PeertubeAccountExtractor(final StreamingService service,
                                    final ListLinkHandler linkHandler) throws ParsingException {
        super(service, linkHandler);
        this.baseUrl = getBaseUrl();
    }

    @Nonnull
    @Override
    public List<Image> getAvatars() {
        return getAvatarsFromOwnerAccountOrVideoChannelObject(baseUrl, json);
    }

    @Nonnull
    @Override
    public List<Image> getBanners() {
        return getBannersFromAccountOrVideoChannelObject(baseUrl, json);
    }

    @Override
    public String getFeedUrl() throws ParsingException {
        return getBaseUrl() + "/feeds/videos.xml?accountId=" + json.get("id");
    }

    @Override
    public long getSubscriberCount() throws ParsingException {
        // The subscriber count cannot be retrieved directly. It needs to be calculated.
        // An accounts subscriber count is the number of the channel owner's subscriptions
        // plus the sum of all sub channels subscriptions.
        long subscribersCount = json.getLong("followersCount");
        String accountVideoChannelUrl = baseUrl + PeertubeChannelLinkHandlerFactory.API_ENDPOINT;
        if (getId().contains(ACCOUNTS)) {
            accountVideoChannelUrl += getId();
        } else {
            accountVideoChannelUrl += ACCOUNTS + getId();
        }
        accountVideoChannelUrl += "/video-channels";

        try {
            final String responseBody = getDownloader().get(accountVideoChannelUrl).responseBody();
            final JsonObject jsonResponse = JsonParser.object().from(responseBody);
            final JsonArray videoChannels = jsonResponse.getArray("data");
            for (final Object videoChannel : videoChannels) {
                final JsonObject videoChannelJsonObject = (JsonObject) videoChannel;
                subscribersCount += videoChannelJsonObject.getInt("followersCount");
            }
        } catch (final IOException | JsonParserException | ReCaptchaException ignored) {
            // something went wrong during video channels extraction,
            // only return subscribers of ownerAccount
        }
        return subscribersCount;
    }

    @Nullable
    @Override
    public String getDescription() {
        return json.getString("description");
    }

    @Override
    public String getParentChannelName() {
        return "";
    }

    @Override
    public String getParentChannelUrl() {
        return "";
    }

    @Nonnull
    @Override
    public List<Image> getParentChannelAvatars() {
        return List.of();
    }

    @Override
    public boolean isVerified() throws ParsingException {
        return false;
    }

    @Nonnull
    @Override
    public List<ListLinkHandler> getTabs() throws ParsingException {
        return List.of(
                PeertubeChannelTabLinkHandlerFactory.getInstance().fromQuery(getId(),
                        List.of(ChannelTabs.VIDEOS), "", getBaseUrl()),
                PeertubeChannelTabLinkHandlerFactory.getInstance().fromQuery(getId(),
                        List.of(ChannelTabs.CHANNELS), "", getBaseUrl()));
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        final Response response = downloader.get(baseUrl
                + PeertubeChannelLinkHandlerFactory.API_ENDPOINT + getId());
        if (response != null) {
            setInitialData(response.responseBody());
        } else {
            throw new ExtractionException("Unable to extract PeerTube account data");
        }
    }

    private void setInitialData(final String responseBody) throws ExtractionException {
        try {
            json = JsonParser.object().from(responseBody);
        } catch (final JsonParserException e) {
            throw new ExtractionException("Unable to extract PeerTube account data", e);
        }
        if (json == null) {
            throw new ExtractionException("Unable to extract PeerTube account data");
        }
    }

    @Nonnull
    @Override
    public String getName() throws ParsingException {
        return JsonUtils.getString(json, "displayName");
    }
}
