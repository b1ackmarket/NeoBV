package dev.aaa1115910.bv.viewmodel.player

import kotlin.math.abs
import kotlin.math.roundToLong

enum class PlayerTouchDragMode {
    None,
    Seek,
    Brightness,
    Volume,
    Blocked
}

enum class PlayerTouchTapZone {
    LeftEdge,
    Center,
    RightEdge
}

object PlayerTouchGesturePolicy {
    private const val edgeTapRatio = 0.25f
    private const val sideVerticalRatio = 0.33f
    private const val directionRatio = 1.25f
    private const val seekActivationSlopMultiplier = 4f
    private const val verticalActivationSlopMultiplier = 4f
    private const val blockSlopMultiplier = 6f
    private const val seekFullWidthDurationRatio = 0.5f
    private const val minFullWidthSeekMs = 60_000L
    private const val maxFullWidthSeekMs = 600_000L

    fun shouldSuppressTapAfterMove(
        deltaX: Float,
        deltaY: Float,
        touchSlopPx: Float
    ): Boolean {
        return abs(deltaX) > touchSlopPx || abs(deltaY) > touchSlopPx
    }

    fun tapZone(
        x: Float,
        widthPx: Float,
        edgeRatio: Float = edgeTapRatio
    ): PlayerTouchTapZone {
        if (widthPx <= 0f) return PlayerTouchTapZone.Center
        val clampedRatio = edgeRatio.coerceIn(0f, 0.5f)
        return when {
            x < widthPx * clampedRatio -> PlayerTouchTapZone.LeftEdge
            x > widthPx * (1f - clampedRatio) -> PlayerTouchTapZone.RightEdge
            else -> PlayerTouchTapZone.Center
        }
    }

    fun dragMode(
        downX: Float,
        deltaX: Float,
        deltaY: Float,
        widthPx: Float,
        touchSlopPx: Float
    ): PlayerTouchDragMode {
        val absDx = abs(deltaX)
        val absDy = abs(deltaY)
        val seekThreshold = touchSlopPx * seekActivationSlopMultiplier
        if (absDx >= seekThreshold && absDx >= absDy * directionRatio) {
            return PlayerTouchDragMode.Seek
        }

        val verticalThreshold = touchSlopPx * verticalActivationSlopMultiplier
        val width = widthPx.coerceAtLeast(1f)
        val verticalEligible = downX <= width * sideVerticalRatio ||
            downX >= width * (1f - sideVerticalRatio)
        if (verticalEligible && absDy >= verticalThreshold && absDy >= absDx * directionRatio) {
            return if (downX <= width * sideVerticalRatio) {
                PlayerTouchDragMode.Brightness
            } else {
                PlayerTouchDragMode.Volume
            }
        }

        val blockThreshold = touchSlopPx * blockSlopMultiplier
        return if (absDx > blockThreshold || absDy > blockThreshold) {
            PlayerTouchDragMode.Blocked
        } else {
            PlayerTouchDragMode.None
        }
    }

    fun seekPreviewPosition(
        startPositionMs: Long,
        dragDeltaPx: Float,
        gestureWidthPx: Float,
        durationMs: Long
    ): Long {
        if (durationMs <= 0L) return 0L
        val width = gestureWidthPx.coerceAtLeast(1f)
        val fullWidthMs = (durationMs * seekFullWidthDurationRatio)
            .roundToLong()
            .coerceIn(minFullWidthSeekMs, maxFullWidthSeekMs)
        val deltaMs = (fullWidthMs * (dragDeltaPx / width)).roundToLong()
        return (startPositionMs + deltaMs).coerceIn(0L, durationMs)
    }
}
