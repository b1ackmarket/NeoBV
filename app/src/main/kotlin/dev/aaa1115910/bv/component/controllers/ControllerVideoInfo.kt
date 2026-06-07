package dev.aaa1115910.bv.component.controllers

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.entity.video.VideoHeatmap
import dev.aaa1115910.biliapi.entity.video.VideoProgressChapter
import dev.aaa1115910.biliapi.entity.video.VideoShot
import dev.aaa1115910.biliapi.entity.video.currentChapterAt
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.entity.ProgressSegmentMark
import dev.aaa1115910.bv.ui.state.JumpModeState
import dev.aaa1115910.bv.ui.state.SeekerState
import dev.aaa1115910.bv.ui.theme.BVTheme
import dev.aaa1115910.bv.util.LayoutConfig
import dev.aaa1115910.bv.util.PlayerBottomOsdControl
import dev.aaa1115910.bv.util.VideoShotImageCache
import dev.aaa1115910.bv.util.formatHourMinSec
import dev.aaa1115910.bv.util.touchClick
import kotlinx.coroutines.delay

@Composable
fun ControllerVideoInfo(
    modifier: Modifier = Modifier,
    show: Boolean,
    isSeeking: Boolean,
    goTime: Long,
    seekerState: SeekerState,
    title: String,
    authorName: String,
    publishDateText: String,
    playCountText: String,
    videoListButtonLabel: String,
    sponsorBlockProgressMarks: List<ProgressSegmentMark> = emptyList(),
    watchedProgressMarks: List<ProgressSegmentMark> = emptyList(),
    videoHeatmap: VideoHeatmap? = null,
    videoProgressChapters: List<VideoProgressChapter> = emptyList(),
    clock: Pair<Int, Int>,
    videoShot: VideoShot?,
    videoShotCache: VideoShotImageCache,
    fromSeason: Boolean,
    danmakuEnabled: Boolean,
    subtitleEnabled: Boolean,
    subtitleAvailable: Boolean,
    jumpModeState: JumpModeState,
    isLooping: Boolean,
    onDirectionLeft: () -> Unit,
    onDirectionRight: () -> Unit,
    onSeekGoTime: () -> Unit,
    onPlayPause: () -> Unit,
    onShowVideoList: () -> Unit,
    onDanmakuSwitchChange: () -> Unit,
    onSubtitleSwitchChange: () -> Unit,
    onShowSettings: () -> Unit,
    onToggleJumpMode: () -> Unit,
    onShowRelatedVideos: () -> Unit,
    onShowComments: () -> Unit,
    onGoToVideoInfo: () -> Unit,
    onToggleLoop: () -> Unit,
    onGoToUpPage: () -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        AnimatedVisibility(
            modifier = Modifier.align(Alignment.TopCenter),
            visible = show,
            enter = expandVertically(),
            exit = shrinkVertically(),
            label = "ControllerTopVideoInfo"
        ) {
            ControllerVideoInfoTop(
                modifier = Modifier.align(Alignment.TopCenter),
                title = title,
                authorName = authorName,
                publishDateText = publishDateText,
                playCountText = playCountText,
                clock = clock
            )
        }
        AnimatedVisibility(
            modifier = Modifier.align(Alignment.BottomCenter),
            visible = show,
            enter = expandVertically(),
            exit = shrinkVertically(),
            label = "ControllerBottomVideoInfo"
        ) {
            ControllerVideoInfoBottom(
                modifier = Modifier
                    .align(Alignment.BottomCenter),
                show = show,
                isSeeking = isSeeking,
                goTime = goTime,
                seekerState = seekerState,
                videoShot = videoShot,
                videoShotCache = videoShotCache,
                fromSeason = fromSeason,
                danmakuEnabled = danmakuEnabled,
                subtitleEnabled = subtitleEnabled,
                subtitleAvailable = subtitleAvailable,
                jumpModeState = jumpModeState,
                isLooping = isLooping,
                videoListButtonLabel = videoListButtonLabel,
                sponsorBlockProgressMarks = sponsorBlockProgressMarks,
                watchedProgressMarks = watchedProgressMarks,
                videoHeatmap = videoHeatmap,
                videoProgressChapters = videoProgressChapters,
                onDirectionLeft = onDirectionLeft,
                onDirectionRight = onDirectionRight,
                onSeekGoTime = onSeekGoTime,
                onPlayPause = onPlayPause,
                onShowVideoList = onShowVideoList,
                onDanmakuSwitchChange = onDanmakuSwitchChange,
                onSubtitleSwitchChange = onSubtitleSwitchChange,
                onShowSettings = onShowSettings,
                onToggleJumpMode = onToggleJumpMode,
                onShowRelatedVideos = onShowRelatedVideos,
                onShowComments = onShowComments,
                onGoToVideoInfo = onGoToVideoInfo,
                onToggleLoop = onToggleLoop,
                onGoToUpPage = onGoToUpPage
            )
        }
    }
}

@Composable
fun ControllerVideoInfoTop(
    modifier: Modifier = Modifier,
    title: String,
    authorName: String,
    publishDateText: String,
    playCountText: String,
    clock: Pair<Int, Int>
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(
                MaterialTheme.shapes.large.copy(
                    topStart = CornerSize(0.dp),
                    topEnd = CornerSize(0.dp)
                )
            )
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.5f), // 上部颜色较深
                        Color.Black.copy(alpha = 0f)  // 下部颜色较浅
                    )
                )
            )
            .padding(horizontal = 32.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    shadow = Shadow(
                        color = Color.Black,
                        blurRadius = 1f
                    ),
                ),
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Clock(
                hour = clock.first,
                minute = clock.second,
            )
        }
        if (
            authorName.isNotBlank() ||
            publishDateText.isNotBlank() ||
            playCountText.isNotBlank()
        ) {
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (authorName.isNotBlank()) {
                    Text(
                        text = authorName,
                        color = Color.White.copy(alpha = 0.84f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (publishDateText.isNotBlank()) {
                    Text(
                        text = publishDateText,
                        color = Color.White.copy(alpha = 0.64f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (playCountText.isNotBlank()) {
                    Text(
                        text = playCountText,
                        color = Color.White.copy(alpha = 0.64f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun ControllerVideoInfoBottom(
    modifier: Modifier = Modifier,
    show: Boolean,
    isSeeking: Boolean,
    goTime: Long,
    seekerState: SeekerState,
    videoShot: VideoShot?,
    videoShotCache: VideoShotImageCache,
    fromSeason: Boolean,
    danmakuEnabled: Boolean,
    subtitleEnabled: Boolean,
    subtitleAvailable: Boolean,
    jumpModeState: JumpModeState,
    isLooping: Boolean,
    videoListButtonLabel: String,
    sponsorBlockProgressMarks: List<ProgressSegmentMark> = emptyList(),
    watchedProgressMarks: List<ProgressSegmentMark> = emptyList(),
    videoHeatmap: VideoHeatmap? = null,
    videoProgressChapters: List<VideoProgressChapter> = emptyList(),
    onDirectionLeft: () -> Unit,
    onDirectionRight: () -> Unit,
    onSeekGoTime: () -> Unit,
    onPlayPause: () -> Unit,
    onShowVideoList: () -> Unit,
    onDanmakuSwitchChange: () -> Unit,
    onSubtitleSwitchChange: () -> Unit,
    onShowSettings: () -> Unit,
    onToggleJumpMode: () -> Unit,
    onShowRelatedVideos: () -> Unit,
    onShowComments: () -> Unit,
    onGoToVideoInfo: () -> Unit,
    onToggleLoop: () -> Unit,
    onGoToUpPage: () -> Unit
) {
    val seekFocusRequester = remember { FocusRequester() }
    val buttonsFocusRequester = remember { FocusRequester() }

    var isSeekFocused by remember { mutableStateOf(false) }
    val previewChapterTitle = remember(videoProgressChapters, goTime, isSeeking) {
        if (isSeeking) videoProgressChapters.currentChapterAt(goTime)?.title.orEmpty() else ""
    }

    LaunchedEffect(show) {
        if (show) {
            delay(50)
            try {
                seekFocusRequester.requestFocus()
            } catch (e: IllegalStateException) {
                Log.d("ControllerVideoInfo", "requestFocus failed")
            }
        }
    }
    Column(
        modifier = modifier
            .clip(
                MaterialTheme.shapes.large
                    .copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp))
            ),
        verticalArrangement = Arrangement.Bottom
    ) {
        if (isSeeking) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                if (videoShot != null) {
                    Surface(
                        modifier = Modifier.widthIn(max = 420.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = SurfaceDefaults.colors(
                            containerColor = Color(0xD9111218)
                        )
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            VideoShot(
                                modifier = Modifier.fillMaxWidth(),
                                videoShot = videoShot,
                                imageCache = videoShotCache,
                                position = goTime,
                                duration = seekerState.totalDuration,
                                centerPreview = true,
                                previewHeight = 172.dp
                            )
                            Column(
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${goTime.formatHourMinSec()} / ${seekerState.totalDuration.formatHourMinSec()}",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleLarge
                                )
                                if (previewChapterTitle.isNotBlank()) {
                                    Text(
                                        modifier = Modifier.padding(top = 4.dp),
                                        text = previewChapterTitle,
                                        color = Color.White.copy(alpha = 0.88f),
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    modifier = Modifier.padding(top = 4.dp),
                                    text = "按确定跳转",
                                    color = Color.White.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        colors = SurfaceDefaults.colors(
                            containerColor = Color(0xD9111218)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${goTime.formatHourMinSec()} / ${seekerState.totalDuration.formatHourMinSec()}",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (previewChapterTitle.isNotBlank()) {
                                Text(
                                    modifier = Modifier.padding(top = 4.dp),
                                    text = previewChapterTitle,
                                    color = Color.White.copy(alpha = 0.88f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                modifier = Modifier.padding(top = 4.dp),
                                text = "按确定跳转",
                                color = Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                modifier = Modifier.padding(bottom = 2.dp, start = 24.dp),
                text = "${if (isSeeking) goTime.formatHourMinSec() else seekerState.currentTime.formatHourMinSec()} / ${seekerState.totalDuration.formatHourMinSec()}",
                color = Color.White,
                style = TextStyle(
                    shadow = Shadow(color = Color.Black, blurRadius = 1f),
                ),
            )
        }
        Row(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = if (isSeekFocused) 1f else 0f),
                    shape = RoundedCornerShape(8.dp)
                )
                .focusable()
                .focusRequester(seekFocusRequester)
                .touchClick {
                    if (isSeeking) {
                        onSeekGoTime()
                    } else {
                        onPlayPause()
                    }
                }
                .onKeyEvent {
                    when (it.key) {
                        Key.DirectionCenter, Key.Enter, Key.Spacebar -> {
                            if (it.type == KeyEventType.KeyDown) return@onKeyEvent true
                            if (isSeeking) {
                                onSeekGoTime()
                            } else {
                                onPlayPause()
                            }
                            return@onKeyEvent true
                        }

                        Key.DirectionLeft, Key.MediaRewind -> {
                            if (it.type == KeyEventType.KeyUp) return@onKeyEvent true
                            onDirectionLeft()
                            return@onKeyEvent true
                        }

                        Key.DirectionRight, Key.MediaFastForward -> {
                            if (it.type == KeyEventType.KeyUp) return@onKeyEvent true
                            onDirectionRight()
                            return@onKeyEvent true
                        }

                        Key.DirectionDown -> {
                            if (it.type == KeyEventType.KeyUp) return@onKeyEvent true
                            buttonsFocusRequester.requestFocus()
                            return@onKeyEvent true
                        }
                    }
                    return@onKeyEvent false
                }
                .onFocusChanged {
                    isSeekFocused = it.isFocused
                },
        ) {
            VideoProgressSeek(
                modifier = Modifier
                    .focusable()
                    .fillMaxWidth(),
                duration = seekerState.totalDuration,
                position = if (isSeeking) goTime else seekerState.currentTime,
                bufferedPercentage = seekerState.bufferedPercentage,
                isPersistentSeek = false,
                segmentMarks = sponsorBlockProgressMarks,
                watchedSegmentMarks = watchedProgressMarks,
                videoHeatmap = videoHeatmap,
                chapters = videoProgressChapters
            )
        }

        val availableButtons = listOfNotNull(
            ControllerInfoButton(
                control = PlayerBottomOsdControl.VideoList,
                iconRes = R.drawable.related_videos_24px,
                contentDescription = videoListButtonLabel,
                onClick = onShowVideoList
            ),
            ControllerInfoButton(
                control = PlayerBottomOsdControl.Danmaku,
                iconRes = if (danmakuEnabled) R.drawable.danmaku_on_24px else R.drawable.danmaku_off_24px,
                contentDescription = "弹幕开关",
                onClick = onDanmakuSwitchChange
            ),
            ControllerInfoButton(
                control = PlayerBottomOsdControl.Subtitle,
                iconRes = if (subtitleEnabled) R.drawable.osd_caption_32 else R.drawable.osd_caption_off_32,
                contentDescription = if (subtitleEnabled) "关闭字幕" else "开启字幕",
                enabled = subtitleAvailable || subtitleEnabled,
                onClick = onSubtitleSwitchChange
            ),
            ControllerInfoButton(
                control = PlayerBottomOsdControl.JumpMode,
                iconRes = if (jumpModeState.enabled) R.drawable.jump_mode_on_24px else R.drawable.jump_mode_off_24px,
                contentDescription = when {
                    !jumpModeState.available -> "跳动模式不可用"
                    jumpModeState.enabled -> "关闭跳动模式"
                    else -> "开启跳动模式"
                },
                enabled = jumpModeState.available,
                onClick = onToggleJumpMode
            ),
            if (!fromSeason) ControllerInfoButton(
                control = PlayerBottomOsdControl.VideoInfo,
                iconRes = R.drawable.info_24px,
                contentDescription = "视频信息",
                onClick = onGoToVideoInfo
            ) else null,
            if (!fromSeason) ControllerInfoButton(
                control = PlayerBottomOsdControl.UpPage,
                iconRes = R.drawable.contact_page_24px,
                contentDescription = "up主页",
                onClick = onGoToUpPage
            ) else null,
            if (!fromSeason) ControllerInfoButton(
                control = PlayerBottomOsdControl.RelatedVideos,
                iconRes = R.drawable.related_videos_24px,
                contentDescription = "相关视频",
                onClick = onShowRelatedVideos
            ) else null,
            ControllerInfoButton(
                control = PlayerBottomOsdControl.Comments,
                iconRes = R.drawable.comment_24px,
                contentDescription = "评论",
                onClick = onShowComments
            ),
            ControllerInfoButton(
                control = PlayerBottomOsdControl.Loop,
                iconRes = if (isLooping) R.drawable.repeat_one_on_24px else R.drawable.repeat_one_24px,
                contentDescription = "循环播放",
                onClick = onToggleLoop
            ),
            ControllerInfoButton(
                control = PlayerBottomOsdControl.Settings,
                iconRes = R.drawable.settings_24px,
                contentDescription = "播放设置",
                onClick = onShowSettings
            ),
        )
        val buttonsByControl = availableButtons.associateBy { it.control }
        val icons = LayoutConfig.applyPlayerBottomOsd(items = availableButtons.map { it.control })
            .mapNotNull(buttonsByControl::get)
        val iconFocusRequesters = remember(icons.map { it.control }) {
            icons.map { FocusRequester() }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(buttonsFocusRequester)
                .onKeyEvent {
                    if (it.key == Key.DirectionUp) {
                        if (it.type == KeyEventType.KeyUp) return@onKeyEvent true
                        seekFocusRequester.requestFocus()
                        return@onKeyEvent true
                    }
                    return@onKeyEvent false
                }
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start)
        ) {
            icons.forEachIndexed { index, button ->
                val clickButton = { if (button.enabled) button.onClick() }
                Surface(
                    modifier = Modifier
                        .focusRequester(iconFocusRequesters[index])
                        .onKeyEvent {
                            if (it.type == KeyEventType.KeyUp) {
                                if (it.key == Key.DirectionLeft || it.key == Key.DirectionRight) return@onKeyEvent true
                                return@onKeyEvent false
                            }
                            when (it.key) {
                                Key.DirectionLeft -> {
                                    if (index == 0) {
                                        iconFocusRequesters.lastOrNull()?.requestFocus()
                                        true
                                    } else {
                                        false
                                    }
                                }

                                Key.DirectionRight -> {
                                    if (index == icons.lastIndex) {
                                        iconFocusRequesters.firstOrNull()?.requestFocus()
                                        true
                                    } else {
                                        false
                                    }
                                }

                                else -> false
                            }
                        }
                        .touchClick(clickButton),
                    onClick = clickButton,
                    shape = ClickableSurfaceDefaults.shape(
                        shape = MaterialTheme.shapes.small,
                    ),
                    colors = ClickableSurfaceDefaults.colors(),
                ) {
                    Icon(
                        painter = painterResource(id = button.iconRes),
                        contentDescription = button.contentDescription,
                        modifier = Modifier
                            .size(32.dp)
                            .padding(4.dp),
                        tint = LocalContentColor.current.copy(alpha = if (button.enabled) 1f else 0.32f)
                    )
                }
            }
        }
    }
}

private data class ControllerInfoButton(
    val control: PlayerBottomOsdControl,
    val iconRes: Int,
    val contentDescription: String,
    val enabled: Boolean = true,
    val onClick: () -> Unit
)

@Composable
private fun Clock(
    modifier: Modifier = Modifier,
    hour: Int,
    minute: Int,
) {
    Text(
        modifier = modifier,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        style = TextStyle(
            shadow = Shadow(color = Color.Black, blurRadius = 1f),
        ),
        text = buildAnnotatedString {
            withStyle(SpanStyle(fontSize = 32.sp)) {
                append("$hour".padStart(2, '0'))
                append(":")
                append("$minute".padStart(2, '0'))
            }
        }
    )
}

@Preview
@Composable
private fun ClockPreview() {
    val clock = Triple(12, 30, 30)
    BVTheme {
        Clock(
            hour = clock.first,
            minute = clock.second,
        )
    }
}

@Preview(device = "id:tv_1080p")
@Composable
private fun ControllerVideoInfoPreview() {
    var show by remember { mutableStateOf(true) }

    BVTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Button(onClick = { show = !show }) {
                Text(text = "Switch")
            }
        }
        ControllerVideoInfo(
            modifier = Modifier.fillMaxSize(),
            show = show,
            isSeeking = false,
            goTime = 0,
            seekerState = SeekerState(0, 0, 0, ""),
            title = "【A320】民航史上最佳逆袭！A320的前世今生！民航史上最佳逆袭！A320的前世今生！",
            authorName = "BV 官方",
            publishDateText = "5月8日",
            playCountText = "12.3万播放",
            videoListButtonLabel = "选集",
            videoHeatmap = null,
            clock = Pair(12, 30),
            videoShot = null,
            videoShotCache = VideoShotImageCache(),
            fromSeason = false,
            danmakuEnabled = false,
            subtitleEnabled = false,
            subtitleAvailable = true,
            jumpModeState = JumpModeState(),
            isLooping = false,
            onDirectionRight = {},
            onDirectionLeft = {},
            onSeekGoTime = {},
            onPlayPause = {},
            onShowVideoList = {},
            onDanmakuSwitchChange = {},
            onSubtitleSwitchChange = {},
            onShowSettings = {},
            onToggleJumpMode = {},
            onShowRelatedVideos = {},
            onShowComments = {},
            onGoToVideoInfo = {},
            onToggleLoop = {},
            onGoToUpPage = {},
        )
    }
}
