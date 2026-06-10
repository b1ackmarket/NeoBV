package dev.aaa1115910.bv.viewmodel.player

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuaishou.akdanmaku.DanmakuConfig
import com.kuaishou.akdanmaku.data.DanmakuItemData
import com.kuaishou.akdanmaku.ecs.component.filter.TypeFilter
import com.kuaishou.akdanmaku.render.SimpleRenderer
import com.kuaishou.akdanmaku.ui.DanmakuPlayer
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.entity.CodeType
import dev.aaa1115910.biliapi.entity.DashAudio
import dev.aaa1115910.biliapi.entity.DashVideo
import dev.aaa1115910.biliapi.entity.PlayData
import dev.aaa1115910.biliapi.util.AvBvConverter
import dev.aaa1115910.biliapi.entity.video.HeartbeatVideoType
import dev.aaa1115910.biliapi.entity.video.Subtitle
import dev.aaa1115910.biliapi.entity.video.SubtitleType
import dev.aaa1115910.biliapi.entity.video.VideoPage
import dev.aaa1115910.biliapi.http.util.toSmartDate
import dev.aaa1115910.biliapi.entity.user.SpaceVideoOrder
import dev.aaa1115910.biliapi.http.BiliHttpApi
import dev.aaa1115910.biliapi.http.entity.reply.ReplyItem
import dev.aaa1115910.biliapi.repositories.UserRepository
import dev.aaa1115910.biliapi.repositories.VideoPlayRepository
import dev.aaa1115910.bilisubtitle.SubtitleParser
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.cast.protocol.CastDirectMediaType
import dev.aaa1115910.bv.component.controllers.DanmakuType
import dev.aaa1115910.bv.danmaku.DanmakuFilterConfig
import dev.aaa1115910.bv.danmaku.DanmakuFilterMatcher
import dev.aaa1115910.bv.danmaku.buildDanmakuFilterRules
import dev.aaa1115910.bv.danmaku.cacheCloudDanmakuFilterRules
import dev.aaa1115910.bv.danmaku.currentDanmakuFilterUid
import dev.aaa1115910.bv.danmaku.readDanmakuFilterConfigFromPrefs
import dev.aaa1115910.bv.danmaku.shouldFetchCloudDanmakuFilterRules
import dev.aaa1115910.bv.danmaku.summarizeDanmakuFilterRules
import dev.aaa1115910.bv.entity.Audio
import dev.aaa1115910.bv.entity.PlayerCommentEmote
import dev.aaa1115910.bv.entity.PlayerCommentItem
import dev.aaa1115910.bv.entity.PlayerCommentPicture
import dev.aaa1115910.bv.entity.PlayerCommentSort
import dev.aaa1115910.bv.entity.PlayerType
import dev.aaa1115910.bv.entity.ProgressSegmentMark
import dev.aaa1115910.bv.entity.Resolution
import dev.aaa1115910.bv.entity.VideoAspectRatio
import dev.aaa1115910.bv.entity.VideoCodec
import dev.aaa1115910.bv.entity.VideoListItem
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.entity.toVideoQualityDisplayName
import dev.aaa1115910.bv.network.HttpServer
import dev.aaa1115910.bv.network.MpdGenerator
import dev.aaa1115910.bv.plugin.api.PlayerPluginContext
import dev.aaa1115910.bv.plugin.api.PluginPlaybackAction
import dev.aaa1115910.bv.plugin.core.PluginManager
import dev.aaa1115910.bv.plugin.impl.sponsorblock.SponsorBlockPlugin
import dev.aaa1115910.bv.player.AbstractVideoPlayer
import dev.aaa1115910.bv.player.VideoPlayerListener
import dev.aaa1115910.bv.player.VideoPlayerOptions
import dev.aaa1115910.bv.player.impl.exo.ExoPlayerFactory
import dev.aaa1115910.bv.repository.JumpModeQueue
import dev.aaa1115910.bv.repository.JumpModeQueueItem
import dev.aaa1115910.bv.repository.JumpModeRepository
import dev.aaa1115910.bv.repository.VideoInfoRepository
import dev.aaa1115910.bv.screen.settings.content.ActionAfterPlayItems
import dev.aaa1115910.bv.subtitle.translation.SubtitleTranslationManager
import dev.aaa1115910.bv.subtitle.translation.buildCachePrefix
import dev.aaa1115910.bv.subtitle.translation.readSubtitleTranslationConfigFromPrefs
import dev.aaa1115910.bv.subtitle.SecondarySubtitleOption
import dev.aaa1115910.bv.subtitle.buildSecondarySubtitleOptions
import dev.aaa1115910.bv.subtitle.resolveDefaultSecondarySubtitleOption
import dev.aaa1115910.bv.subtitle.resolveRememberedSecondarySubtitleOption
import dev.aaa1115910.bv.telemetry.FirebaseTelemetry
import dev.aaa1115910.bv.telemetry.TelemetryErrorType
import dev.aaa1115910.bv.ui.effect.PlayerUiEffect
import dev.aaa1115910.bv.ui.state.DanmakuState
import dev.aaa1115910.bv.ui.state.JumpModeState
import dev.aaa1115910.bv.ui.state.MediaProfileState
import dev.aaa1115910.bv.ui.state.PlayerState
import dev.aaa1115910.bv.ui.state.PlayerUiState
import dev.aaa1115910.bv.ui.state.SeekerState
import dev.aaa1115910.bv.ui.state.SubtitleMemory
import dev.aaa1115910.bv.ui.state.SubtitleState
import dev.aaa1115910.bv.util.PlayerUiTextFormatter
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.fException
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.fWarn
import dev.aaa1115910.bv.util.formatHourMinSec
import dev.aaa1115910.bv.util.toWanString
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.koin.android.annotation.KoinViewModel
import java.net.URI
import java.util.Calendar
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException

internal fun normalizeAvailableVideoCodecs(
    current: List<VideoCodec>,
    active: VideoCodec
): List<VideoCodec> {
    return if (current.contains(active)) current else current + active
}

internal fun normalizeSubtitleUrl(url: String): String {
    return if (url.startsWith("//")) {
        "https:$url"
    } else {
        url
    }
}

internal fun shouldApplyUpPanelLoadResult(
    requestedAuthorMid: Long,
    requestedOrder: SpaceVideoOrder,
    currentAuthorMid: Long,
    currentOrder: SpaceVideoOrder
): Boolean {
    return requestedAuthorMid == currentAuthorMid && requestedOrder == currentOrder
}

internal sealed interface NextPlayTarget {
    val title: String

    data class UgcPage(val parentVideo: VideoListItem, val page: VideoPage) : NextPlayTarget {
        override val title: String = page.title
    }

    data class VideoItem(val video: VideoListItem) : NextPlayTarget {
        override val title: String = video.title
    }
}

internal sealed interface AutoNextTarget {
    val aid: Long
    val cid: Long
    val title: String

    data class NextVideo(
        override val aid: Long,
        override val cid: Long,
        override val title: String,
        val epid: Int? = null,
        val seasonId: Int? = null
    ) : AutoNextTarget

    data class RelatedVideo(
        override val aid: Long,
        override val cid: Long,
        override val title: String
    ) : AutoNextTarget
}

internal fun resolveAutoNextTarget(state: PlayerUiState): AutoNextTarget? {
    val nextPlayTarget = resolveNextPlayTarget(state)
    if (nextPlayTarget != null) {
        return when (nextPlayTarget) {
            is NextPlayTarget.UgcPage -> AutoNextTarget.NextVideo(
                aid = nextPlayTarget.parentVideo.aid,
                cid = nextPlayTarget.page.cid,
                title = nextPlayTarget.title
            )

            is NextPlayTarget.VideoItem -> AutoNextTarget.NextVideo(
                aid = nextPlayTarget.video.aid,
                cid = nextPlayTarget.video.cid,
                title = nextPlayTarget.title,
                epid = nextPlayTarget.video.epid,
                seasonId = nextPlayTarget.video.seasonId
            )
        }
    }

    return state.relatedVideos.firstOrNull { (it.cid ?: 0L) > 0L }?.let { related ->
        AutoNextTarget.RelatedVideo(
            aid = related.avid,
            cid = related.cid!!,
            title = related.title
        )
    }
}

internal fun resolveNextPlayTarget(state: PlayerUiState): NextPlayTarget? {
    val videoList = state.availableVideoList
    val currentCid = state.cid
    val videoListIndex = videoList.indexOfFirst { it.aid == state.aid }
    val currentVideoItem = videoList.getOrNull(videoListIndex)

    if (currentVideoItem?.ugcPages?.isNotEmpty() == true) {
        val currentInnerIndex = currentVideoItem.ugcPages.indexOfFirst { it.cid == currentCid }
        if (currentInnerIndex != -1 && currentInnerIndex + 1 < currentVideoItem.ugcPages.size) {
            return NextPlayTarget.UgcPage(
                parentVideo = currentVideoItem,
                page = currentVideoItem.ugcPages[currentInnerIndex + 1]
            )
        }
    }

    if (videoListIndex != -1 && videoListIndex + 1 < videoList.size) {
        return NextPlayTarget.VideoItem(videoList[videoListIndex + 1])
    }

    return null
}

internal fun rememberSubtitleTrack(subtitle: Subtitle?): SubtitleMemory? {
    if (subtitle == null || subtitle.id == -1L) return null
    return SubtitleMemory(
        id = subtitle.id,
        lang = subtitle.lang,
        langDoc = subtitle.langDoc
    )
}

internal fun resolveRememberedSubtitleId(
    memory: SubtitleMemory?,
    tracks: List<Subtitle>
): Long? {
    if (memory == null) return null
    return tracks.firstOrNull { it.id == memory.id }?.id
        ?: tracks.firstOrNull { it.lang.isNotBlank() && it.lang == memory.lang }?.id
        ?: tracks.firstOrNull { it.langDoc.isNotBlank() && it.langDoc == memory.langDoc }?.id
        ?: tracks.firstOrNull { it.id != -1L }?.id
}

internal fun resolveAutoSubtitleTracks(
    tracks: List<Subtitle>,
    forceAiSubtitle: Boolean
): List<Subtitle> {
    if (!forceAiSubtitle) return tracks
    val aiTracks = tracks.filter { it.type == SubtitleType.AI }
    return aiTracks.ifEmpty { tracks }
}

internal fun resolvePreferredMainSubtitleId(
    memory: SubtitleMemory?,
    tracks: List<Subtitle>
): Long? {
    return resolveRememberedSubtitleId(memory, tracks)
        ?: tracks.firstOrNull { it.id != -1L }?.id
}

internal fun resolvePreferredSecondarySubtitleOption(
    tracks: List<Subtitle>,
    mainSubtitleId: Long,
    memory: SubtitleMemory?,
    config: dev.aaa1115910.bv.subtitle.translation.SubtitleTranslationConfig,
    preferCustom: Boolean,
    sourceSubtitleAvailable: Boolean
): SecondarySubtitleOption? {
    val options = buildSecondarySubtitleOptions(
        tracks = tracks,
        currentMainSubtitleId = mainSubtitleId,
        config = config,
        preferCustom = preferCustom,
        sourceSubtitleAvailable = sourceSubtitleAvailable
    )
    return resolveDefaultSecondarySubtitleOption(options, memory)
}

internal fun resolveOsdSecondarySubtitleOption(
    tracks: List<Subtitle>,
    mainSubtitleId: Long,
    memory: SubtitleMemory?,
    config: dev.aaa1115910.bv.subtitle.translation.SubtitleTranslationConfig,
    preferCustom: Boolean,
    sourceSubtitleAvailable: Boolean
): SecondarySubtitleOption? {
    val options = buildSecondarySubtitleOptions(
        tracks = tracks,
        currentMainSubtitleId = mainSubtitleId,
        config = config,
        preferCustom = preferCustom,
        sourceSubtitleAvailable = sourceSubtitleAvailable
    )
    return resolveRememberedSecondarySubtitleOption(options, memory)
}

internal const val CustomSubtitleTrackId = Long.MIN_VALUE

internal fun dev.aaa1115910.biliapi.http.entity.danmaku.DanmakuData.allowsDanmakuTypes(
    enabledTypes: List<DanmakuType>
): Boolean {
    if (enabledTypes.isEmpty()) return false
    return when (type) {
        4 -> enabledTypes.contains(DanmakuType.All) || enabledTypes.contains(DanmakuType.Bottom)
        5 -> enabledTypes.contains(DanmakuType.All) || enabledTypes.contains(DanmakuType.Top)
        else -> enabledTypes.contains(DanmakuType.All) || enabledTypes.contains(DanmakuType.Rolling)
    }
}

enum class SubtitleRole {
    Main,
    Secondary
}

internal fun disableAllSubtitles(state: PlayerUiState): PlayerUiState {
    val nextSubtitleMemory = if (state.subtitleId != -1L) {
        rememberSubtitleTrack(state.subtitleList.firstOrNull { it.id == state.subtitleId })
            ?: state.subtitleMemory
    } else {
        state.subtitleMemory
    }
    val nextSecondarySubtitleMemory = if (state.secondarySubtitleId != -1L) {
        rememberSubtitleTrack(state.subtitleList.firstOrNull { it.id == state.secondarySubtitleId })
            ?: state.secondarySubtitleMemory
    } else {
        state.secondarySubtitleMemory
    }

    return state.copy(
        subtitleId = -1L,
        subtitleMemory = nextSubtitleMemory,
        subtitleData = emptyList(),
        secondarySubtitleId = -1L,
        secondarySubtitleMemory = nextSecondarySubtitleMemory,
        secondarySubtitleCustom = false,
        secondarySubtitleData = emptyList()
    )
}

private fun AutoNextTarget.toVideoListItem(): VideoListItem {
    return when (this) {
        is AutoNextTarget.NextVideo -> VideoListItem(
            aid = aid,
            cid = cid,
            title = title,
            epid = epid,
            seasonId = seasonId
        )

        is AutoNextTarget.RelatedVideo -> VideoListItem(
            aid = aid,
            cid = cid,
            title = title
        )
    }
}

@KoinViewModel

class VideoPlayerV3ViewModel(
    private val videoInfoRepository: VideoInfoRepository,
    private val videoPlayRepository: VideoPlayRepository,
    private val userRepository: UserRepository,
    private val jumpModeRepository: JumpModeRepository
) : ViewModel() {
    private val logger = KotlinLogging.logger { }

    var videoPlayer: AbstractVideoPlayer? by mutableStateOf(null)
        private set
    var danmakuPlayer: DanmakuPlayer? by mutableStateOf(null)
        private set

    private var playData: PlayData? = null
    private var currentStreamCandidate: StreamCandidate? = null
    private var fallbackPlanner: PlaybackFallbackPlanner? = null
    private var isRetryingPlayback = false

    private val detachedWorkScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var danmakuConfig = DanmakuConfig()
    private val danmakuTypeFilter = TypeFilter()

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState = _uiState.asStateFlow()
    private val _seekerState = MutableStateFlow(SeekerState())
    val seekerState = _seekerState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<PlayerUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    private var seekerUpdateJob: Job? = null
    private var clockUpdateJob: Job? = null
    private var heartbeatJob: Job? = null
    private var loadVideoJob: Job? = null
    private var pluginPollingJob: Job? = null
    private var onlineCountJob: Job? = null
    private var upPanelLoadJob: Job? = null
    private var commentsLoadJob: Job? = null
    private var commentDetailLoadJob: Job? = null
    private var lastWatchedProgressPositionMs = 0L
    private var commentPage = 1
    private var commentsHasMore = true
    private val loadedCommentIds = mutableSetOf<String>()
    private var upPanelOrder by mutableStateOf(SpaceVideoOrder.PubDate)
    val isUpPanelLatestSelected: Boolean
        get() = upPanelOrder == SpaceVideoOrder.PubDate
    var upPanelVideos by mutableStateOf<List<VideoCardData>>(emptyList())
        private set
    var commentSort by mutableStateOf(PlayerCommentSort.Hot)
        private set
    var commentItems by mutableStateOf<List<PlayerCommentItem>>(emptyList())
        private set
    var commentListFirstVisibleItemIndex by mutableStateOf(0)
        private set
    var commentListFirstVisibleItemScrollOffset by mutableStateOf(0)
        private set
    var commentDetailRoot by mutableStateOf<PlayerCommentItem?>(null)
        private set
    var commentDetailReplies by mutableStateOf<List<PlayerCommentItem>>(emptyList())
        private set
    var commentDetailLoading by mutableStateOf(false)
        private set
    var commentDetailError by mutableStateOf<String?>(null)
        private set
    var commentTotalCountText by mutableStateOf("")
        private set
    var commentsLoading by mutableStateOf(false)
        private set
    var commentsError by mutableStateOf<String?>(null)
        private set
    val commentsCanLoadMore: Boolean
        get() = commentsHasMore

    private var backToStartCountdownJob: Job? = null
    private var playNextCountdownJob: Job? = null
    private var previewTipCountdownJob: Job? = null
    private val subtitleTranslationManager = SubtitleTranslationManager()
    private var lastSubtitleTranslationPreloadAtMs = 0L
    private var lastSubtitleTranslationPreloadPositionMs = Long.MIN_VALUE
    private var pendingBilingualSecondaryAfterMainLoad = false
    private var pendingCustomSecondaryAfterMainLoad = false
    private var externalMediaUrl: String? = null
    private var externalMediaBilibili = false
    private var externalMediaType = CastDirectMediaType.Unknown

    val isExternalMedia: Boolean
        get() = !externalMediaUrl.isNullOrBlank()

    private val videoPlayerListener = object : VideoPlayerListener {
        override fun onError(error: Exception) {
            logger.info { "onError: $error" }
            if (tryFallbackPlayback(error)) return
            FirebaseTelemetry.reportVideoError(
                type = TelemetryErrorType.DecodeError,
                throwable = error,
                extras = mapOf("stage" to "player_error")
            )
            _uiState.update {
                it.copy(
                    playerState = PlayerState.Error(
                        error.message ?: "Unknown error"
                    )
                )
            }
        }

        override fun onReady() {
            logger.info { "onReady" }
            _uiState.update { it.copy(playerState = PlayerState.Ready) }

            updatePlaySpeed(forceUpdate = true)
            startSeekerUpdater()
        }

        override fun onPlay() {
            logger.info { "onPlay" }
            danmakuPlayer?.start()
            _uiState.update { it.copy(playerState = PlayerState.Playing, isBuffering = false) }

            if (_uiState.value.lastPlayed > 0) {
                seekToLastPlayed()
                _uiState.update { it.copy(lastPlayed = 0) }
            }
        }

        override fun onPause() {
            logger.info { "onPause" }
            danmakuPlayer?.pause()
            _uiState.update { it.copy(playerState = PlayerState.Paused) }
        }

        override fun onBuffering() {
            logger.info { "onBuffering" }
            danmakuPlayer?.pause()
            _uiState.update { it.copy(isBuffering = true) }
        }

        override fun onEnd() {
            logger.info { "onEnd" }
            danmakuPlayer?.pause()
            stopSeekerUpdater()
            viewModelScope.launch(Dispatchers.IO) {
                PluginManager.getPlayerPlugins().forEach { plugin ->
                    runCatching { plugin.onPlaybackEnded() }
                        .onFailure { logger.fWarn { "Plugin ${plugin.id} onPlaybackEnded failed: ${it.message}" } }
                }
            }

            _uiState.update {
                it.copy(
                    playerState = PlayerState.Ended,
                    sponsorBlockProgressMarks = emptyList(),
                    watchedProgressMarks = emptyList(),
                    videoHeatmap = null,
                    videoProgressChapters = emptyList()
                )
            }
            viewModelScope.launch {
                _uiEffect.emit(PlayerUiEffect.PlayEnded)
            }
        }

        override fun onVideoSizeChanged(width: Int, height: Int) {
            if (width <= 0 || height <= 0) return
            _uiState.update {
                it.copy(videoWidth = width, videoHeight = height)
            }
        }

        override fun onSeekBack(seekBackIncrementMs: Long) {
        }

        override fun onSeekForward(seekForwardIncrementMs: Long) {
        }
    }

    private fun resetUpPanelVideos() {
        upPanelLoadJob?.cancel()
        upPanelLoadJob = null
        upPanelVideos = emptyList()
    }

    private fun resetComments() {
        commentsLoadJob?.cancel()
        commentsLoadJob = null
        commentDetailLoadJob?.cancel()
        commentDetailLoadJob = null
        commentItems = emptyList()
        commentListFirstVisibleItemIndex = 0
        commentListFirstVisibleItemScrollOffset = 0
        commentDetailRoot = null
        commentDetailReplies = emptyList()
        commentDetailLoading = false
        commentDetailError = null
        commentTotalCountText = ""
        commentsLoading = false
        commentsError = null
        commentPage = 1
        commentsHasMore = true
        loadedCommentIds.clear()
    }

    fun init(
        aid: Long,
        cid: Long,
        epid: Int?,
        title: String,
        lastPlayed: Int,
        fromSeason: Boolean,
        subType: Int,
        seasonId: Int,
        proxyArea: ProxyArea = ProxyArea.MainLand,
        authorMid: Long = 0,
        authorName: String,
        authorFace: String = ""
    ) {
        val jumpModeQueue = if (fromSeason) {
            jumpModeRepository.clearPendingQueue()
            null
        } else {
            jumpModeRepository.consumeQueueFor(aid)
        }

        _uiState.update {
            it.copy(
                aid = aid,
                bvid = AvBvConverter.av2bv(aid),
                cid = cid,
                epid = epid.takeIf { epid -> epid != 0 },
                seasonId = seasonId,
                title = title,
                lastPlayed = lastPlayed,
                fromSeason = fromSeason,
                subType = subType,
                isExternalMedia = false,
                proxyArea = proxyArea,
                authorMid = authorMid,
                authorName = authorName,
                authorFace = authorFace,
                mediaProfileState = MediaProfileState(
                    qualityId = Prefs.defaultQuality.code,
                    videoCodec = Prefs.defaultVideoCodec,
                    audio = Prefs.defaultAudio
                ),
                playSpeed = Prefs.defaultPlaySpeed.speed,
                danmakuState = DanmakuState(
                    scale = Prefs.defaultDanmakuScale,
                    opacity = Prefs.defaultDanmakuOpacity,
                    area = Prefs.defaultDanmakuArea,
                    speedFactor = Prefs.defaultDanmakuSpeedFactor,
                    maskEnabled = Prefs.defaultDanmakuMask,
                    enabledTypes = Prefs.defaultDanmakuTypes.takeIf { Prefs.defaultDanmakuEnabled }
                        ?: emptyList(),
                    lastEnabledTypes = Prefs.defaultDanmakuTypes.takeIf { it.isNotEmpty() } ?: DanmakuType.entries,
                ),
                subtitleState = SubtitleState(
                    fontSize = Prefs.defaultSubtitleFontSize,
                    opacity = Prefs.defaultSubtitleBackgroundOpacity,
                    bottomPadding = Prefs.defaultSubtitleBottomPadding
                ),
                showPlayerStats = Prefs.showPlayerStats,
                jumpModeState = jumpModeQueue?.toJumpModeState() ?: JumpModeState()
            )
        }

        resetUpPanelVideos()
        resetComments()

        startClockUpdater()

        videoInfoRepository.videoList
            .onEach { newList ->
                // 过滤DetailViewModel销毁时repo重置
                if (newList.isEmpty()) return@onEach

                _uiState.update { currentState ->
                    currentState.copy(availableVideoList = newList)
                }
                logger.fInfo { "Sync video list from repo, size: ${newList.size}" }
            }
            .launchIn(viewModelScope)

        videoInfoRepository.videoDetailState
            .filter { it?.aid == _uiState.value.aid }
            .onEach { newDetail ->
                // 过滤DetailViewModel销毁时repo重置
                if (newDetail == null) return@onEach

                _uiState.update { currentState ->
                    currentState.copy(
                        bvid = newDetail.bvid.orEmpty().ifBlank { currentState.bvid },
                        relatedVideos = newDetail.relatedVideos,
                        authorMid = newDetail.author.mid,
                        authorName = newDetail.author.name,
                        authorFace = newDetail.author.face,
                        publishDateText = newDetail.publishDate.time.toSmartDate().orEmpty(),
                        playCountText = PlayerUiTextFormatter.playCount(newDetail.stat.view)
                    )
                }
                refreshUpFollowState()
                logger.fInfo { "Sync related videos from repo" }
            }
            .launchIn(viewModelScope)
    }

    fun initExternalMedia(
        mediaUrl: String,
        title: String,
        lastPlayed: Int,
        isBilibiliMedia: Boolean = false,
        mediaType: String = CastDirectMediaType.Unknown.name,
        mediaCover: String = "",
        mediaCreator: String = ""
    ) {
        externalMediaUrl = mediaUrl
        externalMediaBilibili = isBilibiliMedia
        externalMediaType = runCatching { CastDirectMediaType.valueOf(mediaType) }.getOrDefault(CastDirectMediaType.Unknown)
        _uiState.update {
            it.copy(
                aid = 0,
                bvid = "",
                cid = 0,
                epid = null,
                seasonId = 0,
                title = title,
                lastPlayed = lastPlayed,
                fromSeason = false,
                subType = 0,
                isExternalMedia = true,
                isExternalAudio = externalMediaType == CastDirectMediaType.Audio,
                externalMediaCover = mediaCover,
                externalMediaCreator = mediaCreator,
                authorMid = 0,
                authorName = mediaCreator,
                authorFace = "",
                availableQuality = emptyMap(),
                availableVideoCodec = emptyList(),
                availableAudio = emptyList(),
                availableVideoList = emptyList(),
                relatedVideos = emptyList(),
                mediaProfileState = MediaProfileState(),
                playSpeed = Prefs.defaultPlaySpeed.speed,
                danmakuState = DanmakuState(),
                subtitleState = SubtitleState(
                    fontSize = Prefs.defaultSubtitleFontSize,
                    opacity = Prefs.defaultSubtitleBackgroundOpacity,
                    bottomPadding = Prefs.defaultSubtitleBottomPadding
                ),
                subtitleId = -1,
                subtitleMemory = null,
                subtitleData = emptyList(),
                secondarySubtitleId = -1,
                secondarySubtitleMemory = null,
                secondarySubtitleCustom = false,
                secondarySubtitleData = emptyList(),
                subtitleList = emptyList(),
                showPlayerStats = Prefs.showPlayerStats,
                jumpModeState = JumpModeState()
            )
        }
        resetUpPanelVideos()
        resetComments()
        startClockUpdater()
    }

    fun initVideoPlayer(context: Context) {
        logger.info { "Init video player: ${Prefs.playerType.name}" }

        val options = VideoPlayerOptions(
            userAgent = if (isExternalMedia) {
                if (externalMediaBilibili) {
                    context.getString(R.string.video_player_user_agent_http)
                } else {
                    "Mozilla/5.0"
                }
            } else {
                when (Prefs.playbackApiType) {
                    ApiType.Web -> context.getString(R.string.video_player_user_agent_http)
                    ApiType.App -> context.getString(R.string.video_player_user_agent_client)
                }
            },
            referer = if (isExternalMedia) {
                if (externalMediaBilibili) context.getString(R.string.video_player_referer) else null
            } else {
                when (Prefs.playbackApiType) {
                    ApiType.Web -> context.getString(R.string.video_player_referer)
                    ApiType.App -> null
                }
            },
            enableFfmpegAudioRenderer = Prefs.enableFfmpegAudioRenderer,
            enableSoftwareVideoDecoder = Prefs.enableSoftwareVideoDecoder,
            enableVolumeNormalization = Prefs.enableVolumeNormalization
        )

        val newVideoPlayer = when (Prefs.playerType) {
            PlayerType.Media3 -> ExoPlayerFactory().create(context.applicationContext, options)
        }

        newVideoPlayer.setPlayerEventListener(videoPlayerListener)
        videoPlayer = newVideoPlayer
    }

    fun detachPlayer() {
        syncProgress(scope = detachedWorkScope, isDetaching = true)

        videoPlayer?.release()
        videoPlayer = null
    }

    fun initDanmakuPlayer() {
        danmakuPlayer = DanmakuPlayer(SimpleRenderer())
        initDanmakuConfig()
    }

    fun releaseDanmakuPlayer() {
        danmakuPlayer?.release()
        danmakuPlayer = null
    }

    private fun resetCustomSubtitleTranslation() {
        lastSubtitleTranslationPreloadAtMs = 0L
        lastSubtitleTranslationPreloadPositionMs = Long.MIN_VALUE
        pendingCustomSecondaryAfterMainLoad = false
        subtitleTranslationManager.clear()
    }

    private fun resetPendingSecondarySubtitleRestore() {
        pendingBilingualSecondaryAfterMainLoad = false
        pendingCustomSecondaryAfterMainLoad = false
    }

    fun loadSubtitle(id: Long, role: SubtitleRole = SubtitleRole.Main) {
        if (id == CustomSubtitleTrackId && role == SubtitleRole.Secondary) {
            loadCustomTranslatedSubtitle()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            if (id == -1L) {
                _uiState.update { state ->
                    when (role) {
                        SubtitleRole.Main -> state.copy(
                            subtitleId = -1,
                            subtitleMemory = null,
                            subtitleData = emptyList()
                        )

                        SubtitleRole.Secondary -> state.copy(
                            secondarySubtitleId = -1,
                            secondarySubtitleMemory = null,
                            secondarySubtitleCustom = false,
                            secondarySubtitleData = emptyList()
                        )
                    }
                }
                if (role == SubtitleRole.Secondary) resetCustomSubtitleTranslation()
                if (role == SubtitleRole.Main && id == -1L) {
                    resetPendingSecondarySubtitleRestore()
                }
                return@launch
            }
            var subtitleName = ""
            runCatching {
                val subtitle =
                    _uiState.value.subtitleList.find { it.id == id } ?: return@runCatching
                subtitleName = subtitle.langDoc
                val subtitleMemory = rememberSubtitleTrack(subtitle)
                val subtitleUrl = normalizeSubtitleUrl(subtitle.url)
                logger.info { "Subtitle url: $subtitleUrl" }
                val client = HttpClient(OkHttp)
                val responseText = client.get(subtitleUrl).bodyAsText()
                client.close()
                val subtitleData = SubtitleParser.fromBccString(responseText)
                _uiState.update { state ->
                    when (role) {
                        SubtitleRole.Main -> state.copy(
                            subtitleId = id,
                            subtitleMemory = subtitleMemory,
                            subtitleData = subtitleData
                        )

                        SubtitleRole.Secondary -> state.copy(
                            secondarySubtitleId = id,
                            secondarySubtitleMemory = subtitleMemory,
                            secondarySubtitleCustom = false,
                            secondarySubtitleData = subtitleData
                        )
                    }
                }
                if (role == SubtitleRole.Secondary) resetCustomSubtitleTranslation()
                if (role == SubtitleRole.Main) {
                    if (pendingBilingualSecondaryAfterMainLoad) {
                        val shouldPreferCustom = Prefs.preferCustomSecondarySubtitle
                        val config = readSubtitleTranslationConfigFromPrefs()
                        val secondary = resolveOsdSecondarySubtitleOption(
                            tracks = _uiState.value.subtitleList,
                            mainSubtitleId = id,
                            memory = _uiState.value.secondarySubtitleMemory,
                            config = config,
                            preferCustom = shouldPreferCustom,
                            sourceSubtitleAvailable = subtitleData.isNotEmpty()
                        )
                        resetPendingSecondarySubtitleRestore()
                        when (secondary) {
                            SecondarySubtitleOption.CustomTranslation -> loadCustomTranslatedSubtitle()
                            is SecondarySubtitleOption.BiliTrack -> loadSubtitle(secondary.subtitle.id, SubtitleRole.Secondary)
                            null -> Unit
                        }
                    } else if (pendingCustomSecondaryAfterMainLoad) {
                        pendingCustomSecondaryAfterMainLoad = false
                        loadCustomTranslatedSubtitle()
                    } else if (_uiState.value.secondarySubtitleCustom) {
                        startCustomSubtitleTranslation(
                            currentPositionMs = videoPlayer?.currentPosition ?: _seekerState.value.currentTime,
                            restartInFlight = false
                        )
                    }
                }
            }.onFailure {
                logger.fInfo { "Load subtitle failed: ${it.stackTraceToString()}" }
            }.onSuccess {
                logger.fInfo { "Load subtitle $subtitleName success" }
            }
        }
    }

    private fun loadCustomTranslatedSubtitle() {
        val state = _uiState.value
        val config = readSubtitleTranslationConfigFromPrefs()

        if (!Prefs.enableBilingualSubtitle) {
            showToast("请先开启双语字幕")
            return
        }
        if (!config.verified()) {
            showToast("请先在 ${HttpServer.getServerAddress("/subtitle")} 测试并保存翻译配置")
            return
        }
        if (state.subtitleId == -1L || state.subtitleData.isEmpty()) {
            showToast("当前视频没有可翻译字幕")
            return
        }

        val currentPosition = videoPlayer?.currentPosition ?: _seekerState.value.currentTime
        val prefix = buildCachePrefix(
            aid = state.aid,
            cid = state.cid,
            subtitleId = state.subtitleId,
            config = config
        )
        subtitleTranslationManager.reset(prefix)
        _uiState.update {
            it.copy(
                secondarySubtitleId = CustomSubtitleTrackId,
                secondarySubtitleMemory = null,
                secondarySubtitleCustom = true,
                secondarySubtitleData = subtitleTranslationManager.buildTranslatedSubtitles(state.subtitleData)
            )
        }
        startCustomSubtitleTranslation(currentPosition, restartInFlight = true)
    }

    private fun maybePreloadCustomSubtitleTranslation(currentPositionMs: Long) {
        val state = _uiState.value
        if (!state.secondarySubtitleCustom || state.subtitleData.isEmpty()) return
        val now = System.currentTimeMillis()
        val positionDelta = kotlin.math.abs(currentPositionMs - lastSubtitleTranslationPreloadPositionMs)
        if (now - lastSubtitleTranslationPreloadAtMs < 3_000L && positionDelta < 8_000L) return
        startCustomSubtitleTranslation(currentPositionMs, restartInFlight = false)
    }

    private fun startCustomSubtitleTranslation(
        currentPositionMs: Long,
        restartInFlight: Boolean = true
    ) {
        val state = _uiState.value
        val config = readSubtitleTranslationConfigFromPrefs()
        if (!Prefs.enableBilingualSubtitle || !config.verified()) return
        if (!state.secondarySubtitleCustom || state.subtitleId == -1L || state.subtitleData.isEmpty()) return

        lastSubtitleTranslationPreloadAtMs = System.currentTimeMillis()
        lastSubtitleTranslationPreloadPositionMs = currentPositionMs
        subtitleTranslationManager.preload(
            sourceSubtitles = state.subtitleData,
            currentTimeMs = currentPositionMs,
            config = config,
            title = state.title,
            aid = state.aid,
            cid = state.cid,
            subtitleId = state.subtitleId,
            restartInFlight = restartInFlight,
            onUpdate = { translated ->
                _uiState.update { current ->
                    if (!current.secondarySubtitleCustom || current.subtitleId != state.subtitleId) {
                        current
                    } else {
                        current.copy(secondarySubtitleData = translated)
                    }
                }
            },
            onError = { error ->
                logger.fWarn { "Translate custom subtitle failed: ${error.stackTraceToString()}" }
                showToast("翻译字幕失败：${error.message ?: "未知错误"}")
            }
        )
    }

    fun toggleSubtitle() {
        val state = _uiState.value
        if (state.subtitleId != -1L || state.secondarySubtitleId != -1L) {
            _uiState.update { disableAllSubtitles(it) }
            return
        }

        val autoSubtitleTracks = resolveAutoSubtitleTracks(
            tracks = state.subtitleList,
            forceAiSubtitle = false
        )
        val targetSubtitleId = resolvePreferredMainSubtitleId(state.subtitleMemory, autoSubtitleTracks)

        if (targetSubtitleId == null) {
            showToast("当前视频没有字幕")
            return
        }

        loadSubtitle(targetSubtitleId)

        if (Prefs.enableBilingualSubtitle && Prefs.preferBilingualSubtitleOnOsd) {
            pendingBilingualSecondaryAfterMainLoad = true
        }
    }

    fun updatePlaySpeed(
        speed: Float? = null,
        forceUpdate: Boolean = false
    ) {
        val currentSpeed = _uiState.value.playSpeed
        val targetSpeed = speed ?: currentSpeed

        if (!forceUpdate && currentSpeed == targetSpeed) return

        _uiState.update { it.copy(playSpeed = targetSpeed) }
        videoPlayer?.speed = targetSpeed
        danmakuPlayer?.updatePlaySpeed(targetSpeed)
    }

    fun setShowPlayerStats(show: Boolean) {
        Prefs.showPlayerStats = show
        _uiState.update { it.copy(showPlayerStats = show) }
    }

    fun toggleJumpMode() {
        val jumpModeState = _uiState.value.jumpModeState
        if (!jumpModeState.available) {
            showToast("当前列表不支持跳动模式")
            return
        }

        val nextEnabled = !jumpModeState.enabled
        _uiState.update {
            it.copy(jumpModeState = it.jumpModeState.copy(enabled = nextEnabled))
        }
        showToast(if (nextEnabled) "进入跳动模式，按上下键切换视频" else "已退出跳动模式")
    }

    fun playJumpModeAdjacent(offset: Int, showBoundaryToast: Boolean = true): Boolean {
        val jumpModeState = _uiState.value.jumpModeState
        if (!jumpModeState.enabled) return false

        val targetIndex = jumpModeState.currentIndex + offset
        val targetItem = jumpModeState.items.getOrNull(targetIndex)
        if (targetItem == null) {
            if (showBoundaryToast) {
                showToast(if (offset < 0) "已经是第一条视频了" else "已经是最后一条视频了")
            }
            return false
        }

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                targetItem.resolveVideoListItem()
            }.onSuccess { resolvedVideo ->
                withContext(Dispatchers.Main) {
                    playNewVideo(resolvedVideo)
                }
            }.onFailure { error ->
                logger.fWarn {
                    "Resolve jump mode video failed. aid=${targetItem.aid} title=${targetItem.title} error=${error.message}"
                }
                showToast("跳动失败，没取到这个视频")
            }
        }
        return true
    }

    fun updateVideoAspectRatio(aspectRatio: VideoAspectRatio) {
        _uiState.update {
            it.copy(aspectRatio = aspectRatio)
        }
    }

    fun updateMediaProfile(action: MediaProfileSettingAction) {
        val old = _uiState.value.mediaProfileState
        val new = when (action) {
            is MediaProfileSettingAction.SetQuality -> old.copy(qualityId = action.value)
            is MediaProfileSettingAction.SetVideoCodec -> old.copy(videoCodec = action.value)
            is MediaProfileSettingAction.SetAudio -> old.copy(audio = action.value)
        }

        if (old == new) return

        _uiState.update { it.copy(mediaProfileState = new) }

        videoPlayer?.let { player ->
            player.pause()
            val currentPosition = player.currentPosition

            // 解析新配置下的 URL
            val mediaUrls = resolveMediaUrls(new.qualityId, new.videoCodec, new.audio)

            if (mediaUrls != null) {
                executePlayback(mediaUrls, startPositionMs = currentPosition.takeIf { it > 0L })
            }
        }
    }

    fun updateAiAudioTranslation(language: String) {
        val normalizedLanguage = language.trim()
        if (_uiState.value.currentAiAudioLanguage == normalizedLanguage) return

        val state = _uiState.value
        val player = videoPlayer
        val currentPosition = player?.currentPosition?.takeIf { it > 0L }
        player?.pause()

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val newPlayData = fetchPlayData(
                    avid = state.aid,
                    cid = state.cid,
                    epid = state.epid ?: 0,
                    preferApi = Prefs.playbackApiType,
                    proxyArea = state.proxyArea,
                    aiAudioLanguage = normalizedLanguage.takeIf { it.isNotBlank() }
                )
                playData = newPlayData
                val availableAudioList = buildAvailableAudioList(newPlayData)
                val targetAudio = calculateTargetAudio(availableAudioList, state.mediaProfileState.audio)
                _uiState.update {
                    it.copy(
                        availableAudio = availableAudioList,
                        aiAudioTranslations = newPlayData.aiAudioTranslations,
                        currentAiAudioLanguage = newPlayData.currentAiAudioLanguage,
                        mediaProfileState = it.mediaProfileState.copy(audio = targetAudio)
                    )
                }
                resolveMediaUrls(
                    qn = state.mediaProfileState.qualityId,
                    codec = state.mediaProfileState.videoCodec,
                    audio = targetAudio
                ) ?: throw IllegalStateException("AI 原声翻译播放源解析失败")
            }.onSuccess { mediaUrls ->
                withContext(Dispatchers.Main) {
                    executePlayback(mediaUrls, startPositionMs = currentPosition)
                }
            }.onFailure { error ->
                if (error is CancellationException) throw error
                logger.fWarn { "Switch AI audio translation failed: ${error.stackTraceToString()}" }
                showToast("AI原声翻译切换失败")
            }
        }
    }

    fun updateDanmakuState(action: DanmakuSettingAction) {
        val old = _uiState.value.danmakuState
        val new = when (action) {
            is DanmakuSettingAction.SetScale -> old.copy(scale = action.value)
            is DanmakuSettingAction.SetOpacity -> old.copy(opacity = action.value)
            is DanmakuSettingAction.SetArea -> old.copy(area = action.value)
            is DanmakuSettingAction.SetSpeedFactor -> old.copy(speedFactor = action.value)
            is DanmakuSettingAction.SetMaskEnabled -> old.copy(maskEnabled = action.enabled)
            is DanmakuSettingAction.SetEnabledTypes -> {
                if (action.types.isEmpty()) {
                    old.copy(enabledTypes = emptyList(), lastEnabledTypes = old.enabledTypes)
                } else {
                    old.copy(enabledTypes = action.types, lastEnabledTypes = action.types)
                }
            }
            DanmakuSettingAction.ToggleEnabled -> {
                if (old.enabledTypes.isEmpty()) {
                    old.copy(
                        enabledTypes = old.lastEnabledTypes
                            .takeIf { it.isNotEmpty() }
                            ?: Prefs.defaultDanmakuTypes.takeIf { it.isNotEmpty() }
                            ?: DanmakuType.entries
                    )
                } else {
                    old.copy(enabledTypes = emptyList(), lastEnabledTypes = old.enabledTypes)
                }
            }
        }

        if (old == new) return

        // 首先更新UI
        _uiState.update { it.copy(danmakuState = new) }

        // ===== 副作用处理 =====
        val persistEnabledTypes = action !is DanmakuSettingAction.SetEnabledTypes || action.persist
        if (new.enabledTypes != old.enabledTypes) {
            updateDanmakuConfigTypeFilter(new.enabledTypes)
            if (persistEnabledTypes) {
                Prefs.defaultDanmakuEnabled = new.enabledTypes.isNotEmpty()
                if (new.enabledTypes.isNotEmpty()) {
                    Prefs.defaultDanmakuTypes = new.enabledTypes
                }
            }
        }
        if (new.scale != old.scale) {
            updateDanmakuScale(new.scale)
            Prefs.defaultDanmakuScale = new.scale
        }
        if (new.speedFactor != old.speedFactor) {
            updateDanmakuSpeedFactor(new.speedFactor)
            Prefs.defaultDanmakuSpeedFactor = new.speedFactor
        }
        if (new.area != old.area) {
            updateDanmakuArea(new.area)
            Prefs.defaultDanmakuArea = new.area
        }
        if (new.opacity != old.opacity) {
            Prefs.defaultDanmakuOpacity = new.opacity
        }
        if (new.maskEnabled != old.maskEnabled) {
            Prefs.defaultDanmakuMask = new.maskEnabled
        }
    }

    fun updateSubtitleState(action: SubtitleSettingAction) {
        val old = _uiState.value.subtitleState
        val new = when (action) {
            is SubtitleSettingAction.SetFontSize -> old.copy(fontSize = action.value)
            is SubtitleSettingAction.SetOpacity -> old.copy(opacity = action.value)
            is SubtitleSettingAction.SetBottomPadding -> old.copy(bottomPadding = action.value)
        }

        if (old == new) return

        _uiState.update { it.copy(subtitleState = new) }

        // ===== 持久化副作用 =====
        if (new.fontSize != old.fontSize) {
            Prefs.defaultSubtitleFontSize = new.fontSize
        }

        if (new.opacity != old.opacity) {
            Prefs.defaultSubtitleBackgroundOpacity = new.opacity
        }

        if (new.bottomPadding != old.bottomPadding) {
            Prefs.defaultSubtitleBottomPadding = new.bottomPadding
        }
    }

    /**
     * 触发播放结束后的检查逻辑
     */
    fun checkAndPlayNext() {
        if (playJumpModeAdjacent(offset = 1, showBoundaryToast = false)) return

        when (Prefs.actionAfterPlay) {
            ActionAfterPlayItems.Pause -> return
            ActionAfterPlayItems.Exit -> {
                viewModelScope.launch {
                    _uiEffect.emit(PlayerUiEffect.FinishActivity)
                }

                return
            }

            ActionAfterPlayItems.AutoNextOrRelated -> {
                /* 继续执行 */
            }
        }

        when (val target = resolveAutoNextTarget(_uiState.value)) {
            is AutoNextTarget.NextVideo -> startNextEpisodeCountdown(target.toVideoListItem())
            is AutoNextTarget.RelatedVideo -> {
                viewModelScope.launch {
                    _uiEffect.emit(PlayerUiEffect.ShowRecommendedVideos)
                }
            }

            null -> viewModelScope.launch {
                _uiEffect.emit(PlayerUiEffect.FinishActivity)
            }
        }
    }

    fun cancelPlayNext() {
        playNextCountdownJob?.cancel()
        _uiState.update { it.copy(showSkipToNextEp = false) }
    }

    fun backToStart() {
        backToStartCountdownJob?.cancel()
        _uiState.update { it.copy(showBackToStart = false) }

        videoPlayer?.seekTo(0)
        danmakuPlayer?.seekTo(0)
        // akdanmaku 会在跳转后立即播放，如果需要缓冲则会导致弹幕不同步
        danmakuPlayer?.pause()
    }

    /**
     * 开始周期性更新播放进度
     */
    fun startSeekerUpdater() {
        // 防止重复启动
        if (seekerUpdateJob?.isActive == true) return

        seekerUpdateJob = viewModelScope.launch(Dispatchers.Main) {
            while (isActive) {
                updateSeekerState()
                delay(100)
            }
        }
        startPluginPolling()
        startOnlineCountPolling()
    }

    fun seekToTime(time: Long) {
        videoPlayer?.seekTo(time)
        _seekerState.update { it.copy(currentTime = time) }
        danmakuPlayer?.seekTo(time)
        // akdanmaku 会在跳转后立即播放，如果需要缓冲则会导致弹幕不同步
        danmakuPlayer?.pause()
        startCustomSubtitleTranslation(time, restartInFlight = true)
    }

    fun confirmPendingPluginAction() {
        val action = _uiState.value.pendingPluginAction ?: return
        videoPlayer?.seekTo(action.targetPositionMs)
        danmakuPlayer?.seekTo(action.targetPositionMs)
        danmakuPlayer?.pause()
        PluginManager.getPlayerPlugin<SponsorBlockPlugin>("sponsorblock")?.markHandled(action.segmentId)
        _uiState.update {
            it.copy(
                pendingPluginAction = null,
                pluginTipMessage = "已跳过片段"
            )
        }
        startTransientPluginTipCountdown()
    }

    fun dismissPendingPluginAction() {
        _uiState.value.pendingPluginAction?.let { action ->
            PluginManager.getPlayerPlugin<SponsorBlockPlugin>("sponsorblock")
                ?.dismissPrompt(action.segmentId)
        }
        _uiState.update { it.copy(pendingPluginAction = null, pluginTipMessage = null) }
    }

    fun loadUpPanelVideos() {
        val authorMid = _uiState.value.authorMid
        val requestedOrder = upPanelOrder
        resetUpPanelVideos()
        if (authorMid == 0L) return
        upPanelLoadJob = viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val data = userRepository.getSpaceVideos(
                    mid = authorMid,
                    order = requestedOrder,
                    preferApiType = Prefs.playbackApiType
                )
                if (!shouldApplyUpPanelLoadResult(
                        requestedAuthorMid = authorMid,
                        requestedOrder = requestedOrder,
                        currentAuthorMid = _uiState.value.authorMid,
                        currentOrder = upPanelOrder
                    )
                ) {
                    upPanelLoadJob = null
                    logger.fInfo {
                        "Ignore stale up panel load result. requestedMid=$authorMid requestedOrder=$requestedOrder " +
                            "currentMid=${_uiState.value.authorMid} currentOrder=$upPanelOrder"
                    }
                    return@runCatching
                }
                upPanelVideos = data.videos.map { item ->
                    VideoCardData(
                        avid = item.aid,
                        cid = item.cid,
                        title = item.title,
                        cover = item.cover,
                        upName = item.author,
                        upMid = authorMid,
                        playString = item.play.takeIf { it != -1 }.toWanString(),
                        danmakuString = item.danmaku.takeIf { it != -1 }.toWanString(),
                        timeString = (item.duration * 1000L).formatHourMinSec(),
                        pubTime = item.pubTime
                    )
                }
                upPanelLoadJob = null
            }.onFailure { error ->
                if (error is CancellationException) {
                    logger.fInfo { "Up panel load cancelled. mid=$authorMid order=$requestedOrder" }
                    return@onFailure
                }
                upPanelLoadJob = null
                logger.fWarn { "Load up panel videos failed: ${error.message}" }
            }
        }
    }

    fun toggleUpPanelSort() {
        upPanelOrder = if (upPanelOrder == SpaceVideoOrder.PubDate) {
            SpaceVideoOrder.Click
        } else {
            SpaceVideoOrder.PubDate
        }
        loadUpPanelVideos()
    }

    fun loadComments(force: Boolean = false, loadMore: Boolean = false) {
        val aid = _uiState.value.aid
        if (aid <= 0L) return
        if (commentsLoading) return
        if (loadMore && !commentsHasMore) return
        if (!force && !loadMore && commentItems.isNotEmpty()) return
        if (force) {
            commentPage = 1
            commentsHasMore = true
            loadedCommentIds.clear()
        }
        commentsLoadJob?.cancel()
        commentsLoading = true
        commentsError = null
        val requestedAid = aid
        val requestedSort = commentSort
        val requestedPage = if (loadMore) commentPage else 1
        commentsLoadJob = viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                BiliHttpApi.getVideoReplyList(
                    oid = requestedAid,
                    type = 1,
                    sort = requestedSort.toReplyApiSort(),
                    page = requestedPage,
                    pageSize = 20,
                    sessData = Prefs.sessData
                ).getResponseData()
            }.onSuccess { data ->
                if (_uiState.value.aid != requestedAid || commentSort != requestedSort) return@onSuccess
                val topReply = data.top?.upper
                val replies = buildList {
                    if (requestedPage == 1) topReply?.let { add(it) }
                    data.replies.orEmpty().forEach { reply ->
                        if (reply.rpid != topReply?.rpid) add(reply)
                    }
                }
                val newItems = replies
                    .map { it.toPlayerCommentItem() }
                    .filter { loadedCommentIds.add(it.id) }
                commentItems = if (loadMore) commentItems + newItems else newItems
                commentTotalCountText = formatCommentTotalCount(data.page?.count)
                commentsHasMore = newItems.isNotEmpty() &&
                    commentItems.size < (data.page?.count ?: Int.MAX_VALUE)
                if (commentsHasMore) commentPage = requestedPage + 1
                commentsLoading = false
                commentsLoadJob = null
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                if (_uiState.value.aid != requestedAid || commentSort != requestedSort) return@onFailure
                commentsError = error.message ?: "评论加载失败"
                commentsLoading = false
                commentsLoadJob = null
            }
        }
    }

    fun toggleCommentSort() {
        commentSort = if (commentSort == PlayerCommentSort.Hot) {
            PlayerCommentSort.Latest
        } else {
            PlayerCommentSort.Hot
        }
        loadComments(force = true)
    }

    fun loadMoreComments() {
        loadComments(loadMore = true)
    }

    fun updateCommentListPosition(index: Int, offset: Int) {
        commentListFirstVisibleItemIndex = index
        commentListFirstVisibleItemScrollOffset = offset
    }

    fun openCommentDetail(comment: PlayerCommentItem) {
        val aid = _uiState.value.aid
        val root = comment.id.toLongOrNull() ?: return
        if (aid <= 0L || root <= 0L) return
        commentDetailLoadJob?.cancel()
        commentDetailRoot = comment
        commentDetailReplies = emptyList()
        commentDetailLoading = true
        commentDetailError = null
        val requestedAid = aid
        commentDetailLoadJob = viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                BiliHttpApi.getVideoReplyDetail(
                    oid = requestedAid,
                    root = root,
                    type = 1,
                    page = 1,
                    pageSize = 20,
                    sessData = Prefs.sessData
                ).getResponseData()
            }.onSuccess { data ->
                if (_uiState.value.aid != requestedAid || commentDetailRoot?.id != comment.id) return@onSuccess
                commentDetailReplies = data.replies.orEmpty()
                    .filter { it.rpid != root }
                    .map { it.toPlayerCommentItem() }
                commentDetailLoading = false
                commentDetailLoadJob = null
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                if (_uiState.value.aid != requestedAid || commentDetailRoot?.id != comment.id) return@onFailure
                commentDetailError = error.message ?: "回复加载失败"
                commentDetailLoading = false
                commentDetailLoadJob = null
            }
        }
    }

    fun closeCommentDetail() {
        commentDetailLoadJob?.cancel()
        commentDetailLoadJob = null
        commentDetailRoot = null
        commentDetailReplies = emptyList()
        commentDetailLoading = false
        commentDetailError = null
    }

    fun showCommentActionToast(action: String) {
        viewModelScope.launch {
            _uiEffect.emit(PlayerUiEffect.ShowToast("$action 评论功能暂不可用"))
        }
    }

    fun toggleUpPanelFollow() {
        val authorMid = _uiState.value.authorMid
        if (authorMid == 0L) return
        val targetFollowState = !_uiState.value.isFollowingUp

        viewModelScope.launch(Dispatchers.IO) {
            val success = if (targetFollowState) {
                userRepository.followUser(
                    mid = authorMid,
                    preferApiType = Prefs.playbackApiType
                )
            } else {
                userRepository.unfollowUser(
                    mid = authorMid,
                    preferApiType = Prefs.playbackApiType
                )
            }

            if (success) {
                _uiState.update { it.copy(isFollowingUp = targetFollowState) }
            } else {
                refreshUpFollowState()
            }
        }
    }

    fun playNewVideo(newVideo: VideoListItem) {
        videoPlayer?.pause()
        resetUpPanelVideos()
        resetComments()
        resetCustomSubtitleTranslation()
        viewModelScope.launch(Dispatchers.IO) {
            PluginManager.getPlayerPlugins().forEach { plugin ->
                runCatching { plugin.onPlaybackEnded() }
                    .onFailure { logger.fWarn { "Plugin ${plugin.id} reset before switching video failed: ${it.message}" } }
            }
        }
        resetWatchedProgress()
        _uiState.update {
            it.copy(
                sponsorBlockProgressMarks = emptyList(),
                watchedProgressMarks = emptyList(),
                videoHeatmap = null,
                videoProgressChapters = emptyList()
            )
        }

        val state = _uiState.value

        val shouldUpdateVideoDetail = state.aid != newVideo.aid
        val shouldUpdateVideoList = !state.availableVideoList.any { it.aid == newVideo.aid }

        // 切换视频时更新detail
        if (shouldUpdateVideoDetail) {
            viewModelScope.launch(Dispatchers.IO) {
                videoInfoRepository.loadVideoDetail(newVideo.aid, Prefs.playbackApiType)
            }
        }

        // 新视频不在当前视频列表时更新列表
        if (shouldUpdateVideoList) {
            videoInfoRepository.updateVideoList(listOf(newVideo))
        }

        // 更新播放历史并上传
        syncProgress(viewModelScope)

        // 重置弹幕
        releaseDanmakuPlayer()
        initDanmakuPlayer()

        // 更新UiState
        _uiState.update {
            it.copyForVideoSwitch(
                newVideo = newVideo,
                clearDetailMetadata = shouldUpdateVideoDetail
            )
        }

        // 加载新播放url
        loadVideoWithResources()
    }

    override fun onCleared() {
        subtitleTranslationManager.release()
        detachedWorkScope.cancel()
        super.onCleared()
    }

    fun playUpPanelVideoByAid(aid: Long, fallbackTitle: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                videoInfoRepository.resolveDefaultVideoListItem(
                    aid = aid,
                    fallbackTitle = fallbackTitle,
                    preferApiType = Prefs.playbackApiType
                )
            }.onSuccess { resolvedVideo ->
                withContext(Dispatchers.Main) {
                    playNewVideo(resolvedVideo)
                }
            }.onFailure { error ->
                logger.fWarn {
                    "Resolve up panel video cid failed. aid=$aid title=$fallbackTitle error=${error.message}"
                }
            }
        }
    }

    private suspend fun JumpModeQueueItem.resolveVideoListItem(): VideoListItem {
        val resolvedCid = cid?.takeIf { it > 0 }
        return if (resolvedCid != null) {
            VideoListItem(
                aid = aid,
                cid = resolvedCid,
                title = title
            )
        } else {
            videoInfoRepository.resolveDefaultVideoListItem(
                aid = aid,
                fallbackTitle = title,
                preferApiType = Prefs.playbackApiType
            )
        }
    }

    private fun showToast(message: String) {
        viewModelScope.launch {
            _uiEffect.emit(PlayerUiEffect.ShowToast(message))
        }
    }

    fun trySendHeartbeat() {
        syncProgress(scope = viewModelScope, updateLocal = false)
    }

    fun loadVideoWithResources() {
        val externalUrl = externalMediaUrl
        if (!externalUrl.isNullOrBlank()) {
            loadExternalMedia(externalUrl)
            return
        }

        val state = _uiState.value
        val avid = state.aid
        val cid = state.cid
        val epid = state.epid

        loadVideoJob?.cancel()
        resetWatchedProgress()
        _uiState.update {
            it.copy(
                sponsorBlockProgressMarks = emptyList(),
                watchedProgressMarks = emptyList(),
                videoHeatmap = null,
                videoProgressChapters = emptyList()
            )
        }
        loadVideoJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                resolveUrlsAndPlay(avid, cid, epid)
                notifyPluginsVideoLoaded()

                launch {
                    updateSubtitle()
                }
                launch { loadDanmaku(avid, cid) }
                launch { updateDanmakuMask() }
                launch { updateVideoShot() }
                launch { updateVideoHeatmap() }
                launch { updateVideoProgressChapters() }
                launch {
                    ensureVideoDetailLoadedForPlayer(avid)
                    updateVideoPages()
                }
                launch { refreshOnlineCount() }
            } catch (e: CancellationException) {
                throw e // 让结构化并发正常取消，不作为播放错误处理
            } catch (e: Exception) {
                logger.error(e) { "Loading video data error: $e" }
                FirebaseTelemetry.reportVideoError(
                    type = FirebaseTelemetry.classifyThrowable(e),
                    throwable = e,
                    extras = mapOf("stage" to "load_video")
                )

                _uiState.update {
                    it.copy(playerState = PlayerState.Error(e.message ?: "未知错误"))
                }
            }
        }
    }

    private fun loadExternalMedia(mediaUrl: String) {
        loadVideoJob?.cancel()
        resetWatchedProgress()
        _uiState.update {
            it.copy(
                sponsorBlockProgressMarks = emptyList(),
                watchedProgressMarks = emptyList(),
                videoHeatmap = null,
                videoProgressChapters = emptyList(),
                subtitleData = emptyList(),
                secondarySubtitleData = emptyList(),
                isBuffering = false
            )
        }
        loadVideoJob = viewModelScope.launch(Dispatchers.Main) {
            runCatching {
                executePlayback(
                    MediaUrls(
                        videoUrl = mediaUrl,
                        audioUrl = null,
                        useDashMpd = externalMediaType == CastDirectMediaType.Dash ||
                            mediaUrl.substringBefore('?').endsWith(".mpd", ignoreCase = true),
                        useHls = externalMediaType == CastDirectMediaType.Hls
                    )
                )
            }.onFailure { error ->
                logger.error(error) { "Loading external cast media failed: ${error.message}" }
                _uiState.update {
                    it.copy(playerState = PlayerState.Error(error.message ?: "外部投屏播放失败"))
                }
            }
        }
    }

    private suspend fun resolveUrlsAndPlay(avid: Long, cid: Long, epid: Int? = 0) {
        try {
            val mediaUrls = fetchMediaUrls(avid, cid, epid ?: 0)

            withContext(Dispatchers.Main) {
                executePlayback(mediaUrls)
                logger.info { "Video source loaded successfully" }
            }
        } catch (e: CancellationException) {
            throw e // 重新抛出，让结构化并发正常传播取消信号
        } catch (e: Exception) {
            logger.error(e) { "Failed to load media: ${e.message}" }
            FirebaseTelemetry.reportVideoError(
                type = FirebaseTelemetry.classifyThrowable(e),
                throwable = e,
                extras = mapOf("stage" to "resolve_play_url")
            )
            // 保留原始异常作为 cause，上层 catch 可通过 e.cause 获取根因
            throw IllegalStateException("${e.message}", e)
        }

    }

    private suspend fun fetchMediaUrls(avid: Long, cid: Long, epid: Int): MediaUrls {
        val config = loadPlaybackConfig(
            avid, cid, epid,
            Prefs.playbackApiType,
            _uiState.value.proxyArea
        )

        return resolveMediaUrls(
            config.qn,
            config.codec,
            config.audio
        ) ?: throw IllegalStateException("视频源解析失败")
    }

    private suspend fun loadPlaybackConfig(
        avid: Long,
        cid: Long,
        epid: Int = 0,
        preferApi: ApiType = Prefs.playbackApiType,
        proxyArea: ProxyArea = ProxyArea.MainLand
    ): PlaybackConfig {
        logger.fInfo { "Load play url: [av=$avid, cid=$cid, preferApi=$preferApi, proxyArea=$proxyArea]" }

        return runCatching {
            // 1. 获取播放数据
            val playData = fetchPlayData(avid, cid, epid, preferApi, proxyArea)
            this@VideoPlayerV3ViewModel.playData = playData

            logger.fInfo { "Load play data response success. Play data: $playData" }

            // 2. 解析并去重可用的清晰度 (使用 associate 替代 forEach + mutableMap)
            val resolutionMap = playData.dashVideos.associate { video ->
                video.quality to video.quality.toVideoQualityDisplayName(
                    context = BVApp.context,
                    apiDescription = playData.qualityDescriptions[video.quality]
                )
            }
            logger.fInfo { "Video available resolution: $resolutionMap" }

            // 3. 解析并去重可用的音质 (使用 buildList 和 distinct 替代 forEach + mutableList)
            val availableAudioList = buildAvailableAudioList(playData)

            logger.fInfo { "Video available audio: $availableAudioList" }

            // 4. 计算目标清晰度、音质和编码 (已抽取业务逻辑)
            val preferredQualityId = _uiState.value.mediaProfileState.qualityId
            val targetQualityId =
                calculateTargetQuality(
                    availableQualities = resolutionMap.keys,
                    defaultQualityCode = preferredQualityId.takeIf { it > 0 } ?: Prefs.defaultQuality.code
                )
            val targetAudio = calculateTargetAudio(availableAudioList, Prefs.defaultAudio)
            val targetCodec = getTargetVideoCodec()

            // 5. 统一批量更新 UI State (避免多次触发重组)
            _uiState.update {
                it.copy(
                    availableQuality = resolutionMap,
                    availableAudio = availableAudioList,
                    aiAudioTranslations = playData.aiAudioTranslations,
                    currentAiAudioLanguage = playData.currentAiAudioLanguage,
                    mediaProfileState = it.mediaProfileState.copy(
                        qualityId = targetQualityId,
                        audio = targetAudio
                    )
                )
            }

            // 6. 付费视频预览状态提示
            if (playData.needPay) {
                startShowPreviewTipCountdown()
            }

            PlaybackConfig(
                qn = targetQualityId,
                codec = targetCodec,
                audio = targetAudio
            )
        }.onFailure { throwable ->
            logger.fException(throwable) { "Load video failed" }
        }.onSuccess {
            logger.fInfo { "Load play url success" }
        }.getOrThrow()
    }

    private suspend fun fetchPlayData(
        avid: Long,
        cid: Long,
        epid: Int,
        preferApi: ApiType,
        proxyArea: ProxyArea,
        aiAudioLanguage: String? = _uiState.value.currentAiAudioLanguage.takeIf { it.isNotBlank() }
    ): PlayData {
        return if (_uiState.value.fromSeason) {
            videoPlayRepository.getPgcPlayData(
                aid = avid,
                cid = cid,
                epid = epid,
                preferCodec = Prefs.defaultVideoCodec.toBiliApiCodeType(),
                preferApiType = preferApi,
                enableProxy = Prefs.enableProxy,
                proxyArea = proxyArea.toQueryParam(),
                curAiAudioLanguage = aiAudioLanguage
            )
        } else {
            videoPlayRepository.getPlayData(
                aid = avid,
                cid = cid,
                curAiAudioLanguage = aiAudioLanguage,
                preferApiType = preferApi
            )
        }
    }

    private fun buildAvailableAudioList(playData: PlayData): List<Audio> {
        return buildList {
            addAll(playData.dashAudios.map { Audio.fromCode(it.codecId) })
            playData.dolby?.let { add(Audio.fromCode(it.codecId)) }
            playData.flac?.let { add(Audio.fromCode(it.codecId)) }
        }.distinct()
    }

    private fun calculateTargetQuality(availableQualities: Set<Int>, defaultQualityCode: Int): Int {
        if (availableQualities.contains(defaultQualityCode)) return defaultQualityCode

        val sortedQualities = availableQualities.sorted()
        return sortedQualities.findLast { it <= defaultQualityCode }
            ?: sortedQualities.firstOrNull()
            ?: 0
    }

    private fun calculateTargetAudio(availableAudio: List<Audio>, defaultAudio: Audio): Audio {
        if (availableAudio.contains(defaultAudio)) return defaultAudio

        // Fallback 逻辑
        return when {
            defaultAudio == Audio.ADolbyAtoms && availableAudio.contains(Audio.AHiRes) -> Audio.AHiRes
            defaultAudio == Audio.AHiRes && availableAudio.contains(Audio.ADolbyAtoms) -> Audio.ADolbyAtoms
            availableAudio.contains(Audio.A192K) -> Audio.A192K
            availableAudio.contains(Audio.A132K) -> Audio.A132K
            availableAudio.contains(Audio.A64K) -> Audio.A64K
            else -> availableAudio.firstOrNull() ?: Audio.A132K
        }
    }

    private fun getTargetVideoCodec(): VideoCodec? {
        val state = _uiState.value
        val playData = playData ?: return null

        if (Prefs.playbackApiType == ApiType.App && playData.codec.isEmpty()) {
            val videoItem = playData.dashVideos
                .find { it.quality == state.mediaProfileState.qualityId }
                ?: playData.dashVideos.firstOrNull()
                ?: return null

            val codec = VideoCodec.fromCodecId(videoItem.codecId)
            _uiState.update {
                it.copy(
                    availableVideoCodec = normalizeAvailableVideoCodecs(
                        current = listOf(codec),
                        active = codec
                    ),
                    mediaProfileState = it.mediaProfileState.copy(
                        videoCodec = VideoCodec.fromCodecId(videoItem.codecId)
                    )
                )
            }
            return codec
        }

        val supportedCodec = playData.codec
        val codecList = supportedCodec[state.mediaProfileState.qualityId]
            ?.mapNotNull { VideoCodec.fromCodecString(it) }
            ?.takeIf { it.isNotEmpty() }
            ?: return null

        val targetVideoCodec = if (codecList.contains(Prefs.defaultVideoCodec)) {
            Prefs.defaultVideoCodec
        } else {
            codecList.minByOrNull { it.ordinal } ?: return null
        }

        _uiState.update {
            it.copy(
                availableVideoCodec = normalizeAvailableVideoCodecs(
                    current = codecList,
                    active = targetVideoCodec
                ),
                mediaProfileState = it.mediaProfileState.copy(videoCodec = targetVideoCodec)
            )
        }
        logger.fInfo { "Select codec: $targetVideoCodec" }
        return targetVideoCodec
    }

    private fun resolveMediaUrls(
        qn: Int? = null,
        codec: VideoCodec? = null,
        audio: Audio? = null
    ): MediaUrls? {
        val currentPlayData = playData ?: return null

        val state = _uiState.value

        val targetQn = qn ?: state.mediaProfileState.qualityId
        val targetCodec = codec ?: state.mediaProfileState.videoCodec
        val targetAudio = audio ?: state.mediaProfileState.audio

        logger.fInfo {
            "Video quality：${state.availableQuality[targetQn]}, video encoding：$targetCodec"
        }
        logger.fInfo { "Available dash videos count: ${currentPlayData.dashVideos.size}" }

        val audioItem = currentPlayData.dashAudios.find { it.codecId == targetAudio.code }
            ?: currentPlayData.dolby.takeIf { it?.codecId == targetAudio.code }
            ?: currentPlayData.flac.takeIf { it?.codecId == targetAudio.code }
            ?: currentPlayData.dashAudios.minByOrNull { it.codecId }

        var audioUrl: String? = audioItem?.baseUrl
        val audioUrls = mutableListOf<String>()
        audioItem?.baseUrl?.let { audioUrls.add(it) }
        audioUrls.addAll(audioItem?.backUrl ?: emptyList())

        val planner = PlaybackFallbackPlanner(
            candidates = currentPlayData.dashVideos.map { video ->
                StreamCandidate(
                    quality = video.quality,
                    codecPrefix = VideoCodec.fromCodecString(video.codecs.orEmpty())?.prefix.orEmpty(),
                    videoUrl = video.baseUrl,
                    audioUrl = audioUrl
                )
            }
        )

        val preferredCandidate = planner.selectPreferred(
            quality = targetQn,
            codecPrefix = targetCodec.prefix
        )
        fallbackPlanner = planner
        currentStreamCandidate = preferredCandidate
        logger.fInfo {
            "Preferred candidate: ${preferredCandidate?.quality}/${preferredCandidate?.codecPrefix} " +
                "video=${preferredCandidate?.videoUrl}"
        }

        val actualVideoItem = currentPlayData.dashVideos.firstOrNull { video ->
            preferredCandidate != null &&
                video.quality == preferredCandidate.quality &&
                video.baseUrl == preferredCandidate.videoUrl
        } ?: currentPlayData.dashVideos.firstOrNull() ?: run {
            logger.fWarn { "No available video stream found" }
            return null
        }

        var videoUrl = actualVideoItem.baseUrl
        val videoUrls = mutableListOf<String?>()
        videoUrls.add(actualVideoItem.baseUrl)
        videoUrls.addAll(actualVideoItem.backUrl)

        logger.fInfo { "all video hosts: ${videoUrls.map { with(URI(it)) { "$scheme://$authority" } }}" }
        logger.fInfo { "all audio hosts: ${audioUrls.map { with(URI(it)) { "$scheme://$authority" } }}" }

        //replace cdn
        if (Prefs.enableProxy && state.proxyArea != ProxyArea.MainLand) {
            videoUrl = videoUrl.replaceUrlDomainWithAliCdn()
            audioUrl = audioUrl?.replaceUrlDomainWithAliCdn()
        } else {
            // 如果未通过网络代理获得播放地址，才判断是否应该替换为官方 cdn
            videoUrl = selectOfficialCdnUrl(videoUrls.filterNotNull())
            audioUrl = if (audioUrls.isNotEmpty()) selectOfficialCdnUrl(audioUrls) else null
        }

        logger.fInfo { "Audio encoding：${(Audio.fromCode(audioItem?.codecId ?: 0))}" }
        logger.info { "Video url: $videoUrl" }
        logger.info { "Audio url: $audioUrl" }

        _uiState.update {
            val actualCodec = VideoCodec.fromCodecString(actualVideoItem.codecs.orEmpty())
            val mediaProfileState = actualCodec?.let { codecValue ->
                it.mediaProfileState.copy(videoCodec = codecValue)
            } ?: it.mediaProfileState
            it.copy(
                availableVideoCodec = normalizeAvailableVideoCodecs(
                    current = it.availableVideoCodec,
                    active = mediaProfileState.videoCodec
                ),
                mediaProfileState = mediaProfileState,
                videoHeight = actualVideoItem.height,
                videoWidth = actualVideoItem.width,
                mediaStatsInfo = buildBiliMediaStatsInfo(
                    video = actualVideoItem,
                    audio = audioItem
                )
            )
        }

        val shouldUseDashMpd =
            actualVideoItem.quality >= Resolution.R8K.code &&
                !actualVideoItem.initialization.isNullOrBlank() &&
                !actualVideoItem.indexRange.isNullOrBlank()

        val playbackUrl = if (shouldUseDashMpd) {
            val mpdContent = MpdGenerator.generate(
                videoUrl = videoUrl,
                audioUrl = audioUrl,
                videoCodec = actualVideoItem.codecs.orEmpty().ifBlank { targetCodec.prefix },
                width = actualVideoItem.width,
                height = actualVideoItem.height,
                frameRate = actualVideoItem.frameRate,
                bandwidth = actualVideoItem.bandwidth,
                initialization = actualVideoItem.initialization,
                indexRange = actualVideoItem.indexRange
            )
            HttpServer.setMpdContent(mpdContent)
            HttpServer.getMpdUrl()
        } else {
            videoUrl
        }

        return MediaUrls(
            videoUrl = playbackUrl,
            audioUrl = if (shouldUseDashMpd) null else audioUrl,
            useDashMpd = shouldUseDashMpd
        )
    }

    private fun tryFallbackPlayback(error: Exception): Boolean {
        if (isRetryingPlayback) return false
        val planner = fallbackPlanner ?: return false
        val currentCandidate = currentStreamCandidate ?: return false
        val nextCandidate = planner.nextAfterFailure(currentCandidate) ?: return false
        val currentAudio = _uiState.value.mediaProfileState.audio
        logger.fWarn {
            "Retry playback after error. from=${currentCandidate.quality}/${currentCandidate.codecPrefix} " +
                "to=${nextCandidate.quality}/${nextCandidate.codecPrefix}. error=${error.message}"
        }

        isRetryingPlayback = true
        viewModelScope.launch(Dispatchers.Main) {
            runCatching {
                val mediaUrls = resolveMediaUrls(
                    qn = nextCandidate.quality,
                    codec = VideoCodec.fromCodecString(nextCandidate.codecPrefix) ?: _uiState.value.mediaProfileState.videoCodec,
                    audio = currentAudio
                ) ?: return@runCatching
                executePlayback(mediaUrls)
            }.onSuccess {
                _uiState.update { it.copy(pluginTipMessage = null) }
            }.onFailure {
                logger.fWarn { "Fallback playback failed: ${it.message}" }
            }
            isRetryingPlayback = false
        }
        return true
    }

    private fun executePlayback(mediaUrls: MediaUrls, startPositionMs: Long? = null) {
        val player = videoPlayer ?: run {
            logger.error { "VideoPlayer is not initialized!" }
            return
        }

        logger.info { "Execute playback -> Video: ${mediaUrls.videoUrl}, Audio: ${mediaUrls.audioUrl}, dash=${mediaUrls.useDashMpd}" }
        logger.fInfo { "Current stream candidate before play: $currentStreamCandidate" }
        if (mediaUrls.useDashMpd) {
            player.playDash(mediaUrls.videoUrl)
        } else if (mediaUrls.useHls) {
            player.playHls(mediaUrls.videoUrl)
        } else {
            player.playUrl(mediaUrls.videoUrl, mediaUrls.audioUrl)
        }
        player.prepare()
        if (startPositionMs != null && startPositionMs > 0L) {
            player.seekTo(startPositionMs)
        }
        player.start()
    }

    // 加载合集内的分P
    private suspend fun updateVideoPages() {
        videoInfoRepository.updateUgcPages(Prefs.playbackApiType)
    }

    private suspend fun ensureVideoDetailLoadedForPlayer(aid: Long) {
        if (aid <= 0L) return
        val currentDetail = videoInfoRepository.videoDetailState.value
        val currentList = videoInfoRepository.videoList.value
        if (currentDetail?.aid == aid && currentList.isNotEmpty()) return
        runCatching {
            videoInfoRepository.loadVideoDetail(aid, Prefs.playbackApiType)
        }.onFailure { error ->
            if (error is CancellationException) throw error
            logger.fWarn { "Preload video detail for player failed. aid=$aid error=${error.message}" }
        }
    }

    private suspend fun loadDanmaku(aid: Long, cid: Long) {
        if (_uiState.value.danmakuState.enabledTypes.isEmpty()) {
            withContext(Dispatchers.Main) {
                updateDanmakuConfigTypeFilter(emptyList())
            }
            logger.fInfo { "Skip loading danmaku because it is disabled" }
            return
        }

        val list = runCatching {
            val danmakuData = BiliHttpApi.getDanmakuXml(cid = cid, sessData = Prefs.sessData).data
            val filterConfig = readDanmakuFilterConfigFromPrefs()
            val cloudRules = loadCloudDanmakuFilterRules(filterConfig)
            val rules = buildDanmakuFilterRules(filterConfig, cloudRules)
            val filterMatcher = DanmakuFilterMatcher(filterConfig, rules)
            val summary = summarizeDanmakuFilterRules(filterConfig, cloudRules)
            val enabledTypes = _uiState.value.danmakuState.enabledTypes
            var blockedCount = 0

            danmakuData.asSequence().filterNot {
                val blocked = filterMatcher.blocks(it) ||
                    !it.allowsDanmakuTypes(enabledTypes)
                if (blocked) blockedCount++
                blocked
            }.map {
                DanmakuItemData(
                    danmakuId = it.dmid,
                    position = (it.time * 1000).toLong(),
                    content = it.text,
                    mode = when (it.type) {
                        4 -> DanmakuItemData.DANMAKU_MODE_CENTER_BOTTOM
                        5 -> DanmakuItemData.DANMAKU_MODE_CENTER_TOP
                        else -> DanmakuItemData.DANMAKU_MODE_ROLLING
                    },
                    textSize = it.size,
                    textColor = Color(it.color).toArgb()
                )
            }.toList().also {
                logger.fInfo {
                    "Apply danmaku filters: cloud=${summary.cloudRuleCount}, " +
                        "localKeyword=${summary.localKeywordCount}, localRegex=${summary.localRegexCount}, " +
                        "localUser=${summary.localUserCount}, blocked=$blockedCount"
                }
            }
        }.onFailure { error ->
            logger.fWarn { "Load danmaku failed: ${error.stackTraceToString()}" }
        }.getOrNull() ?: return

        withContext(Dispatchers.Main) {
            danmakuPlayer?.updateData(list)
            logger.fInfo { "Load danmaku success, size: ${list.size}" }
        }
    }

    private suspend fun loadCloudDanmakuFilterRules(
        config: DanmakuFilterConfig
    ): List<dev.aaa1115910.biliapi.http.entity.danmaku.DanmakuFilterRuleData> {
        if (!config.enabled || !config.syncCloudRules) return emptyList()
        val uid = currentDanmakuFilterUid() ?: return emptyList()
        if (!shouldFetchCloudDanmakuFilterRules()) return emptyList()
        return runCatching {
            videoPlayRepository.getDanmakuFilterRules()
                .also { cacheCloudDanmakuFilterRules(uid, it) }
        }.onFailure {
            logger.fWarn { "Load cloud danmaku filters failed: ${it.message}" }
        }.getOrDefault(emptyList())
    }

    private suspend fun updateSubtitle() {
        val state = _uiState.value

        runCatching {
            val subtitleList = videoPlayRepository.getSubtitle(
                aid = state.aid,
                cid = state.cid,
                preferApiType = Prefs.playbackApiType
            )
            _uiState.update { currentState ->
                currentState.copy(
                    subtitleList = subtitleList
                )
            }
            logger.fInfo { "Update subtitle size: ${subtitleList.size}" }
            val autoSubtitleTracks = resolveAutoSubtitleTracks(
                tracks = subtitleList,
                forceAiSubtitle = false
            )
            val mainSubtitleId = resolveRememberedSubtitleId(_uiState.value.subtitleMemory, autoSubtitleTracks)
            mainSubtitleId?.let { subtitleId ->
                logger.info { "Restore remembered subtitle: $subtitleId" }
                loadSubtitle(subtitleId)
            }
        }.onFailure {
            logger.fWarn { "Update subtitle failed: ${it.stackTraceToString()}" }
        }
    }

    private fun syncProgress(
        scope: CoroutineScope,
        updateLocal: Boolean = true,
        isDetaching: Boolean = false
    ) {
        val player = videoPlayer ?: return
        val state = _uiState.value

        val currentTime = (player.currentPosition.coerceAtLeast(0) / 1000).toInt()
        val totalTime = (player.duration.coerceAtLeast(0) / 1000).toInt()
        val reportTime = if (currentTime >= totalTime) -1 else currentTime

        if (updateLocal) {
            videoInfoRepository.updateHistory(
                progress = reportTime,
                lastPlayedCid = state.cid
            )
        }

        if (!Prefs.incognitoMode) {
            heartbeatJob?.cancel()
            heartbeatJob = scope.launch(Dispatchers.IO) {
                try {
                    if (isDetaching) {
                        withTimeout(3000L) { uploadHistory(state, reportTime) }
                    } else {
                        uploadHistory(state, reportTime)
                    }
                } catch (e: Exception) {
                    logger.warn { "Failed to upload history: $e" }
                }
            }
        }
    }

    private suspend fun uploadHistory(uiState: PlayerUiState, time: Int) {
        try {
            with(uiState) {
                val currentApiType = Prefs.playbackApiType

                if (!fromSeason) {
                    logger.info { "Send heartbeat: [avid=$aid, cid=$cid, time=$time]" }
                    videoPlayRepository.sendHeartbeat(
                        aid = aid,
                        cid = cid,
                        time = time,
                        preferApiType = currentApiType
                    )
                } else {
                    logger.info { "Send heartbeat: [avid=$aid, cid=$cid, epid=$epid, sid=$seasonId, time=$time]" }
                    videoPlayRepository.sendHeartbeat(
                        aid = aid,
                        cid = cid,
                        time = time,
                        type = HeartbeatVideoType.Season,
                        subType = subType,
                        epid = epid,
                        seasonId = seasonId,
                        preferApiType = currentApiType
                    )
                }
            }
            logger.info { "Send heartbeat success" }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.warn { "Send heartbeat failed: ${e.stackTraceToString()}" }
        }
    }

    private suspend fun updateDanmakuMask() {
        val state = _uiState.value

        runCatching {
            val mask = videoPlayRepository.getDanmakuMask(
                aid = state.aid,
                cid = state.cid,
                preferApiType = Prefs.playbackApiType
            )

            _uiState.update { it.copy(danmakuMask = mask) }

            logger.fInfo { "Load danmaku mask segments: ${mask?.segmentCount ?: 0}" }
        }.onFailure {
            logger.fWarn { "Load danmaku mask failed: ${it.stackTraceToString()}" }
        }
    }

    private suspend fun updateVideoShot() {
        val state = _uiState.value
        runCatching {
            val videoShot = videoPlayRepository.getVideoShot(
                aid = state.aid,
                cid = state.cid,
                preferApiType = Prefs.playbackApiType
            )
            _uiState.update { it.copy(videoShot = videoShot) }
            logger.fInfo { "Load video shot success" }
        }.onFailure { err ->
            logger.fWarn { "Load video shot failed: ${err.stackTraceToString()}" }
        }
    }

    private suspend fun updateVideoHeatmap() {
        val state = _uiState.value
        runCatching {
            val videoHeatmap = videoPlayRepository.getVideoHeatmap(
                bvid = state.bvid,
                cid = state.cid
            )
            _uiState.update { it.copy(videoHeatmap = videoHeatmap) }
            logger.fInfo { "Load video heatmap points: ${videoHeatmap?.points?.size ?: 0}" }
        }.onFailure { err ->
            logger.fWarn { "Load video heatmap failed: ${err.stackTraceToString()}" }
        }
    }

    private suspend fun updateVideoProgressChapters() {
        val state = _uiState.value
        runCatching {
            val chapters = videoPlayRepository.getVideoProgressChapters(
                aid = state.aid,
                cid = state.cid,
                durationMs = maxOf(
                    videoPlayer?.duration?.coerceAtLeast(0L) ?: 0L,
                    seekerState.value.totalDuration
                )
            )
            _uiState.update { it.copy(videoProgressChapters = chapters) }
            logger.fInfo { "Load video progress chapters: ${chapters.size}" }
        }.onFailure { err ->
            logger.fWarn { "Load video progress chapters failed: ${err.stackTraceToString()}" }
        }
    }

    private fun initDanmakuConfig() {
        val state = _uiState.value.danmakuState
        val danmakuTypes = state.enabledTypes
        val area = state.area.takeIf { it > 0f } ?: Prefs.defaultDanmakuArea
        val scale = state.scale.takeIf { it > 0f } ?: Prefs.defaultDanmakuScale
        val factor = state.speedFactor.takeIf { it > 0f } ?: Prefs.defaultDanmakuSpeedFactor

        danmakuTypeFilter.clear()
        if (!danmakuTypes.contains(DanmakuType.All)) {
            val types = DanmakuType.entries.toMutableList()
            types.remove(DanmakuType.All)
            types.removeAll(danmakuTypes)
            val filterTypes = types.mapNotNull {
                when (it) {
                    DanmakuType.Rolling -> DanmakuItemData.DANMAKU_MODE_ROLLING
                    DanmakuType.Top -> DanmakuItemData.DANMAKU_MODE_CENTER_TOP
                    DanmakuType.Bottom -> DanmakuItemData.DANMAKU_MODE_CENTER_BOTTOM
                    else -> null
                }
            }
            filterTypes.forEach { danmakuTypeFilter.addFilterItem(it) }
        }
        danmakuConfig = danmakuConfig.copy(
            density = 120,
            textSizeScale = scale,
            screenPart = area,
            dataFilter = listOf(danmakuTypeFilter),
            rollingSpeedFactor = factor
        )
        danmakuConfig.updateFilter()
        logger.info { "Init danmaku config: $danmakuConfig" }
        danmakuPlayer?.updateConfig(danmakuConfig)
    }

    private fun updateDanmakuConfigTypeFilter(enabledDanmakuTypes: List<DanmakuType>) {
        danmakuTypeFilter.clear()

        if (!enabledDanmakuTypes.contains(DanmakuType.All)) {
            val types = DanmakuType.entries.toMutableList()
            types.remove(DanmakuType.All)
            types.removeAll(enabledDanmakuTypes)
            val filterTypes = types.mapNotNull {
                when (it) {
                    DanmakuType.Rolling -> DanmakuItemData.DANMAKU_MODE_ROLLING
                    DanmakuType.Top -> DanmakuItemData.DANMAKU_MODE_CENTER_TOP
                    DanmakuType.Bottom -> DanmakuItemData.DANMAKU_MODE_CENTER_BOTTOM
                    else -> null
                }
            }
            filterTypes.forEach { danmakuTypeFilter.addFilterItem(it) }
        }
        logger.info { "Update danmaku type filters: ${danmakuTypeFilter.filterSet}" }
        danmakuConfig.updateFilter()
        danmakuPlayer?.updateConfig(danmakuConfig)
    }

    private fun updateDanmakuArea(area: Float) {
        logger.info { "Update danmaku area: $area" }

        danmakuConfig = danmakuConfig.copy(
            screenPart = area
        )
        danmakuPlayer?.updateConfig(danmakuConfig)

        // 更新弹幕库之后updateConfig会导致滚动速度被重置，所以这里需要重新设置
        danmakuPlayer?.setDanmakuRollingSpeed(_uiState.value.danmakuState.speedFactor)
    }

    private fun updateDanmakuScale(scale: Float) {
        logger.info { "Update danmaku config: $danmakuConfig" }

        danmakuConfig = danmakuConfig.copy(
            textSizeScale = scale,
        )
        danmakuPlayer?.updateConfig(danmakuConfig)

        // 更新弹幕库之后updateConfig会导致滚动速度被重置，所以这里需要重新设置
        danmakuPlayer?.setDanmakuRollingSpeed(_uiState.value.danmakuState.speedFactor)
    }

    private fun updateDanmakuSpeedFactor(factor: Float) {
        logger.info { "Update danmaku rolling speed factor: $factor" }
        _uiState.update { it.copy(danmakuState = it.danmakuState.copy(speedFactor = factor)) }

        danmakuPlayer?.setDanmakuRollingSpeed(factor)
    }

    private fun startNextEpisodeCountdown(target: VideoListItem) {
        playNextCountdownJob?.cancel()

        playNextCountdownJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    showSkipToNextEp = true,
                )
            }
            delay(5000)

            playNewVideo(target)
            _uiState.update { it.copy(showSkipToNextEp = false) }
        }
    }

    private fun startShowPreviewTipCountdown() {
        previewTipCountdownJob?.cancel()

        previewTipCountdownJob = viewModelScope.launch {
            _uiState.update {
                it.copy(showPreviewTip = true)
            }

            delay(5000)

            _uiState.update {
                it.copy(showPreviewTip = false)
            }
        }
    }

    private fun seekToLastPlayed() {
        val time = _uiState.value.lastPlayed.toLong()
        logger.fInfo { "Back to history: ${time.formatHourMinSec()}" }

        videoPlayer?.seekTo(time)
        danmakuPlayer?.seekTo(time)
        // akdanmaku 会在跳转后立即播放，如果需要缓冲则会导致弹幕不同步
        danmakuPlayer?.pause()

        _uiState.update { it.copy(showBackToStart = true) }

        backToStartCountdownJob?.cancel()
        backToStartCountdownJob = viewModelScope.launch {
            delay(5000)
            _uiState.update { it.copy(showBackToStart = false) }
        }
    }

    private fun stopSeekerUpdater() {
        seekerUpdateJob?.cancel()
        seekerUpdateJob = null
        pluginPollingJob?.cancel()
        pluginPollingJob = null
        onlineCountJob?.cancel()
        onlineCountJob = null
    }

    private fun updateSeekerState() {
        val player = videoPlayer ?: return

        val currentPos = player.currentPosition.coerceAtLeast(0L)
        val duration = player.duration.coerceAtLeast(0L)
        updateWatchedProgress(currentPos, duration)
        _seekerState.update {
            it.copy(
                totalDuration = duration,
                currentTime = currentPos,
                bufferedPercentage = player.bufferedPercentage,
                debugInfo = listOf(
                    _uiState.value.mediaStatsInfo,
                    player.debugInfo.toPlayerStatsDebugText()
                ).filter { info -> info.isNotBlank() }.joinToString("\n")
            )
        }
        maybePreloadCustomSubtitleTranslation(currentPos)
    }

    private fun resetWatchedProgress(positionMs: Long = 0L) {
        lastWatchedProgressPositionMs = positionMs
        _uiState.update { it.copy(watchedProgressMarks = emptyList()) }
    }

    private fun updateWatchedProgress(currentPos: Long, duration: Long) {
        val isPlaying = _uiState.value.playerState == PlayerState.Playing && videoPlayer?.isPlaying == true
        if (!isPlaying || duration <= 0L) {
            lastWatchedProgressPositionMs = currentPos
            return
        }

        val previous = lastWatchedProgressPositionMs
        lastWatchedProgressPositionMs = currentPos
        val delta = currentPos - previous
        if (previous <= 0L || delta !in 1L..1_500L) return

        val start = previous.coerceIn(0L, duration)
        val end = currentPos.coerceIn(0L, duration)
        if (end <= start) return

        val nextRanges = mergeWatchedProgressMarks(
            marks = _uiState.value.watchedProgressMarks,
            newMark = ProgressSegmentMark(
                startMs = start,
                endMs = end,
                colorArgb = 0L
            )
        )
        if (nextRanges != _uiState.value.watchedProgressMarks) {
            _uiState.update { it.copy(watchedProgressMarks = nextRanges) }
        }
    }

    private fun startPluginPolling() {
        if (pluginPollingJob?.isActive == true) return
        pluginPollingJob = viewModelScope.launch(Dispatchers.Main) {
            while (isActive) {
                processPluginPlaybackActions()
                delay(300)
            }
        }
    }

    private fun startOnlineCountPolling() {
        if (onlineCountJob?.isActive == true) return
        onlineCountJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                refreshOnlineCount()
                delay(60_000)
            }
        }
    }

    private suspend fun refreshOnlineCount() {
        val state = _uiState.value
        val aid = state.aid
        val cid = state.cid
        val onlineCountText = videoPlayRepository.getOnlineCountText(
            aid = aid,
            cid = cid
        )

        if (onlineCountText == null) {
            logger.fWarn { "Update online count failed or empty. aid=$aid cid=$cid" }
            return
        }

        _uiState.update { currentState ->
            if (currentState.aid == aid && currentState.cid == cid) {
                currentState.copy(onlineCountText = onlineCountText)
            } else {
                currentState
            }
        }
    }

    private fun refreshUpFollowState() {
        val authorMid = _uiState.value.authorMid
        if (authorMid == 0L) return

        viewModelScope.launch(Dispatchers.IO) {
            val isFollowing = userRepository.checkIsFollowing(
                mid = authorMid,
                preferApiType = Prefs.playbackApiType
            )
            _uiState.update { it.copy(isFollowingUp = isFollowing ?: false) }
        }
    }

    private suspend fun notifyPluginsVideoLoaded() {
        val state = _uiState.value
        val context = PlayerPluginContext(
            aid = state.aid,
            cid = state.cid,
            bvid = state.bvid,
            title = state.title,
            fromSeason = state.fromSeason
        )
        PluginManager.getPlayerPlugins().forEach { plugin ->
            runCatching { plugin.onVideoLoaded(context) }
                .onFailure { logger.fWarn { "Plugin ${plugin.id} onVideoLoaded failed: ${it.message}" } }
        }
        _uiState.update {
            it.copy(
                sponsorBlockProgressMarks = PluginManager
                    .getPlayerPlugin<SponsorBlockPlugin>("sponsorblock")
                    ?.progressMarks()
                    .orEmpty()
            )
        }
    }

    private suspend fun processPluginPlaybackActions() {
        val player = videoPlayer ?: return
        val positionMs = player.currentPosition
        val pendingAction = _uiState.value.pendingPluginAction
        if (pendingAction != null) {
            val stillInsidePromptWindow =
                positionMs in pendingAction.startPositionMs..<pendingAction.targetPositionMs
            if (!stillInsidePromptWindow) {
                _uiState.update { it.copy(pendingPluginAction = null, pluginTipMessage = null) }
            }
            return
        }
        if (positionMs < 5_000L) return
        PluginManager.getPlayerPlugins().forEach { plugin ->
            when (val action = runCatching { plugin.onPlaybackPosition(positionMs) }.getOrNull()) {
                is PluginPlaybackAction.AutoSkip -> {
                    player.seekTo(action.targetPositionMs)
                    danmakuPlayer?.seekTo(action.targetPositionMs)
                    danmakuPlayer?.pause()
                    _uiState.update {
                        it.copy(
                            pendingPluginAction = null,
                            pluginTipMessage = action.message
                        )
                    }
                    startTransientPluginTipCountdown()
                    return
                }

                is PluginPlaybackAction.PromptSkip -> {
                    _uiState.update {
                        it.copy(
                            pendingPluginAction = action,
                            pluginTipMessage = "点击 OK 键后跳过片段"
                        )
                    }
                    return
                }

                else -> Unit
            }
        }
    }

    private fun startTransientPluginTipCountdown() {
        previewTipCountdownJob?.cancel()
        previewTipCountdownJob = viewModelScope.launch {
            delay(2500)
            _uiState.update { it.copy(pluginTipMessage = null) }
        }
    }

    private fun startClockUpdater() {
        clockUpdateJob?.cancel()
        clockUpdateJob = viewModelScope.launch(Dispatchers.Main) {
            while (isActive) {
                updateClock()
                delay(1000)
            }
        }
    }

    private fun updateClock() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        _uiState.update { it.copy(clock = Pair(hour, minute)) }
    }

    private fun selectOfficialCdnUrl(urls: List<String>): String {
        if (!Prefs.preferOfficialCdn) {
            logger.fInfo { "doesn't need to filter official cdn url, select the first url" }
            return urls.first()
        }
        val filteredUrls = urls
            .filter { !it.contains(".mcdn.bilivideo.") }
            .filter { !it.contains(".szbdyd.com") }
            .filter {
                !Regex("^(https?://)?(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}(:\\d{1,5})?)(/[a-zA-Z0-9_./-]*)?(\\?.*)?$")
                    .matches(it)
            }
        if (filteredUrls.isEmpty()) {
            logger.fInfo { "doesn't find any official cdn url, select the first url" }
            return urls.first()
        } else {
            logger.fInfo { "filtered official cdn urls: $filteredUrls" }
            return filteredUrls.first()
        }
    }

    private fun String.replaceUrlDomainWithAliCdn(): String {
        val replaceDomainKeywords = listOf(
            "mirroraliov",
            "mirrorakam"
        )
        if (replaceDomainKeywords.none { this.contains(it) }) return this

        return Uri.parse(this)
            .buildUpon()
            .authority("upos-sz-mirrorali.bilivideo.com")
            .build()
            .toString()
    }

    private data class PlaybackConfig(
        val qn: Int,           // 画质 ID
        val codec: VideoCodec?,     // 编码格式
        val audio: Audio      // 音频配置
    )

    private data class MediaUrls(
        val videoUrl: String,
        val audioUrl: String?,
        val useDashMpd: Boolean = false,
        val useHls: Boolean = false
    )
}

internal fun PlayerUiState.copyForVideoSwitch(
    newVideo: VideoListItem,
    clearDetailMetadata: Boolean
): PlayerUiState {
    val nextSubtitleMemory = if (subtitleId != -1L) {
        rememberSubtitleTrack(subtitleList.firstOrNull { it.id == subtitleId }) ?: subtitleMemory
    } else {
        null
    }
    val nextSecondarySubtitleMemory = if (secondarySubtitleId != -1L) {
        rememberSubtitleTrack(subtitleList.firstOrNull { it.id == secondarySubtitleId })
            ?: secondarySubtitleMemory
    } else {
        null
    }

    return copy(
        aid = newVideo.aid,
        bvid = AvBvConverter.av2bv(newVideo.aid),
        cid = newVideo.cid,
        epid = newVideo.epid,
        seasonId = newVideo.seasonId ?: 0,
        title = newVideo.title,
        isBuffering = true,
        videoShot = null,
        videoHeatmap = null,
        videoProgressChapters = emptyList(),
        watchedProgressMarks = emptyList(),
        onlineCountText = null,
        mediaStatsInfo = "",
        publishDateText = if (clearDetailMetadata) "" else publishDateText,
        playCountText = if (clearDetailMetadata) "" else playCountText,
        danmakuMask = null,
        subtitleId = -1L,
        subtitleMemory = nextSubtitleMemory,
        subtitleList = emptyList(),
        subtitleData = emptyList(),
        secondarySubtitleId = -1L,
        secondarySubtitleMemory = nextSecondarySubtitleMemory,
        secondarySubtitleCustom = false,
        secondarySubtitleData = emptyList(),
        aiAudioTranslations = emptyList(),
        currentAiAudioLanguage = "",
        relatedVideos = emptyList(),
        isFollowingUp = false,
        jumpModeState = jumpModeState.copyForVideoSwitch(newVideo.aid)
    )
}

internal fun PlayerCommentSort.toReplyApiSort(): Int {
    return when (this) {
        PlayerCommentSort.Latest -> 0
        PlayerCommentSort.Hot -> 1
    }
}

internal fun ReplyItem.toPlayerCommentItem(): PlayerCommentItem {
    return PlayerCommentItem(
        id = rpid.toString(),
        mid = mid,
        username = member.uname,
        avatar = member.avatar,
        message = content.message,
        emotes = content.emote.orEmpty()
            .mapNotNull { (key, emote) ->
                val text = emote.text.ifBlank { key }
                val url = emote.url
                if (text.isBlank() || url.isBlank()) {
                    null
                } else {
                    PlayerCommentEmote(text = text, url = url, size = emote.size)
                }
            },
        pictures = content.pictures.orEmpty()
            .mapNotNull { picture ->
                picture.imgSrc
                    .takeIf { it.isNotBlank() }
                    ?.let { PlayerCommentPicture(url = it, width = picture.imgWidth, height = picture.imgHeight) }
            },
        timeText = ctime.toSmartDate().orEmpty(),
        likeText = like.takeIf { it > 0 }?.let { "${it.toWanString()}赞" }.orEmpty(),
        replyText = rcount.takeIf { it > 0 }?.let { "${it.toWanString()}回复" }.orEmpty(),
        ipLocation = replyControl?.location.orEmpty(),
        color = member.vip?.nicknameColor?.parseReplyColor()
    )
}

internal fun formatCommentTotalCount(count: Int?): String {
    return count
        ?.takeIf { it > 0 }
        ?.toWanString()
        ?.let { "${it}条" }
        .orEmpty()
}

internal fun mergeWatchedProgressMarks(
    marks: List<ProgressSegmentMark>,
    newMark: ProgressSegmentMark
): List<ProgressSegmentMark> {
    if (newMark.endMs <= newMark.startMs) return marks
    val merged = mutableListOf<ProgressSegmentMark>()
    var start = newMark.startMs
    var end = newMark.endMs
    var inserted = false
    (marks + newMark)
        .sortedBy { it.startMs }
        .forEach { mark ->
            if (mark.endMs <= mark.startMs) return@forEach
            if (!inserted && end < mark.startMs - 250L) {
                merged += ProgressSegmentMark(start, end, newMark.colorArgb)
                inserted = true
            }
            if (!inserted && mark.startMs <= end + 250L) {
                start = minOf(start, mark.startMs)
                end = maxOf(end, mark.endMs)
            } else {
                merged += mark
            }
        }
    if (!inserted) {
        merged += ProgressSegmentMark(start, end, newMark.colorArgb)
    }
    return merged
        .takeLast(128)
        .map { it.copy(colorArgb = newMark.colorArgb) }
}

private fun String.parseReplyColor(): Int? {
    val value = trim().removePrefix("#")
    if (value.length != 6) return null
    return value.toIntOrNull(radix = 16)
}

internal fun JumpModeQueue.toJumpModeState(): JumpModeState {
    return JumpModeState(
        available = isUsable,
        enabled = false,
        source = source,
        currentIndex = selectedIndex,
        items = items
    )
}

internal fun JumpModeState.copyForVideoSwitch(aid: Long): JumpModeState {
    if (items.size <= 1) return JumpModeState()

    val nextIndex = items.indexOfFirst { it.aid == aid }
    return if (nextIndex == -1) {
        JumpModeState()
    } else {
        copy(
            available = true,
            currentIndex = nextIndex
        )
    }
}

internal fun buildBiliMediaStatsInfo(
    video: DashVideo,
    audio: DashAudio?
): String {
    return buildList {
        if (video.width > 0 && video.height > 0) {
            add("resolution: ${video.width} x ${video.height}")
        }
        video.frameRate.takeIf { it.isNotBlank() }?.let {
            add("video FPS: $it")
        }
        formatBiliBitrate(video.bandwidth)?.let {
            add("stream bitrate: $it")
        }
        video.formatVideoCodec()?.let {
            add("video codec: $it")
        }
        audio?.formatAudioCodec()?.let {
            add("audio codec: $it")
        }
        audio?.bandwidth?.let(::formatBiliBitrate)?.let {
            add("audio bitrate: $it")
        }
    }.joinToString("\n")
}

private fun String.toPlayerStatsDebugText(): String {
    return lineSequence()
        .filterNot { it.startsWith("mime type:", ignoreCase = true) }
        .joinToString("\n")
}

private fun DashVideo.formatVideoCodec(): String? {
    return codecs.takeMeaningfulCodec()
        ?: CodeType.fromCodecId(codecId).str.takeMeaningfulCodec()
        ?: codecId.takeIf { it > 0 }?.let { "id $it" }
}

private fun DashAudio.formatAudioCodec(): String? {
    return codecs.takeMeaningfulCodec()
        ?: codecId.takeIf { it > 0 }?.let { "id $it" }
}

private fun String?.takeMeaningfulCodec(): String? {
    return this
        ?.takeIf { it.isNotBlank() }
        ?.takeUnless { it.lowercase(Locale.US) == "none" || it.lowercase(Locale.US) == "unknown" }
}

private fun formatBiliBitrate(bitrate: Int): String? {
    if (bitrate <= 0) return null
    return if (bitrate >= 1_000_000) {
        String.format(Locale.US, "%.2f Mbps", bitrate / 1_000_000f)
    } else {
        "${bitrate / 1000} kbps"
    }
}

sealed interface DanmakuSettingAction {
    data class SetScale(val value: Float) : DanmakuSettingAction
    data class SetOpacity(val value: Float) : DanmakuSettingAction
    data class SetArea(val value: Float) : DanmakuSettingAction
    data class SetSpeedFactor(val value: Float) : DanmakuSettingAction
    data class SetMaskEnabled(val enabled: Boolean) : DanmakuSettingAction
    data class SetEnabledTypes(
        val types: List<DanmakuType>,
        val persist: Boolean = true
    ) : DanmakuSettingAction
    data object ToggleEnabled : DanmakuSettingAction
}

sealed interface SubtitleSettingAction {
    data class SetFontSize(val value: TextUnit) : SubtitleSettingAction
    data class SetOpacity(val value: Float) : SubtitleSettingAction
    data class SetBottomPadding(val value: Dp) : SubtitleSettingAction
}

sealed interface MediaProfileSettingAction {
    data class SetQuality(val value: Int) : MediaProfileSettingAction
    data class SetVideoCodec(val value: VideoCodec) : MediaProfileSettingAction
    data class SetAudio(val value: Audio) : MediaProfileSettingAction
}
