package dev.aaa1115910.bv.component.controllers

import dev.aaa1115910.biliapi.entity.video.Dimension
import dev.aaa1115910.biliapi.entity.video.VideoPage
import dev.aaa1115910.biliapi.entity.video.season.Episode
import dev.aaa1115910.biliapi.entity.video.season.Section
import dev.aaa1115910.biliapi.entity.video.season.UgcSeason
import dev.aaa1115910.bv.entity.VideoListItem
import kotlin.test.Test
import kotlin.test.assertEquals

class VideoListPanelStateTest {
    @Test
    fun `video list panel prefers current video pages over collection`() {
        val currentVideo = VideoListItem(
            aid = 1L,
            cid = 10L,
            title = "当前视频",
            ugcPages = listOf(
                VideoPage(cid = 11L, index = 1, title = "P1", duration = 60, dimension = Dimension(1920, 1080)),
                VideoPage(cid = 12L, index = 2, title = "P2", duration = 60, dimension = Dimension(1920, 1080))
            )
        )
        val collectionVideo = VideoListItem(
            aid = 2L,
            cid = 20L,
            title = "合集视频"
        )

        val state = resolveVideoListPanelState(
            currentCid = 12L,
            videoList = listOf(currentVideo, collectionVideo)
        )

        assertEquals("分P", state.buttonLabel)
        assertEquals("视频分P", state.headerText)
        assertEquals(VideoListPanelMode.Pages, state.mode)
        assertEquals(listOf("P1", "P2"), state.items.map { it.title })
        assertEquals(listOf(11L, 12L), state.items.map { it.cid })
    }

    @Test
    fun `video list panel falls back to collection when current video has no pages`() {
        val state = resolveVideoListPanelState(
            currentCid = 20L,
            videoList = listOf(
                VideoListItem(aid = 1L, cid = 10L, title = "第一集"),
                VideoListItem(aid = 2L, cid = 20L, title = "第二集")
            )
        )

        assertEquals("合集", state.buttonLabel)
        assertEquals("视频合集", state.headerText)
        assertEquals(VideoListPanelMode.Collection, state.mode)
        assertEquals(listOf("第一集", "第二集"), state.items.map { it.title })
    }

    @Test
    fun `playback video list prefers pages then ugc collection then single video`() {
        val pagesOnly = resolvePlaybackVideoList(
            aid = 1L,
            currentCid = 12L,
            title = "当前视频",
            pages = listOf(
                VideoPage(cid = 11L, index = 1, title = "P1", duration = 60, dimension = Dimension(1920, 1080)),
                VideoPage(cid = 12L, index = 2, title = "P2", duration = 60, dimension = Dimension(1920, 1080))
            ),
            ugcSeason = null
        )
        val collectionOnly = resolvePlaybackVideoList(
            aid = 2L,
            currentCid = 22L,
            title = "当前合集视频",
            pages = listOf(
                VideoPage(cid = 22L, index = 1, title = "正文", duration = 60, dimension = Dimension(1920, 1080))
            ),
            ugcSeason = UgcSeason(
                id = 1,
                title = "合集",
                cover = "",
                sections = listOf(
                    Section(
                        id = 10L,
                        title = "正片",
                        episodes = listOf(
                            Episode(1, 100L, "BV1", 21L, null, "第一集", "", "", 60, Dimension(1920, 1080)),
                            Episode(2, 200L, "BV2", 22L, null, "第二集", "", "", 60, Dimension(1920, 1080))
                        )
                    )
                )
            )
        )
        val singleOnly = resolvePlaybackVideoList(
            aid = 3L,
            currentCid = 31L,
            title = "单视频",
            pages = listOf(
                VideoPage(cid = 31L, index = 1, title = "正文", duration = 60, dimension = Dimension(1920, 1080))
            ),
            ugcSeason = null
        )

        assertEquals(listOf(12L), pagesOnly.map { it.cid })
        assertEquals(listOf(11L, 12L), pagesOnly.first().ugcPages?.map { it.cid })
        assertEquals(listOf(21L, 22L), collectionOnly.map { it.cid })
        assertEquals(listOf(31L), singleOnly.map { it.cid })
    }
}
