package dev.aaa1115910.bv.screen.live

import dev.aaa1115910.biliapi.http.entity.live.DanmakuEvent
import dev.aaa1115910.bv.entity.PlayerCommentItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LiveDanmakuAppendQueueTest {
    @Test
    fun `append positions are monotonic even when playback clock is static`() {
        val queue = LiveDanmakuAppendQueue(currentPositionMs = { 1_000L })

        val first = queue.add(danmakuEvent("一"))
        val second = queue.add(danmakuEvent("二"))
        val third = queue.add(danmakuEvent("三"))

        assertEquals(1_500L, first.positionMs)
        assertTrue(second.positionMs > first.positionMs)
        assertTrue(third.positionMs > second.positionMs)
    }

    @Test
    fun `seeded items and live items share one ordered queue`() {
        var position = 10_000L
        val queue = LiveDanmakuAppendQueue(currentPositionMs = { position })

        queue.seed(listOf(danmakuEvent("历史一"), danmakuEvent("历史二")))
        position = 10_100L
        val live = queue.add(danmakuEvent("实时"))

        assertEquals(listOf("历史一", "历史二", "实时"), queue.snapshot().map { it.event.content })
        assertTrue(live.positionMs > queue.snapshot()[1].positionMs)
    }

    @Test
    fun `seeded future history does not delay live danmaku at stream start`() {
        val queue = LiveDanmakuAppendQueue(currentPositionMs = { 0L })

        queue.seed(listOf(danmakuEvent("历史一"), danmakuEvent("历史二"), danmakuEvent("历史三")))
        val live = queue.add(danmakuEvent("实时"))

        assertEquals(500L, live.positionMs)
    }

    @Test
    fun `queue trims oldest items`() {
        val queue = LiveDanmakuAppendQueue(
            currentPositionMs = { 0L },
            maxItems = 2
        )

        queue.add(danmakuEvent("一"))
        queue.add(danmakuEvent("二"))
        queue.add(danmakuEvent("三"))

        assertEquals(listOf("二", "三"), queue.snapshot().map { it.event.content })
    }

    @Test
    fun `stable danmaku key uses timestamp sender and content for polling dedupe`() {
        val first = danmakuEvent("重复").copy(mid = 12L, username = "user", rndTimeMs = 1000L)
        val second = danmakuEvent("重复").copy(mid = 12L, username = "user", rndTimeMs = 1000L)
        val third = danmakuEvent("重复").copy(mid = 12L, username = "user", rndTimeMs = 2000L)

        assertEquals(first.stableKey(), second.stableKey())
        assertTrue(first.stableKey() != third.stableKey())
    }

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

    private fun danmakuEvent(content: String) = DanmakuEvent(
        content = content,
        mid = 0L,
        username = "",
        color = 0xff00ff
    )

    private fun liveComment(id: String, mid: Long, message: String) = PlayerCommentItem(
        id = id,
        mid = mid,
        username = "user-$mid",
        message = message
    )
}
