// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback;

import com.vyonex.internal.fallback.exceptions.ExtractionException;
import com.vyonex.internal.fallback.linkhandler.LinkHandler;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public abstract class Info implements Serializable {
    private final int serviceId;
    private final String id;
    private final String url;
    private final String name;
    private String originalUrl;
    private final List<Throwable> errors = new ArrayList<>();

    public Info(final int serviceId, final String id, final String url,
                final String originalUrl, final String name) {
        this.serviceId = serviceId;
        this.id = id;
        this.url = url;
        this.originalUrl = originalUrl;
        this.name = name;
    }

    public Info(final int serviceId, final LinkHandler linkHandler, final String name) {
        this(serviceId, linkHandler.getId(), linkHandler.getUrl(),
                linkHandler.getOriginalUrl(), name);
    }

    public void addError(final Throwable error) {
        errors.add(error);
    }

    public void addAllErrors(final Collection<Throwable> additionalErrors) {
        errors.addAll(additionalErrors);
    }

    public void setOriginalUrl(final String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public int getServiceId() {
        return serviceId;
    }

    public StreamingService getService() {
        try {
            return VyonEx.getService(serviceId);
        } catch (final ExtractionException e) {
            throw new RuntimeException("Info object has invalid service id", e);
        }
    }

    public String getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public String getName() {
        return name;
    }

    public List<Throwable> getErrors() {
        return errors;
    }

    @Override
    public String toString() {
        final String sourceUrl = Objects.equals(url, originalUrl)
                ? "" : " (originalUrl=\"" + originalUrl + "\")";
        return getClass().getSimpleName() + "[url=\"" + url + "\"" + sourceUrl
                + ", name=\"" + name + "\"]";
    }
}
