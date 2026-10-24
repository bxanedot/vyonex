package com.vyonex
// bxane

interface VyonExConfigStore {
    val enabled: Boolean

    fun getString(key: String): String?
    fun getLong(key: String): Long?
    fun putString(key: String, value: String)
    fun putLong(key: String, value: Long)
}

data class VyonExSession(
    val cookie: String?,
    val visitorData: String?,
    val region: String,
    val language: String,
)

enum class VyonExLogLevel { DEBUG, INFO, WARN, ERROR }
