// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback;

import com.vyonex.internal.fallback.downloader.Downloader;
import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.localization.ContentCountry;
import com.vyonex.internal.fallback.localization.Localization;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public final class VyonEx {
    @Nullable private static Downloader downloader;
    @Nullable private static Localization preferredLocalization;
    @Nullable private static ContentCountry preferredContentCountry;

    private VyonEx() { }

    public static void init(@Nullable final Downloader value) {
        init(value, Localization.DEFAULT);
    }

    public static void init(@Nullable final Downloader value,
                            @Nonnull final Localization localization) {
        final ContentCountry country = localization.getCountryCode().isEmpty()
                ? ContentCountry.DEFAULT : new ContentCountry(localization.getCountryCode());
        init(value, localization, country);
    }

    public static void init(@Nullable final Downloader value,
                            @Nonnull final Localization localization,
                            @Nonnull final ContentCountry country) {
        downloader = value;
        preferredLocalization = localization;
        preferredContentCountry = country;
    }

    @Nullable public static Downloader getDownloader() { return downloader; }

    @Nonnull public static List<StreamingService> getServices() { return ServiceList.all(); }

    @Nonnull public static StreamingService getService(final int serviceId)
            throws ExtractionException {
        for (final StreamingService service : ServiceList.all()) {
            if (service.getServiceId() == serviceId) return service;
        }
        throw new ExtractionException("There's no service with the id = \"" + serviceId + "\"");
    }

    @Nonnull public static StreamingService getService(final String serviceName)
            throws ExtractionException {
        for (final StreamingService service : ServiceList.all()) {
            if (service.getServiceInfo().getName().equals(serviceName)) return service;
        }
        throw new ExtractionException("There's no service with the name = \""
                + serviceName + "\"");
    }

    @Nonnull public static StreamingService getServiceByUrl(final String url)
            throws ExtractionException {
        for (final StreamingService service : ServiceList.all()) {
            if (service.getLinkTypeByUrl(url) != StreamingService.LinkType.NONE) return service;
        }
        throw new ExtractionException("No service can handle the url = \"" + url + "\"");
    }

    public static void setupLocalization(@Nonnull final Localization localization) {
        setupLocalization(localization, null);
    }

    public static void setupLocalization(@Nonnull final Localization localization,
                                         @Nullable final ContentCountry country) {
        preferredLocalization = localization;
        preferredContentCountry = country != null ? country
                : localization.getCountryCode().isEmpty() ? ContentCountry.DEFAULT
                : new ContentCountry(localization.getCountryCode());
    }

    @Nonnull public static Localization getPreferredLocalization() {
        return preferredLocalization == null ? Localization.DEFAULT : preferredLocalization;
    }

    public static void setPreferredLocalization(@Nonnull final Localization localization) {
        preferredLocalization = localization;
    }

    @Nonnull public static ContentCountry getPreferredContentCountry() {
        return preferredContentCountry == null ? ContentCountry.DEFAULT : preferredContentCountry;
    }

    public static void setPreferredContentCountry(@Nonnull final ContentCountry country) {
        preferredContentCountry = country;
    }
}
