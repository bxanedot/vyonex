// bxane
package com.vyonex.internal.primary

import kotlin.test.Test
import kotlin.test.assertEquals

class InnerTubeLoggerContractTest {
    @Test
    fun levelHelpersPreserveEventFields() {
        val events = mutableListOf<InnerTubeLogEvent>()
        val logger = InnerTubeLogger(events::add)

        logger.d("tag", "debug", "media", mapOf("key" to "value"))
        logger.i("tag", "info")
        logger.w("tag", "warning")
        logger.e("tag", "error")

        assertEquals(
            listOf(
                InnerTubeLogEvent(InnerTubeLogLevel.DEBUG, "tag", "debug", "media", mapOf("key" to "value")),
                InnerTubeLogEvent(InnerTubeLogLevel.INFO, "tag", "info"),
                InnerTubeLogEvent(InnerTubeLogLevel.WARN, "tag", "warning"),
                InnerTubeLogEvent(InnerTubeLogLevel.ERROR, "tag", "error"),
            ),
            events,
        )
    }
}
