package dev.aaa1115910.bv.screen.settings.content

import dev.aaa1115910.bv.entity.Resolution
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AudioVideoSettingStateTest {
    @Test
    fun `default video quality selector excludes removed 720p60`() {
        assertFalse(Resolution.R720P60 in selectableDefaultVideoQualities)
        assertTrue(Resolution.R720P in selectableDefaultVideoQualities)
        assertTrue(Resolution.R1080P60 in selectableDefaultVideoQualities)
    }

    @Test
    fun `legacy play next and related actions restore to merged autoplay action`() {
        assertEquals(ActionAfterPlayItems.AutoNextOrRelated, ActionAfterPlayItems.fromCode(1))
        assertEquals(ActionAfterPlayItems.AutoNextOrRelated, ActionAfterPlayItems.fromCode(3))
        assertEquals(ActionAfterPlayItems.AutoNextOrRelated, ActionAfterPlayItems.fromCode(4))
    }
}
