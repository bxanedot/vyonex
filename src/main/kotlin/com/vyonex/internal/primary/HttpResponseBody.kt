// bxane
package com.vyonex.internal.primary

import java.io.ByteArrayOutputStream
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.utils.io.cancel
import io.ktor.utils.io.readAvailable

/** Reads a response body while enforcing a byte limit before and during streaming. */
public suspend fun HttpResponse.bodyAsTextLimited(maxBytes: Int): String {
    require(maxBytes > 0) { "Maximum response size must be positive" }
    val channel = bodyAsChannel()
    val declaredLength = contentLength()
    if (declaredLength != null && declaredLength > maxBytes) {
        channel.cancel(null)
        throw IllegalStateException("Response exceeded the $maxBytes byte limit")
    }

    val readBuffer = ByteArray(minOf(DEFAULT_READ_BUFFER_SIZE, maxBytes))
    val output = ByteArrayOutputStream(minOf(DEFAULT_READ_BUFFER_SIZE, maxBytes))
    try {
        while (true) {
            val count = channel.readAvailable(readBuffer, 0, readBuffer.size)
            if (count == -1) break
            if (count == 0) continue
            if (output.size() > maxBytes - count) {
                throw IllegalStateException("Response exceeded the $maxBytes byte limit")
            }
            output.write(readBuffer, 0, count)
        }
    } catch (error: Throwable) {
        channel.cancel(error)
        throw error
    }
    return output.toString(Charsets.UTF_8)
}

private const val DEFAULT_READ_BUFFER_SIZE = 8 * 1024
