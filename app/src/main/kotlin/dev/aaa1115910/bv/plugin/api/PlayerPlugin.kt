package dev.aaa1115910.bv.plugin.api

data class PlayerPluginContext(
    val aid: Long,
    val cid: Long,
    val bvid: String,
    val title: String
)

interface PlayerPlugin {
    val id: String
    val displayName: String

    suspend fun onVideoLoaded(context: PlayerPluginContext) {}

    suspend fun onPlaybackPosition(positionMs: Long): PluginPlaybackAction = PluginPlaybackAction.None

    suspend fun onPlaybackEnded() {}
}
