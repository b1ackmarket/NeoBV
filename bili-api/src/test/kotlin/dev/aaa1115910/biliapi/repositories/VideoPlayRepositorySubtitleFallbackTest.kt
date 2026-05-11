package dev.aaa1115910.biliapi.repositories

import dev.aaa1115910.biliapi.entity.video.Subtitle
import dev.aaa1115910.biliapi.entity.video.SubtitleAiStatus
import dev.aaa1115910.biliapi.entity.video.SubtitleAiType
import dev.aaa1115910.biliapi.entity.video.SubtitleType
import kotlin.test.Test
import kotlin.test.assertEquals

class VideoPlayRepositorySubtitleFallbackTest {
    @Test
    fun `subtitle fallback keeps preferred track first and adds missing fallback languages`() {
        val preferred = listOf(
            Subtitle(
                id = 1L,
                lang = "zh-CN",
                langDoc = "中文",
                url = "https://example.com/zh.json",
                type = SubtitleType.CC,
                aiType = SubtitleAiType.Normal,
                aiStatus = SubtitleAiStatus.None
            )
        )
        val fallback = listOf(
            Subtitle(
                id = 2L,
                lang = "en",
                langDoc = "English",
                url = "https://example.com/en.json",
                type = SubtitleType.CC,
                aiType = SubtitleAiType.Normal,
                aiStatus = SubtitleAiStatus.None
            )
        )

        assertEquals(listOf(preferred.first(), fallback.first()), resolveSubtitleFallback(preferred, fallback))
    }

    @Test
    fun `subtitle fallback supplements missing languages from secondary source`() {
        val preferred = listOf(
            Subtitle(
                id = 1L,
                lang = "ai-zh",
                langDoc = "中文",
                url = "https://example.com/zh.json",
                type = SubtitleType.AI,
                aiType = SubtitleAiType.Normal,
                aiStatus = SubtitleAiStatus.Assist
            )
        )
        val fallback = listOf(
            Subtitle(
                id = 2L,
                lang = "ai-en",
                langDoc = "English",
                url = "https://example.com/en.json",
                type = SubtitleType.AI,
                aiType = SubtitleAiType.Translate,
                aiStatus = SubtitleAiStatus.Assist
            ),
            Subtitle(
                id = 3L,
                lang = "ai-zh",
                langDoc = "中文",
                url = "https://example.com/zh-fallback.json",
                type = SubtitleType.AI,
                aiType = SubtitleAiType.Normal,
                aiStatus = SubtitleAiStatus.Assist
            )
        )

        assertEquals(
            listOf(preferred.first(), fallback.first()),
            resolveSubtitleFallback(preferred, fallback)
        )
    }

    @Test
    fun `subtitle fallback supplements missing ai english and japanese tracks without replacing existing preferred track`() {
        val preferred = listOf(
            Subtitle(
                id = 1L,
                lang = "ai-zh",
                langDoc = "中文",
                url = "https://example.com/zh.json",
                type = SubtitleType.AI,
                aiType = SubtitleAiType.Normal,
                aiStatus = SubtitleAiStatus.Assist
            )
        )
        val fallback = listOf(
            Subtitle(
                id = 2L,
                lang = "ai-en",
                langDoc = "English",
                url = "https://example.com/en.json",
                type = SubtitleType.AI,
                aiType = SubtitleAiType.Translate,
                aiStatus = SubtitleAiStatus.Assist
            ),
            Subtitle(
                id = 3L,
                lang = "ai-ja",
                langDoc = "日本語",
                url = "https://example.com/ja.json",
                type = SubtitleType.AI,
                aiType = SubtitleAiType.Translate,
                aiStatus = SubtitleAiStatus.Assist
            )
        )

        assertEquals(
            listOf(preferred.first(), fallback[0], fallback[1]),
            resolveSubtitleFallback(preferred, fallback)
        )
    }

    @Test
    fun `subtitle fallback keeps preferred track when language already exists`() {
        val preferred = listOf(
            Subtitle(
                id = 1L,
                lang = "ai-en",
                langDoc = "English",
                url = "https://example.com/en-primary.json",
                type = SubtitleType.AI,
                aiType = SubtitleAiType.Translate,
                aiStatus = SubtitleAiStatus.Assist
            )
        )
        val fallback = listOf(
            Subtitle(
                id = 2L,
                lang = "ai-en",
                langDoc = "English",
                url = "https://example.com/en-fallback.json",
                type = SubtitleType.AI,
                aiType = SubtitleAiType.Translate,
                aiStatus = SubtitleAiStatus.Assist
            )
        )

        assertEquals(preferred, resolveSubtitleFallback(preferred, fallback))
    }

    @Test
    fun `subtitle fallback uses secondary source when preferred source is empty`() {
        val fallback = listOf(
            Subtitle(
                id = 2L,
                lang = "en",
                langDoc = "English",
                url = "https://example.com/en.json",
                type = SubtitleType.CC,
                aiType = SubtitleAiType.Normal,
                aiStatus = SubtitleAiStatus.None
            )
        )

        assertEquals(fallback, resolveSubtitleFallback(emptyList(), fallback))
    }
}
