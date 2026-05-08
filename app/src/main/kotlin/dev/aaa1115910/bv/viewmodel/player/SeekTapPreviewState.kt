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
    data class DirectJump(val targetPositionMs: Long) : SeekTapAction
    data class StartOrUpdatePreview(val targetPositionMs: Long) : SeekTapAction
}

class SeekTapPreviewState(
    private val previewWindowMs: Long = 1_000L
) {
    private var lastDirectionalTapAtMs: Long = Long.MIN_VALUE
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
        val basePosition = if (previewActive) previewPositionMs else currentPositionMs
        val nextTarget = (basePosition + delta).coerceIn(0L, totalDurationMs)

        lastDirectionalTapAtMs = nowMs
        previewPositionMs = nextTarget

        return if (previewActive || previewWindowOpen) {
            previewActive = true
            SeekTapAction.StartOrUpdatePreview(nextTarget)
        } else {
            previewActive = false
            SeekTapAction.DirectJump(nextTarget)
        }
    }

    fun clearPreview() {
        previewActive = false
        lastDirectionalTapAtMs = Long.MIN_VALUE
    }
}
