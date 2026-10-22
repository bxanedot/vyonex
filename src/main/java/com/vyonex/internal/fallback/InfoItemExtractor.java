// bxane
package com.vyonex.internal.fallback;

import com.vyonex.internal.fallback.exceptions.ParsingException;

import javax.annotation.Nonnull;
import java.util.List;

public interface InfoItemExtractor {
    String getName() throws ParsingException;
    String getUrl() throws ParsingException;
    @Nonnull
    List<Image> getThumbnails() throws ParsingException;
}
