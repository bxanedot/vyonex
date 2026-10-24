// bxane
package com.vyonex.internal.primary.utils

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.SHA1

/** SHA-1 compatibility hash. Do not use for passwords, signatures, or security decisions. */
@OptIn(DelicateCryptographyApi::class)
public fun sha1(input: String): String =
    CryptographyProvider.Default.get(SHA1).hasher().hashBlocking(input.encodeToByteArray()).toHexString()
