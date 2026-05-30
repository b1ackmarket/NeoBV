package dev.aaa1115910.bv.telemetry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class TelemetrySanitizerTest {
    @Test
    fun `sanitize extras drops identity content and auth keys`() {
        val sanitized = TelemetrySanitizer.sanitizeExtras(
            mapOf(
                "uid" to 1000L,
                "bvid" to "BV1xx",
                "room_id" to 1000,
                "search_keyword" to "private",
                "cookie" to "SESSDATA=value",
                "error_type" to "timeout"
            )
        )

        assertEquals(mapOf("error_type" to "timeout"), sanitized)
    }

    @Test
    fun `api endpoint keeps only safe path without query`() {
        val sanitized = TelemetrySanitizer.sanitizeExtras(
            mapOf("api_endpoint" to "/x/player/wbi/playurl?avid=1&cid=2")
        )

        assertEquals("/x/player/wbi/playurl", sanitized["api_endpoint"])
    }

    @Test
    fun `api endpoint rejects non path values`() {
        assertNull(TelemetrySanitizer.sanitizeValue("api_endpoint", "https://api.bilibili.com/x"))
        assertNull(TelemetrySanitizer.sanitizeValue("api_endpoint", "/x//unsafe"))
    }

    @Test
    fun `string values are capped`() {
        val value = "a".repeat(140)

        val sanitized = TelemetrySanitizer.sanitizeValue("error_type", value) as String

        assertEquals(100, sanitized.length)
        assertFalse(sanitized.contains("b"))
    }

    @Test
    fun `event params allow coarse screen name but drop unsafe keys`() {
        val sanitized = TelemetrySanitizer.sanitizeEventParams(
            mapOf(
                "screen_name" to "video_player",
                "title" to "private title",
                "source" to "foreground"
            )
        )

        assertEquals(
            mapOf("screen_name" to "video_player", "source" to "foreground"),
            sanitized
        )
    }
}
