package com.vyonex
// bxane

/** Audio formats understood by the host application's download policy. */
enum class VyonExAudioFormat { M4A, WEBM_OPUS, OTHER }

/** A progressive audio URL, normalized so callers do not depend on implementation types. */
data class VyonExAudioCandidate(
    val url: String,
    val kbps: Int,
    val mimeType: String,
    val format: VyonExAudioFormat,
)
// bxane
