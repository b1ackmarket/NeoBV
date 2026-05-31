package dev.aaa1115910.bv.ui.state

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aaa1115910.biliapi.entity.AiAudioTranslation
import dev.aaa1115910.biliapi.entity.danmaku.DanmakuMask
import dev.aaa1115910.biliapi.entity.video.Subtitle
import dev.aaa1115910.biliapi.entity.video.VideoHeatmap
import dev.aaa1115910.biliapi.entity.video.VideoShot
import dev.aaa1115910.bilisubtitle.entity.SubtitleItem
import dev.aaa1115910.bv.component.controllers.DanmakuType
import dev.aaa1115910.bv.entity.Audio
import dev.aaa1115910.bv.entity.ProgressSegmentMark
import dev.aaa1115910.bv.entity.VideoAspectRatio
import dev.aaa1115910.bv.entity.VideoCodec
import dev.aaa1115910.bv.entity.VideoListItem
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.plugin.api.PluginPlaybackAction
import dev.aaa1115910.bv.repository.JumpModeQueueItem
import dev.aaa1115910.bv.repository.JumpModeSource

// 1. 核心 UI 状态 (低频更新)
data class PlayerUiState(
    // 视频信息
    val aid: Long = 0,
    val bvid: String = "",
    val cid: Long = 0,
    val epid: Int? = null,
    val seasonId: Int = 0,
    val authorMid: Long = 0,
    val authorName: String = "",
    val authorFace: String = "",
    val publishDateText: String = "",
    val playCountText: String = "",
    val title: String = "",
    val videoHeight: Int = 0,
    val videoWidth: Int = 0,
    val lastPlayed: Int = 0,
    val fromSeason: Boolean = false,
    val proxyArea: ProxyArea = ProxyArea.MainLand,
    val subType: Int = 0,

    // 播放状态
    val playerState: PlayerState = PlayerState.Ready,
    val isBuffering: Boolean = false, // 缓冲和暂停会同时出现，故单独列出
    // 进度条缩略图
    val videoShot: VideoShot? = null,
    val videoHeatmap: VideoHeatmap? = null,
    // 播放器时钟
    val clock: Pair<Int, Int> = Pair(0, 0),

    // 显示tip
    val showSkipToNextEp: Boolean = false,
    val showBackToStart: Boolean = false,
    val showPreviewTip: Boolean = false,
    val pendingPluginAction: PluginPlaybackAction.PromptSkip? = null,
    val pluginTipMessage: String? = null,
    val onlineCountText: String? = null,
    val showPlayerStats: Boolean = false,
    val mediaStatsInfo: String = "",
    val sponsorBlockProgressMarks: List<ProgressSegmentMark> = emptyList(),
    val watchedProgressMarks: List<ProgressSegmentMark> = emptyList(),
    val jumpModeState: JumpModeState = JumpModeState(),

    // 播放器配置与资源
    val availableQuality: Map<Int, String> = emptyMap(),
    val availableVideoCodec: List<VideoCodec> = emptyList(),
    val availableAudio: List<Audio> = emptyList(),
    val availableVideoList: List<VideoListItem> = emptyList(),

    // 相关视频
    val relatedVideos: List<VideoCardData> = emptyList(),
    val isFollowingUp: Boolean = false,

    // ==== 当前选中状态 ====

    // 媒体格式
    val mediaProfileState: MediaProfileState = MediaProfileState(),

    // 播放速度和宽高比
    val playSpeed: Float = 1f,
    val aspectRatio: VideoAspectRatio = VideoAspectRatio.Default,

    // 弹幕状态
    val danmakuState: DanmakuState = DanmakuState(),
    val danmakuMask: DanmakuMask? = null,

    // 字幕状态
    val subtitleState: SubtitleState = SubtitleState(),
    val subtitleId: Long = -1L,
    val subtitleMemory: SubtitleMemory? = null,
    val subtitleData: List<SubtitleItem> = emptyList(),
    val secondarySubtitleId: Long = -1L,
    val secondarySubtitleMemory: SubtitleMemory? = null,
    val secondarySubtitleCustom: Boolean = false,
    val secondarySubtitleData: List<SubtitleItem> = emptyList(),
    val subtitleList: List<Subtitle> = emptyList(),

    // AI 原声翻译
    val aiAudioTranslations: List<AiAudioTranslation> = emptyList(),
    val currentAiAudioLanguage: String = "",
)

// 2. 播放器进度条状态 (高频更新)
data class SeekerState(
    val totalDuration: Long = 0,
    val currentTime: Long = 0,
    val bufferedPercentage: Int = 0,
    val debugInfo: String = ""
)

data class JumpModeState(
    val available: Boolean = false,
    val enabled: Boolean = false,
    val source: JumpModeSource? = null,
    val currentIndex: Int = -1,
    val items: List<JumpModeQueueItem> = emptyList()
) {
    val hasPrevious: Boolean
        get() = available && currentIndex > 0

    val hasNext: Boolean
        get() = available && currentIndex in 0 until items.lastIndex
}

data class DanmakuState(
    val scale: Float = 0f,
    val opacity: Float = 0f,
    val area: Float = 0f,
    val speedFactor: Float = 1f,
    val maskEnabled: Boolean = false,
    val enabledTypes: List<DanmakuType> = emptyList(),
    val lastEnabledTypes: List<DanmakuType> = emptyList(),
)

data class SubtitleState(
    val fontSize: TextUnit = 0.sp,
    val opacity: Float = 0f,
    val bottomPadding: Dp = 0.dp
)

data class SubtitleMemory(
    val id: Long,
    val lang: String,
    val langDoc: String
)

data class MediaProfileState(
    val qualityId: Int = 0,
    val videoCodec: VideoCodec = VideoCodec.AVC,
    val audio: Audio = Audio.A192K
)

sealed class PlayerState {
    data object Ready: PlayerState()
    data object Playing: PlayerState()
    data object Paused: PlayerState()
    data object Ended: PlayerState()
    data class Error(val message: String): PlayerState()
}
