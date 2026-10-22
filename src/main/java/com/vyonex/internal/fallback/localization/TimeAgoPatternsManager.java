// bxane
package com.vyonex.internal.fallback.localization;

import com.ibm.icu.util.ULocale;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class TimeAgoPatternsManager {
    private static final Set<String> SUPPORTED_LANGUAGES = Arrays.stream(ULocale.getAvailableLocales())
            .map(ULocale::getLanguage)
            .map(language -> language.toLowerCase(Locale.ROOT))
            .collect(Collectors.toUnmodifiableSet());

    private TimeAgoPatternsManager() {
    }

    @Nullable
    public static TimeAgoParser getTimeAgoParserFor(@Nonnull final Localization localization) {
        return getTimeAgoParserFor(localization, LocalDateTime.now());
    }

    @Nullable
    public static TimeAgoParser getTimeAgoParserFor(@Nonnull final Localization localization,
                                                    @Nonnull final LocalDateTime now) {
        final ULocale locale = new ULocale(localization.getLocalizationCode().replace('-', '_'));
        return SUPPORTED_LANGUAGES.contains(locale.getLanguage().toLowerCase(Locale.ROOT))
                ? new TimeAgoParser(locale, now)
                : null;
    }
}
