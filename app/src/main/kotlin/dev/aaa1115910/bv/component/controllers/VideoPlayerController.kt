package dev.aaa1115910.bv.component.controllers

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.entity.video.Subtitle
import dev.aaa1115910.bv.BuildConfig
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.activities.video.VideoInfoActivity
import dev.aaa1115910.bv.entity.PlayerCommentItem
import dev.aaa1115910.bv.entity.VideoAspectRatio
import dev.aaa1115910.bv.entity.VideoListItem
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.ui.state.PlayerState
import dev.aaa1115910.bv.ui.state.PlayerUiState
import dev.aaa1115910.bv.ui.state.SeekerState
import dev.aaa1115910.bv.util.PlayerUiTextFormatter
import dev.aaa1115910.bv.util.VideoShotImageCache
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

internal fun hasSecondaryControllerOverlay(
    showListController: Boolean,
    showMenuController: Boolean,
    activePanel: PlayerSidePanel
): Boolean {
    return showListController || showMenuController || activePanel != PlayerSidePanel.None
}

internal fun hasClickableControllerOverlay(
    showListController: Boolean,
    showMenuController: Boolean,
    showInfoSeekController: Boolean,
    activePanel: PlayerSidePanel
): Boolean {
    return showListController ||
        showMenuController ||
        showInfoSeekController ||
        activePanel != PlayerSidePanel.None
}

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
    onPlayNewVideo: (VideoListItem) -> Unit,
    onToggleLoop: () -> Unit,
    onToggleJumpMode: () -> Unit,
    onJumpToPreviousVideo: () -> Unit,
    onJumpToNextVideo: () -> Unit,
    upPanelUiState: PlayerUpPanelUiState,
    commentPanelUiState: PlayerCommentPanelUiState,
    onOpenUpPanel: () -> Unit,
    onOpenCommentsPanel: () -> Unit,
    onUpVideoClicked: (VideoCardData) -> Unit,
    onToggleUpSort: () -> Unit,
    onToggleUpFollow: () -> Unit,
    onToggleCommentSort: () -> Unit,
    onLoadMoreComments: () -> Unit,
    onCommentListPositionChanged: (Int, Int) -> Unit,
    onOpenCommentDetail: (PlayerCommentItem) -> Unit,
    onCloseCommentDetail: () -> Unit,
    onOpenCommentUpPage: (Long, String) -> Unit,
    onCommentLike: (PlayerCommentItem) -> Unit,
    onCommentDislike: (PlayerCommentItem) -> Unit,

    //menu events
    onMediaProfileSettingChange: (MediaProfileSettingAction) -> Unit,
    onAspectRatioChange: (VideoAspectRatio) -> Unit,
    onPlaySpeedChange: (Float) -> Unit,
    setShowPlayerStats: (Boolean) -> Unit,
    onDanmakuSettingChange: (DanmakuSettingAction) -> Unit,
    onSubtitleChange: (Subtitle) -> Unit,
    onSubtitleSettingChange: (SubtitleSettingAction) -> Unit,
    onRelatedVideoClicked: (VideoCardData) -> Unit,
    confirmPendingPluginAction: () -> Unit,
    dismissPendingPluginAction: () -> Unit,
    showEndedRelatedVideosToken: Int = 0,

    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val logger = KotlinLogging.logger {}

    var showListController by remember { mutableStateOf(false) }
    var showMenuController by remember { mutableStateOf(false) }
    var showInfoSeekController by remember { mutableStateOf(false) }
    var showRelatedVideosController by remember { mutableStateOf(false) }
    var overlayState by remember { mutableStateOf(PlayerOverlayState()) }
    val hasSecondaryOverlayOpen = showRelatedVideosController || hasSecondaryControllerOverlay(
        showListController = showListController,
        showMenuController = showMenuController,
        activePanel = overlayState.activePanel
    )
    val showJumpModePausedInfoController =
        uiState.jumpModeState.enabled &&
            !isPlaying &&
            !uiState.isBuffering &&
            uiState.playerState !is PlayerState.Error &&
            uiState.playerState != PlayerState.Ended &&
            !hasSecondaryOverlayOpen
    val showClickableControllers = showRelatedVideosController || hasClickableControllerOverlay(
        showListController = showListController,
        showMenuController = showMenuController,
        showInfoSeekController = showInfoSeekController,
        activePanel = overlayState.activePanel
    )
    val showPrimaryInfoController = showInfoSeekController || showJumpModePausedInfoController

    var lastPressBack by remember { mutableLongStateOf(0L) }
    var goTime by remember { mutableLongStateOf(0L) }

    var isSeeking by remember { mutableStateOf(false) }
    var resumeAfterSeekPreview by remember { mutableStateOf(false) }
    var seekChangeCount by remember { mutableIntStateOf(0) }
    var lastSeekChangeTime by remember { mutableLongStateOf(0L) }
    val seekTapState = remember { SeekTapPreviewState() }
    val tempSpeedHoldState = remember { TempSpeedHoldState() }

    var seekCountdown: Job? by remember { mutableStateOf(null) }
    var hideInfoSeekControllerCountdown: Job? by remember { mutableStateOf(null) }
    var jumpModeDownHoldJob: Job? by remember { mutableStateOf(null) }
    var jumpModeDownHoldConsumed by remember { mutableStateOf(false) }
    var showInitialOnlineCountTip by remember { mutableStateOf(false) }
    var onlineCountTipVideoKey: Pair<Long, Long>? by remember { mutableStateOf(null) }
    val videoListPanelState = remember(uiState.availableVideoList, uiState.cid) {
        resolveVideoListPanelState(
            currentCid = uiState.cid,
            videoList = uiState.availableVideoList
        )
    }
    val onlineCountVideoKey = uiState.aid to uiState.cid
    val canShowInitialOnlineCountTip =
        !uiState.onlineCountText.isNullOrBlank() && uiState.playerState == PlayerState.Playing

    fun calCoefficient(): Int {
        return if (System.currentTimeMillis() - lastSeekChangeTime < 200) {
            seekChangeCount++
            seekChangeCount / 5
        } else {
            seekChangeCount = 0
            0
        }
    }

    fun onTimeForward() {
        isSeeking = true
        val targetTime = goTime + (10000 + calCoefficient() * 5000)
        goTime =
            if (targetTime > seekerState.value.totalDuration) seekerState.value.totalDuration else targetTime
        lastSeekChangeTime = System.currentTimeMillis()
        logger.info { "onTimeForward: [goTime=$goTime]" }
    }

    fun onTimeBack() {
        isSeeking = true
        val targetTime = goTime - (10000 + calCoefficient() * 5000)
        goTime = if (targetTime < 0) 0 else targetTime
        lastSeekChangeTime = System.currentTimeMillis()
        logger.info { "onTimeBack: [goTime=$goTime]" }
    }

    fun onDirectionLeft() {
        if (!isSeeking && isPlaying) onPause()
        if (!isSeeking) goTime = seekerState.value.currentTime
        onTimeBack()
    }

    fun onDirectionRight() {
        if (!isSeeking && isPlaying) onPause()
        if (!isSeeking) goTime = seekerState.value.currentTime
        onTimeForward()
    }

    fun applyOuterDirectionalTap(direction: SeekDirection) {
        when (
            val action = seekTapState.onDirectionalTap(
                direction = direction,
                nowMs = System.currentTimeMillis(),
                currentPositionMs = seekerState.value.currentTime,
                totalDurationMs = seekerState.value.totalDuration,
                stepMs = seekStepMs
            )
        ) {
            is SeekTapAction.PendingDirectJump -> {
                goTime = action.targetPositionMs
                isSeeking = false
                showInfoSeekController = false
                seekCountdown?.cancel()
                seekCountdown = scope.launch {
                    delay(1_000L)
                    onGoTime(action.targetPositionMs)
                    seekTapState.clearPendingJump()
                    seekCountdown = null
                }
            }

            is SeekTapAction.StartOrUpdatePreview -> {
                if (!isSeeking) {
                    resumeAfterSeekPreview = isPlaying
                    if (resumeAfterSeekPreview) onPause()
                }
                goTime = action.targetPositionMs
                isSeeking = true
                showInfoSeekController = true
                seekCountdown?.cancel()
                seekCountdown = null
            }
        }
    }

    fun onSeekGoTime() {
        onGoTime(goTime)
        isSeeking = false
        if (resumeAfterSeekPreview) onPlay()
        resumeAfterSeekPreview = false
        showInfoSeekController = false
        seekCountdown?.cancel()
        seekTapState.clearPreview()
    }

    fun cancelSeekPreview() {
        isSeeking = false
        if (resumeAfterSeekPreview) onPlay()
        resumeAfterSeekPreview = false
        showInfoSeekController = false
        seekCountdown?.cancel()
        seekTapState.clearPreview()
    }

    fun triggerJumpModeDownHold(): Boolean {
        if (jumpModeDownHoldConsumed) return true
        jumpModeDownHoldConsumed = true
        jumpModeDownHoldJob?.cancel()
        jumpModeDownHoldJob = null
        onToggleJumpMode()
        return true
    }

    fun startJumpModeDownHold() {
        if (jumpModeDownHoldJob?.isActive == true) return
        jumpModeDownHoldConsumed = false
        jumpModeDownHoldJob = scope.launch {
            delay(3_000L)
            triggerJumpModeDownHold()
            jumpModeDownHoldJob = null
        }
    }

    fun cancelJumpModeDownHold(): Boolean {
        val consumed = jumpModeDownHoldConsumed
        jumpModeDownHoldJob?.cancel()
        jumpModeDownHoldJob = null
        jumpModeDownHoldConsumed = false
        return consumed
    }

    fun onPlayPause() {
        if (isPlaying) onPause() else onPlay()
    }

    fun handleKeyEvent(event: KeyEvent): Boolean {
        if (overlayState.activePanel != PlayerSidePanel.None) {
            return false
        }

        // 中键需要区分短按和长按
        val isConfirmKey =
            event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.Spacebar

        if (isConfirmKey && event.type == KeyEventType.KeyUp && tempSpeedHoldState.isHoldingSpeed) {
            onPlaySpeedChange(tempSpeedHoldState.onKeyReleased())
            return true
        }

        if (
            event.key == Key.DirectionUp &&
            uiState.jumpModeState.enabled &&
            !showJumpModePausedInfoController &&
            !hasSecondaryOverlayOpen
        ) {
            when (event.type) {
                KeyEventType.KeyDown -> return true
                KeyEventType.KeyUp -> {
                    onJumpToPreviousVideo()
                    return true
                }
            }
        }

        if (
            event.key == Key.DirectionDown &&
            !showJumpModePausedInfoController &&
            !hasSecondaryOverlayOpen
        ) {
            when (event.type) {
                KeyEventType.KeyDown -> {
                    if (event.nativeKeyEvent.repeatCount == 0) {
                        startJumpModeDownHold()
                    }
                    val heldMs = event.nativeKeyEvent.eventTime - event.nativeKeyEvent.downTime
                    if (event.nativeKeyEvent.repeatCount > 0 && heldMs >= 3_000L) {
                        return triggerJumpModeDownHold()
                    }
                    if (uiState.jumpModeState.enabled) return true
                    if (showClickableControllers) return false
                    return true
                }

                KeyEventType.KeyUp -> {
                    val wasConsumedByLongPress = cancelJumpModeDownHold()
                    if (wasConsumedByLongPress) return true
                    if (uiState.jumpModeState.enabled) {
                        onJumpToNextVideo()
                        return true
                    }
                    if (!showClickableControllers) {
                        showInfoSeekController = true
                        return true
                    }
                    return false
                }
            }
        }

        if (event.type == KeyEventType.KeyUp && !isConfirmKey) {
            return true
        }

        logger.info { "[${event.key} press]" }

        when (event.key) {
            Key.Back -> {
                if (uiState.pendingPluginAction != null) {
                    dismissPendingPluginAction()
                    return true
                }
                if (showClickableControllers && (hasSecondaryOverlayOpen || isSeeking || showInfoSeekController)) {
                    if (hasSecondaryOverlayOpen) {
                        showMenuController = false
                        showListController = false
                        showRelatedVideosController = false
                        overlayState = overlayState.closePanel()
                        showInfoSeekController = isSeeking
                    } else if (isSeeking) {
                        cancelSeekPreview()
                    } else {
                        showInfoSeekController = false
                    }
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

        if (showClickableControllers || showJumpModePausedInfoController) {
            return false
        } else {
            when (event.key) {
                Key.DirectionCenter, Key.Enter, Key.Spacebar -> {
                    if (event.type == KeyEventType.KeyDown) {
                        if (!showClickableControllers && event.nativeKeyEvent.isLongPress) {
                            if (!tempSpeedHoldState.isHoldingSpeed) {
                                tempSpeedHoldState.onLongPressTriggered(uiState.playSpeed)
                                onPlaySpeedChange(tempSpeedHoldState.temporarySpeed)
                            }
                            return true
                        }
                        return true
                    } else {
                        if (uiState.showBackToStart) {
                            onBackToStart()
                        } else if (uiState.pendingPluginAction != null) {
                            confirmPendingPluginAction()
                        } else {
                            onPlayPause()
                        }
                        return true
                    }
                }

                Key.DirectionUp -> {
                    if (uiState.jumpModeState.enabled) {
                        if (event.nativeKeyEvent.repeatCount > 0) return true
                        onJumpToPreviousVideo()
                        return true
                    }
                    showListController = true
                    return true
                }

                Key.DirectionDown -> {
                    showInfoSeekController = true
                    return true
                }

                Key.MediaRewind, Key.DirectionLeft -> {
                    if (uiState.showSkipToNextEp) onCancelSkipToNextEp()
                    applyOuterDirectionalTap(SeekDirection.Backward)
                    return true
                }

                Key.MediaFastForward, Key.DirectionRight -> {
                    applyOuterDirectionalTap(SeekDirection.Forward)
                    return true
                }
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
                    if (!isSeeking) {
                        hideInfoSeekControllerCountdown = scope.launch {
                            delay(5000)
                            showInfoSeekController = false
                        }
                    }
                }
                // 调用分离出去的处理函数
                handleKeyEvent(event)
            }
    ) {
        LaunchedEffect(showEndedRelatedVideosToken) {
            if (showEndedRelatedVideosToken > 0) {
                showInfoSeekController = false
                showMenuController = false
                showListController = false
                overlayState = overlayState.closePanel()
                showRelatedVideosController = true
            }
        }
        LaunchedEffect(onlineCountVideoKey) {
            onlineCountTipVideoKey = null
            showInitialOnlineCountTip = false
        }
        LaunchedEffect(onlineCountVideoKey, canShowInitialOnlineCountTip) {
            if (!canShowInitialOnlineCountTip) {
                showInitialOnlineCountTip = false
                return@LaunchedEffect
            }
            if (onlineCountTipVideoKey == onlineCountVideoKey) return@LaunchedEffect

            onlineCountTipVideoKey = onlineCountVideoKey
            showInitialOnlineCountTip = true
            delay(3_000L)
            if (onlineCountTipVideoKey == onlineCountVideoKey) {
                showInitialOnlineCountTip = false
            }
        }
        content()
        if (BuildConfig.DEBUG || uiState.showPlayerStats) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 8.dp, top = if (uiState.showPlayerStats) 56.dp else 8.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(Color.Black.copy(alpha = 0.3f))
            ) {
                Text(
                    modifier = Modifier.padding(8.dp),
                    text = seekerState.value.debugInfo
                )
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
            showOnlineCount = !uiState.onlineCountText.isNullOrBlank() &&
                (showInitialOnlineCountTip || showPrimaryInfoController),
            onlineCountText = uiState.onlineCountText?.let { PlayerUiTextFormatter.onlineCount(it) },
            pluginTipMessage = uiState.pluginTipMessage,
        )

        PlayStateTips(
            isPlaying = uiState.playerState == PlayerState.Playing,
            isBuffering = uiState.isBuffering,
            isError = uiState.playerState is PlayerState.Error,
            errorMessage = (uiState.playerState as? PlayerState.Error)?.message,
        )

        RelatedVideosController(
            show = showRelatedVideosController,
            relatedVideos = uiState.relatedVideos,
            onVideoClicked = {
                onRelatedVideoClicked(it)
                showRelatedVideosController = false
            }
        )

        ControllerVideoInfo(
            modifier = Modifier.focusable(),
            show = showPrimaryInfoController,
            isSeeking = isSeeking,
            goTime = goTime,
            seekerState = seekerState.value,
            title = uiState.title,
            authorName = uiState.authorName,
            publishDateText = uiState.publishDateText,
            playCountText = uiState.playCountText,
            videoListButtonLabel = videoListPanelState.buttonLabel,
            sponsorBlockProgressMarks = uiState.sponsorBlockProgressMarks,
            watchedProgressMarks = uiState.watchedProgressMarks,
            videoHeatmap = uiState.videoHeatmap,
            clock = uiState.clock,
            videoShot = uiState.videoShot,
            videoShotCache = videoShotCache,
            fromSeason = fromSeason,
            danmakuEnabled = uiState.danmakuState.enabledTypes.isNotEmpty(),
            jumpModeState = uiState.jumpModeState,
            isLooping = isLooping,
            onDirectionLeft = { onDirectionLeft() },
            onDirectionRight = { onDirectionRight() },
            onSeekGoTime = { onSeekGoTime() },
            onPlayPause = { onPlayPause() },
            onShowVideoList = {
                showInfoSeekController = false
                showListController = true
            },
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
            onToggleJumpMode = onToggleJumpMode,
            onShowRelatedVideos = {
                showInfoSeekController = false
                showMenuController = false
                showListController = false
                overlayState = overlayState.open(PlayerSidePanel.RelatedVideos)
            },
            onShowComments = {
                showInfoSeekController = false
                showMenuController = false
                showListController = false
                showRelatedVideosController = false
                onOpenCommentsPanel()
                overlayState = overlayState.open(PlayerSidePanel.Comments)
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
                showListController = false
                showMenuController = false
                showRelatedVideosController = false
                onOpenUpPanel()
                overlayState = overlayState.open(PlayerSidePanel.UpSpace)
            }
        )

        PlayerSidePanels(
            activePanel = overlayState.activePanel,
            relatedVideos = uiState.relatedVideos,
            upPanelUiState = upPanelUiState,
            commentPanelUiState = commentPanelUiState,
            onClose = {
                overlayState = overlayState.closePanel()
            },
            onRelatedVideoClicked = { video ->
                onRelatedVideoClicked(video)
                overlayState = overlayState.closePanel()
            },
            onUpVideoClicked = { video ->
                onUpVideoClicked(video)
                overlayState = overlayState.closePanel()
            },
            onOpenUpPage = onOpenCommentUpPage,
            onToggleUpSort = onToggleUpSort,
            onToggleUpFollow = onToggleUpFollow,
            onToggleCommentSort = onToggleCommentSort,
            onLoadMoreComments = onLoadMoreComments,
            onCommentListPositionChanged = onCommentListPositionChanged,
            onOpenCommentDetail = onOpenCommentDetail,
            onCloseCommentDetail = onCloseCommentDetail,
            onCommentLike = onCommentLike,
            onCommentDislike = onCommentDislike
        )

        VideoListController(
            show = showListController,
            currentCid = uiState.cid,
            panelState = videoListPanelState,
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
            onShowPlayerStatsChange = setShowPlayerStats,
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
