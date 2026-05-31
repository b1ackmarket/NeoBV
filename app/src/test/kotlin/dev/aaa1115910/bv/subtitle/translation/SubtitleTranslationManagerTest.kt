package dev.aaa1115910.bv.subtitle.translation

import dev.aaa1115910.bilisubtitle.entity.SubtitleItem
import dev.aaa1115910.bilisubtitle.entity.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class SubtitleTranslationManagerTest {
    @Test
    fun `priority batches start from current playback window then append the rest`() {
        val subtitles = (0 until 6).map { index ->
            subtitle(
                fromSeconds = index * 10,
                toSeconds = index * 10 + 5,
                content = "line $index"
            )
        }

        val batches = subtitles.priorityBatches(
            currentTimeMs = 21_000,
            preTranslateMs = 20_000,
            batchSize = 2
        )

        assertEquals(
            listOf(listOf(2, 3), listOf(4), listOf(0, 1), listOf(5)),
            batches
        )
    }

    @Test
    fun `context includes configured nearby subtitle lines`() {
        val subtitles = (0 until 5).map { index ->
            subtitle(
                fromSeconds = index,
                toSeconds = index + 1,
                content = "line $index"
            )
        }

        val context = buildContext(
            sourceSubtitles = subtitles,
            startIndex = 2,
            contextBefore = 1,
            contextAfter = 2
        )

        assertEquals("line 1\nline 2\nline 3\nline 4", context)
    }

    @Test
    fun `cache prefix changes with translation config signature`() {
        val base = SubtitleTranslationConfig(
            targetLanguage = "en",
            openAiBaseUrl = "https://example.com/v1",
            openAiApiKey = "key",
            openAiModel = "model"
        )

        val english = buildCachePrefix(1L, 2L, 3L, base)
        val japanese = buildCachePrefix(1L, 2L, 3L, base.copy(targetLanguage = "ja"))

        assertNotEquals(english, japanese)
    }

    @Test
    fun `manager ignores stale translation result after clear`() = runBlocking {
        val source = listOf(
            subtitle(0, 2, "一"),
            subtitle(3, 5, "二")
        )
        val config = SubtitleTranslationConfig(
            openAiBaseUrl = "https://example.com/v1",
            openAiApiKey = "key",
            openAiModel = "model"
        ).let { it.copy(verifiedSignature = it.configSignature()) }
        val provider = object : SubtitleTranslationProvider {
            override suspend fun translate(request: SubtitleTranslationRequest): SubtitleTranslationResult {
                return SubtitleTranslationResult(
                    mapOf(
                        0 to "one",
                        1 to "two"
                    )
                )
            }
        }
        val manager = SubtitleTranslationManager(
            scope = this,
            updateDispatcher = Dispatchers.Unconfined,
            providerFactory = { provider }
        )
        val updates = mutableListOf<List<SubtitleItem>>()

        manager.preload(
            sourceSubtitles = source,
            currentTimeMs = 0L,
            config = config,
            title = "旧视频",
            aid = 1L,
            cid = 2L,
            subtitleId = 3L,
            onUpdate = updates::add,
            onError = { throw it }
        )
        manager.clear()

        assertEquals(emptyList(), updates)
        assertEquals(emptyList(), manager.buildTranslatedSubtitles(source))
    }

    private fun subtitle(
        fromSeconds: Int,
        toSeconds: Int,
        content: String
    ) = SubtitleItem(
        from = Timestamp.fromBccString(fromSeconds.toFloat()),
        to = Timestamp.fromBccString(toSeconds.toFloat()),
        content = content
    )
}
