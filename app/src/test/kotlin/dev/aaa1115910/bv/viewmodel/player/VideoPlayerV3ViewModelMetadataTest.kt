package dev.aaa1115910.bv.viewmodel.player

import dev.aaa1115910.biliapi.entity.DashAudio
import dev.aaa1115910.biliapi.entity.DashVideo
import dev.aaa1115910.bv.entity.VideoListItem
import dev.aaa1115910.bv.repository.JumpModeQueueItem
import dev.aaa1115910.bv.ui.state.JumpModeState
import dev.aaa1115910.bv.ui.state.PlayerUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
            subtitleId = 42L,
            onlineCountText = "256",
            mediaStatsInfo = "video FPS: 30"
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
        assertEquals(null, nextState.onlineCountText)
        assertEquals("", nextState.mediaStatsInfo)
    }

    @Test
    fun `jump mode switch keeps queue and enabled state`() {
        val currentState = PlayerUiState(
            aid = 100L,
            cid = 10L,
            title = "Old",
            jumpModeState = JumpModeState(
                available = true,
                enabled = true,
                currentIndex = 0,
                items = listOf(
                    JumpModeQueueItem(aid = 100L, title = "Old"),
                    JumpModeQueueItem(aid = 200L, title = "New")
                )
            )
        )

        val nextState = currentState.copyForVideoSwitch(
            newVideo = VideoListItem(
                aid = 200L,
                cid = 30L,
                title = "New"
            ),
            clearDetailMetadata = true
        )

        assertTrue(nextState.jumpModeState.enabled)
        assertEquals(1, nextState.jumpModeState.currentIndex)
    }

    @Test
    fun `jump mode switch disables when new video is outside queue`() {
        val currentState = PlayerUiState(
            aid = 100L,
            cid = 10L,
            title = "Old",
            jumpModeState = JumpModeState(
                available = true,
                enabled = true,
                currentIndex = 0,
                items = listOf(
                    JumpModeQueueItem(aid = 100L, title = "Old"),
                    JumpModeQueueItem(aid = 200L, title = "Queued")
                )
            )
        )

        val nextState = currentState.copyForVideoSwitch(
            newVideo = VideoListItem(
                aid = 300L,
                cid = 30L,
                title = "Outside"
            ),
            clearDetailMetadata = true
        )

        assertFalse(nextState.jumpModeState.available)
        assertFalse(nextState.jumpModeState.enabled)
    }

    @Test
    fun `bili media stats hides empty fields and keeps selected stream facts`() {
        val stats = buildBiliMediaStatsInfo(
            video = DashVideo(
                quality = 80,
                baseUrl = "https://video",
                bandwidth = 2_500_000,
                codecId = 7,
                width = 1920,
                height = 1080,
                frameRate = "30",
                backUrl = emptyList(),
                codecs = "avc1.640032"
            ),
            audio = DashAudio(
                baseUrl = "https://audio",
                bandwidth = 192_000,
                codecId = 30280,
                backUrl = emptyList(),
                codecs = "mp4a.40.2"
            )
        )

        assertEquals(
            """
            resolution: 1920 x 1080
            video FPS: 30
            stream bitrate: 2.50 Mbps
            video codec: avc1.640032
            audio codec: mp4a.40.2
            audio bitrate: 192 kbps
            """.trimIndent(),
            stats
        )
    }

    @Test
    fun `bili media stats does not render unknown placeholders`() {
        val stats = buildBiliMediaStatsInfo(
            video = DashVideo(
                quality = 80,
                baseUrl = "https://video",
                bandwidth = 0,
                codecId = 0,
                width = 0,
                height = 0,
                frameRate = "",
                backUrl = emptyList(),
                codecs = ""
            ),
            audio = null
        )

        assertEquals("", stats)
    }
}
