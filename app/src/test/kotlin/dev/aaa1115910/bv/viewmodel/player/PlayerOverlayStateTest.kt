package dev.aaa1115910.bv.viewmodel.player

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import dev.aaa1115910.bv.entity.VideoListItem
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.component.controllers.hasClickableControllerOverlay
import dev.aaa1115910.bv.component.controllers.hasSecondaryControllerOverlay
import dev.aaa1115910.bv.screen.UpPanelVideoClickAction
import dev.aaa1115910.bv.screen.resolveUpPanelVideoClickAction
import dev.aaa1115910.bv.screen.main.LeftNaviItem
import dev.aaa1115910.bv.component.controllers.shouldCloseSidePanelForPreviewKey
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
    fun `up panel videos without cid fall back to opening details`() {
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
        assertEquals(UpPanelVideoClickAction.OpenDetails(aid = 3L), openDetails)
    }

    @Test
    fun `homepage target resolves dynamic directly but recommend popular through home`() {
        assertEquals(
            MainNavigationTarget(LeftNaviItem.Dynamic, null),
            HomeEntryResolver.resolve(HomeStartDestination.Dynamic)
        )
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
}
