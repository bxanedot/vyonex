// bxane
package com.vyonex.internal.primary.utils

import java.security.MessageDigest

public fun sha1(input: String): String {
    val md = MessageDigest.getInstance("SHA-1")
    val bytes = md.digest(input.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}
