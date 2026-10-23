// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.vyonex.internal.fallback.services.youtube.ClientsConstants.ANDROID_CLIENT_ID;
import static com.vyonex.internal.fallback.services.youtube.ClientsConstants.ANDROID_CLIENT_NAME;
import static com.vyonex.internal.fallback.services.youtube.ClientsConstants.ANDROID_CLIENT_VERSION;
import static com.vyonex.internal.fallback.services.youtube.ClientsConstants.DESKTOP_CLIENT_PLATFORM;
import static com.vyonex.internal.fallback.services.youtube.ClientsConstants.EMBED_CLIENT_SCREEN;
import static com.vyonex.internal.fallback.services.youtube.ClientsConstants.IOS_CLIENT_ID;
import static com.vyonex.internal.fallback.services.youtube.ClientsConstants.IOS_CLIENT_NAME;
import static com.vyonex.internal.fallback.services.youtube.ClientsConstants.IOS_CLIENT_VERSION;
import static com.vyonex.internal.fallback.services.youtube.ClientsConstants.IOS_DEVICE_MODEL;
import static com.vyonex.internal.fallback.services.youtube.ClientsConstants.IOS_OS_VERSION;
import static com.vyonex.internal.fallback.services.youtube.ClientsConstants.MOBILE_CLIENT_PLATFORM;
import static com.vyonex.internal.fallback.services.youtube.ClientsConstants.VISIONOS_CLIENT_ID;
import static com.vyonex.internal.fallback.services.youtube.ClientsConstants.VISIONOS_CLIENT_NAME;
import static com.vyonex.internal.fallback.services.youtube.ClientsConstants.VISIONOS_CLIENT_VERSION;
import static com.vyonex.internal.fallback.services.youtube.ClientsConstants.VISIONOS_DEVICE_MODEL;
import static com.vyonex.internal.fallback.services.youtube.ClientsConstants.VISIONOS_VERSION;
import static com.vyonex.internal.fallback.services.youtube.ClientsConstants.WATCH_CLIENT_SCREEN;
import static com.vyonex.internal.fallback.services.youtube.ClientsConstants.WEB_CLIENT_ID;
import static com.vyonex.internal.fallback.services.youtube.ClientsConstants.WEB_CLIENT_NAME;
import static com.vyonex.internal.fallback.services.youtube.ClientsConstants.WEB_EMBEDDED_CLIENT_ID;
import static com.vyonex.internal.fallback.services.youtube.ClientsConstants.WEB_EMBEDDED_CLIENT_NAME;
import static com.vyonex.internal.fallback.services.youtube.ClientsConstants.WEB_EMBEDDED_CLIENT_VERSION;
import static com.vyonex.internal.fallback.services.youtube.ClientsConstants.WEB_HARDCODED_CLIENT_VERSION;
import static com.vyonex.internal.fallback.services.youtube.ClientsConstants.WEB_MUSIC_ANALYTICS_CLIENT_ID;
import static com.vyonex.internal.fallback.services.youtube.ClientsConstants.WEB_MUSIC_ANALYTICS_CLIENT_NAME;
import static com.vyonex.internal.fallback.services.youtube.ClientsConstants.WEB_MUSIC_ANALYTICS_CLIENT_VERSION;

public final class InnertubeClientRequestInfo {
    @Nonnull public ClientInfo clientInfo;
    @Nonnull public DeviceInfo deviceInfo;

    public static final class ClientInfo {
        @Nonnull public String clientName;
        @Nonnull public String clientVersion;
        @Nonnull public String clientId;
        @Nullable public String clientScreen;
        @Nullable public String visitorData;

        private ClientInfo(final String name, final String version, final String id,
                           @Nullable final String screen, @Nullable final String visitor) {
            clientName = name;
            clientVersion = version;
            clientId = id;
            clientScreen = screen;
            visitorData = visitor;
        }
    }

    public static final class DeviceInfo {
        @Nullable public String platform;
        @Nullable public String deviceMake;
        @Nullable public String deviceModel;
        @Nullable public String osName;
        @Nullable public String osVersion;
        public int androidSdkVersion;

        private DeviceInfo(@Nullable final String platform, @Nullable final String make,
                           @Nullable final String model, @Nullable final String os,
                           @Nullable final String version, final int sdk) {
            this.platform = platform;
            deviceMake = make;
            deviceModel = model;
            osName = os;
            osVersion = version;
            androidSdkVersion = sdk;
        }
    }

    private InnertubeClientRequestInfo(final ClientInfo client, final DeviceInfo device) {
        clientInfo = client;
        deviceInfo = device;
    }

    @Nonnull
    public static InnertubeClientRequestInfo ofWebClient() {
        return desktop(WEB_CLIENT_NAME, WEB_HARDCODED_CLIENT_VERSION, WEB_CLIENT_ID,
                WATCH_CLIENT_SCREEN);
    }

    @Nonnull
    public static InnertubeClientRequestInfo ofWebEmbeddedPlayerClient() {
        return desktop(WEB_EMBEDDED_CLIENT_NAME, WEB_EMBEDDED_CLIENT_VERSION,
                WEB_EMBEDDED_CLIENT_ID, EMBED_CLIENT_SCREEN);
    }

    @Nonnull
    public static InnertubeClientRequestInfo ofWebMusicAnalyticsChartsClient() {
        return new InnertubeClientRequestInfo(
                new ClientInfo(WEB_MUSIC_ANALYTICS_CLIENT_NAME,
                        WEB_MUSIC_ANALYTICS_CLIENT_VERSION, WEB_MUSIC_ANALYTICS_CLIENT_ID,
                        null, null),
                new DeviceInfo(null, null, null, null, null, -1));
    }

    @Nonnull
    public static InnertubeClientRequestInfo ofAndroidClient() {
        return new InnertubeClientRequestInfo(
                new ClientInfo(ANDROID_CLIENT_NAME, ANDROID_CLIENT_VERSION,
                        ANDROID_CLIENT_ID, WATCH_CLIENT_SCREEN, null),
                new DeviceInfo(MOBILE_CLIENT_PLATFORM, null, null, "Android", "16", 36));
    }

    @Nonnull
    public static InnertubeClientRequestInfo ofIosClient() {
        return new InnertubeClientRequestInfo(
                new ClientInfo(IOS_CLIENT_NAME, IOS_CLIENT_VERSION, IOS_CLIENT_ID,
                        WATCH_CLIENT_SCREEN, null),
                new DeviceInfo(MOBILE_CLIENT_PLATFORM, "Apple", IOS_DEVICE_MODEL, "iOS",
                        IOS_OS_VERSION, -1));
    }

    @Nonnull
    public static InnertubeClientRequestInfo ofVisionOsClient() {
        return new InnertubeClientRequestInfo(
                new ClientInfo(VISIONOS_CLIENT_NAME, VISIONOS_CLIENT_VERSION,
                        VISIONOS_CLIENT_ID, WATCH_CLIENT_SCREEN, null),
                new DeviceInfo(MOBILE_CLIENT_PLATFORM, "Apple", VISIONOS_DEVICE_MODEL,
                        "visionOS", VISIONOS_VERSION, -1));
    }

    private static InnertubeClientRequestInfo desktop(final String name, final String version,
                                                      final String id, final String screen) {
        return new InnertubeClientRequestInfo(new ClientInfo(name, version, id, screen, null),
                new DeviceInfo(DESKTOP_CLIENT_PLATFORM, null, null, null, null, -1));
    }
}
