// bxane
package com.vyonex.internal.primary.utils

import java.security.MessageDigest
import java.util.HexFormat

public fun sha1(input: String): String =
    HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(input.toByteArray()))
