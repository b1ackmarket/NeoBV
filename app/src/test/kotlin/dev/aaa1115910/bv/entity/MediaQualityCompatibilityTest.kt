package dev.aaa1115910.bv.entity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MediaQualityCompatibilityTest {
    @Test
    fun `resolution recognizes HDR Vivid quality code`() {
        assertEquals(Resolution.RHdrVivid, Resolution.fromCode(129))
    }

    @Test
    fun `dolby atmos accepts both current audio ids`() {
        assertEquals(Audio.ADolbyAtoms, Audio.fromCode(30250))
        assertEquals(Audio.ADolbyAtoms, Audio.fromCode(30255))
        assertTrue(Audio.ADolbyAtoms.matchesCode(30250))
        assertTrue(Audio.ADolbyAtoms.matchesCode(30255))
    }
}
