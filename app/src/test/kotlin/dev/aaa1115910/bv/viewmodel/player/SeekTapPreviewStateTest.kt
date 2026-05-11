package dev.aaa1115910.bv.viewmodel.player

import kotlin.test.Test
import kotlin.test.assertEquals

class SeekTapPreviewStateTest {
    @Test
    fun `first tap jumps directly and second tap within one second opens preview`() {
        val state = SeekTapPreviewState()

        val first = state.onDirectionalTap(
            direction = SeekDirection.Forward,
            nowMs = 1_000L,
            currentPositionMs = 120_000L,
            totalDurationMs = 300_000L,
            stepMs = SeekStepOption.Ten.millis
        )
        val second = state.onDirectionalTap(
            direction = SeekDirection.Forward,
            nowMs = 1_500L,
            currentPositionMs = 130_000L,
            totalDurationMs = 300_000L,
            stepMs = SeekStepOption.Ten.millis
        )

        assertEquals(SeekTapAction.PendingDirectJump(130_000L), first)
        assertEquals(SeekTapAction.StartOrUpdatePreview(140_000L), second)
    }

    @Test
    fun `clearing preview resets tap window so next tap becomes pending jump again`() {
        val state = SeekTapPreviewState()

        state.onDirectionalTap(
            direction = SeekDirection.Forward,
            nowMs = 1_000L,
            currentPositionMs = 120_000L,
            totalDurationMs = 300_000L,
            stepMs = SeekStepOption.Ten.millis
        )
        state.onDirectionalTap(
            direction = SeekDirection.Forward,
            nowMs = 1_500L,
            currentPositionMs = 130_000L,
            totalDurationMs = 300_000L,
            stepMs = SeekStepOption.Ten.millis
        )

        state.clearPreview()

        val next = state.onDirectionalTap(
            direction = SeekDirection.Forward,
            nowMs = 1_800L,
            currentPositionMs = 140_000L,
            totalDurationMs = 300_000L,
            stepMs = SeekStepOption.Ten.millis
        )

        assertEquals(SeekTapAction.PendingDirectJump(150_000L), next)
    }

    @Test
    fun `window expiry resets to pending jump and boundaries clamp correctly`() {
        val state = SeekTapPreviewState()

        state.onDirectionalTap(
            direction = SeekDirection.Backward,
            nowMs = 1_000L,
            currentPositionMs = 4_000L,
            totalDurationMs = 300_000L,
            stepMs = SeekStepOption.Five.millis
        )

        val expired = state.onDirectionalTap(
            direction = SeekDirection.Backward,
            nowMs = 2_500L,
            currentPositionMs = 0L,
            totalDurationMs = 300_000L,
            stepMs = SeekStepOption.Five.millis
        )

        val upperBound = state.onDirectionalTap(
            direction = SeekDirection.Forward,
            nowMs = 4_000L,
            currentPositionMs = 298_000L,
            totalDurationMs = 300_000L,
            stepMs = SeekStepOption.Five.millis
        )

        assertEquals(SeekTapAction.PendingDirectJump(0L), expired)
        assertEquals(SeekTapAction.PendingDirectJump(300_000L), upperBound)
    }

    @Test
    fun `clearing pending jump cancels the double tap window`() {
        val state = SeekTapPreviewState()

        state.onDirectionalTap(
            direction = SeekDirection.Forward,
            nowMs = 1_000L,
            currentPositionMs = 120_000L,
            totalDurationMs = 300_000L,
            stepMs = SeekStepOption.Ten.millis
        )
        state.clearPendingJump()

        val next = state.onDirectionalTap(
            direction = SeekDirection.Forward,
            nowMs = 1_300L,
            currentPositionMs = 130_000L,
            totalDurationMs = 300_000L,
            stepMs = SeekStepOption.Ten.millis
        )

        assertEquals(SeekTapAction.PendingDirectJump(140_000L), next)
    }

    @Test
    fun `seek step option restores unknown persisted value to ten seconds`() {
        assertEquals(SeekStepOption.Five, SeekStepOption.fromSeconds(5))
        assertEquals(SeekStepOption.Ten, SeekStepOption.fromSeconds(10))
        assertEquals(SeekStepOption.Ten, SeekStepOption.fromSeconds(99))
    }
}
