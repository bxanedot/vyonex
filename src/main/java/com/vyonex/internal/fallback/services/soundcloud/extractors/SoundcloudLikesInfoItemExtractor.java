// bxane
package com.vyonex.internal.fallback.services.soundcloud.extractors;

import com.vyonex.internal.json.JsonObject;

public class SoundcloudLikesInfoItemExtractor extends SoundcloudStreamInfoItemExtractor {

    public SoundcloudLikesInfoItemExtractor(final JsonObject itemObject) {
        super(itemObject.getObject("track"));
    }

}
