package dev.aaa1115910.bv.util

import dev.aaa1115910.bv.component.PersonalTopNavItem
import dev.aaa1115910.bv.component.controllers.LiveDanmakuSourceMode
import dev.aaa1115910.bv.entity.Audio
import dev.aaa1115910.bv.entity.Resolution
import dev.aaa1115910.bv.entity.VideoCodec
import dev.aaa1115910.bv.entity.live.LiveDefaultQuality
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PrefDefaultValuesTest {
    @Test
    fun `media defaults prefer highest quality hevc hires and enabled audio soft decode`() {
        assertEquals(Resolution.R8K, PrefDefaultValues.defaultQuality)
        assertEquals(LiveDefaultQuality.Dolby, PrefDefaultValues.defaultLiveQuality)
        assertEquals(VideoCodec.HEVC, PrefDefaultValues.defaultVideoCodec)
        assertEquals(Audio.AHiRes, PrefDefaultValues.defaultAudio)
        assertEquals(LiveDanmakuSourceMode.HistoryOnly, PrefDefaultValues.defaultLiveDanmakuSourceMode)
        assertTrue(PrefDefaultValues.enableFfmpegAudioRenderer)
    }

    @Test
    fun `ui and network defaults prefer history video details and official cdn`() {
        assertEquals(PersonalTopNavItem.History, PrefDefaultValues.firstPersonalTopNavItem)
        assertTrue(PrefDefaultValues.showVideoInfo)
        assertTrue(PrefDefaultValues.preferOfficialCdn)
    }
}
