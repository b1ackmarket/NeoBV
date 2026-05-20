package dev.aaa1115910.bv.screen.live

import dev.aaa1115910.bv.entity.PlayerCommentItem
import kotlin.test.Test
import kotlin.test.assertEquals

class LiveChatCommentsTest {
    @Test
    fun `live chat dedupe ignores changed polling ids for same sender and text`() {
        val seen = setOf(liveComment(id = "history-1", mid = 12L, message = "重复").liveChatDedupeKey())
        val items = listOf(
            liveComment(id = "poll-2", mid = 12L, message = "重复"),
            liveComment(id = "poll-3", mid = 12L, message = "新弹幕")
        )

        assertEquals(
            listOf("新弹幕"),
            filterNewLiveChatItems(items, seen).map { it.message }
        )
    }

    @Test
    fun `live chat dedupe removes duplicates inside one poll batch`() {
        val items = listOf(
            liveComment(id = "poll-1", mid = 12L, message = "重复"),
            liveComment(id = "poll-2", mid = 12L, message = "重复"),
            liveComment(id = "poll-3", mid = 13L, message = "重复")
        )

        assertEquals(
            listOf("12|重复", "13|重复"),
            filterNewLiveChatItems(items, emptySet()).map { it.liveChatDedupeKey() }
        )
    }

    private fun liveComment(id: String, mid: Long, message: String) = PlayerCommentItem(
        id = id,
        mid = mid,
        username = "user-$mid",
        message = message
    )
}
