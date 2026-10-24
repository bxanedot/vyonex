// bxane
package com.vyonex.internal.fallback.services.youtube.extractors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.protobuf.CodedInputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class YoutubePlaylistContinuationContractTest {
    @Test
    void serializesContinuationFields() throws IOException {
        final CodedInputStream envelope = CodedInputStream.newInstance(
                YoutubePlaylistExtractor.encodePlaylistContinuation("PL123"));

        assertEquals((80226972 << 3) | 2, envelope.readTag());
        final CodedInputStream parameters = CodedInputStream.newInstance(envelope.readByteArray());
        assertTrue(envelope.isAtEnd());

        assertEquals((2 << 3) | 2, parameters.readTag());
        assertEquals("VLPL123", parameters.readStringRequireUtf8());
        assertEquals((3 << 3) | 2, parameters.readTag());
        assertEquals("CADCBgIIAA%3D%3D", parameters.readStringRequireUtf8());
        assertEquals((35 << 3) | 2, parameters.readTag());
        assertEquals("PL123", parameters.readStringRequireUtf8());
        assertEquals(0, parameters.readTag());
    }
}
