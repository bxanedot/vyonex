// bxane
package com.vyonex

internal expect class PlatformStateLock() {
    fun <T> withLock(block: () -> T): T
}
