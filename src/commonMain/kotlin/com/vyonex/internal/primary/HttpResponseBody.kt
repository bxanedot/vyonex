// bxane
package com.vyonex.internal.primary

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.utils.io.cancel
import io.ktor.utils.io.readBuffer
import kotlinx.io.readByteArray

/** Reads a response body while enforcing a byte limit before and during streaming. */
public suspend fun HttpResponse.bodyAsTextLimited(maxBytes: Int): String {
    require(maxBytes > 0) { "Maximum response size must be positive" }
    val channel = bodyAsChannel()
    val declaredLength = contentLength()
    if (declaredLength != null && declaredLength > maxBytes) {
        channel.cancel(null)
        throw IllegalStateException("Response exceeded the $maxBytes byte limit")
    }

    try {
        val bytes = channel.readBuffer(maxBytes).readByteArray()
        if (bytes.size == maxBytes && channel.readBuffer(1).readByteArray().isNotEmpty()) {
            throw IllegalStateException("Response exceeded the $maxBytes byte limit")
        }
        return bytes.decodeToString()
    } catch (error: Throwable) {
        channel.cancel(error)
        throw error
    }
}
