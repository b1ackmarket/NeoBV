package dev.aaa1115910.bv.subtitle.translation

import kotlin.test.Test
import kotlin.test.assertEquals

class SubtitleTranslationProviderParseTest {
    @Test
    fun `parse openai json array translations by id`() {
        val result = parseIdTextTranslations(
            """
            [
              {"id": 2, "text": "world"},
              {"id": 1, "translation": "hello"}
            ]
            """.trimIndent()
        )

        assertEquals(mapOf(2 to "world", 1 to "hello"), result)
    }

    @Test
    fun `parse openai sequential ids by source order when model rewrites ids`() {
        val source = listOf(
            SubtitleTranslationItem(8, "你好"),
            SubtitleTranslationItem(9, "世界")
        )
        val result = parseIdTextTranslations(
            raw = """
            [
              {"id": 1, "text": "hello"},
              {"id": 2, "text": "world"}
            ]
            """.trimIndent(),
            sourceItems = source
        )

        assertEquals(mapOf(8 to "hello", 9 to "world"), result)
    }

    @Test
    fun `parse tab line translations keeps source item ids`() {
        val source = listOf(
            SubtitleTranslationItem(10, "你好"),
            SubtitleTranslationItem(11, "世界")
        )
        val result = parseTabLineTranslations(source, listOf("10\thello", "11\tworld"))

        assertEquals(mapOf(10 to "hello", 11 to "world"), result)
    }
}
