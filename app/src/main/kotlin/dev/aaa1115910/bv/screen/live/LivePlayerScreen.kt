package dev.aaa1115910.bv.screen.live

import android.app.Activity
import android.os.Looper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.TopCenter
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.PlaybackException
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.activities.live.LiveCastController
import dev.aaa1115910.bv.activities.live.LivePlayerActivity
import dev.aaa1115910.bv.activities.video.UpInfoActivity
import dev.aaa1115910.bv.activities.video.VideoInfoActivity
import dev.aaa1115910.bv.cast.CastPlaybackSnapshot
import dev.aaa1115910.bv.cast.CastTransportState
import dev.aaa1115910.bv.component.controllers.LiveDanmakuMenuState
import dev.aaa1115910.bv.component.controllers.LiveBottomMenuController
import dev.aaa1115910.bv.component.controllers.LiveBottomMenuItem
import dev.aaa1115910.bv.component.controllers.LiveMenuController
import dev.aaa1115910.bv.component.controllers.PlayerCommentPanelUiState
import dev.aaa1115910.bv.component.controllers.PlayerSidePanels
import dev.aaa1115910.bv.component.controllers.PlayerUpPanelUiState
import dev.aaa1115910.bv.component.controllers.DanmakuType
import dev.aaa1115910.bv.component.LiveDanmakuOverlay
import dev.aaa1115910.bv.entity.PlayerCommentItem
import dev.aaa1115910.bv.entity.PlayerCommentSort
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.entity.user.SpaceVideoOrder
import dev.aaa1115910.biliapi.http.BiliHttpApi
import dev.aaa1115910.biliapi.http.entity.live.DanmakuEvent
import dev.aaa1115910.biliapi.http.entity.live.SuperChatEvent
import dev.aaa1115910.biliapi.repositories.UserRepository
import dev.aaa1115910.biliapi.websocket.LiveDataWebSocketDebugEvent
import dev.aaa1115910.biliapi.websocket.LiveDataWebSocketState
import dev.aaa1115910.biliapi.websocket.LiveDataWebSocket
import dev.aaa1115910.bv.player.BvVideoPlayer
import dev.aaa1115910.bv.player.VideoPlayerOptions
import dev.aaa1115910.bv.player.VideoPlayerListener
import dev.aaa1115910.bv.player.impl.exo.ExoPlayerFactory
import dev.aaa1115910.bv.repository.LivePlaybackSource
import dev.aaa1115910.bv.repository.LiveQualityOption
import dev.aaa1115910.bv.repository.LiveJumpModeQueue
import dev.aaa1115910.bv.repository.LiveJumpModeRepository
import dev.aaa1115910.bv.repository.LiveRepository
import dev.aaa1115910.bv.repository.LiveRoomContext
import dev.aaa1115910.bv.repository.LiveStreamResolver
import dev.aaa1115910.bv.telemetry.FirebaseTelemetry
import dev.aaa1115910.bv.telemetry.TelemetryErrorType
import dev.aaa1115910.bv.telemetry.TelemetryScreen
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.formatHourMinSec
import dev.aaa1115910.bv.util.toWanString
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.viewmodel.player.PlayerSidePanel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private fun currentClockText(): String {
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
    val minute = calendar.get(Calendar.MINUTE).toString().padStart(2, '0')
    return "$hour:$minute"
}

private const val LiveJumpModeHoldMs = 3_000L
private const val LiveHistoryHeartbeatIntervalSec = 60

private data class LiveDanmakuDebugStats(
    val wsState: LiveDataWebSocketState = LiveDataWebSocketState.Disabled,
    val wsHost: String = "",
    val wsInfo: String = "",
    val wsError: String = "",
    val wsRecv: Long = 0L,
    val wsDanmaku: Long = 0L,
    val wsLastDanmakuAtMs: Long = 0L,
    val overlayEmitted: Long = 0L
)

@Composable
fun LivePlayerScreen() {
    val context = LocalContext.current
    val activity = context as Activity
    val viewConfiguration = LocalViewConfiguration.current
    var roomId by remember { mutableStateOf(activity.intent.getIntExtra("room_id", 0)) }
    var title by remember { mutableStateOf(activity.intent.getStringExtra("title").orEmpty()) }
    var upName by remember { mutableStateOf(activity.intent.getStringExtra("up_name").orEmpty()) }
    var online by remember { mutableStateOf(activity.intent.getIntExtra("online", 0)) }
    val liveRepository = remember { BVApp.koinApplication.koin.get<LiveRepository>() }
    val liveJumpModeRepository = remember { BVApp.koinApplication.koin.get<LiveJumpModeRepository>() }
    val apiUserRepository = remember { BVApp.koinApplication.koin.get<UserRepository>() }
    var liveJumpModeQueue by remember { mutableStateOf<LiveJumpModeQueue?>(null) }
    var liveJumpModeEnabled by remember { mutableStateOf(false) }
    var liveJumpModeHoldJob by remember { mutableStateOf<Job?>(null) }
    var liveJumpModeHoldKey by remember { mutableStateOf<Key?>(null) }
    var liveJumpModeConsumedKey by remember { mutableStateOf<Key?>(null) }
    var roomContext by remember { mutableStateOf<LiveRoomContext?>(null) }
    var playbackSource by remember { mutableStateOf<LivePlaybackSource?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isLivePaused by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf<String?>(null) }
    var activeOverlay by remember { mutableStateOf(LiveOverlayPanel.None) }
    var lastBackPressedAt by remember { mutableStateOf(0L) }
    var clockText by remember { mutableStateOf(currentClockText()) }
    var reloadToken by remember { mutableStateOf(0) }
    var selectedQuality by remember {
        mutableStateOf(activity.intent.getIntExtra("cast_quality", Prefs.defaultLiveQuality.qn))
    }
    var selectedLineIndex by remember { mutableStateOf(0) }
    var showLiveStats by remember { mutableStateOf(Prefs.showPlayerStats) }
    var preferLiveHighBitrate by remember { mutableStateOf(Prefs.preferLiveHighBitrate) }
    var liveStatsText by remember { mutableStateOf("") }
    var liveDanmakuDebugText by remember { mutableStateOf("") }
    var liveQualityMenuOptions by remember { mutableStateOf<List<LiveQualityOption>>(emptyList()) }
    var liveDanmakuDebugStats by remember {
        mutableStateOf(LiveDanmakuDebugStats())
    }
    var liveDanmakuSocketJob by remember { mutableStateOf<Job?>(null) }
    var liveChatMessages by remember { mutableStateOf<List<PlayerCommentItem>>(emptyList()) }
    var lastNonEmptyDanmakuTypes by remember {
        mutableStateOf(
            Prefs.defaultLiveDanmakuTypes.takeIf { it.isNotEmpty() } ?: DanmakuType.entries
        )
    }
    var liveUpPanelVideos by remember { mutableStateOf<List<VideoCardData>>(emptyList()) }
    var liveUpPanelOrder by remember { mutableStateOf(SpaceVideoOrder.PubDate) }
    var liveUpPanelFace by remember { mutableStateOf("") }
    var liveUpPanelFollowing by remember { mutableStateOf(false) }
    val liveScope = rememberCoroutineScope()
    val initialLiveDanmakuTypes = remember {
        val enabled = if (activity.intent.hasExtra("danmaku_enabled")) {
            activity.intent.getBooleanExtra("danmaku_enabled", true)
        } else {
            Prefs.defaultLiveDanmakuEnabled
        }
        if (enabled) {
            Prefs.defaultLiveDanmakuTypes.takeIf { it.isNotEmpty() } ?: DanmakuType.entries
        } else {
            emptyList()
        }
    }
    var liveDanmakuState by remember {
        mutableStateOf(
            LiveDanmakuMenuState(
                enabledTypes = initialLiveDanmakuTypes,
                scale = Prefs.defaultDanmakuScale,
                opacity = Prefs.defaultDanmakuOpacity,
                speedFactor = Prefs.defaultDanmakuSpeedFactor,
                area = Prefs.defaultDanmakuArea,
                maskEnabled = Prefs.defaultDanmakuMask
            )
        )
    }
    val currentLiveDanmakuState by rememberUpdatedState(liveDanmakuState)
    val liveDanmakuSession = remember { LiveDanmakuSession() }
    val player by remember {
        mutableStateOf(
            ExoPlayerFactory().create(
                context = context,
                options = VideoPlayerOptions(
                    userAgent = "Mozilla/5.0",
                    referer = "https://live.bilibili.com/",
                    enableFfmpegAudioRenderer = false,
                    enableSoftwareVideoDecoder = Prefs.enableSoftwareVideoDecoder,
                    enableVolumeNormalization = Prefs.enableVolumeNormalization
                )
            )
        )
    }
    val screenFocusRequester = remember { FocusRequester() }

    fun hasLiveJumpModeQueue(): Boolean {
        return liveJumpModeQueue?.isUsable == true
    }

    fun toggleLiveJumpMode() {
        if (!hasLiveJumpModeQueue()) {
            "当前列表不支持跳动模式".toast(context)
            return
        }
        liveJumpModeEnabled = !liveJumpModeEnabled
        val message = if (liveJumpModeEnabled) {
            "进入跳动模式，按左右键切换直播间"
        } else {
            "已退出跳动模式"
        }
        message.toast(context)
    }

    fun startLiveJumpModeHold(key: Key) {
        if (activeOverlay != LiveOverlayPanel.None) return
        if (liveJumpModeConsumedKey == key) return
        if (liveJumpModeHoldJob?.isActive == true) return
        liveJumpModeHoldKey = key
        liveJumpModeHoldJob = liveScope.launch {
            delay(LiveJumpModeHoldMs)
            toggleLiveJumpMode()
            liveJumpModeConsumedKey = key
            liveJumpModeHoldJob = null
            liveJumpModeHoldKey = null
        }
    }

    fun appendLiveChatMessage(item: PlayerCommentItem): Boolean {
        liveChatMessages = (liveChatMessages + item).takeLast(200)
        return true
    }

    fun resetLiveDanmakuDebugStats() {
        liveDanmakuDebugStats = LiveDanmakuDebugStats()
        liveDanmakuDebugText = ""
    }

    fun updateLiveDanmakuDebugStats(block: (LiveDanmakuDebugStats) -> LiveDanmakuDebugStats) {
        liveDanmakuDebugStats = block(liveDanmakuDebugStats)
    }

    fun switchLiveJumpRoom(offset: Int): Boolean {
        if (!liveJumpModeEnabled) return false
        val queue = liveJumpModeQueue ?: return false
        val currentIndex = queue.items.indexOfFirst { it.roomId == roomId }
        if (currentIndex == -1) {
            statusText = "跳动失败，没找到当前直播间"
            return true
        }
        val target = queue.items.getOrNull(currentIndex + offset)
        if (target == null) {
            statusText = if (offset < 0) "已经是上一个直播间" else "已经是下一个直播间"
            return true
        }
        title = target.title
        upName = target.upName
        online = target.online
        roomId = target.roomId
        liveJumpModeQueue = queue.copyForRoom(target.roomId)
        activeOverlay = LiveOverlayPanel.None
        roomContext = null
        playbackSource = null
        errorMessage = null
        isLoading = true
        selectedLineIndex = 0
        liveDanmakuSocketJob?.cancel()
        liveDanmakuSocketJob = null
        liveDanmakuSession.clear()
        liveChatMessages = emptyList()
        resetLiveDanmakuDebugStats()
        liveUpPanelVideos = emptyList()
        liveUpPanelFace = ""
        liveUpPanelFollowing = false
        statusText = "切换到 ${target.title}"
        return true
    }

    fun handleLiveJumpModeKeyUp(key: Key, offset: Int): Boolean {
        if (liveJumpModeConsumedKey == key) {
            liveJumpModeConsumedKey = null
            liveJumpModeHoldKey = null
            return true
        }
        val hadPendingHold = liveJumpModeHoldKey == key && liveJumpModeHoldJob?.isActive == true
        liveJumpModeHoldJob?.cancel()
        liveJumpModeHoldJob = null
        liveJumpModeHoldKey = null
        if (liveJumpModeEnabled) {
            return switchLiveJumpRoom(offset)
        }
        return hadPendingHold
    }

    fun openLiveBottomMenuFromSurface() {
        if (activeOverlay == LiveOverlayPanel.None) {
            activeOverlay = openLiveBottomMenu()
        }
    }

    fun handleLiveDanmakuEvent(event: DanmakuEvent) {
        val shouldEmitOverlay = currentLiveDanmakuState.enabledTypes.isNotEmpty() &&
                liveDanmakuSession.overlayController.allows(event)
        if (shouldEmitOverlay) {
            liveDanmakuSession.addDanmaku(
                event = event,
                isPlaying = player.isPlaying
            )
        }
        appendLiveChatMessage(event.toLiveCommentItem(prefix = "live"))
        updateLiveDanmakuDebugStats {
            it.copy(overlayEmitted = it.overlayEmitted + if (shouldEmitOverlay) 1 else 0)
        }
    }

    val loadLiveUpPanelVideos = fun() {
        val ownerMid = roomContext?.ownerMid ?: 0L
        if (ownerMid <= 0L) return
        liveScope.launch(Dispatchers.IO) {
            runCatching {
                val data = apiUserRepository.getSpaceVideos(
                    mid = ownerMid,
                    order = liveUpPanelOrder,
                    preferApiType = ApiType.Web
                )
                liveUpPanelVideos = data.videos.map { item ->
                    VideoCardData(
                        avid = item.aid,
                        cid = item.cid,
                        title = item.title,
                        cover = item.cover,
                        upName = item.author,
                        upMid = ownerMid,
                        playString = item.play.takeIf { it != -1 }.toWanString(),
                        danmakuString = item.danmaku.takeIf { it != -1 }.toWanString(),
                        timeString = (item.duration * 1000L).formatHourMinSec(),
                        pubTime = item.pubTime
                    )
                }
            }
        }
    }

    DisposableEffect(player) {
        player.setPlayerEventListener(
            object : VideoPlayerListener {
                override fun onError(error: Exception) {
                    val detailedMessage = error.toLivePlayerErrorText()
                    FirebaseTelemetry.reportLiveError(
                        type = TelemetryErrorType.DecodeError,
                        throwable = error,
                        extras = mapOf(
                            "stage" to "player_error",
                            "detail" to detailedMessage
                        )
                    )
                    errorMessage = detailedMessage
                    isLoading = false
                }

                override fun onReady() {
                    isLoading = false
                    errorMessage = null
                }

                override fun onPlay() {
                    isLivePaused = false
                    liveDanmakuSession.start()
                }

                override fun onPause() {
                    if (playbackSource != null && !isLoading && errorMessage == null) {
                        isLivePaused = true
                    }
                    liveDanmakuSession.pause()
                }

                override fun onBuffering() {
                    liveDanmakuSession.pause()
                }

                override fun onEnd() = Unit

                override fun onSeekBack(seekBackIncrementMs: Long) = Unit

                override fun onSeekForward(seekForwardIncrementMs: Long) = Unit
            }
        )
        onDispose {
            player.setPlayerEventListener(null)
        }
    }

    LaunchedEffect(Unit) {
        FirebaseTelemetry.setLastScreen(TelemetryScreen.LivePlayer)
        liveJumpModeQueue = liveJumpModeRepository.consumeQueueFor(roomId)
    }

    LaunchedEffect(roomId) {
        val result = withContext(Dispatchers.IO) {
            runCatching { liveRepository.resolveRoomContext(roomId) }
        }
        result
            .onSuccess { roomContext = it }
            .onFailure {
                FirebaseTelemetry.reportLiveError(
                    type = FirebaseTelemetry.classifyThrowable(it),
                    throwable = it,
                    extras = mapOf("stage" to "resolve_room")
                )
                errorMessage = resolveLivePlaybackErrorMessage(
                    liveStatus = null,
                    source = null,
                    throwable = it
                )
                isLoading = false
            }
    }

    LaunchedEffect(roomContext?.title, roomContext?.roomId) {
        val contextTitle = roomContext?.title.orEmpty()
        if (contextTitle.isNotBlank()) title = contextTitle
    }

    LaunchedEffect(roomContext?.roomId, roomContext?.liveStatus) {
        val resolvedRoomId = roomContext?.roomId ?: return@LaunchedEffect
        if (resolvedRoomId <= 0 || Prefs.incognitoMode) return@LaunchedEffect
        runCatching {
            withContext(Dispatchers.IO) {
                liveRepository.reportRoomEntry(resolvedRoomId)
                liveRepository.sendLiveHeartbeat(
                    roomId = resolvedRoomId,
                    intervalSeconds = LiveHistoryHeartbeatIntervalSec
                )
            }
        }
        while (isActive) {
            delay(LiveHistoryHeartbeatIntervalSec * 1000L)
            runCatching {
                withContext(Dispatchers.IO) {
                    liveRepository.sendLiveHeartbeat(
                        roomId = resolvedRoomId,
                        intervalSeconds = LiveHistoryHeartbeatIntervalSec
                    )
                }
            }
        }
    }

    LaunchedEffect(roomContext?.ownerMid) {
        val ownerMid = roomContext?.ownerMid ?: return@LaunchedEffect
        if (ownerMid <= 0L) return@LaunchedEffect
        runCatching {
            val card = withContext(Dispatchers.IO) {
                BiliHttpApi.getUserCardInfo(
                    uid = ownerMid,
                    sessData = Prefs.sessData
                ).getResponseData()
            }
            liveUpPanelFace = card.card.face
            liveUpPanelFollowing = card.following
            if (upName.isBlank()) {
                upName = card.card.name
            }
        }
    }

    LaunchedEffect(roomContext?.roomId, roomContext?.liveStatus) {
        val resolvedRoomId = roomContext?.roomId ?: return@LaunchedEffect
        if ((roomContext?.liveStatus ?: 0) != 1) {
            liveDanmakuSocketJob?.cancel()
            liveDanmakuSocketJob = null
            updateLiveDanmakuDebugStats {
                it.copy(
                    wsState = LiveDataWebSocketState.Disabled,
                    wsHost = "",
                    wsInfo = "",
                    wsError = ""
                )
            }
            return@LaunchedEffect
        }
        liveDanmakuSocketJob?.cancel()
        resetLiveDanmakuDebugStats()
        liveDanmakuSocketJob = LiveDataWebSocket.connectLiveEvent(
            roomId = resolvedRoomId,
            uid = Prefs.uid,
            sessData = Prefs.sessData,
            biliJct = Prefs.biliJct,
            uidCkMd5 = Prefs.uidCkMd5,
            sid = Prefs.sid,
            buvid3 = Prefs.buvid3,
            onDebug = { debugEvent ->
                liveScope.launch {
                    when (debugEvent) {
                        is LiveDataWebSocketDebugEvent.StateChanged -> {
                            updateLiveDanmakuDebugStats {
                                it.copy(
                                    wsState = debugEvent.state,
                                    wsError = if (
                                        debugEvent.state == LiveDataWebSocketState.Connecting ||
                                        debugEvent.state == LiveDataWebSocketState.Authed
                                    ) {
                                        ""
                                    } else {
                                        it.wsError
                                    }
                                )
                            }
                        }

                        is LiveDataWebSocketDebugEvent.HostChanged -> {
                            updateLiveDanmakuDebugStats {
                                it.copy(wsHost = debugEvent.host)
                            }
                        }

                        is LiveDataWebSocketDebugEvent.Info -> {
                            updateLiveDanmakuDebugStats {
                                it.copy(wsInfo = debugEvent.message)
                            }
                        }

                        is LiveDataWebSocketDebugEvent.Error -> {
                            FirebaseTelemetry.reportDanmakuError(
                                type = TelemetryErrorType.NetworkError,
                                throwable = IllegalStateException("live danmaku connection failed"),
                                extras = mapOf("stage" to "websocket")
                            )
                            updateLiveDanmakuDebugStats {
                                it.copy(wsError = debugEvent.reason)
                            }
                        }

                        LiveDataWebSocketDebugEvent.RawMessage -> {
                            updateLiveDanmakuDebugStats {
                                it.copy(wsRecv = it.wsRecv + 1)
                            }
                        }

                        is LiveDataWebSocketDebugEvent.DanmakuParsed -> {
                            updateLiveDanmakuDebugStats {
                                it.copy(
                                    wsDanmaku = it.wsDanmaku + debugEvent.count,
                                    wsLastDanmakuAtMs = System.currentTimeMillis()
                                )
                            }
                        }
                    }
                }
            }
        ) { event ->
            when (event) {
                is DanmakuEvent -> {
                    liveScope.launch {
                        handleLiveDanmakuEvent(event)
                    }
                }

                is SuperChatEvent -> {
                    liveScope.launch {
                        appendLiveChatMessage(event.toLiveCommentItem())
                    }
                }
            }
        }
    }

    LaunchedEffect(liveDanmakuState) {
        liveDanmakuSession.overlayController.applyState(liveDanmakuState)
    }

    LaunchedEffect(
        showLiveStats,
        playbackSource,
        player.videoWidth,
        player.videoHeight,
        roomContext?.liveStartTime
    ) {
        if (!showLiveStats) {
            liveStatsText = ""
            return@LaunchedEffect
        }
        while (true) {
            liveStatsText = buildLiveStatsText(
                playerDebugInfo = player.debugInfo,
                playbackSource = playbackSource,
                videoWidth = player.videoWidth,
                videoHeight = player.videoHeight,
                liveStartTime = roomContext?.liveStartTime ?: 0L
            )
            delay(1_000)
        }
    }

    LaunchedEffect(playbackSource) {
        val source = playbackSource
        liveQualityMenuOptions = if (source == null) {
            emptyList()
        } else {
            source.qualities.map { quality ->
                quality.copy(desc = LiveStreamResolver.buildPlaybackQualityLabel(source, quality))
            }
        }
    }

    LaunchedEffect(showLiveStats, liveDanmakuDebugStats) {
        if (!showLiveStats) {
            liveDanmakuDebugText = ""
            return@LaunchedEffect
        }
        while (true) {
            liveDanmakuDebugText = buildLiveDanmakuDebugText(liveDanmakuDebugStats)
            delay(1_000)
        }
    }

    LaunchedEffect(roomContext?.roomId, selectedQuality, selectedLineIndex, preferLiveHighBitrate, reloadToken) {
        if (roomId == 0) {
            errorMessage = "直播间参数无效"
            isLoading = false
            return@LaunchedEffect
        }
        val resolvedRoomId = roomContext?.roomId ?: return@LaunchedEffect
        errorMessage = null
        isLoading = true
        if (roomContext?.liveStatus != 1) {
            errorMessage = resolveLivePlaybackErrorMessage(
                liveStatus = roomContext?.liveStatus,
                source = null
            )
            isLoading = false
            return@LaunchedEffect
        }
        val playbackResult = withContext(Dispatchers.IO) {
            runCatching {
                liveRepository.resolvePlayableSource(
                    roomId = resolvedRoomId,
                    qn = selectedQuality,
                    lineIndex = selectedLineIndex
                )
            }
        }
        val resolvedSource = playbackResult.getOrElse {
            FirebaseTelemetry.reportLiveError(
                type = FirebaseTelemetry.classifyThrowable(it),
                throwable = it,
                extras = mapOf("stage" to "resolve_play_url")
            )
            errorMessage = resolveLivePlaybackErrorMessage(
                liveStatus = roomContext?.liveStatus,
                source = null,
                throwable = it
            )
            isLoading = false
            return@LaunchedEffect
        }
        if (resolvedSource == null) {
            FirebaseTelemetry.reportLiveError(
                type = TelemetryErrorType.EmptyPlayUrl,
                throwable = IllegalStateException("live play url is empty"),
                extras = mapOf("stage" to "resolve_play_url")
            )
            errorMessage = resolveLivePlaybackErrorMessage(
                liveStatus = roomContext?.liveStatus,
                source = null
            )
            isLoading = false
        } else {
            playbackSource = resolvedSource
            player.stop()
            liveDanmakuSession.clear()
            liveChatMessages = emptyList()
            isLivePaused = false
            player.setOptions()
            player.playUrl(resolvedSource.playUrl, null)
            player.prepare()
            player.start()
            selectedQuality = resolvedSource.currentQuality
            selectedLineIndex = resolvedSource.currentLineIndex
        }
    }

    LaunchedEffect(statusText) {
        if (statusText != null) {
            delay(2_000)
            statusText = null
        }
    }

    LaunchedEffect(Unit) {
        screenFocusRequester.requestFocus()
        while (true) {
            clockText = currentClockText()
            delay(1_000)
        }
    }

    LaunchedEffect(activeOverlay) {
        if (activeOverlay == LiveOverlayPanel.None) {
            screenFocusRequester.requestFocus()
        }
    }

    DisposableEffect(
        activity,
        player,
        playbackSource,
        liveQualityMenuOptions,
        liveDanmakuState,
        lastNonEmptyDanmakuTypes,
        selectedQuality,
        isLoading,
        errorMessage,
        roomId,
        title
    ) {
        val liveActivity = activity as? LivePlayerActivity
        if (liveActivity == null) {
            onDispose { }
        } else {
            val controller = object : LiveCastController {
                private fun runOnMain(block: () -> Unit) {
                    if (Looper.myLooper() == Looper.getMainLooper()) {
                        block()
                    } else {
                        activity.runOnUiThread(block)
                    }
                }

                override fun play() {
                    runOnMain {
                        isLivePaused = false
                        player.start()
                        liveDanmakuSession.start()
                    }
                }

                override fun pause() {
                    runOnMain {
                        isLivePaused = true
                        player.pause()
                        liveDanmakuSession.pause()
                    }
                }

                override fun stop() {
                    runOnMain {
                        player.stop()
                        liveDanmakuSession.pause()
                        if (!activity.isFinishing) {
                            activity.finish()
                        }
                    }
                }

                override fun setSpeed(speed: Float) {
                    runOnMain {
                        player.speed = speed
                    }
                }

                override fun setQuality(qualityId: Int) {
                    if (qualityId <= 0) return
                    runOnMain {
                        if (selectedQuality != qualityId) {
                            selectedQuality = qualityId
                            selectedLineIndex = 0
                            statusText = "切换到 ${liveQualityMenuOptions.firstOrNull { it.qn == qualityId }?.desc ?: qualityId}"
                        }
                    }
                }

                override fun setDanmakuEnabled(enabled: Boolean) {
                    runOnMain {
                        val nextTypes = if (enabled) {
                            lastNonEmptyDanmakuTypes.takeIf { it.isNotEmpty() } ?: DanmakuType.entries
                        } else {
                            emptyList()
                        }
                        liveDanmakuState = liveDanmakuState.copy(enabledTypes = nextTypes)
                        if (nextTypes.isNotEmpty()) {
                            lastNonEmptyDanmakuTypes = nextTypes
                        }
                    }
                }

                override fun snapshot(): CastPlaybackSnapshot {
                    val state = when {
                        isLoading -> CastTransportState.TRANSITIONING
                        playbackSource == null && errorMessage != null -> CastTransportState.STOPPED
                        player.isPlaying -> CastTransportState.PLAYING
                        playbackSource != null -> CastTransportState.PAUSED
                        else -> CastTransportState.STOPPED
                    }
                    return CastPlaybackSnapshot(
                        state = state,
                        speed = player.speed,
                        roomId = roomId.toLong(),
                        title = title,
                        qualityId = playbackSource?.currentQuality ?: selectedQuality,
                        availableQuality = liveQualityMenuOptions.associate { it.qn to it.desc },
                        danmakuEnabled = liveDanmakuState.enabledTypes.isNotEmpty()
                    )
                }
            }
            liveActivity.castController = controller
            onDispose {
                if (liveActivity.castController === controller) {
                    liveActivity.castController = null
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            liveJumpModeHoldJob?.cancel()
            liveDanmakuSocketJob?.cancel()
            isLivePaused = true
            player.pause()
            player.release()
            liveDanmakuSession.release()
        }
    }

    val showTopOverlay = shouldShowLiveTopOverlay(activeOverlay)
    val liveAspectRatio = when {
        player.videoWidth > 0 && player.videoHeight > 0 -> {
            player.videoWidth / player.videoHeight.toFloat()
        }

        roomContext?.isPortrait == true -> 9 / 16f
        else -> 16 / 9f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(screenFocusRequester)
            .focusable()
            .background(Color.Black)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp) {
                    if (activeOverlay != LiveOverlayPanel.None) {
                        liveJumpModeHoldJob?.cancel()
                        liveJumpModeHoldJob = null
                        liveJumpModeHoldKey = null
                        return@onPreviewKeyEvent false
                    }
                    when (event.key) {
                        Key.DirectionLeft -> {
                            return@onPreviewKeyEvent handleLiveJumpModeKeyUp(event.key, -1)
                        }

                        Key.DirectionRight -> {
                            return@onPreviewKeyEvent handleLiveJumpModeKeyUp(event.key, 1)
                        }

                        else -> {
                            return@onPreviewKeyEvent false
                        }
                    }
                }
                when (event.key) {
                    Key.Back -> {
                        val result = handleLiveBackPress(
                            activeOverlay = activeOverlay,
                            lastBackPressedAt = lastBackPressedAt,
                            now = System.currentTimeMillis()
                        )
                        activeOverlay = result.activeOverlay
                        lastBackPressedAt = result.lastBackPressedAt
                        if (result.statusText != null) {
                            R.string.video_player_press_back_again_to_exit.toast(context)
                            statusText = null
                        }
                        if (result.shouldExit) {
                            activity.finish()
                        }
                        true
                    }

                    Key.DirectionLeft -> {
                        if (activeOverlay == LiveOverlayPanel.None) {
                            startLiveJumpModeHold(event.key)
                            true
                        } else {
                            false
                        }
                    }

                    Key.DirectionRight -> {
                        if (activeOverlay == LiveOverlayPanel.None) {
                            startLiveJumpModeHold(event.key)
                            true
                        } else {
                            false
                        }
                    }

                    Key.DirectionCenter, Key.Enter, Key.Spacebar -> {
                        if (activeOverlay == LiveOverlayPanel.None) {
                            openLiveBottomMenuFromSurface()
                            true
                        } else {
                            false
                        }
                    }

                    Key.DirectionDown -> {
                        if (activeOverlay == LiveOverlayPanel.None) {
                            activeOverlay = openLiveBottomMenu()
                            true
                        } else {
                            false
                        }
                    }

                    Key.Menu -> {
                        if (
                            activeOverlay == LiveOverlayPanel.None ||
                            activeOverlay == LiveOverlayPanel.RightMenu
                        ) {
                            val nextOverlay = toggleLiveRightMenu(activeOverlay)
                            activeOverlay = nextOverlay
                            if (nextOverlay == LiveOverlayPanel.None) {
                                screenFocusRequester.requestFocus()
                            }
                            true
                        } else {
                            false
                        }
                    }

                    else -> false
                }
            }
            .pointerInput(activeOverlay, liveJumpModeEnabled) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downPosition = down.position
                    var handled = false
                    var moved = false
                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: continue
                        val delta = change.position - downPosition
                        if (abs(delta.x) > viewConfiguration.touchSlop || abs(delta.y) > viewConfiguration.touchSlop) {
                            moved = true
                        }
                        if (
                            !handled &&
                            activeOverlay == LiveOverlayPanel.None &&
                            liveJumpModeEnabled &&
                            abs(delta.x) >= viewConfiguration.touchSlop * 8f &&
                            abs(delta.x) >= abs(delta.y) * 1.5f
                        ) {
                            handled = true
                            switchLiveJumpRoom(if (delta.x < 0f) 1 else -1)
                            change.consume()
                        }
                    } while (event.changes.any { it.id == down.id && it.pressed })

                    if (!handled && !moved && activeOverlay == LiveOverlayPanel.None) {
                        openLiveBottomMenuFromSurface()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            BvVideoPlayer(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(liveAspectRatio),
                videoPlayer = player,
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT,
                keepScreenAwake = true
            )
            if (roomContext?.isChatRoom == true) {
                LiveChatRoomStage(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(liveAspectRatio),
                    roomContext = roomContext!!,
                    fallbackTitle = title,
                    fallbackUpName = upName
                )
            }
            if (liveDanmakuState.enabledTypes.isNotEmpty()) {
                LiveDanmakuOverlay(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(liveAspectRatio),
                    controller = liveDanmakuSession.overlayController,
                    state = liveDanmakuState
                )
            }
        }

        LaunchedEffect(liveJumpModeEnabled, activeOverlay) {
            while (liveJumpModeEnabled && activeOverlay == LiveOverlayPanel.None) {
                screenFocusRequester.requestFocus()
                delay(1_000)
            }
        }

        when {
            errorMessage != null -> Text(text = errorMessage!!)
            isLoading -> Text(text = "直播加载中…")
            playbackSource == null -> Text(text = "直播加载中…")
        }

        if (!statusText.isNullOrBlank() && errorMessage == null) {
            Text(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                text = statusText.orEmpty(),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        AnimatedVisibility(
            visible = showTopOverlay && playbackSource != null,
            modifier = Modifier.align(TopCenter),
            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.48f))
                    .padding(horizontal = 28.dp, vertical = 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = title,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        modifier = Modifier.padding(start = 16.dp, end = 12.dp),
                        text = clockText,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "直播中",
                        color = Color(0xFFFF6EAF),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (upName.isNotBlank()) {
                        Text(
                            text = upName,
                            color = Color.White.copy(alpha = 0.84f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (online > 0) {
                        Text(
                            modifier = Modifier.padding(start = 14.dp),
                            text = "${online} 人正在看",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        LiveMenuController(
            modifier = Modifier
                .align(Alignment.CenterEnd),
            show = activeOverlay == LiveOverlayPanel.RightMenu && playbackSource != null,
            qualityOptions = liveQualityMenuOptions,
            currentQuality = playbackSource?.currentQuality ?: selectedQuality,
            lineOptions = playbackSource?.lines.orEmpty(),
            currentLineIndex = playbackSource?.currentLineIndex ?: selectedLineIndex,
            danmakuState = liveDanmakuState,
            showStats = showLiveStats,
            preferHighBitrate = preferLiveHighBitrate,
            onQualitySelected = { quality ->
                if (quality.qn != selectedQuality) {
                    selectedQuality = quality.qn
                    statusText = "切换到 ${quality.desc}"
                }
            },
            onLineSelected = { index ->
                if (index != selectedLineIndex) {
                    selectedLineIndex = index
                    statusText = "切换到 ${playbackSource?.lines?.getOrNull(index)?.label ?: "线路"}"
                }
            },
            onDanmakuStateChange = { newState ->
                liveDanmakuState = newState
                Prefs.defaultLiveDanmakuEnabled = newState.enabledTypes.isNotEmpty()
                if (newState.enabledTypes.isNotEmpty()) {
                    lastNonEmptyDanmakuTypes = newState.enabledTypes
                    Prefs.defaultLiveDanmakuTypes = newState.enabledTypes
                }
                Prefs.defaultDanmakuScale = newState.scale
                Prefs.defaultDanmakuOpacity = newState.opacity
                Prefs.defaultDanmakuSpeedFactor = newState.speedFactor
                Prefs.defaultDanmakuArea = newState.area
                Prefs.defaultDanmakuMask = newState.maskEnabled
            },
            onShowStatsChange = { show ->
                showLiveStats = show
                Prefs.showPlayerStats = show
            },
            onPreferHighBitrateChange = { enabled ->
                if (enabled != preferLiveHighBitrate) {
                    preferLiveHighBitrate = enabled
                    Prefs.preferLiveHighBitrate = enabled
                    statusText = if (enabled) {
                        "码率增强已开启，正在重载直播流…"
                    } else {
                        "码率增强已关闭，正在重载直播流…"
                    }
                    player.stop()
                    playbackSource = null
                    errorMessage = null
                    isLoading = true
                    selectedLineIndex = 0
                }
            }
        )

        if (showLiveStats && liveStatsText.isNotBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 8.dp, top = 56.dp)
                    .widthIn(max = 620.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(Color.Black.copy(alpha = 0.3f))
            ) {
                Text(
                    modifier = Modifier.padding(8.dp),
                    text = liveStatsText,
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    softWrap = true,
                    overflow = TextOverflow.Clip
                )
            }
        }

        if (showLiveStats && liveDanmakuDebugText.isNotBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 8.dp, top = 56.dp)
                    .widthIn(max = 420.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(Color.Black.copy(alpha = 0.3f))
            ) {
                Text(
                    modifier = Modifier.padding(8.dp),
                    text = liveDanmakuDebugText,
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    softWrap = true,
                    overflow = TextOverflow.Clip
                )
            }
        }

        LiveBottomMenuController(
            modifier = Modifier.align(Alignment.BottomCenter),
            show = activeOverlay == LiveOverlayPanel.BottomMenu,
            items = buildList {
                add(
                    LiveBottomMenuItem(
                        iconRes = R.drawable.play_pause_24px,
                        label = if (isLivePaused) "继续" else "暂停"
                    ) {
                        if (isLivePaused) {
                            isLivePaused = false
                            player.start()
                            liveDanmakuSession.start()
                        } else {
                            isLivePaused = true
                            player.pause()
                            liveDanmakuSession.pause()
                        }
                        activeOverlay = LiveOverlayPanel.None
                    }
                )
                add(
                    LiveBottomMenuItem(
                        iconRes = R.drawable.related_videos_24px,
                        label = "刷新"
                    ) {
                        statusText = "正在刷新直播流…"
                        player.stop()
                        isLivePaused = false
                        playbackSource = null
                        errorMessage = null
                        isLoading = true
                        selectedLineIndex = 0
                        reloadToken += 1
                        activeOverlay = LiveOverlayPanel.None
                    }
                )
                add(
                    LiveBottomMenuItem(
                        iconRes = if (liveDanmakuState.enabledTypes.isEmpty()) {
                            R.drawable.danmaku_off_24px
                        } else {
                            R.drawable.danmaku_on_24px
                        },
                        label = "弹幕开关"
                    ) {
                        val nextTypes = if (liveDanmakuState.enabledTypes.isEmpty()) {
                            lastNonEmptyDanmakuTypes.takeIf { it.isNotEmpty() } ?: DanmakuType.entries
                        } else {
                            emptyList()
                        }
                        liveDanmakuState = liveDanmakuState.copy(enabledTypes = nextTypes)
                        Prefs.defaultLiveDanmakuEnabled = nextTypes.isNotEmpty()
                        if (nextTypes.isNotEmpty()) {
                            lastNonEmptyDanmakuTypes = nextTypes
                            Prefs.defaultLiveDanmakuTypes = nextTypes
                        }
                    }
                )
                add(
                    LiveBottomMenuItem(
                        iconRes = if (liveJumpModeEnabled) {
                            R.drawable.jump_mode_on_24px
                        } else {
                            R.drawable.jump_mode_off_24px
                        },
                        label = "跳动模式"
                    ) {
                        toggleLiveJumpMode()
                    }
                )
                add(
                    LiveBottomMenuItem(
                        iconRes = R.drawable.comment_24px,
                        label = "评论"
                    ) {
                        activeOverlay = LiveOverlayPanel.Comments
                    }
                )
                if ((roomContext?.ownerMid ?: 0L) > 0L) {
                    add(
                        LiveBottomMenuItem(
                            iconRes = R.drawable.contact_page_24px,
                            label = "up主页"
                        ) {
                            loadLiveUpPanelVideos()
                            activeOverlay = LiveOverlayPanel.UpSpace
                        }
                    )
                }
            },
            onDismiss = {
                activeOverlay = LiveOverlayPanel.None
            }
        )

        PlayerSidePanels(
            activePanel = when (activeOverlay) {
                LiveOverlayPanel.UpSpace -> PlayerSidePanel.UpSpace
                LiveOverlayPanel.Comments -> PlayerSidePanel.Comments
                else -> PlayerSidePanel.None
            },
            relatedVideos = emptyList(),
            upPanelUiState = PlayerUpPanelUiState(
                upMid = roomContext?.ownerMid ?: 0L,
                upName = upName,
                upFace = liveUpPanelFace,
                latestSelected = liveUpPanelOrder == SpaceVideoOrder.PubDate,
                isFollowing = liveUpPanelFollowing,
                videos = liveUpPanelVideos
            ),
            commentPanelUiState = PlayerCommentPanelUiState(
                title = "直播评论",
                emptyText = "暂无评论",
                sort = PlayerCommentSort.Latest,
                showSortToggle = false,
                comments = liveChatMessages,
                focusLatest = true
            ),
            onClose = {
                activeOverlay = LiveOverlayPanel.None
            },
            onRelatedVideoClicked = {},
            onUpVideoClicked = { video ->
                VideoInfoActivity.actionStart(context, video.avid)
            },
            onToggleUpSort = {
                liveUpPanelOrder = if (liveUpPanelOrder == SpaceVideoOrder.PubDate) {
                    SpaceVideoOrder.Click
                } else {
                    SpaceVideoOrder.PubDate
                }
                loadLiveUpPanelVideos()
            },
            onToggleUpFollow = {
                val ownerMid = roomContext?.ownerMid ?: 0L
                if (ownerMid > 0L) {
                    liveScope.launch(Dispatchers.IO) {
                        val target = !liveUpPanelFollowing
                        val success = if (target) {
                            apiUserRepository.followUser(ownerMid)
                        } else {
                            apiUserRepository.unfollowUser(ownerMid)
                        }
                        if (success) {
                            liveUpPanelFollowing = target
                        }
                    }
                }
            },
            onToggleCommentSort = {},
            onCommentListPositionChanged = { _, _ -> },
            onOpenUpPage = { mid, name ->
                if (mid > 0L) {
                    UpInfoActivity.actionStart(
                        context = context,
                        mid = mid,
                        name = name,
                        initialFocus = UpInfoActivity.INITIAL_FOCUS_VIDEOS
                    )
                }
            }
        )
    }
}

private fun DanmakuEvent.toLiveCommentItem(prefix: String = "live"): PlayerCommentItem {
    val badge = listOfNotNull(
        medalName?.takeIf { it.isNotBlank() },
        medalLevel?.takeIf { it > 0 }?.toString()
    ).joinToString(" ").takeIf { it.isNotBlank() }
    return PlayerCommentItem(
        id = "$prefix-${eventTimeMs}-${mid}-${content.hashCode()}",
        mid = mid,
        username = username,
        message = content,
        timeText = eventTimeMs.toLiveChatTimeText(),
        badgeText = badge,
        color = color
    )
}

private fun SuperChatEvent.toLiveCommentItem(): PlayerCommentItem {
    val priceText = price.takeIf { it > 0L }?.let { "¥$it" }
    return PlayerCommentItem(
        id = "sc-${eventTimeMs}-${uid}-${id}",
        mid = uid,
        username = username.ifBlank { "醒目留言" },
        message = message,
        timeText = eventTimeMs.toLiveChatTimeText(),
        likeText = priceText.orEmpty(),
        badgeText = "SC",
        color = 0xFFB84D
    )
}

private fun Long.toLiveChatTimeText(): String {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = if (this@toLiveChatTimeText < 10_000_000_000L) {
            this@toLiveChatTimeText * 1000L
        } else {
            this@toLiveChatTimeText
        }
    }
    return "${calendar.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')}:" +
        calendar.get(Calendar.MINUTE).toString().padStart(2, '0')
}

private fun buildLiveStatsText(
    playerDebugInfo: String,
    playbackSource: LivePlaybackSource?,
    videoWidth: Int,
    videoHeight: Int,
    liveStartTime: Long
): String {
    return buildString {
        val source = playbackSource
        if (source != null) {
            val currentMasterVariant = LiveStreamResolver.resolveCurrentMasterVariant(
                source = source,
                videoWidth = videoWidth,
                videoHeight = videoHeight
            )
            appendLine(
                "api current_qn: ${source.currentQuality.toLiveQualityStatsText(source, videoWidth, videoHeight)}"
            )
            source.playUrl.toQueryParamOrNull("qn")?.let { appendLine("play_url qn: $it") }
            appendLine("line: ${source.currentLineIndex + 1}/${source.lines.size.coerceAtLeast(1)}")
            source.currentLineOrNull()?.let { line ->
                appendLine("source type: ${line.sourceType.ifBlank { "-" }}")
                appendLine("source protocol: ${line.sourceProtocol.ifBlank { "-" }}")
                appendLine("container/url: ${line.container.ifBlank { source.playUrl.toLiveContainerOrDash() }}")
            }
            source.playUrl.toHostOrNull()?.let { appendLine("stream host: $it") }
            liveStartTime.toLiveUptimeTextOrNull()?.let { appendLine("live uptime: $it") }
            (
                playerDebugInfo.toLiveVideoFpsOrNull()
                    ?: currentMasterVariant?.frameRate?.toLiveFrameRateTextOrNull()
                )?.let { appendLine("video fps: $it") }
            appendLine("live bitrate boost: ${source.toLiveBitrateBoostStatsText()}")
            if (source.masterRequestedQn > 0) {
                appendLine("master requested qn: ${source.masterRequestedQn}")
            }
            currentMasterVariant?.let { variant ->
                appendLine("master variant:")
                variant.toStatsLines().forEach { line ->
                    appendLine("  $line")
                }
            }
        }
        append(playerDebugInfo.toLiveStatsPlayerDebugText())
    }.trim()
}

private fun String.toLiveStatsPlayerDebugText(): String {
    return lineSequence()
        .filterNot { it.startsWith("mime type:", ignoreCase = true) }
        .filterNot { it.startsWith("speed:", ignoreCase = true) }
        .filterNot { it.startsWith("stream host:", ignoreCase = true) }
        .filterNot { it.startsWith("video fps:", ignoreCase = true) }
        .filterNot { it.startsWith("buffered:", ignoreCase = true) }
        .joinToString("\n")
}

private fun buildLiveDanmakuDebugText(stats: LiveDanmakuDebugStats): String {
    return buildString {
        appendLine("live danmaku debug")
        appendLine("ws state: ${stats.wsState.toDebugName()}")
        appendWrappedDebugLine("ws host", stats.wsHost.ifBlank { "-" })
        if (stats.wsInfo.isNotBlank()) appendWrappedDebugLine("ws info", stats.wsInfo)
        if (stats.wsError.isNotBlank()) appendWrappedDebugLine("ws error", stats.wsError)
        appendLine("ws recv: ${stats.wsRecv}")
        appendLine("ws danmaku: ${stats.wsDanmaku}")
        appendLine("ws last: ${stats.wsLastDanmakuAtMs.toElapsedText()}")
        appendLine("overlay emitted: ${stats.overlayEmitted}")
    }.trim()
}

private fun StringBuilder.appendWrappedDebugLine(
    label: String,
    value: String,
    maxValueLineLength: Int = 42
) {
    val wrappedLines = value.wrapDebugValue(maxValueLineLength)
    append(label)
    append(": ")
    appendLine(wrappedLines.firstOrNull().orEmpty())
    wrappedLines.drop(1).forEach { line ->
        append("  ")
        appendLine(line)
    }
}

private fun String.wrapDebugValue(maxLineLength: Int): List<String> {
    if (length <= maxLineLength) return listOf(this)
    val chunks = mutableListOf<String>()
    var remaining = this
    while (remaining.length > maxLineLength) {
        val splitAt = remaining
            .take(maxLineLength + 1)
            .lastIndexOfAny(charArrayOf(' ', '/', '?', '&', '=', ':', ',', ';'))
            .takeIf { it in 16 until maxLineLength }
            ?: maxLineLength
        chunks += remaining.substring(0, splitAt).trim()
        remaining = remaining.substring(splitAt).trimStart()
    }
    if (remaining.isNotBlank()) chunks += remaining
    return chunks
}

private fun LiveDataWebSocketState.toDebugName(): String = name
    .lowercase(Locale.US)
    .replace('_', ' ')

private fun Long.toElapsedText(): String {
    if (this <= 0L) return "-"
    val elapsedMs = (System.currentTimeMillis() - this).coerceAtLeast(0L)
    return when {
        elapsedMs < 1_000L -> "now"
        elapsedMs < 60_000L -> "${elapsedMs / 1_000L}s ago"
        else -> "${elapsedMs / 60_000L}m ago"
    }
}

private fun Exception.toLivePlayerErrorText(): String {
    val playbackException = this as? PlaybackException
    val cause = cause
    val baseMessage = message ?: "直播播放失败"
    val errorCode = playbackException?.errorCodeName?.takeIf { it.isNotBlank() }
    val causeText = cause?.let {
        listOfNotNull(
            it.javaClass.simpleName,
            it.message?.takeIf(String::isNotBlank)
        ).joinToString(": ")
    }
    return buildString {
        append(baseMessage)
        errorCode?.let { append(" ($it)") }
        causeText?.let { append(": ").append(it) }
    }
}

private fun Long.toLiveUptimeTextOrNull(nowMs: Long = System.currentTimeMillis()): String? {
    if (this <= 0L) return null
    val startMs = if (this < 10_000_000_000L) this * 1000L else this
    val elapsedMs = (nowMs - startMs).coerceAtLeast(0L)
    return elapsedMs.formatHourMinSec()
}

private fun String.toHostOrNull(): String? {
    return runCatching { java.net.URI(this).host }.getOrNull()?.takeIf { it.isNotBlank() }
}

private fun LivePlaybackSource.toLiveBitrateBoostStatsText(): String =
    if (playUrl.contains("gotcha204b", ignoreCase = true)) {
        "on (gotcha204b)"
    } else {
        "off"
    }

private fun LivePlaybackSource.currentLineOrNull(): dev.aaa1115910.bv.repository.LiveLineOption? =
    lines.getOrNull(currentLineIndex)

private fun String.toLiveContainerOrDash(): String {
    val path = substringBefore('?').substringBefore('#').lowercase()
    return when {
        path.endsWith(".m3u8") -> "m3u8"
        path.endsWith(".flv") -> "flv"
        else -> "-"
    }
}

private fun Int.toLiveQualityStatsText(
    source: LivePlaybackSource,
    videoWidth: Int,
    videoHeight: Int
): String {
    val desc = LiveStreamResolver.buildPlaybackQualityLabel(
        source = source,
        videoWidth = videoWidth,
        videoHeight = videoHeight
    )
    return if (desc.isNullOrBlank()) toString() else "$this ($desc)"
}

private fun String.toQueryParamOrNull(name: String): String? {
    val query = runCatching { java.net.URI(this).rawQuery }.getOrNull()
        ?: substringAfter('?', missingDelimiterValue = "")
            .substringBefore('#')
            .takeIf { it.isNotBlank() }
        ?: return null
    return query
        .split('&')
        .firstNotNullOfOrNull { part ->
            val key = part.substringBefore('=', missingDelimiterValue = part)
            if (key == name) part.substringAfter('=', missingDelimiterValue = "") else null
        }
        ?.takeIf { it.isNotBlank() }
}

private fun dev.aaa1115910.bv.repository.LiveMasterVariant.toStatsLines(): List<String> {
    val resolution = if (width > 0 && height > 0) "${width}x$height" else "-"
    val fps = frameRate.toLiveFrameRateTextOrNull()?.let { "${it}fps" } ?: "-"
    val bitrate = bandwidth.takeIf { it > 0L }?.let { "${it / 1000}kbps" } ?: "-"
    val streamText = stream.takeIf { it.isNotBlank() } ?: "-"
    return listOf(
        "$inferredDisplayName qn=$qn res=$resolution fps=$fps",
        "    bw=$bitrate stream=$streamText"
    )
}

private fun String.toLiveVideoFpsOrNull(): String? =
    lineSequence()
        .firstOrNull { it.startsWith("video fps:", ignoreCase = true) }
        ?.substringAfter(':')
        ?.trim()
        ?.removeSuffixIgnoreCase("FPS")
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: lineSequence()
        .firstOrNull { it.startsWith("video info:", ignoreCase = true) }
        ?.substringAfter(':')
        ?.split(',')
        ?.map { it.trim() }
        ?.firstOrNull { it.endsWith("FPS", ignoreCase = true) }
        ?.removeSuffixIgnoreCase("FPS")
        ?.trim()
        ?.takeIf { it.isNotBlank() }

private fun Float.toLiveFrameRateTextOrNull(): String? {
    if (this <= 0f || isNaN()) return null
    val rounded = roundToInt()
    return if (abs(this - rounded) < 0.05f) {
        rounded.toString()
    } else {
        String.format(Locale.US, "%.1f", this)
    }
}

private fun String.removeSuffixIgnoreCase(suffix: String): String =
    if (endsWith(suffix, ignoreCase = true)) {
        dropLast(suffix.length)
    } else {
        this
    }
