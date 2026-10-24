// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback;

import com.vyonex.internal.fallback.stream.Description;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MetaInfo implements Serializable {
    private String title = "";
    private Description content = Description.EMPTY_DESCRIPTION;
    private List<URL> urls = new ArrayList<>();
    private List<String> urlTexts = new ArrayList<>();

    public MetaInfo(@Nonnull final String title, @Nonnull final Description content,
                    @Nonnull final List<URL> urls, @Nonnull final List<String> urlTexts) {
        this.title = title;
        this.content = content;
        this.urls = urls;
        this.urlTexts = urlTexts;
    }

    public MetaInfo() { }

    @Nonnull public String getTitle() { return title; }
    public void setTitle(@Nonnull final String value) { title = value; }
    @Nonnull public Description getContent() { return content; }
    public void setContent(@Nonnull final Description value) { content = value; }
    @Nonnull public List<URL> getUrls() { return urls; }
    public void setUrls(@Nonnull final List<URL> value) { urls = value; }
    public void addUrl(@Nonnull final URL value) { urls.add(value); }
    @Nonnull public List<String> getUrlTexts() { return urlTexts; }
    public void setUrlTexts(@Nonnull final List<String> value) { urlTexts = value; }
    public void addUrlText(@Nonnull final String value) { urlTexts.add(value); }
}
