package dev.aaa1115910.bilisubtitle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SubtitleParserTest {
    @Test
    fun `read bcc subtitle`() {
        val fileContent = this::class.java.getResource("/example.bcc")?.readText()!!
        val result = SubtitleParser.fromBccString(fileContent)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `read srt subtitle`() {
        val fileContent = this::class.java.getResource("/example.srt")?.readText()!!
        val result = SubtitleParser.fromSrtString(fileContent)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `read bcc subtitle with string version`() {
        val result = SubtitleParser.fromBccString(
            """
                {
                  "font_size": 0.4,
                  "version": "default",
                  "body": [
                    {
                      "from": 1.0,
                      "to": 2.5,
                      "content": "字幕正文"
                    }
                  ]
                }
            """.trimIndent()
        )

        assertEquals(1, result.size)
        assertEquals("字幕正文", result.first().content)
    }

    @Test
    fun `read bcc subtitle with object version and ai fields`() {
        val result = SubtitleParser.fromBccString(
            """
                {
                  "font_size": 0.4,
                  "font_color": "#FFFFFF",
                  "background_alpha": 0.5,
                  "background_color": "#9C27B0",
                  "Stroke": "none",
                  "type": "AIsubtitle",
                  "lang": "ja",
                  "version": {
                    "dubbing": "v.ja.3.0.0.93",
                    "translate_model": "ja_20260512"
                  },
                  "body": [
                    {
                      "from": 0.32,
                      "to": 3.695,
                      "sid": 1,
                      "location": 2,
                      "content": "あたしは朝の八九時の太陽なんだから…",
                      "music": 0.046
                    }
                  ]
                }
            """.trimIndent()
        )

        assertEquals(1, result.size)
        assertEquals("あたしは朝の八九時の太陽なんだから…", result.first().content)
    }
}
