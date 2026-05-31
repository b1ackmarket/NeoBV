package dev.aaa1115910.bv.component.controllers

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.media.AudioManager
import android.provider.Settings
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.aaa1115910.bv.viewmodel.player.PlayerTouchDragMode
import dev.aaa1115910.bv.viewmodel.player.PlayerTouchGesturePolicy
import dev.aaa1115910.bv.viewmodel.player.PlayerTouchTapZone
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun PlayerTouchGestureOverlay(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    controlsVisible: Boolean,
    hasSecondaryOverlay: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    seekStepMs: Long,
    onShowControls: () -> Unit,
    onHideControls: () -> Unit,
    onCloseSecondaryOverlay: () -> Unit,
    onPlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekPreviewStart: (Long) -> Unit,
    onSeekPreview: (Long) -> Unit,
    onSeekPreviewEnd: (Boolean) -> Unit,
    onTempSpeedStart: () -> Unit,
    onTempSpeedEnd: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }
    val scope = rememberCoroutineScope()
    val viewConfiguration = LocalViewConfiguration.current

    val latestIsPlaying by rememberUpdatedState(isPlaying)
    val latestControlsVisible by rememberUpdatedState(controlsVisible)
    val latestHasSecondaryOverlay by rememberUpdatedState(hasSecondaryOverlay)
    val latestCurrentPositionMs by rememberUpdatedState(currentPositionMs)
    val latestDurationMs by rememberUpdatedState(durationMs)
    val latestSeekStepMs by rememberUpdatedState(seekStepMs)
    val latestOnShowControls by rememberUpdatedState(onShowControls)
    val latestOnHideControls by rememberUpdatedState(onHideControls)
    val latestOnCloseSecondaryOverlay by rememberUpdatedState(onCloseSecondaryOverlay)
    val latestOnPlayPause by rememberUpdatedState(onPlayPause)
    val latestOnSeekTo by rememberUpdatedState(onSeekTo)
    val latestOnSeekPreviewStart by rememberUpdatedState(onSeekPreviewStart)
    val latestOnSeekPreview by rememberUpdatedState(onSeekPreview)
    val latestOnSeekPreviewEnd by rememberUpdatedState(onSeekPreviewEnd)
    val latestOnTempSpeedStart by rememberUpdatedState(onTempSpeedStart)
    val latestOnTempSpeedEnd by rememberUpdatedState(onTempSpeedEnd)

    var hintText by remember { mutableStateOf<String?>(null) }
    var hintAtUpperEighth by remember { mutableStateOf(false) }
    var hintHideJob by remember { mutableStateOf<Job?>(null) }
    var pendingSingleTapJob by remember { mutableStateOf<Job?>(null) }
    var lastTapAtMs by remember { mutableStateOf(0L) }
    var lastTapZone by remember { mutableStateOf<PlayerTouchTapZone?>(null) }

    fun showHint(text: String, hold: Boolean, atUpperEighth: Boolean = false) {
        hintHideJob?.cancel()
        hintText = text
        hintAtUpperEighth = atUpperEighth
        if (!hold) {
            hintHideJob = scope.launch {
                delay(900L)
                hintText = null
            }
        }
    }

    fun scheduleHideHint() {
        hintHideJob?.cancel()
        hintHideJob = scope.launch {
            delay(700L)
            hintText = null
        }
    }

    fun singleTap() {
        if (latestHasSecondaryOverlay) {
            latestOnCloseSecondaryOverlay()
            return
        }
        if (latestControlsVisible) {
            latestOnHideControls()
        } else {
            latestOnShowControls()
        }
    }

    fun doubleTap(zone: PlayerTouchTapZone) {
        if (latestHasSecondaryOverlay) {
            latestOnCloseSecondaryOverlay()
            return
        }
        when (zone) {
            PlayerTouchTapZone.Center -> latestOnPlayPause()
            PlayerTouchTapZone.LeftEdge -> {
                val target = (latestCurrentPositionMs - latestSeekStepMs).coerceAtLeast(0L)
                latestOnSeekTo(target)
                showHint("-${latestSeekStepMs / 1000}s", hold = false)
            }
            PlayerTouchTapZone.RightEdge -> {
                val duration = latestDurationMs.coerceAtLeast(0L)
                val target = (latestCurrentPositionMs + latestSeekStepMs).coerceAtMost(duration)
                latestOnSeekTo(target)
                showHint("+${latestSeekStepMs / 1000}s", hold = false)
            }
        }
    }

    fun handleTap(x: Float, width: Float) {
        val zone = PlayerTouchGesturePolicy.tapZone(x, width)
        val now = SystemClock.uptimeMillis()
        val isDoubleTap = lastTapZone == zone &&
            now - lastTapAtMs <= viewConfiguration.doubleTapTimeoutMillis
        if (isDoubleTap) {
            pendingSingleTapJob?.cancel()
            pendingSingleTapJob = null
            lastTapAtMs = 0L
            lastTapZone = null
            doubleTap(zone)
        } else {
            pendingSingleTapJob?.cancel()
            lastTapAtMs = now
            lastTapZone = zone
            pendingSingleTapJob = scope.launch {
                delay(viewConfiguration.doubleTapTimeoutMillis.toLong())
                singleTap()
                lastTapAtMs = 0L
                lastTapZone = null
                pendingSingleTapJob = null
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            hintHideJob?.cancel()
            pendingSingleTapJob?.cancel()
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var dragMode = PlayerTouchDragMode.None
                    var tapSuppressed = false
                    var seekStarted = false
                    var tempSpeedActive = false
                    var volumeStart = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
                    var brightnessStart = activity?.readCurrentBrightness() ?: 0.5f
                    val downPosition = down.position
                    val startPositionMs = latestCurrentPositionMs
                    val gestureWidth = size.width.toFloat().coerceAtLeast(1f)
                    val gestureHeight = size.height.toFloat().coerceAtLeast(1f)
                    var longPressJob: Job? = null

                    if (!latestHasSecondaryOverlay && latestIsPlaying) {
                        longPressJob = scope.launch {
                            delay(viewConfiguration.longPressTimeoutMillis.toLong())
                            if (!tapSuppressed && dragMode == PlayerTouchDragMode.None && latestIsPlaying) {
                                tempSpeedActive = true
                                tapSuppressed = true
                                latestOnTempSpeedStart()
                                showHint("倍速播放中", hold = true, atUpperEighth = true)
                            }
                        }
                    }

                    var pointerUp: PointerInputChange? = null
                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: continue
                        val position = change.position
                        val delta = position - downPosition

                        if (change.pressed) {
                            if (
                                PlayerTouchGesturePolicy.shouldSuppressTapAfterMove(
                                    deltaX = delta.x,
                                    deltaY = delta.y,
                                    touchSlopPx = viewConfiguration.touchSlop
                                )
                            ) {
                                tapSuppressed = true
                                longPressJob?.cancel()
                            }

                            if (!tempSpeedActive) {
                                if (dragMode == PlayerTouchDragMode.None) {
                                    dragMode = PlayerTouchGesturePolicy.dragMode(
                                        downX = downPosition.x,
                                        deltaX = delta.x,
                                        deltaY = delta.y,
                                        widthPx = gestureWidth,
                                        touchSlopPx = viewConfiguration.touchSlop
                                    )
                                    when (dragMode) {
                                        PlayerTouchDragMode.Seek -> {
                                            seekStarted = true
                                            latestOnSeekPreviewStart(startPositionMs)
                                        }
                                        PlayerTouchDragMode.Brightness -> {
                                            brightnessStart = activity?.readCurrentBrightness() ?: brightnessStart
                                        }
                                        PlayerTouchDragMode.Volume -> {
                                            volumeStart = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: volumeStart
                                        }
                                        PlayerTouchDragMode.None,
                                        PlayerTouchDragMode.Blocked -> Unit
                                    }
                                }

                                when (dragMode) {
                                    PlayerTouchDragMode.Seek -> {
                                        val preview = PlayerTouchGesturePolicy.seekPreviewPosition(
                                            startPositionMs = startPositionMs,
                                            dragDeltaPx = delta.x,
                                            gestureWidthPx = gestureWidth,
                                            durationMs = latestDurationMs
                                        )
                                        latestOnSeekPreview(preview)
                                        change.consume()
                                    }
                                    PlayerTouchDragMode.Brightness -> {
                                        activity?.setGestureBrightness(
                                            start = brightnessStart,
                                            deltaY = delta.y,
                                            height = gestureHeight,
                                            onHint = { percent ->
                                                showHint("亮度 $percent%", hold = true)
                                            }
                                        )
                                        change.consume()
                                    }
                                    PlayerTouchDragMode.Volume -> {
                                        audioManager?.setGestureVolume(
                                            start = volumeStart,
                                            deltaY = delta.y,
                                            height = gestureHeight,
                                            onHint = { percent ->
                                                showHint("音量 $percent%", hold = true)
                                            }
                                        )
                                        change.consume()
                                    }
                                    PlayerTouchDragMode.Blocked -> change.consume()
                                    PlayerTouchDragMode.None -> Unit
                                }
                            }
                        } else {
                            pointerUp = change
                        }
                    } while (pointerUp == null)

                    longPressJob?.cancel()
                    if (tempSpeedActive) {
                        latestOnTempSpeedEnd()
                        scheduleHideHint()
                    } else {
                        when (dragMode) {
                            PlayerTouchDragMode.Seek -> latestOnSeekPreviewEnd(seekStarted)
                            PlayerTouchDragMode.Brightness,
                            PlayerTouchDragMode.Volume -> scheduleHideHint()
                            PlayerTouchDragMode.None -> {
                                if (!tapSuppressed) {
                                    handleTap(pointerUp.position.x, gestureWidth)
                                    pointerUp.consume()
                                }
                            }
                            PlayerTouchDragMode.Blocked -> Unit
                        }
                    }
                }
            },
    ) {
        hintText?.let { text ->
            Text(
                modifier = Modifier
                    .align(if (hintAtUpperEighth) Alignment.TopCenter else Alignment.Center)
                    .padding(top = if (hintAtUpperEighth) (maxHeight.value / 8f).dp else 0.dp)
                    .background(Color.Black.copy(alpha = 0.58f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                text = text,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

private fun AudioManager.setGestureVolume(
    start: Int,
    deltaY: Float,
    height: Float,
    onHint: (Int) -> Unit
) {
    val maxVolume = getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    val volume = (start + ((-deltaY / height) * maxVolume).roundToInt())
        .coerceIn(0, maxVolume)
    setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
    onHint((volume * 100f / maxVolume).roundToInt().coerceIn(0, 100))
}

private fun Activity.readCurrentBrightness(): Float {
    val fromWindow = window.attributes.screenBrightness
    if (fromWindow >= 0f) return fromWindow.coerceIn(0.05f, 1f)
    val fromSystem = runCatching {
        Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
    }.getOrNull()
    return (fromSystem?.div(255f) ?: 0.5f).coerceIn(0.05f, 1f)
}

private fun Activity.setGestureBrightness(
    start: Float,
    deltaY: Float,
    height: Float,
    onHint: (Int) -> Unit
) {
    val brightness = (start + (-deltaY / height)).coerceIn(0.05f, 1f)
    val attrs = window.attributes
    attrs.screenBrightness = brightness
    window.attributes = attrs
    onHint((brightness * 100f).roundToInt().coerceIn(0, 100))
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
