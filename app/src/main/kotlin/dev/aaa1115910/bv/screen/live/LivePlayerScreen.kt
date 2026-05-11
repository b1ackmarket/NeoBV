package dev.aaa1115910.bv.screen.live

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.activities.video.VideoInfoActivity
import dev.aaa1115910.bv.component.controllers.LiveDanmakuMenuState
import dev.aaa1115910.bv.component.controllers.LiveBottomMenuController
import dev.aaa1115910.bv.component.controllers.LiveBottomMenuItem
import dev.aaa1115910.bv.component.controllers.LiveMenuController
import dev.aaa1115910.bv.component.controllers.PlayerSidePanels
import dev.aaa1115910.bv.component.controllers.PlayerUpPanelUiState
import dev.aaa1115910.bv.component.controllers.DanmakuType
import dev.aaa1115910.bv.component.DanmakuPlayerCompose
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.entity.user.SpaceVideoOrder
import dev.aaa1115910.biliapi.http.BiliHttpApi
import dev.aaa1115910.biliapi.http.entity.live.DanmakuEvent
import dev.aaa1115910.biliapi.repositories.UserRepository
import dev.aaa1115910.biliapi.websocket.LiveDataWebSocket
import dev.aaa1115910.bv.player.BvVideoPlayer
import dev.aaa1115910.bv.player.VideoPlayerOptions
import dev.aaa1115910.bv.player.VideoPlayerListener
import dev.aaa1115910.bv.player.impl.exo.ExoPlayerFactory
import dev.aaa1115910.bv.repository.LivePlaybackSource
import dev.aaa1115910.bv.repository.LiveRepository
import dev.aaa1115910.bv.repository.LiveRoomContext
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.formatHourMinSec
import dev.aaa1115910.bv.util.toWanString
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.viewmodel.player.PlayerSidePanel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

private fun currentClockText(): String {
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
    val minute = calendar.get(Calendar.MINUTE).toString().padStart(2, '0')
    return "$hour:$minute"
}

@Composable
fun LivePlayerScreen() {
    val context = LocalContext.current
    val activity = context as Activity
    val roomId = remember { activity.intent.getIntExtra("room_id", 0) }
    val title = remember { activity.intent.getStringExtra("title").orEmpty() }
    val upName = remember { activity.intent.getStringExtra("up_name").orEmpty() }
    var online by remember { mutableStateOf(activity.intent.getIntExtra("online", 0)) }
    val liveRepository = remember { BVApp.koinApplication.koin.get<LiveRepository>() }
    val apiUserRepository = remember { BVApp.koinApplication.koin.get<UserRepository>() }
    var roomContext by remember { mutableStateOf<LiveRoomContext?>(null) }
    var playbackSource by remember { mutableStateOf<LivePlaybackSource?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var statusText by remember { mutableStateOf<String?>(null) }
    var activeOverlay by remember { mutableStateOf(LiveOverlayPanel.None) }
    var lastBackPressedAt by remember { mutableStateOf(0L) }
    var clockText by remember { mutableStateOf(currentClockText()) }
    var reloadToken by remember { mutableStateOf(0) }
    var selectedQuality by remember { mutableStateOf(Prefs.defaultLiveQuality.qn) }
    var selectedLineIndex by remember { mutableStateOf(0) }
    var liveDanmakuSocketJob by remember { mutableStateOf<Job?>(null) }
    var liveUpPanelVideos by remember { mutableStateOf<List<VideoCardData>>(emptyList()) }
    var liveUpPanelOrder by remember { mutableStateOf(SpaceVideoOrder.PubDate) }
    var liveUpPanelFace by remember { mutableStateOf("") }
    var liveUpPanelFollowing by remember { mutableStateOf(false) }
    val liveScope = rememberCoroutineScope()
    var liveDanmakuState by remember {
        mutableStateOf(
            LiveDanmakuMenuState(
                enabledTypes = Prefs.defaultDanmakuTypes,
                scale = Prefs.defaultDanmakuScale,
                opacity = Prefs.defaultDanmakuOpacity,
                speedFactor = Prefs.defaultDanmakuSpeedFactor,
                area = Prefs.defaultDanmakuArea,
                maskEnabled = Prefs.defaultDanmakuMask
            )
        )
    }
    val liveDanmakuSession = remember { LiveDanmakuSession() }
    val player by remember {
        mutableStateOf(
            ExoPlayerFactory().create(
                context = context,
                options = VideoPlayerOptions(
                    userAgent = "Mozilla/5.0",
                    referer = "https://live.bilibili.com/",
                    enableFfmpegAudioRenderer = false,
                    enableSoftwareVideoDecoder = false
                )
            )
        )
    }
    val screenFocusRequester = remember { FocusRequester() }
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
                    errorMessage = error.message ?: "直播播放失败"
                    isLoading = false
                }

                override fun onReady() {
                    isLoading = false
                    errorMessage = null
                }

                override fun onPlay() {
                    liveDanmakuSession.start()
                }

                override fun onPause() {
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

    LaunchedEffect(roomId) {
        val result = withContext(Dispatchers.IO) {
            runCatching { liveRepository.resolveRoomContext(roomId) }
        }
        result
            .onSuccess { roomContext = it }
            .onFailure {
                errorMessage = resolveLivePlaybackErrorMessage(
                    liveStatus = null,
                    source = null,
                    throwable = it
                )
                isLoading = false
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
        }
    }

    LaunchedEffect(roomContext?.roomId) {
        val resolvedRoomId = roomContext?.roomId ?: return@LaunchedEffect
        if ((roomContext?.liveStatus ?: 0) != 1) return@LaunchedEffect
        liveDanmakuSocketJob?.cancel()
        liveDanmakuSocketJob = LiveDataWebSocket.connectLiveEvent(resolvedRoomId) { event ->
            if (event is DanmakuEvent && liveDanmakuState.enabledTypes.isNotEmpty()) {
                liveDanmakuSession.addDanmaku(
                    event = event,
                    isPlaying = player.isPlaying
                )
            }
        }
    }

    LaunchedEffect(liveDanmakuState) {
        liveDanmakuSession.applyState(liveDanmakuState)
    }

    LaunchedEffect(roomContext?.roomId, selectedQuality, selectedLineIndex, reloadToken) {
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
                val source = liveRepository.resolvePlayableSource(
                    roomId = resolvedRoomId,
                    qn = selectedQuality,
                    lineIndex = selectedLineIndex
                )
                val history = liveRepository.getHistoryDanmaku(resolvedRoomId)
                source to history
            }
        }
        val (resolvedSource, historyDanmaku) = playbackResult.getOrElse {
            errorMessage = resolveLivePlaybackErrorMessage(
                liveStatus = roomContext?.liveStatus,
                source = null,
                throwable = it
            )
            isLoading = false
            return@LaunchedEffect
        }
        if (resolvedSource == null) {
            errorMessage = resolveLivePlaybackErrorMessage(
                liveStatus = roomContext?.liveStatus,
                source = null
            )
            isLoading = false
        } else {
            playbackSource = resolvedSource
            player.stop()
            liveDanmakuSession.clear()
            player.setOptions()
            player.playUrl(resolvedSource.playUrl, null)
            player.prepare()
            player.start()
            liveDanmakuSession.seedRecentDanmaku(
                events = historyDanmaku,
                isPlaying = true
            )
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

    DisposableEffect(Unit) {
        onDispose {
            liveDanmakuSocketJob?.cancel()
            player.pause()
            player.release()
            liveDanmakuSession.release()
        }
    }

    val showTopOverlay = activeOverlay == LiveOverlayPanel.RightMenu || activeOverlay == LiveOverlayPanel.BottomMenu
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
                if (event.type == KeyEventType.KeyUp) return@onPreviewKeyEvent false
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

                    Key.DirectionCenter, Key.Enter, Key.Spacebar -> {
                        false
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
                            activeOverlay = toggleLiveRightMenu(activeOverlay)
                            true
                        } else {
                            false
                        }
                    }

                    else -> false
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
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
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
                DanmakuPlayerCompose(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(liveAspectRatio),
                    danmakuPlayer = liveDanmakuSession.player
                )
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
            modifier = Modifier.align(TopCenter)
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
            qualityOptions = playbackSource?.qualities.orEmpty(),
            currentQuality = playbackSource?.currentQuality ?: selectedQuality,
            lineOptions = playbackSource?.lines.orEmpty(),
            currentLineIndex = playbackSource?.currentLineIndex ?: selectedLineIndex,
            danmakuState = liveDanmakuState,
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
                Prefs.defaultDanmakuTypes = newState.enabledTypes
                Prefs.defaultDanmakuScale = newState.scale
                Prefs.defaultDanmakuOpacity = newState.opacity
                Prefs.defaultDanmakuSpeedFactor = newState.speedFactor
                Prefs.defaultDanmakuArea = newState.area
                Prefs.defaultDanmakuMask = newState.maskEnabled
            }
        )

        LiveBottomMenuController(
            modifier = Modifier.align(Alignment.BottomCenter),
            show = activeOverlay == LiveOverlayPanel.BottomMenu,
            items = buildList {
                add(
                    LiveBottomMenuItem(
                        iconRes = R.drawable.related_videos_24px,
                        label = "刷新"
                    ) {
                        statusText = "正在刷新直播流…"
                        player.stop()
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
                            DanmakuType.entries
                        } else {
                            emptyList()
                        }
                        liveDanmakuState = liveDanmakuState.copy(enabledTypes = nextTypes)
                        Prefs.defaultDanmakuTypes = nextTypes
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
            activePanel = if (activeOverlay == LiveOverlayPanel.UpSpace) {
                PlayerSidePanel.UpSpace
            } else {
                PlayerSidePanel.None
            },
            relatedVideos = emptyList(),
            upPanelUiState = PlayerUpPanelUiState(
                upName = upName,
                upFace = liveUpPanelFace,
                latestSelected = liveUpPanelOrder == SpaceVideoOrder.PubDate,
                isFollowing = liveUpPanelFollowing,
                videos = liveUpPanelVideos
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
            }
        )
    }
}
