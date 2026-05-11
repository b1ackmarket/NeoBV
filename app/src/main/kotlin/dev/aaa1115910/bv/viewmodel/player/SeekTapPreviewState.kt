package dev.aaa1115910.bv.viewmodel.player

enum class SeekDirection {
    Backward,
    Forward
}

enum class SeekStepOption(val seconds: Int) {
    Five(5),
    Ten(10);

    val millis: Long
        get() = seconds * 1_000L

    companion object {
        fun fromSeconds(seconds: Int): SeekStepOption {
            return entries.find { it.seconds == seconds } ?: Ten
        }
    }
}

sealed interface SeekTapAction {
    data class PendingDirectJump(val targetPositionMs: Long) : SeekTapAction
    data class StartOrUpdatePreview(val targetPositionMs: Long) : SeekTapAction
}

class SeekTapPreviewState(
    private val previewWindowMs: Long = 1_000L
) {
    private var lastDirectionalTapAtMs: Long = Long.MIN_VALUE
    private var pendingDirectJumpTargetMs: Long? = null
    private var previewActive = false
    private var previewPositionMs = 0L

    fun onDirectionalTap(
        direction: SeekDirection,
        nowMs: Long,
        currentPositionMs: Long,
        totalDurationMs: Long,
        stepMs: Long
    ): SeekTapAction {
        val delta = if (direction == SeekDirection.Forward) stepMs else -stepMs
        val previewWindowOpen = lastDirectionalTapAtMs != Long.MIN_VALUE &&
            nowMs - lastDirectionalTapAtMs <= previewWindowMs
        if (previewActive) {
            val nextTarget = (previewPositionMs + delta).coerceIn(0L, totalDurationMs)
            lastDirectionalTapAtMs = nowMs
            previewPositionMs = nextTarget
            return SeekTapAction.StartOrUpdatePreview(nextTarget)
        }

        val basePosition = if (previewWindowOpen) {
            pendingDirectJumpTargetMs ?: currentPositionMs
        } else {
            currentPositionMs
        }
        val nextTarget = (basePosition + delta).coerceIn(0L, totalDurationMs)

        lastDirectionalTapAtMs = nowMs
        return if (previewWindowOpen && pendingDirectJumpTargetMs != null) {
            pendingDirectJumpTargetMs = null
            previewActive = true
            previewPositionMs = nextTarget
            SeekTapAction.StartOrUpdatePreview(nextTarget)
        } else {
            previewActive = false
            previewPositionMs = nextTarget
            pendingDirectJumpTargetMs = nextTarget
            SeekTapAction.PendingDirectJump(nextTarget)
        }
    }

    fun clearPendingJump() {
        pendingDirectJumpTargetMs = null
        if (!previewActive) {
            lastDirectionalTapAtMs = Long.MIN_VALUE
        }
    }

    fun clearPreview() {
        previewActive = false
        pendingDirectJumpTargetMs = null
        lastDirectionalTapAtMs = Long.MIN_VALUE
    }
}
