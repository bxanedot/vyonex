// bxane
package com.vyonex.internal.primary.extraction.potoken

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WebPageAttestationChallengeTest {
    @Test
    fun skipsUnpairedAttestationPayloadsBeforeTheUsableChallenge() {
        val page =
            """
            window.ytAtN({"R":{"opaque":true}});
            window.ytAtN({"R":{"bgChallenge":{"id":"valid"}}});
            ytcfg.set({"EVENT_ID":"event-7"});
            """.trimIndent()

        val context = extractWebPageAttestationContext(page)

        assertEquals("{\"bgChallenge\":{\"id\":\"valid\"}}", context?.challenge)
        assertEquals("event-7", context?.eventId)
    }

    @Test
    fun ignoresAttestationCallsInsideCommentsAndStringValues() {
        val page =
            """
            const sample = "window.ytAtN({\"R\":{\"bgChallenge\":{\"id\":\"string\"}}});";
            /* window.ytAtN({"R":{"bgChallenge":{"id":"comment"}}}); */
            window.ytAtN({"R":{"bgChallenge":{"id":"actual"}}});
            ytcfg.set({"EVENT_ID":"event-8"});
            """.trimIndent()

        assertEquals("{\"bgChallenge\":{\"id\":\"actual\"}}", extractWebPageAttestationChallenge(page))
    }

    @Test
    fun readsEscapedChallengeFromAssignmentFallback() {
        val page =
            """window.ytAtR = "{\"bgChallenge\":{\"id\":\"fallback\"}}"; ytcfg.set({"EVENT_ID":"event-9"});"""

        assertEquals("{\"bgChallenge\":{\"id\":\"fallback\"}}", extractWebPageAttestationChallenge(page))
    }

    @Test
    fun readsChallengeFromSingleQuotedInnerTubePayload() {
        val page = "window.ytAtN({R:'{\\\"bgChallenge\\\":{\\\"id\\\":\\\"nested\\\"}}'});"

        assertEquals("{\"bgChallenge\":{\"id\":\"nested\"}}", extractWebPageAttestationChallenge(page))
    }

    @Test
    fun ignoresAttestationCallsInsideHtmlComments() {
        val page =
            """
            <!-- window.ytAtN({"R":{"bgChallenge":{"id":"comment"}}}); -->
            window.ytAtN({"R":{"bgChallenge":{"id":"actual"}}});
            """.trimIndent()

        assertEquals("{\"bgChallenge\":{\"id\":\"actual\"}}", extractWebPageAttestationChallenge(page))
    }
}
