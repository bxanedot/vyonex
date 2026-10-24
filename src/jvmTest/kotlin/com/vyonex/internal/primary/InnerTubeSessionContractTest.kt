// bxane
package com.vyonex.internal.primary

import com.vyonex.internal.primary.models.YouTubeLocale
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InnerTubeSessionContractTest {
    @Test
    fun sessionSnapshotRedactsCredentials() {
        val snapshot =
            InnerTube.SessionSnapshot(
                visitorData = "visitor-secret",
                dataSyncId = "sync-secret",
                authUser = "2",
                cookie = "SID=cookie-secret",
                sapisid = "sapisid-secret",
            ).toString()

        listOf("visitor-secret", "sync-secret", "cookie-secret", "sapisid-secret").forEach {
            assertFalse(snapshot.contains(it))
        }
        assertTrue(snapshot.contains("cookie=present"))
    }

    @Test
    fun isolatedSessionKeepsLocaleAndVisitorDataButCanDropAccountState() {
        val client = HttpClient(MockEngine { error("No request expected") })
        val source = InnerTube(client)
        try {
            source.locale = YouTubeLocale(gl = "CH", hl = "de-CH")
            source.regionOverrideActive = true
            source.replaceSession(
                cookie = "SID=session; SAPISID=token",
                visitorData = "visitor",
                dataSyncId = "sync",
                authUser = "2",
            )

            val publicSession = source.createIsolatedSession(includeAccount = false)
            val accountSession = source.createIsolatedSession(includeAccount = true)
            try {
                assertEquals(source.locale, publicSession.locale)
                assertTrue(publicSession.regionOverrideActive)
                assertEquals("visitor", publicSession.visitorData)
                assertNull(publicSession.cookie)
                assertNull(publicSession.dataSyncId)
                assertNull(publicSession.sessionSnapshot().sapisid)
                assertEquals("0", publicSession.authUser)

                assertEquals("SID=session; SAPISID=token", accountSession.cookie)
                assertEquals("sync", accountSession.dataSyncId)
                assertEquals("token", accountSession.sessionSnapshot().sapisid)
                assertEquals("2", accountSession.authUser)
            } finally {
                publicSession.close()
                accountSession.close()
            }
        } finally {
            source.close()
            client.close()
        }
    }

    @Test
    fun staleSnapshotCannotClearNewCredentials() {
        val client = HttpClient(MockEngine { error("No request expected") })
        val innerTube = InnerTube(client)
        try {
            val oldSession = innerTube.sessionSnapshot()
            innerTube.cookie = "SID=current; SAPISID=current-token"

            assertFalse(innerTube.clearSessionIfMatches(oldSession))
            assertEquals("SID=current; SAPISID=current-token", innerTube.cookie)
            assertEquals("current-token", innerTube.sessionSnapshot().sapisid)
        } finally {
            innerTube.close()
            client.close()
        }
    }
}
