package dev.aaa1115910.bv.util

import kotlin.test.Test
import kotlin.test.assertEquals

class PlayerUiTextFormatterTest {
    @Test
    fun `online count uses raw people watching copy`() {
        assertEquals("256 人一起看", PlayerUiTextFormatter.onlineCount(256))
        assertEquals("1.2万 人一起看", PlayerUiTextFormatter.onlineCount("1.2万"))
    }

    @Test
    fun `play count uses wan suffix for large values`() {
        assertEquals("12.3万播放", PlayerUiTextFormatter.playCount(123_456))
    }
}
