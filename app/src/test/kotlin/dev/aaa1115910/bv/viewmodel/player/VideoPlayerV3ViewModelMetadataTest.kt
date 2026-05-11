package dev.aaa1115910.bv.viewmodel.player

import dev.aaa1115910.bv.entity.VideoListItem
import dev.aaa1115910.bv.ui.state.PlayerUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VideoPlayerV3ViewModelMetadataTest {
    @Test
    fun `same aid switch keeps publish date and play count text`() {
        val currentState = PlayerUiState(
            aid = 100L,
            cid = 10L,
            title = "P1",
            publishDateText = "5月8日",
            playCountText = "12.3万播放"
        )

        val nextState = currentState.copyForVideoSwitch(
            newVideo = VideoListItem(
                aid = 100L,
                cid = 20L,
                title = "P2"
            ),
            clearDetailMetadata = false
        )

        assertEquals("5月8日", nextState.publishDateText)
        assertEquals("12.3万播放", nextState.playCountText)
    }

    @Test
    fun `different aid switch clears publish date and play count text`() {
        val currentState = PlayerUiState(
            aid = 100L,
            cid = 10L,
            title = "Old",
            publishDateText = "5月8日",
            playCountText = "12.3万播放"
        )

        val nextState = currentState.copyForVideoSwitch(
            newVideo = VideoListItem(
                aid = 200L,
                cid = 30L,
                title = "New"
            ),
            clearDetailMetadata = true
        )

        assertEquals("", nextState.publishDateText)
        assertEquals("", nextState.playCountText)
    }

    @Test
    fun `video switch resets subtitle selection and loaded subtitle data`() {
        val currentState = PlayerUiState(
            aid = 100L,
            cid = 10L,
            title = "Old",
            subtitleId = 42L
        ).copy(subtitleData = listOf())

        val nextState = currentState.copyForVideoSwitch(
            newVideo = VideoListItem(
                aid = 200L,
                cid = 30L,
                title = "New"
            ),
            clearDetailMetadata = true
        )

        assertEquals(-1L, nextState.subtitleId)
        assertTrue(nextState.subtitleData.isEmpty())
        assertTrue(nextState.subtitleList.isEmpty())
    }
}
