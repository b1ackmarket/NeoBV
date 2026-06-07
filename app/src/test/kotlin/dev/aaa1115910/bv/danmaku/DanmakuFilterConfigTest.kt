package dev.aaa1115910.bv.danmaku

import dev.aaa1115910.biliapi.http.entity.danmaku.DanmakuData
import dev.aaa1115910.biliapi.http.entity.danmaku.DanmakuFilterRuleData
import dev.aaa1115910.bv.component.controllers.DanmakuType
import dev.aaa1115910.bv.viewmodel.player.allowsDanmakuTypes
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DanmakuFilterConfigTest {
    @Test
    fun `keyword regex and user hash rules block matching danmaku`() {
        val config = DanmakuFilterConfig(
            enabled = true,
            syncCloudRules = true,
            localKeywords = "剧透",
            localRegexes = "第\\d+集",
            localUserHashes = "ffee0011"
        )
        val cloudRules = listOf(
            DanmakuFilterRuleData(type = 2, filter = "a1b2c3d4")
        )
        val matcher = DanmakuFilterMatcher(
            config = config,
            rules = buildDanmakuFilterRules(config, cloudRules)
        )

        assertTrue(matcher.blocks(danmaku(text = "这有剧透")))
        assertTrue(matcher.blocks(danmaku(text = "第12集来了")))
        assertTrue(matcher.blocks(danmaku(midHash = "a1b2c3d4")))
        assertTrue(matcher.blocks(danmaku(midHash = "ffee0011")))
        assertFalse(matcher.blocks(danmaku(text = "正常弹幕", midHash = "eeeeffff")))
    }

    @Test
    fun `disabled config does not block rules`() {
        val config = DanmakuFilterConfig(
            enabled = false,
            localKeywords = "剧透"
        )
        val matcher = DanmakuFilterMatcher(
            config = config,
            rules = buildDanmakuFilterRules(config, emptyList())
        )

        assertFalse(matcher.blocks(danmaku(text = "剧透", type = 5)))
    }

    @Test
    fun `stable danmaku types filter by display position`() {
        assertTrue(
            danmaku(type = 5).allowsDanmakuTypes(listOf(DanmakuType.All))
        )
        assertTrue(
            danmaku(type = 5).allowsDanmakuTypes(listOf(DanmakuType.Top))
        )
        assertFalse(
            danmaku(type = 5).allowsDanmakuTypes(listOf(DanmakuType.Rolling, DanmakuType.Bottom))
        )
    }

    private fun danmaku(
        text: String = "hello",
        type: Int = 1,
        color: Int = 0xffffff,
        midHash: String = "12345678"
    ): DanmakuData {
        return DanmakuData(
            time = 1f,
            type = type,
            size = 25,
            color = color,
            timestamp = 0,
            pool = 0,
            midHash = midHash,
            dmid = 1L,
            level = 0,
            text = text
        )
    }
}
