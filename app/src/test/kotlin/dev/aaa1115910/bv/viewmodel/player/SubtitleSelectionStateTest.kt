package dev.aaa1115910.bv.viewmodel.player

import dev.aaa1115910.biliapi.entity.video.Subtitle
import dev.aaa1115910.biliapi.entity.video.SubtitleAiStatus
import dev.aaa1115910.biliapi.entity.video.SubtitleAiType
import dev.aaa1115910.biliapi.entity.video.SubtitleType
import dev.aaa1115910.bv.ui.state.PlayerUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SubtitleSelectionStateTest {
    @Test
    fun `subtitle total switch turns off main and secondary subtitles`() {
        val state = PlayerUiState(
            subtitleId = 1L,
            secondarySubtitleId = 2L,
            subtitleList = listOf(subtitle(1L), subtitle(2L))
        )

        val next = disableAllSubtitles(state)

        assertEquals(-1L, next.subtitleId)
        assertEquals(-1L, next.secondarySubtitleId)
        assertEquals(false, next.secondarySubtitleCustom)
        assertTrue(next.subtitleData.isEmpty())
        assertTrue(next.secondarySubtitleData.isEmpty())
    }

    @Test
    fun `video switch preserves both subtitle memories and clears loaded data`() {
        val state = PlayerUiState(
            subtitleId = 1L,
            secondarySubtitleId = 2L,
            secondarySubtitleCustom = true,
            subtitleList = listOf(
                subtitle(1L, "en", "English"),
                subtitle(2L, "ja", "日本語")
            )
        )

        val next = state.copyForVideoSwitch(
            newVideo = dev.aaa1115910.bv.entity.VideoListItem(
                aid = 10L,
                cid = 20L,
                title = "P2"
            ),
            clearDetailMetadata = true
        )

        assertEquals("en", next.subtitleMemory?.lang)
        assertEquals("ja", next.secondarySubtitleMemory?.lang)
        assertEquals(-1L, next.subtitleId)
        assertEquals(-1L, next.secondarySubtitleId)
        assertEquals(false, next.secondarySubtitleCustom)
        assertTrue(next.subtitleData.isEmpty())
        assertTrue(next.secondarySubtitleData.isEmpty())
    }

    private fun subtitle(
        id: Long,
        lang: String = "en",
        langDoc: String = "English"
    ) = Subtitle(
        id = id,
        lang = lang,
        langDoc = langDoc,
        url = "https://example.com/$id.json",
        type = SubtitleType.CC,
        aiType = SubtitleAiType.Normal,
        aiStatus = SubtitleAiStatus.None
    )
}
