package dev.aaa1115910.bv.plugin.api

sealed interface PluginPlaybackAction {
    data object None : PluginPlaybackAction

    data class AutoSkip(
        val targetPositionMs: Long,
        val message: String
    ) : PluginPlaybackAction

    data class PromptSkip(
        val segmentId: String,
        val startPositionMs: Long,
        val targetPositionMs: Long,
        val message: String,
        val category: String
    ) : PluginPlaybackAction
}
