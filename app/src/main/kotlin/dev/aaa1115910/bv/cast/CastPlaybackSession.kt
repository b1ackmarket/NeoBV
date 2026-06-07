package dev.aaa1115910.bv.cast

data class CastPlaybackSnapshot(
    val state: CastTransportState = CastTransportState.STOPPED,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val speed: Float = 1f,
    val aid: Long = 0L,
    val cid: Long = 0L,
    val epid: Int? = null,
    val seasonId: Int = 0,
    val roomId: Long = 0L,
    val title: String = "",
    val qualityId: Int = 0,
    val availableQuality: Map<Int, String> = emptyMap(),
    val danmakuEnabled: Boolean = false
)

enum class CastTransportState(val dlnaName: String) {
    STOPPED("STOPPED"),
    PLAYING("PLAYING"),
    PAUSED("PAUSED_PLAYBACK"),
    TRANSITIONING("TRANSITIONING")
}

interface CastPlaybackSession {
    fun play()
    fun pause()
    fun stop()
    fun seekTo(positionMs: Long)
    fun setSpeed(speed: Float)
    fun setQuality(qualityId: Int)
    fun setDanmakuEnabled(enabled: Boolean)
    fun snapshot(): CastPlaybackSnapshot
}

object CastPlaybackSessionRegistry {
    @Volatile
    private var session: CastPlaybackSession? = null

    fun register(session: CastPlaybackSession) {
        this.session = session
    }

    fun unregister(session: CastPlaybackSession) {
        if (this.session === session) {
            this.session = null
        }
    }

    fun current(): CastPlaybackSession? = session
}
