package dev.aaa1115910.bv.component.controllers.playermenu

import kotlin.test.Test
import kotlin.test.assertEquals

class PlaySpeedMenuStateTest {
    @Test
    fun `custom panel keeps exact preset display for non tenth preset speeds`() {
        assertEquals(1.25f, resolveCustomPlaySpeedDisplay(1.25f))
        assertEquals(1.3f, resolveCustomPlaySpeedDisplay(1.26f))
    }

    @Test
    fun `custom panel steps from preset speed to nearest tenth in pressed direction`() {
        assertEquals(1.3f, stepCustomPlaySpeed(1.25f, direction = 1))
        assertEquals(1.2f, stepCustomPlaySpeed(1.25f, direction = -1))
        assertEquals(1.4f, stepCustomPlaySpeed(1.3f, direction = 1))
    }

    @Test
    fun `exact preset speed keeps preset selection instead of rounding into custom`() {
        assertEquals(
            PlaySpeedMenuItem.Preset(PlaySpeedPreset.x1_25),
            resolvePlaySpeedMenuItem(1.25f)
        )
        assertEquals(
            PlaySpeedMenuItem.Preset(PlaySpeedPreset.x2),
            resolvePlaySpeedMenuItem(2.0f)
        )
    }

    @Test
    fun `non preset speed resolves to custom entry`() {
        assertEquals(PlaySpeedMenuItem.Custom, resolvePlaySpeedMenuItem(1.3f))
        assertEquals(PlaySpeedMenuItem.Preset(PlaySpeedPreset.x1_5), resolvePlaySpeedMenuItem(1.5f))
    }

    @Test
    fun `custom speed is clamped to supported range and rounded to one decimal`() {
        assertEquals(0.5f, clampCustomPlaySpeed(0.31f))
        assertEquals(1.3f, clampCustomPlaySpeed(1.26f))
        assertEquals(2.0f, clampCustomPlaySpeed(2.41f))
    }

    @Test
    fun `custom selection stays on custom even when speed lands on preset`() {
        assertEquals(
            PlaySpeedMenuItem.Custom,
            syncPlaySpeedMenuItem(
                currentSelection = PlaySpeedMenuItem.Custom,
                currentSpeed = 1.5f
            )
        )
        assertEquals(
            PlaySpeedMenuItem.Custom,
            syncPlaySpeedMenuItem(
                currentSelection = PlaySpeedMenuItem.Preset(PlaySpeedPreset.x1),
                currentSpeed = 1.3f
            )
        )
    }

    @Test
    fun `preset speed only applies after confirm while custom stays realtime`() {
        assertEquals(1.25f, resolveConfirmedPresetSpeed(PlaySpeedMenuItem.Preset(PlaySpeedPreset.x1_25)))
        assertEquals(null, resolveConfirmedPresetSpeed(PlaySpeedMenuItem.Custom))
    }

    @Test
    fun `menu highlights focused speed while navigating but keeps applied speed when collapsed`() {
        val applied = PlaySpeedMenuItem.Custom
        val focused = PlaySpeedMenuItem.Preset(PlaySpeedPreset.x1_25)

        assertEquals(
            focused,
            resolveDisplayedPlaySpeedMenuItem(
                appliedSelection = applied,
                focusedSelection = focused,
                showFocusedSelection = true
            )
        )
        assertEquals(
            applied,
            resolveDisplayedPlaySpeedMenuItem(
                appliedSelection = applied,
                focusedSelection = focused,
                showFocusedSelection = false
            )
        )
    }
}
