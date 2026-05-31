package dev.aaa1115910.bv.viewmodel.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlayerTouchGesturePolicyTest {
    @Test
    fun `horizontal drag seek preview is clamped to media duration`() {
        val durationMs = 120_000L

        assertEquals(
            0L,
            PlayerTouchGesturePolicy.seekPreviewPosition(
                startPositionMs = 5_000L,
                dragDeltaPx = -10_000f,
                gestureWidthPx = 1_000f,
                durationMs = durationMs
            )
        )
        assertEquals(
            durationMs,
            PlayerTouchGesturePolicy.seekPreviewPosition(
                startPositionMs = 115_000L,
                dragDeltaPx = 10_000f,
                gestureWidthPx = 1_000f,
                durationMs = durationMs
            )
        )
    }

    @Test
    fun `movement past touch slop suppresses tap handling`() {
        assertFalse(
            PlayerTouchGesturePolicy.shouldSuppressTapAfterMove(
                deltaX = 3f,
                deltaY = 4f,
                touchSlopPx = 8f
            )
        )
        assertTrue(
            PlayerTouchGesturePolicy.shouldSuppressTapAfterMove(
                deltaX = 9f,
                deltaY = 0f,
                touchSlopPx = 8f
            )
        )
    }

    @Test
    fun `drag mode separates seek brightness and volume regions`() {
        assertEquals(
            PlayerTouchDragMode.Seek,
            PlayerTouchGesturePolicy.dragMode(
                downX = 500f,
                deltaX = 120f,
                deltaY = 10f,
                widthPx = 1_000f,
                touchSlopPx = 8f
            )
        )
        assertEquals(
            PlayerTouchDragMode.Brightness,
            PlayerTouchGesturePolicy.dragMode(
                downX = 100f,
                deltaX = 8f,
                deltaY = -120f,
                widthPx = 1_000f,
                touchSlopPx = 8f
            )
        )
        assertEquals(
            PlayerTouchDragMode.Volume,
            PlayerTouchGesturePolicy.dragMode(
                downX = 900f,
                deltaX = 8f,
                deltaY = -120f,
                widthPx = 1_000f,
                touchSlopPx = 8f
            )
        )
    }
}
