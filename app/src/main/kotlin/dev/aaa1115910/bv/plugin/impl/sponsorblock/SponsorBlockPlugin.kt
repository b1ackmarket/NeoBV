package dev.aaa1115910.bv.plugin.impl.sponsorblock

import dev.aaa1115910.bv.plugin.api.PlayerPlugin
import dev.aaa1115910.bv.plugin.api.PlayerPluginContext
import dev.aaa1115910.bv.plugin.api.PluginPlaybackAction

class SponsorBlockPlugin(
    private val api: SponsorBlockApi,
    private val configStore: SponsorBlockConfigStore
) : PlayerPlugin {
    override val id: String = "sponsorblock"
    override val displayName: String = "空降助手"

    private var config: SponsorBlockConfig = SponsorBlockConfig.default()
    private var enabled: Boolean = false
    private var segments: List<SponsorSegment> = emptyList()
    private val handledSegmentIds = linkedSetOf<String>()
    private val dismissedSegmentIds = linkedSetOf<String>()
    private val minimumActivationPositionMs = 5_000L
    private var allowEarlyActivation: Boolean = false

    override suspend fun onVideoLoaded(context: PlayerPluginContext) {
        enabled = configStore.isEnabled()
        config = configStore.readConfig().copy(enabled = enabled)
        allowEarlyActivation = context.fromSeason
        handledSegmentIds.clear()
        dismissedSegmentIds.clear()
        segments = if (!enabled) {
            emptyList()
        } else {
            api.getSegments(context.bvid)
        }
    }

    override suspend fun onPlaybackPosition(positionMs: Long): PluginPlaybackAction {
        if (!enabled || segments.isEmpty()) return PluginPlaybackAction.None
        if (!allowEarlyActivation && positionMs < minimumActivationPositionMs) {
            return PluginPlaybackAction.None
        }

        dismissedSegmentIds.removeAll { segmentId ->
            segments.firstOrNull { it.id == segmentId }?.contains(positionMs) != true
        }

        val segment = segments.firstOrNull {
            it.id !in handledSegmentIds &&
                it.id !in dismissedSegmentIds &&
                it.contains(positionMs)
        } ?: return PluginPlaybackAction.None

        return when (config.categoryPolicy[segment.category] ?: SkipPolicy.Disabled) {
            SkipPolicy.Auto -> {
                handledSegmentIds += segment.id
                PluginPlaybackAction.AutoSkip(
                    targetPositionMs = segment.endMs,
                    message = autoMessageFor(segment.category)
                )
            }

            SkipPolicy.Prompt -> {
                PluginPlaybackAction.PromptSkip(
                    segmentId = segment.id,
                    startPositionMs = segment.startMs,
                    targetPositionMs = segment.endMs,
                    message = promptMessageFor(segment.category),
                    category = segment.category
                )
            }

            SkipPolicy.Disabled -> PluginPlaybackAction.None
        }
    }

    override suspend fun onPlaybackEnded() {
        handledSegmentIds.clear()
        dismissedSegmentIds.clear()
        allowEarlyActivation = false
        segments = emptyList()
    }

    fun debugLoadedSegmentCount(): Int = segments.size

    fun progressMarks() = buildSponsorBlockProgressMarks(config = config, segments = segments)

    fun markHandled(segmentId: String) {
        dismissedSegmentIds -= segmentId
        handledSegmentIds += segmentId
    }

    fun dismissPrompt(segmentId: String) {
        if (segments.any { it.id == segmentId }) {
            dismissedSegmentIds += segmentId
        }
    }

    private fun autoMessageFor(category: String): String = when (category) {
        "sponsor", "selfpromo" -> "已跳过赞助片段"
        "intro" -> "已跳过片头"
        "outro" -> "已跳过片尾"
        else -> "已自动跳过"
    }

    private fun promptMessageFor(category: String): String = when (category) {
        "sponsor" -> "显示提示：赞助/恰饭"
        "selfpromo" -> "显示提示：无偿/自我推广"
        "exclusive_access" -> "显示提示：独家访问/抢先体验"
        "interaction" -> "显示提示：三连/互动提醒"
        "poi_highlight" -> "显示提示：精彩时刻/重点"
        "intro" -> "显示提示：片头"
        "outro" -> "显示提示：片尾"
        "preview" -> "显示提示：预览/剧透"
        "filler" -> "显示提示：填充内容"
        "music_offtopic" -> "显示提示：音乐-非音乐部分"
        else -> "显示提示：片段"
    }
}
