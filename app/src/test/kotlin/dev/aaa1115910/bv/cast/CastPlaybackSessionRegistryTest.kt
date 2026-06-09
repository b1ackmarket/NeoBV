package dev.aaa1115910.bv.cast

import kotlin.test.Test
import kotlin.test.assertEquals

class CastPlaybackSessionRegistryTest {
    @Test
    fun `pause current delegates to registered session`() {
        val session = FakeCastPlaybackSession()
        CastPlaybackSessionRegistry.register(session)
        try {
            CastPlaybackSessionRegistry.pauseCurrent()
            assertEquals(1, session.pauseCount)
        } finally {
            CastPlaybackSessionRegistry.unregister(session)
        }
    }

    private class FakeCastPlaybackSession : CastPlaybackSession {
        var pauseCount = 0

        override fun play() = Unit
        override fun pause() {
            pauseCount++
        }
        override fun stop() = Unit
        override fun seekTo(positionMs: Long) = Unit
        override fun setSpeed(speed: Float) = Unit
        override fun setQuality(qualityId: Int) = Unit
        override fun setDanmakuEnabled(enabled: Boolean) = Unit
        override fun snapshot(): CastPlaybackSnapshot = CastPlaybackSnapshot()
    }
}
