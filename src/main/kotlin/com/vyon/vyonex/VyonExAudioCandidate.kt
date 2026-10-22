package com.vyon.vyonex
// bxane

/** Audio formats understood by Vyon's download-container policy. */
enum class VyonExAudioFormat { M4A, WEBM_OPUS, OTHER }

/** A progressive NewPipe audio URL, normalized so callers do not depend on NewPipe types. */
data class VyonExAudioCandidate(
    val url: String,
    val kbps: Int,
    val mimeType: String,
    val format: VyonExAudioFormat,
)
// bxane
