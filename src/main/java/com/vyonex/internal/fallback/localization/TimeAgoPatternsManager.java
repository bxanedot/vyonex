// bxane
package com.vyonex.internal.fallback.localization;

import com.vyonex.internal.fallback.timeago.PatternsHolder;
import com.vyonex.internal.fallback.timeago.PatternsManager;

import java.time.LocalDateTime;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class TimeAgoPatternsManager {
    private TimeAgoPatternsManager() {
    }

    @Nullable
    private static PatternsHolder getPatternsFor(@Nonnull final Localization localization) {
        return PatternsManager.getPatterns(localization.getLanguageCode(),
                localization.getCountryCode());
    }

    @Nullable
    public static TimeAgoParser getTimeAgoParserFor(@Nonnull final Localization localization) {
        return getTimeAgoParserFor(localization, LocalDateTime.now());
    }

    @Nullable
    public static TimeAgoParser getTimeAgoParserFor(@Nonnull final Localization localization,
                                                    @Nonnull final LocalDateTime now) {
        final PatternsHolder holder = getPatternsFor(localization);
        return holder == null ? null : new TimeAgoParser(holder, now);
    }
}
