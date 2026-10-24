// bxane
package com.vyonex

internal actual class PlatformStateLock actual constructor() {
    private val lock = Any()

    actual fun <T> withLock(block: () -> T): T = synchronized(lock, block)
}
