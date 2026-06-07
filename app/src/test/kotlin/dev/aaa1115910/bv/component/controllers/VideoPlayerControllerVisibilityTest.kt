package dev.aaa1115910.bv.component.controllers

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoPlayerControllerVisibilityTest {
    @Test
    fun `immersive chapter bar shows when chapters exist and controllers are hidden`() {
        assertTrue(
            shouldShowImmersiveChapterBar(
                hasChapters = true,
                showClickableControllers = false,
                showJumpModePausedInfoController = false,
                isSeeking = false
            )
        )
    }

    @Test
    fun `immersive chapter bar hides while osd or seek preview is visible`() {
        assertFalse(
            shouldShowImmersiveChapterBar(
                hasChapters = true,
                showClickableControllers = true,
                showJumpModePausedInfoController = false,
                isSeeking = false
            )
        )
        assertFalse(
            shouldShowImmersiveChapterBar(
                hasChapters = true,
                showClickableControllers = false,
                showJumpModePausedInfoController = false,
                isSeeking = true
            )
        )
    }

    @Test
    fun `persistent seek only shows without chapters and without visible controllers`() {
        assertTrue(
            shouldShowImmersivePersistentSeek(
                showPersistentSeek = true,
                hasChapters = false,
                showClickableControllers = false,
                showJumpModePausedInfoController = false,
                isSeeking = false
            )
        )
        assertFalse(
            shouldShowImmersivePersistentSeek(
                showPersistentSeek = true,
                hasChapters = true,
                showClickableControllers = false,
                showJumpModePausedInfoController = false,
                isSeeking = false
            )
        )
        assertFalse(
            shouldShowImmersivePersistentSeek(
                showPersistentSeek = true,
                hasChapters = false,
                showClickableControllers = true,
                showJumpModePausedInfoController = false,
                isSeeking = false
            )
        )
    }
}
