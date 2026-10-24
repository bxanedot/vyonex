// SPDX-FileCopyrightText: 2026 bxane
// SPDX-License-Identifier: GPL-3.0-only
package com.vyonex.internal.fallback.services.peertube

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonPrimitive

class PeertubeCommentsPageParserTest {
    @Test
    fun decodesCommentPagesAndRetainsNestedReplyJson() {
        val page = PeertubeCommentsPageParser.parse(
            """{"total":9007199254740993,"data":[
                {"id":9007199254740993,"threadId":101,"text":"Hello",
                 "createdAt":"2026-01-02T03:04:05Z","totalReplies":2,
                 "totalRepliesFromVideoAuthor":1,
                 "account":{"displayName":"Alice","name":"alice","host":"tube.example",
                   "avatar":{"path":"/alice.png"}},"future":true},
                {"id":102,"isDeleted":true},null],
                "children":[{"comment":{"id":103,"text":"Reply"},
                  "children":[{"comment":{"id":104,"text":"Nested"},"children":[]}]}]}""",
        )
        val comment = page.comments!!.first()
        val reply = page.threads.single()

        assertEquals(9_007_199_254_740_993L, page.total)
        assertEquals("9007199254740993", comment.id)
        assertEquals("101", comment.threadId)
        assertEquals("Alice", (comment.account["displayName"] as JsonPrimitive).content)
        assertTrue(comment.hasAuthorReply)
        assertEquals(2, comment.totalReplies)
        assertEquals("103", reply.comment?.id)
        assertEquals(1, reply.children.size)
        assertTrue(page.hasThreadsField)
    }

    @Test
    fun distinguishesMissingCommentDataAndRejectsMalformedRoots() {
        assertEquals(null, PeertubeCommentsPageParser.parse("{}").comments)
        assertEquals(1, PeertubeCommentsPageParser.parse("""{"data":[null]}""").commentCount)
        assertFailsWith<PeertubeCommentsPageParseException> {
            PeertubeCommentsPageParser.parse("not-json")
        }
        assertFailsWith<PeertubeCommentsPageParseException> {
            PeertubeCommentsPageParser.parse("[]")
        }
    }
}
