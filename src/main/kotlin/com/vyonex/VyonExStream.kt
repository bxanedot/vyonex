package com.vyonex
// bxane

data class VyonExStream(
    val videoId: String,
    val url: String,
    val kbps: Int,
    val mimeType: String,
    val loudnessDb: Double?,
    val clientName: String,
    val profileId: String,
    val headers: Map<String, String>,
)
// bxane
