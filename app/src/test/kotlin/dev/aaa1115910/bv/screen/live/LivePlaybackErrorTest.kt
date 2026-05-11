package dev.aaa1115910.bv.screen.live

import kotlin.test.Test
import kotlin.test.assertEquals

class LivePlaybackErrorTest {
    @Test
    fun `not live room shows in-player unavailable message`() {
        assertEquals(
            "主播暂未开播",
            resolveLivePlaybackErrorMessage(liveStatus = 0, source = null)
        )
    }

    @Test
    fun `empty playable source shows retryable message`() {
        assertEquals(
            "无法获取直播流地址，请稍后重试",
            resolveLivePlaybackErrorMessage(liveStatus = 1, source = null)
        )
    }
}
