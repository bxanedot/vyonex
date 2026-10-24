// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
// bxane
package com.vyonex.internal.fallback;

import com.vyonex.internal.fallback.exceptions.FoundAdException;
import com.vyonex.internal.fallback.exceptions.ParsingException;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InfoItemsCollectorContractTest {
    @Test
    void sortsItemsAndReturnsLiveReadOnlyViews() throws Exception {
        final NamedCollector collector = new NamedCollector();
        collector.commit(new NamedExtractor("zeta"));
        collector.commit(new NamedExtractor("alpha"));
        final List<NamedItem> view = collector.getItems();

        assertEquals(List.of("alpha", "zeta"), view.stream().map(InfoItem::getName).toList());
        assertThrows(UnsupportedOperationException.class, view::clear);
        collector.commit(new NamedExtractor("middle"));
        assertEquals(List.of("alpha", "zeta", "middle"),
                view.stream().map(InfoItem::getName).toList());
        assertEquals(List.of("alpha", "middle", "zeta"),
                collector.getItems().stream().map(InfoItem::getName).toList());

        collector.reset();
        assertEquals(0, view.size());
    }

    @Test
    void ignoresAdsAndRecordsParsingFailures() {
        final NamedCollector collector = new NamedCollector();
        collector.commit(new NamedExtractor("ad"));
        collector.commit(new NamedExtractor("broken"));

        assertEquals(0, collector.getItems().size());
        assertEquals(1, collector.getErrors().size());
        assertEquals("unreadable item", collector.getErrors().get(0).getMessage());
        assertThrows(UnsupportedOperationException.class,
                () -> collector.getErrors().clear());
    }

    private static final class NamedCollector extends InfoItemsCollector<NamedItem, NamedExtractor> {
        private NamedCollector() {
            super(5, Comparator.comparing(InfoItem::getName));
        }

        @Override
        public NamedItem extract(final NamedExtractor extractor) throws ParsingException {
            if ("ad".equals(extractor.name)) throw new FoundAdException("ad");
            if ("broken".equals(extractor.name)) throw new ParsingException("unreadable item");
            return new NamedItem(extractor.name);
        }
    }

    private static final class NamedItem extends InfoItem {
        private NamedItem(final String name) {
            super(InfoType.COMMENT, 5, "https://example.org/" + name, name);
        }
    }

    private static final class NamedExtractor implements InfoItemExtractor {
        private final String name;
        private NamedExtractor(final String name) { this.name = name; }
        @Override public String getName() { return name; }
        @Override public String getUrl() { return "https://example.org/" + name; }
        @Override public List<Image> getThumbnails() { return List.of(); }
    }
}
