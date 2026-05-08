package dev.aaa1115910.bv.component.controllers

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.entity.video.Subtitle
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.activities.video.VideoInfoActivity
import dev.aaa1115910.bv.entity.VideoAspectRatio
import dev.aaa1115910.bv.entity.VideoListItem
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.ui.state.PlayerState
import dev.aaa1115910.bv.ui.state.PlayerUiState
import dev.aaa1115910.bv.ui.state.SeekerState
import dev.aaa1115910.bv.util.VideoShotImageCache
import dev.aaa1115910.bv.util.PlayerUiTextFormatter
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.viewmodel.player.DanmakuSettingAction
import dev.aaa1115910.bv.viewmodel.player.MediaProfileSettingAction
import dev.aaa1115910.bv.viewmodel.player.PlayerOverlayState
import dev.aaa1115910.bv.viewmodel.player.PlayerSidePanel
import dev.aaa1115910.bv.viewmodel.player.SeekDirection
import dev.aaa1115910.bv.viewmodel.player.SeekTapAction
import dev.aaa1115910.bv.viewmodel.player.SeekTapPreviewState
import dev.aaa1115910.bv.viewmodel.player.SubtitleSettingAction
import dev.aaa1115910.bv.viewmodel.player.TempSpeedHoldState
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VideoPlayerController(
    modifier: Modifier = Modifier,
    aid: Long,
    fromSeason: Boolean,
    proxyArea: ProxyArea,
    seekStepMs: Long,

    // play state
    isLooping: Boolean,
    isPlaying: Boolean,
    
    // UI related state
    videoShotCache: VideoShotImageCache,
    uiState: PlayerUiState,
    seekerState: State<SeekerState>,

    // player events
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onExit: () -> Unit,
    onGoTime: (time: Long) -> Unit,
    onBackToStart: () -> Unit,
    onCancelSkipToNextEp: () -> Unit,
    onConfirmPendingPluginAction: () -> Unit,
    onDismissPendingPluginAction: () -> Unit,
    onPlayNewVideo: (VideoListItem) -> Unit,
    onToggleLoop: () -> Unit,
    onGoToUpPage: () -> Unit,
    upPanelUiState: PlayerUpPanelUiState = PlayerUpPanelUiState(),
    onUpPanelVideoClicked: (VideoCardData) -> Unit = {},
    onToggleUpPanelSort: () -> Unit = {},
    onToggleUpPanelFollow: () -> Unit = {},

    //menu events
    onMediaProfileSettingChange: (MediaProfileSettingAction) -> Unit,
    onAspectRatioChange: (VideoAspectRatio) -> Unit,
    onPlaySpeedChange: (Float) -> Unit,
    onShowPlayerStatsChange: (Boolean) -> Unit,
    onDanmakuSettingChange: (DanmakuSettingAction) -> Unit,
    onSubtitleChange: (Subtitle) -> Unit,
    onSubtitleSettingChange: (SubtitleSettingAction) -> Unit,
    onRelatedVideoClicked: (VideoCardData) -> Unit,

    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val logger = KotlinLogging.logger {}

    var showListController by remember { mutableStateOf(false) }
    var showMenuController by remember { mutableStateOf(false) }
    var showInfoSeekController by remember { mutableStateOf(false) }
    var overlayState by remember { mutableStateOf(PlayerOverlayState()) }
    val showClickableControllers by remember {
        derivedStateOf {
            showListController ||
                showMenuController ||
                showInfoSeekController ||
                overlayState.activePanel != PlayerSidePanel.None ||
                uiState.pendingPluginAction != null
        }
    }

    var lastPressBack by remember { mutableLongStateOf(0L) }
    var goTime by remember { mutableLongStateOf(0L) }

    var isSeeking by remember { mutableStateOf(false) }
    val tempSpeedHoldState = remember { TempSpeedHoldState() }
    val seekTapState = remember { SeekTapPreviewState() }

    var seekCountdown: Job? by remember { mutableStateOf(null) }
    var hideInfoSeekControllerCountdown: Job? by remember { mutableStateOf(null) }

    fun startSeekCountdown() {
        seekCountdown?.cancel()
        seekCountdown = scope.launch {
            delay(1000)

            onGoTime(goTime)
            if (!isPlaying) onPlay()

            isSeeking = false
            showInfoSeekController = false
            seekTapState.clearPreview()
            hideInfoSeekControllerCountdown?.cancel()
        }
    }

    fun applyDirectionalTap(direction: SeekDirection) {
        val currentBase = if (isSeeking) goTime else seekerState.value.currentTime
        when (
            val action = seekTapState.onDirectionalTap(
                direction = direction,
                nowMs = System.currentTimeMillis(),
                currentPositionMs = currentBase,
                totalDurationMs = seekerState.value.totalDuration,
                stepMs = seekStepMs
            )
        ) {
            is SeekTapAction.DirectJump -> {
                goTime = action.targetPositionMs
                isSeeking = false
                showInfoSeekController = false
                seekCountdown?.cancel()
                onGoTime(action.targetPositionMs)
            }

            is SeekTapAction.StartOrUpdatePreview -> {
                if (!isSeeking && isPlaying) onPause()
                goTime = action.targetPositionMs
                isSeeking = true
                showInfoSeekController = true
                startSeekCountdown()
            }
        }
    }

    fun onSeekGoTime() {
        onGoTime(goTime)
        isSeeking = false
        if (!isPlaying) onPlay()
        showInfoSeekController = false
        seekCountdown?.cancel()
        seekTapState.clearPreview()
    }

    fun cancelSeekPreview() {
        isSeeking = false
        showInfoSeekController = false
        seekCountdown?.cancel()
        seekTapState.clearPreview()
    }

    fun onPlayPause() {
        if (isPlaying) onPause() else onPlay()
    }

    fun handleKeyEvent(event: KeyEvent): Boolean {
        // 中键需要区分短按和长按
        val isConfirmKey =
            event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.Spacebar

        if (event.type == KeyEventType.KeyUp && !isConfirmKey) {
            return true
        }

        logger.info { "[${event.key} press]" }

        when (event.key) {
            Key.Back -> {
                if (showClickableControllers) {
                    if (isSeeking) {
                        cancelSeekPreview()
                    }
                    if (uiState.pendingPluginAction != null) {
                        onDismissPendingPluginAction()
                    }
                    showMenuController = false
                    showListController = false
                    if (!isSeeking) showInfoSeekController = false
                    overlayState = overlayState.closePanel()
                } else {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastPressBack < 3000) {
                        onExit()
                    } else {
                        lastPressBack = currentTime
                        R.string.video_player_press_back_again_to_exit.toast(context)
                    }
                }
                return true
            }

            Key.Menu -> {
                showInfoSeekController = false
                showMenuController = !showMenuController
                return true
            }

            Key(763) -> {
                showMenuController = true
                return true
            }

            Key.MediaPlayPause -> {
                onPlayPause()
                return true
            }

            Key.MediaPlay -> {
                if (!isPlaying) onPlay()
                return true
            }

            Key.MediaPause -> {
                if (isPlaying) onPause()
                return true
            }
        }

        when (event.key) {
            Key.DirectionCenter, Key.Enter, Key.Spacebar -> {
                if (event.type == KeyEventType.KeyDown) {
                    if (showClickableControllers && !isSeeking && uiState.pendingPluginAction == null) {
                        return false
                    }
                    if (event.nativeKeyEvent.isLongPress && !tempSpeedHoldState.isHoldingSpeed) {
                        tempSpeedHoldState.onLongPressTriggered(uiState.playSpeed)
                        onPlaySpeedChange(tempSpeedHoldState.temporarySpeed)
                    }
                    return true
                } else {
                    if (tempSpeedHoldState.isHoldingSpeed) {
                        onPlaySpeedChange(tempSpeedHoldState.onKeyReleased())
                    } else if (showClickableControllers && !isSeeking && uiState.pendingPluginAction == null) {
                        return false
                    } else if (uiState.pendingPluginAction != null) {
                        onConfirmPendingPluginAction()
                    } else if (isSeeking) {
                        onSeekGoTime()
                    } else if (!showClickableControllers) {
                        if (uiState.showBackToStart) {
                            onBackToStart()
                        } else {
                            onPlayPause()
                        }
                    }
                    return true
                }
            }
        }

        if (showClickableControllers) {
            return false
        }

        when (event.key) {
            Key.DirectionUp -> {
                showListController = true
                return true
            }

            Key.DirectionDown -> {
                showInfoSeekController = true
                return true
            }

            Key.MediaRewind, Key.DirectionLeft -> {
                if (uiState.showSkipToNextEp) onCancelSkipToNextEp()
                applyDirectionalTap(SeekDirection.Backward)
                return true
            }

            Key.MediaFastForward, Key.DirectionRight -> {
                applyDirectionalTap(SeekDirection.Forward)
                return true
            }
        }

        return false
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .focusable()
                .onPreviewKeyEvent { event ->
                // 重置 info 控制器的隐藏倒计时 (只要有按键活动就重置)
                if (showInfoSeekController) {
                    hideInfoSeekControllerCountdown?.cancel()
                    hideInfoSeekControllerCountdown = scope.launch {
                        delay(5000)
                        isSeeking = false
                        showInfoSeekController = false
                    }
                }
                // 调用分离出去的处理函数
                handleKeyEvent(event)
            }
    ) {
        content()
        if (uiState.showPlayerStats) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(Color.Black.copy(alpha = 0.3f))
            ) {
                Text(
                    modifier = Modifier.padding(8.dp),
                    text = seekerState.value.debugInfo
                )
            }
        }

        if (showClickableControllers && uiState.onlineCount != null && uiState.onlineCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 28.dp, bottom = 92.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(Color.Black.copy(alpha = 0.36f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.contact_page_24px),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                    Text(
                        text = PlayerUiTextFormatter.onlineCount(uiState.onlineCount),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
        if (uiState.subtitleId != -1L) {
            val currentTime = seekerState.value.currentTime

            BottomSubtitle(
                subtitleData = uiState.subtitleData,
                currentTime = currentTime,
                fontSize = uiState.subtitleState.fontSize,
                opacity = uiState.subtitleState.opacity,
                padding = uiState.subtitleState.bottomPadding,
            )
        }

        SkipTips(
            showBackToStart = uiState.showBackToStart,
            showSkipToNextEp = uiState.showSkipToNextEp,
            showPreviewTip = uiState.showPreviewTip,
            pluginTipMessage = uiState.pluginTipMessage,
        )

        PlayStateTips(
            isPlaying = uiState.playerState == PlayerState.Playing,
            isBuffering = uiState.isBuffering,
            isError = uiState.playerState is PlayerState.Error,
            errorMessage = (uiState.playerState as? PlayerState.Error)?.message,
        )

        PlayerSidePanels(
            activePanel = overlayState.activePanel,
            relatedVideos = uiState.relatedVideos,
            upPanelUiState = upPanelUiState,
            onClose = { overlayState = overlayState.closePanel() },
            onRelatedVideoClicked = {
                onRelatedVideoClicked(it)
                overlayState = overlayState.closePanel()
            },
            onUpVideoClicked = {
                onUpPanelVideoClicked(it)
                overlayState = overlayState.closePanel()
            },
            onToggleUpSort = onToggleUpPanelSort,
            onToggleUpFollow = onToggleUpPanelFollow
        )

        ControllerVideoInfo(
            modifier = Modifier.focusable(),
            show = showInfoSeekController,
            isSeeking = isSeeking,
            goTime = goTime,
            seekerState = seekerState.value,
            title = uiState.title,
            clock = uiState.clock,
            videoShot = uiState.videoShot,
            videoShotCache = videoShotCache,
            fromSeason = fromSeason,
            danmakuEnabled = uiState.danmakuState.enabledTypes.isNotEmpty(),
            isLooping = isLooping,
            onDirectionLeft = { applyDirectionalTap(SeekDirection.Backward) },
            onDirectionRight = { applyDirectionalTap(SeekDirection.Forward) },
            onSeekGoTime = { onSeekGoTime() },
            onPlayPause = { onPlayPause() },
            onDanmakuSwitchChange = {
                if (uiState.danmakuState.enabledTypes.isEmpty()) {
                    onDanmakuSettingChange(DanmakuSettingAction.SetEnabledTypes(DanmakuType.entries))
                } else {
                    onDanmakuSettingChange(DanmakuSettingAction.SetEnabledTypes(emptyList()))
                }
            },
            onShowSettings = {
                showInfoSeekController = false
                showMenuController = true
            },
            onShowRelatedVideos = {
                showInfoSeekController = false
                overlayState = overlayState.open(PlayerSidePanel.RelatedVideos)
            },
            onGoToVideoInfo = {
                VideoInfoActivity.actionStart(
                    context = context,
                    aid = aid,
                    fromSeason = fromSeason,
                    fromController = true,
                    proxyArea = proxyArea
                )
            },
            onToggleLoop = onToggleLoop,
            onGoToUpPage = {
                showInfoSeekController = false
                onGoToUpPage()
                overlayState = overlayState.open(PlayerSidePanel.UpSpace)
            }
        )

        VideoListController(
            show = showListController,
            currentCid = uiState.cid,
            videoList = uiState.availableVideoList,
            onPlayNewVideo = onPlayNewVideo
        )

        MenuController(
            show = showMenuController,
            uiState = uiState,
            onResolutionChange = { qualityId ->
                onMediaProfileSettingChange(
                    MediaProfileSettingAction.SetQuality(qualityId)
                )
            },
            onCodecChange = { codec ->
                onMediaProfileSettingChange(
                    MediaProfileSettingAction.SetVideoCodec(codec)
                )
            },
            onAudioChange = { audio ->
                onMediaProfileSettingChange(
                    MediaProfileSettingAction.SetAudio(audio)
                )
            },
            onAspectRatioChange = onAspectRatioChange,
            onPlaySpeedChange = onPlaySpeedChange,
            onShowPlayerStatsChange = onShowPlayerStatsChange,
            onDanmakuSwitchChange = { danmakuTypes ->
                onDanmakuSettingChange(DanmakuSettingAction.SetEnabledTypes(danmakuTypes))
            },
            onDanmakuSizeChange = { scale ->
                onDanmakuSettingChange(DanmakuSettingAction.SetScale(scale))
            },
            onDanmakuOpacityChange = { opacity ->
                onDanmakuSettingChange(DanmakuSettingAction.SetOpacity(opacity))
            },
            onDanmakuSpeedFactorChange = { factor ->
                onDanmakuSettingChange(DanmakuSettingAction.SetSpeedFactor(factor))
            },
            onDanmakuAreaChange = { area ->
                onDanmakuSettingChange(DanmakuSettingAction.SetArea(area))
            },
            onDanmakuMaskChange = { enabled ->
                onDanmakuSettingChange(DanmakuSettingAction.SetMaskEnabled(enabled))
            },
            onSubtitleChange = onSubtitleChange,
            onSubtitleSizeChange = { size ->
                onSubtitleSettingChange(SubtitleSettingAction.SetFontSize(size))
            },
            onSubtitleBackgroundOpacityChange = { opacity ->
                onSubtitleSettingChange(SubtitleSettingAction.SetOpacity(opacity))
            },
            onSubtitleBottomPadding = { padding ->
                onSubtitleSettingChange(SubtitleSettingAction.SetBottomPadding(padding))
            }
        )
    }
}
