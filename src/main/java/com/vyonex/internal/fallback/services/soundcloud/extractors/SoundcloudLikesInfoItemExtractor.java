// bxane
package com.vyonex.internal.fallback.services.soundcloud.extractors;

import com.grack.nanojson.JsonObject;

public class SoundcloudLikesInfoItemExtractor extends SoundcloudStreamInfoItemExtractor {

    public SoundcloudLikesInfoItemExtractor(final JsonObject itemObject) {
        super(itemObject.getObject("track"));
    }

}
