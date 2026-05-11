package dev.aaa1115910.bv.viewmodel.player

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import dev.aaa1115910.bv.entity.VideoCodec
import dev.aaa1115910.bv.entity.VideoListItem
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.component.controllers.hasClickableControllerOverlay
import dev.aaa1115910.bv.component.controllers.hasSecondaryControllerOverlay
import dev.aaa1115910.bv.screen.UpPanelVideoClickAction
import dev.aaa1115910.bv.screen.resolveUpPanelVideoClickAction
import dev.aaa1115910.bv.screen.main.LeftNaviItem
import dev.aaa1115910.bv.component.controllers.UP_PANEL_NAME_MAX_DISPLAY_UNITS
import dev.aaa1115910.bv.component.controllers.shouldCloseSidePanelForPreviewKey
import dev.aaa1115910.bv.component.controllers.truncateUpPanelName
import dev.aaa1115910.bv.viewmodel.player.normalizeAvailableVideoCodecs
import dev.aaa1115910.bv.viewmodel.player.normalizeSubtitleUrl
import dev.aaa1115910.biliapi.entity.user.SpaceVideoOrder
import dev.aaa1115910.bv.viewmodel.player.shouldApplyUpPanelLoadResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlayerOverlayStateTest {
    @Test
    fun `opening uploader panel closes related panel and keeps playback active`() {
        val initial = PlayerOverlayState(
            activePanel = PlayerSidePanel.RelatedVideos,
            pausePlaybackForPanel = false
        )

        val updated = initial.open(PlayerSidePanel.UpSpace)

        assertEquals(PlayerSidePanel.UpSpace, updated.activePanel)
        assertFalse(updated.pausePlaybackForPanel)
    }

    @Test
    fun `closing panel clears active panel`() {
        val initial = PlayerOverlayState(activePanel = PlayerSidePanel.UpSpace)

        val updated = initial.closePanel()

        assertEquals(PlayerSidePanel.None, updated.activePanel)
    }

    @Test
    fun `up side panel counts as secondary clickable overlay in controller`() {
        assertTrue(
            hasSecondaryControllerOverlay(
                showListController = false,
                showMenuController = false,
                activePanel = PlayerSidePanel.UpSpace
            )
        )
        assertTrue(
            hasClickableControllerOverlay(
                showListController = false,
                showMenuController = false,
                showInfoSeekController = false,
                activePanel = PlayerSidePanel.UpSpace
            )
        )
        assertFalse(
            hasClickableControllerOverlay(
                showListController = false,
                showMenuController = false,
                showInfoSeekController = false,
                activePanel = PlayerSidePanel.None
            )
        )
    }

    @Test
    fun `left only closes side panel when focus is outside header controls`() {
        assertFalse(
            shouldCloseSidePanelForPreviewKey(
                eventType = KeyEventType.KeyDown,
                key = Key.DirectionLeft,
                headerHasFocus = true
            )
        )
        assertTrue(
            shouldCloseSidePanelForPreviewKey(
                eventType = KeyEventType.KeyDown,
                key = Key.DirectionLeft,
                headerHasFocus = false
            )
        )
        assertTrue(
            shouldCloseSidePanelForPreviewKey(
                eventType = KeyEventType.KeyDown,
                key = Key.Back,
                headerHasFocus = true
            )
        )
    }

    @Test
    fun `up panel videos without cid resolve first cid before inline play`() {
        val playInline = resolveUpPanelVideoClickAction(
            VideoCardData(
                avid = 1L,
                cid = 2L,
                title = "Playable",
                cover = "",
                upName = ""
            )
        )
        val openDetails = resolveUpPanelVideoClickAction(
            VideoCardData(
                avid = 3L,
                cid = null,
                title = "Detail only",
                cover = "",
                upName = ""
            )
        )

        assertEquals(
            UpPanelVideoClickAction.PlayInline(
                VideoListItem(
                    aid = 1L,
                    cid = 2L,
                    title = "Playable"
                )
            ),
            playInline
        )
        assertEquals(
            UpPanelVideoClickAction.ResolveAndPlay(
                aid = 3L,
                fallbackTitle = "Detail only"
            ),
            openDetails
        )
    }

    @Test
    fun `resolved codec variant is added to available codec list for active selection`() {
        assertEquals(
            listOf(VideoCodec.HEVC, VideoCodec.HVC1),
            normalizeAvailableVideoCodecs(
                current = listOf(VideoCodec.HEVC),
                active = VideoCodec.HVC1
            )
        )
    }

    @Test
    fun `stale up panel load result is ignored when author or sort changed`() {
        assertTrue(
            shouldApplyUpPanelLoadResult(
                requestedAuthorMid = 1L,
                requestedOrder = SpaceVideoOrder.PubDate,
                currentAuthorMid = 1L,
                currentOrder = SpaceVideoOrder.PubDate
            )
        )
        assertFalse(
            shouldApplyUpPanelLoadResult(
                requestedAuthorMid = 1L,
                requestedOrder = SpaceVideoOrder.PubDate,
                currentAuthorMid = 2L,
                currentOrder = SpaceVideoOrder.PubDate
            )
        )
        assertFalse(
            shouldApplyUpPanelLoadResult(
                requestedAuthorMid = 1L,
                requestedOrder = SpaceVideoOrder.PubDate,
                currentAuthorMid = 1L,
                currentOrder = SpaceVideoOrder.Click
            )
        )
    }

    @Test
    fun `homepage target resolves dynamic directly but recommend popular through home`() {
        assertEquals(
            MainNavigationTarget(LeftNaviItem.Home, HomeStartDestination.Recommend),
            HomeEntryResolver.resolve(HomeStartDestination.Recommend)
        )
        assertEquals(
            MainNavigationTarget(LeftNaviItem.Home, HomeStartDestination.Popular),
            HomeEntryResolver.resolve(HomeStartDestination.Popular)
        )
    }

    @Test
    fun `touch support policy makes leanback optional for phone debugging`() {
        val policy = TouchSupportPolicy.forPhoneDebugging()

        assertFalse(policy.requireLeanback)
        assertTrue(policy.supportTouch)
    }

    @Test
    fun `up panel name keeps reference length but truncates longer mixed width names`() {
        assertEquals(
            "AG超玩会官方…",
            truncateUpPanelName("AG超玩会官方账号")
        )
        assertEquals(
            "AG超玩会官方…",
            truncateUpPanelName("AG超玩会官方账号直播间")
        )
        assertEquals(14, UP_PANEL_NAME_MAX_DISPLAY_UNITS)
    }

    @Test
    fun `subtitle url normalizer adds https to protocol relative urls`() {
        assertEquals(
            "https://i0.hdslb.com/bfs/subtitle/demo.json",
            normalizeSubtitleUrl("//i0.hdslb.com/bfs/subtitle/demo.json")
        )
        assertEquals(
            "https://i0.hdslb.com/bfs/subtitle/demo.json",
            normalizeSubtitleUrl("https://i0.hdslb.com/bfs/subtitle/demo.json")
        )
    }
}
