// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback.services.youtube;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class InnertubeClientRequestInfoTest {
    @Test
    void setsClientAndDeviceFieldsForEachSupportedClient() {
        final InnertubeClientRequestInfo web = InnertubeClientRequestInfo.ofWebClient();
        assertEquals("WEB", web.clientInfo.clientName);
        assertEquals("WATCH", web.clientInfo.clientScreen);
        assertEquals("DESKTOP", web.deviceInfo.platform);
        assertEquals(-1, web.deviceInfo.androidSdkVersion);

        final InnertubeClientRequestInfo embedded =
                InnertubeClientRequestInfo.ofWebEmbeddedPlayerClient();
        assertEquals("EMBED", embedded.clientInfo.clientScreen);

        final InnertubeClientRequestInfo charts =
                InnertubeClientRequestInfo.ofWebMusicAnalyticsChartsClient();
        assertNull(charts.clientInfo.clientScreen);
        assertNull(charts.deviceInfo.platform);

        final InnertubeClientRequestInfo android = InnertubeClientRequestInfo.ofAndroidClient();
        assertEquals("MOBILE", android.deviceInfo.platform);
        assertEquals(36, android.deviceInfo.androidSdkVersion);

        final InnertubeClientRequestInfo ios = InnertubeClientRequestInfo.ofIosClient();
        assertEquals("iPhone16,2", ios.deviceInfo.deviceModel);
        assertEquals("18.7.2.22H124", ios.deviceInfo.osVersion);

        final InnertubeClientRequestInfo vision = InnertubeClientRequestInfo.ofVisionOsClient();
        assertEquals("visionOS", vision.deviceInfo.osName);
        assertEquals("RealityDevice14,1", vision.deviceInfo.deviceModel);
    }
}
