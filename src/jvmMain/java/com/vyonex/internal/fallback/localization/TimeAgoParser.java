// bxane
package com.vyonex.internal.fallback.localization;

import com.vyonex.internal.fallback.exceptions.ParsingException;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.DisplayContext;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.RelativeDateTimeFormatter;
import com.ibm.icu.text.RelativeDateTimeFormatter.RelativeDateTimeUnit;
import com.ibm.icu.text.RelativeDateTimeFormatter.Style;
import com.ibm.icu.util.ULocale;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A helper class that is meant to be used by services that need to parse durations such as
 * {@code 23 seconds} and/or upload dates in the format {@code 2 days ago} or similar.
 */
public class TimeAgoParser {
    private static final int[] PLURAL_SAMPLES = {0, 1, 2, 3, 4, 5, 11, 21, 22, 25, 100, 101, 102};
    private static final Set<String> COMPACT_WORD_LANGUAGES = Set.of("ja", "km", "ko", "lo", "my", "th", "zh");
    private static final List<Unit> UNITS = List.of(
            new Unit(ChronoUnit.SECONDS, RelativeDateTimeUnit.SECOND),
            new Unit(ChronoUnit.MINUTES, RelativeDateTimeUnit.MINUTE),
            new Unit(ChronoUnit.HOURS, RelativeDateTimeUnit.HOUR),
            new Unit(ChronoUnit.DAYS, RelativeDateTimeUnit.DAY),
            new Unit(ChronoUnit.WEEKS, RelativeDateTimeUnit.WEEK),
            new Unit(ChronoUnit.MONTHS, RelativeDateTimeUnit.MONTH),
            new Unit(ChronoUnit.YEARS, RelativeDateTimeUnit.YEAR));
    private static final Map<ULocale, Lexicon> LEXICONS = new ConcurrentHashMap<>();

    private final ULocale locale;
    private final LocalDateTime now;
    private final Lexicon lexicon;

    /**
     * Creates a helper to parse upload dates in the format '2 days ago'.
     * <p>
     * Instantiate a new {@link TimeAgoParser} every time you extract a new batch of items.
     * </p>
     *
     * @param locale          The locale of the textual date
     * @param now            The current time
     */
    public TimeAgoParser(final ULocale locale, final LocalDateTime now) {
        this.locale = locale;
        this.now = now;
        this.lexicon = LEXICONS.computeIfAbsent(locale, TimeAgoParser::createLexicon);
    }

    private static Lexicon createLexicon(final ULocale locale) {
        final Map<ChronoUnit, Set<String>> unitWords = new EnumMap<>(ChronoUnit.class);
        final Map<ChronoUnit, Set<String>> unitFragments = new EnumMap<>(ChronoUnit.class);
        final Map<List<String>, TimeAgoAmount> relativePhrases = new LinkedHashMap<>();
        for (final Unit unit : UNITS) {
            unitWords.put(unit.chronoUnit(), new HashSet<>());
            unitFragments.put(unit.chronoUnit(), new HashSet<>());
        }
        for (final Style style : List.of(Style.LONG, Style.SHORT, Style.NARROW)) {
            final RelativeDateTimeFormatter formatter = RelativeDateTimeFormatter.getInstance(
                    locale, NumberFormat.getInstance(locale), style,
                    DisplayContext.CAPITALIZATION_NONE);
            for (final Unit unit : UNITS) {
                for (final int amount : PLURAL_SAMPLES) {
                    final String past = formatter.formatNumeric(-amount, unit.icuUnit());
                    final String future = formatter.formatNumeric(amount, unit.icuUnit());
                    final Set<String> futureWords = new HashSet<>(words(future, locale, true));
                    boolean hasCommonWord = false;
                    for (final String word : words(past, locale, true)) {
                        if (futureWords.contains(word)) {
                            unitWords.get(unit.chronoUnit()).add(word);
                            hasCommonWord = true;
                        }
                    }
                    if (!hasCommonWord
                            && COMPACT_WORD_LANGUAGES.contains(locale.getLanguage())) {
                        final String fragment = longestCommonSubstring(
                                comparableText(past), comparableText(future));
                        if (!fragment.isEmpty()) {
                            unitFragments.get(unit.chronoUnit()).add(fragment);
                        }
                    }

                    final String relative = formatter.format(-amount, unit.icuUnit());
                    if (relative != null && !containsDigit(relative)) {
                        relativePhrases.putIfAbsent(words(relative, locale, false),
                                new TimeAgoAmount(unit.chronoUnit(), amount));
                    }
                }
            }
        }
        unitWords.replaceAll((unit, words) -> Set.copyOf(words));
        unitFragments.replaceAll((unit, fragments) -> Set.copyOf(fragments));
        return new Lexicon(Map.copyOf(unitWords), Map.copyOf(unitFragments),
                Collections.unmodifiableMap(relativePhrases));
    }

    /**
     * Parses a textual date in the format '2 days ago' into a Calendar representation which is then
     * wrapped in a {@link DateWrapper} object.
     * <p>
     * Beginning with days ago, the date is considered as an approximation.
     *
     * @param textualDate The original date as provided by the streaming service
     * @return The parsed time (can be approximated)
     * @throws ParsingException if the time unit could not be recognized
     */
    public DateWrapper parse(final String textualDate) throws ParsingException {
        final List<String> inputWords = words(textualDate, locale, true);
        final String inputText = comparableText(textualDate);
        for (final Map.Entry<List<String>, TimeAgoAmount> entry : lexicon.relativePhrases().entrySet()) {
            if (containsPhrase(inputWords, entry.getKey())) {
                return getResultFor(entry.getValue().amount(), entry.getValue().unit());
            }
        }

        final int amount = parseTimeAgoAmount(textualDate);
        for (final Unit unit : UNITS) {
            if (!Collections.disjoint(inputWords, lexicon.unitWords().get(unit.chronoUnit()))
                    || lexicon.unitFragments().get(unit.chronoUnit()).stream().anyMatch(inputText::contains)) {
                return getResultFor(amount, unit.chronoUnit());
            }
        }
        throw new ParsingException("Unable to parse the date: " + textualDate);
    }

    private int parseTimeAgoAmount(final String textualDate) {
        try {
            return Integer.parseInt(textualDate.replaceAll("[^\\p{Nd}]+", ""));
        } catch (final NumberFormatException ignored) {
            // If there is no valid number in the textual date,
            // assume it is 1 (as in 'a second ago').
            return 1;
        }
    }

    private static boolean containsDigit(final String value) {
        return value.codePoints().anyMatch(Character::isDigit);
    }

    private static String comparableText(final String value) {
        return value.replaceAll("\\p{Nd}+", " ")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\p{P}\\p{S}]+", " ")
                .replaceAll("(?U)\\s+", " ")
                .trim();
    }

    private static String longestCommonSubstring(final String left, final String right) {
        final int[][] lengths = new int[left.length() + 1][right.length() + 1];
        int longest = 0;
        int end = 0;
        for (int i = 1; i <= left.length(); i++) {
            for (int j = 1; j <= right.length(); j++) {
                if (left.charAt(i - 1) == right.charAt(j - 1)) {
                    lengths[i][j] = lengths[i - 1][j - 1] + 1;
                    if (lengths[i][j] > longest) {
                        longest = lengths[i][j];
                        end = i;
                    }
                }
            }
        }
        return left.substring(end - longest, end);
    }

    private static List<String> words(final String value, final ULocale locale, final boolean removeNumbers) {
        final String text = (removeNumbers ? value.replaceAll("\\p{Nd}+", " ") : value)
                .toLowerCase(Locale.ROOT);
        final BreakIterator iterator = BreakIterator.getWordInstance(locale);
        iterator.setText(text);
        final List<String> result = new ArrayList<>();
        for (int start = iterator.first(), end = iterator.next(); end != BreakIterator.DONE;
             start = end, end = iterator.next()) {
            final String word = text.substring(start, end).trim();
            if (!word.isEmpty() && word.codePoints().anyMatch(Character::isLetter)) {
                result.add(word);
            }
        }
        return List.copyOf(result);
    }

    private static boolean containsPhrase(final List<String> words, final List<String> phrase) {
        if (phrase.isEmpty() || phrase.size() > words.size()) {
            return false;
        }
        for (int start = 0; start <= words.size() - phrase.size(); start++) {
            if (words.subList(start, start + phrase.size()).equals(phrase)) {
                return true;
            }
        }
        return false;
    }

    private DateWrapper getResultFor(final int timeAgoAmount, final ChronoUnit chronoUnit) {
        final var localDateTime = chronoUnit == ChronoUnit.YEARS
                // minusDays is needed to prevent `PrettyTime` from showing '12 months ago'.
                ? now.minusYears(timeAgoAmount).minusDays(1)
                : now.minus(timeAgoAmount, chronoUnit);
        final boolean isApproximate = chronoUnit.isDateBased();
        final var resolvedDateTime =
                isApproximate ? localDateTime.truncatedTo(ChronoUnit.DAYS) : localDateTime;
        return new DateWrapper(resolvedDateTime, isApproximate);
    }

    private record Unit(ChronoUnit chronoUnit, RelativeDateTimeUnit icuUnit) { }

    private record TimeAgoAmount(ChronoUnit unit, int amount) { }

    private record Lexicon(Map<ChronoUnit, Set<String>> unitWords,
                           Map<ChronoUnit, Set<String>> unitFragments,
                           Map<List<String>, TimeAgoAmount> relativePhrases) { }
}
