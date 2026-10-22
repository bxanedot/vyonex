// bxane
package com.vyonex.internal.fallback;

/*
 * Created by Christian Schabesberger on 23.08.15.
 *
 * Copyright (C) 2015 Christian Schabesberger <chris.schabesberger@mailbox.org>
 * VyonEx.java is part of VyonEx.
 *
 * VyonEx is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * VyonEx is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with VyonEx.  If not, see <http://www.gnu.org/licenses/>.
 */

import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.localization.ContentCountry;
import com.vyonex.internal.fallback.localization.Localization;
import com.vyonex.internal.fallback.utils.ExtractorLogger;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Provides access to streaming services supported by VyonEx.
 */
public final class VyonEx {
    private static final String TAG = VyonEx.class.getSimpleName();
    private static Downloader downloader;
    private static Localization preferredLocalization;
    private static ContentCountry preferredContentCountry;

    private VyonEx() {
    }

    public static void init(final Downloader d) {
        ExtractorLogger.d(TAG, "Default init called");
        init(d, Localization.DEFAULT);
    }

    public static void init(final Downloader d, final Localization l) {
        ExtractorLogger.d(TAG, "Default init called with localization={}");
        init(d, l, l.getCountryCode().isEmpty()
                ? ContentCountry.DEFAULT : new ContentCountry(l.getCountryCode()));
    }

    public static void init(final Downloader d, final Localization l, final ContentCountry c) {
        ExtractorLogger.d(TAG, "Initializing with downloader={}, localization={}, country={}",
                          d, l, c);
        downloader = d;
        preferredLocalization = l;
        preferredContentCountry = c;
    }

    public static Downloader getDownloader() {
        return downloader;
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Utils
    //////////////////////////////////////////////////////////////////////////*/

    public static List<StreamingService> getServices() {
        return ServiceList.all();
    }

    public static StreamingService getService(final int serviceId) throws ExtractionException {
        return ServiceList.all().stream()
                .filter(service -> service.getServiceId() == serviceId)
                .findFirst()
                .orElseThrow(() -> new ExtractionException(
                        "There's no service with the id = \"" + serviceId + "\""));
    }

    public static StreamingService getService(final String serviceName) throws ExtractionException {
        return ServiceList.all().stream()
                .filter(service -> service.getServiceInfo().getName().equals(serviceName))
                .findFirst()
                .orElseThrow(() -> new ExtractionException(
                        "There's no service with the name = \"" + serviceName + "\""));
    }

    public static StreamingService getServiceByUrl(final String url) throws ExtractionException {
        for (final StreamingService service : ServiceList.all()) {
            if (service.getLinkTypeByUrl(url) != StreamingService.LinkType.NONE) {
                return service;
            }
        }
        throw new ExtractionException("No service can handle the url = \"" + url + "\"");
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Localization
    //////////////////////////////////////////////////////////////////////////*/

    public static void setupLocalization(final Localization thePreferredLocalization) {
        setupLocalization(thePreferredLocalization, null);
    }

    public static void setupLocalization(
            final Localization thePreferredLocalization,
            @Nullable final ContentCountry thePreferredContentCountry) {
        VyonEx.preferredLocalization = thePreferredLocalization;

        if (thePreferredContentCountry != null) {
            VyonEx.preferredContentCountry = thePreferredContentCountry;
        } else {
            VyonEx.preferredContentCountry = thePreferredLocalization.getCountryCode().isEmpty()
                    ? ContentCountry.DEFAULT
                    : new ContentCountry(thePreferredLocalization.getCountryCode());
        }
    }

    @Nonnull
    public static Localization getPreferredLocalization() {
        return preferredLocalization == null ? Localization.DEFAULT : preferredLocalization;
    }

    public static void setPreferredLocalization(final Localization preferredLocalization) {
        VyonEx.preferredLocalization = preferredLocalization;
    }

    @Nonnull
    public static ContentCountry getPreferredContentCountry() {
        return preferredContentCountry == null ? ContentCountry.DEFAULT : preferredContentCountry;
    }

    public static void setPreferredContentCountry(final ContentCountry preferredContentCountry) {
        VyonEx.preferredContentCountry = preferredContentCountry;
    }
}
