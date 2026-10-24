// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback;

import com.vyonex.internal.fallback.services.bandcamp.BandcampService;
import com.vyonex.internal.fallback.services.media_ccc.MediaCCCService;
import com.vyonex.internal.fallback.services.peertube.PeertubeService;
import com.vyonex.internal.fallback.services.soundcloud.SoundcloudService;
import com.vyonex.internal.fallback.services.youtube.YoutubeService;

import java.util.List;

public final class ServiceList {
    public static final YoutubeService YouTube = new YoutubeService(0);
    public static final SoundcloudService SoundCloud = new SoundcloudService(1);
    public static final MediaCCCService MediaCCC = new MediaCCCService(2);
    public static final PeertubeService PeerTube = new PeertubeService(3);
    public static final BandcampService Bandcamp = new BandcampService(4);

    private static final List<StreamingService> SERVICES = List.of(
            YouTube, SoundCloud, MediaCCC, PeerTube, Bandcamp);

    private ServiceList() { }

    public static List<StreamingService> all() { return SERVICES; }
}
