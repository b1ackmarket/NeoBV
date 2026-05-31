package dev.aaa1115910.bv.component.controllers

import android.content.Context
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import dev.aaa1115910.biliapi.entity.video.Subtitle
import dev.aaa1115910.biliapi.entity.video.SubtitleAiStatus
import dev.aaa1115910.biliapi.entity.video.SubtitleAiType
import dev.aaa1115910.biliapi.entity.video.SubtitleType
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.component.controllers.playermenu.ClosedCaptionMenuList
import dev.aaa1115910.bv.component.controllers.playermenu.DanmakuMenuList
import dev.aaa1115910.bv.component.controllers.playermenu.MenuNavList
import dev.aaa1115910.bv.component.controllers.playermenu.PlayerStatsMenuList
import dev.aaa1115910.bv.component.controllers.playermenu.PictureMenuList
import dev.aaa1115910.bv.component.controllers.playermenu.PlaySpeedMenuList
import dev.aaa1115910.bv.entity.Audio
import dev.aaa1115910.bv.entity.VideoAspectRatio
import dev.aaa1115910.bv.entity.VideoCodec
import dev.aaa1115910.bv.subtitle.translation.readSubtitleTranslationConfigFromPrefs
import dev.aaa1115910.bv.ui.state.PlayerUiState
import dev.aaa1115910.bv.ui.theme.BVTheme
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.swapList
import dev.aaa1115910.bv.viewmodel.player.SubtitleRole

internal data class PlayerMenuNavState(
    val selectedNavItem: VideoPlayerMenuNavItem,
    val focusedNavItem: VideoPlayerMenuNavItem,
    val focusState: MenuFocusState
)

internal fun defaultPlayerMenuNavState(): PlayerMenuNavState {
    return PlayerMenuNavState(
        selectedNavItem = VideoPlayerMenuNavItem.PlaySpeed,
        focusedNavItem = VideoPlayerMenuNavItem.PlaySpeed,
        focusState = MenuFocusState.MenuNav
    )
}

@Composable
fun MenuController(
    modifier: Modifier = Modifier,
    show: Boolean,
    uiState: PlayerUiState,
    onResolutionChange: (Int) -> Unit = {},
    onCodecChange: (VideoCodec) -> Unit = {},
    onAspectRatioChange: (VideoAspectRatio) -> Unit,
    onPlaySpeedChange: (Float) -> Unit = {},
    onShowPlayerStatsChange: (Boolean) -> Unit = {},
    onAudioChange: (Audio) -> Unit,
    onDanmakuSwitchChange: (List<DanmakuType>) -> Unit,
    onDanmakuSizeChange: (Float) -> Unit,
    onDanmakuOpacityChange: (Float) -> Unit,
    onDanmakuSpeedFactorChange: (Float) -> Unit,
    onDanmakuAreaChange: (Float) -> Unit,
    onDanmakuMaskChange: (Boolean) -> Unit = {},
    onSubtitleChange: (Subtitle, SubtitleRole) -> Unit,
    onAiAudioTranslationChange: (String) -> Unit,
    onSubtitleSizeChange: (TextUnit) -> Unit,
    onSubtitleBackgroundOpacityChange: (Float) -> Unit,
    onSubtitleBottomPadding: (Dp) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(show) {
        if (show) {
            focusRequester.requestFocus()
        }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .onFocusChanged { Log.d("MenuController", "focus: $it") },
        contentAlignment = Alignment.CenterEnd
    ) {
        AnimatedVisibility(
            visible = show,
            enter = expandHorizontally(),
            exit = shrinkHorizontally()
        ) {
            key(show) {
                MenuController(
                    uiState = uiState,
                    onResolutionChange = onResolutionChange,
                    onCodecChange = onCodecChange,
                    onAspectRatioChange = onAspectRatioChange,
                    onPlaySpeedChange = onPlaySpeedChange,
                    onShowPlayerStatsChange = onShowPlayerStatsChange,
                    onAudioChange = onAudioChange,
                    onDanmakuSwitchChange = onDanmakuSwitchChange,
                    onDanmakuSizeChange = onDanmakuSizeChange,
                    onDanmakuOpacityChange = onDanmakuOpacityChange,
                    onDanmakuSpeedFactorChange = onDanmakuSpeedFactorChange,
                    onDanmakuAreaChange = onDanmakuAreaChange,
                    onDanmakuMaskChange = onDanmakuMaskChange,
                    onSubtitleChange = onSubtitleChange,
                    onAiAudioTranslationChange = onAiAudioTranslationChange,
                    onSubtitleSizeChange = onSubtitleSizeChange,
                    onSubtitleBackgroundOpacityChange = onSubtitleBackgroundOpacityChange,
                    onSubtitleBottomPadding = onSubtitleBottomPadding
                )
            }
        }
    }
}

@Composable
fun MenuController(
    modifier: Modifier = Modifier,
    uiState: PlayerUiState,
    onResolutionChange: (Int) -> Unit = {},
    onCodecChange: (VideoCodec) -> Unit = {},
    onAspectRatioChange: (VideoAspectRatio) -> Unit,
    onPlaySpeedChange: (Float) -> Unit,
    onShowPlayerStatsChange: (Boolean) -> Unit,
    onAudioChange: (Audio) -> Unit,
    onDanmakuSwitchChange: (List<DanmakuType>) -> Unit,
    onDanmakuSizeChange: (Float) -> Unit,
    onDanmakuOpacityChange: (Float) -> Unit,
    onDanmakuSpeedFactorChange: (Float) -> Unit,
    onDanmakuAreaChange: (Float) -> Unit,
    onDanmakuMaskChange: (Boolean) -> Unit = {},
    onSubtitleChange: (Subtitle, SubtitleRole) -> Unit,
    onAiAudioTranslationChange: (String) -> Unit,
    onSubtitleSizeChange: (TextUnit) -> Unit,
    onSubtitleBackgroundOpacityChange: (Float) -> Unit,
    onSubtitleBottomPadding: (Dp) -> Unit
) {
    val defaultState = defaultPlayerMenuNavState()
    var selectedNavItem by remember { mutableStateOf(defaultState.selectedNavItem) }
    var focusedNavItem by remember { mutableStateOf(defaultState.focusedNavItem) }
    var focusState by remember { mutableStateOf(defaultState.focusState) }

    Surface(
        modifier = modifier
            .fillMaxHeight(),
        colors = SurfaceDefaults.colors(
            containerColor = Color.Black.copy(alpha = 0.5f)
        )
    ) {
        CompositionLocalProvider(
            LocalMenuFocusStateData provides MenuFocusStateData(
                focusState = focusState
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                MenuList(
                    uiState = uiState,
                    selectedNavMenu = selectedNavItem,
                    onResolutionChange = onResolutionChange,
                    onCodecChange = onCodecChange,
                    onPlaySpeedChange = onPlaySpeedChange,
                    onAspectRatioChange = onAspectRatioChange,
                    onAudioChange = onAudioChange,
                    onShowPlayerStatsChange = onShowPlayerStatsChange,
                    onDanmakuSwitchChange = onDanmakuSwitchChange,
                    onDanmakuSizeChange = onDanmakuSizeChange,
                    onDanmakuOpacityChange = onDanmakuOpacityChange,
                    onDanmakuSpeedFactorChange = onDanmakuSpeedFactorChange,
                    onDanmakuAreaChange = onDanmakuAreaChange,
                    onDanmakuMaskChange = onDanmakuMaskChange,
                    onFocusStateChange = { focusState = it },
                    onSubtitleChange = onSubtitleChange,
                    onAiAudioTranslationChange = onAiAudioTranslationChange,
                    onSubtitleSizeChange = onSubtitleSizeChange,
                    onSubtitleBackgroundOpacityChange = onSubtitleBackgroundOpacityChange,
                    onSubtitleBottomPadding = onSubtitleBottomPadding
                )
                MenuNavList(
                    modifier = Modifier
                        .onPreviewKeyEvent {
                            if (it.type == KeyEventType.KeyUp) {
                                if (listOf(Key.Enter, Key.DirectionCenter).contains(it.key)) {
                                    return@onPreviewKeyEvent false
                                }
                                return@onPreviewKeyEvent true
                            }
                            if (it.key == Key.DirectionLeft) {
                                focusState = MenuFocusState.Menu
                                return@onPreviewKeyEvent true
                            }
                            false
                        },
                    focusedMenu = focusedNavItem,
                    selectedMenu = selectedNavItem,
                    onSelectedChanged = { item ->
                        focusedNavItem = item
                        selectedNavItem = item
                    },
                    onItemClick = { item ->
                        focusedNavItem = item
                        selectedNavItem = item
                        focusState = MenuFocusState.Menu
                    },
                    isFocusing = focusState == MenuFocusState.MenuNav
                )
            }
        }
    }
}

@Composable
private fun MenuList(
    modifier: Modifier = Modifier,
    uiState: PlayerUiState,
    selectedNavMenu: VideoPlayerMenuNavItem,
    onResolutionChange: (Int) -> Unit,
    onCodecChange: (VideoCodec) -> Unit,
    onAspectRatioChange: (VideoAspectRatio) -> Unit,
    onPlaySpeedChange: (Float) -> Unit,
    onShowPlayerStatsChange: (Boolean) -> Unit,
    onAudioChange: (Audio) -> Unit,
    onDanmakuSwitchChange: (List<DanmakuType>) -> Unit,
    onDanmakuSizeChange: (Float) -> Unit,
    onDanmakuOpacityChange: (Float) -> Unit,
    onDanmakuSpeedFactorChange: (Float) -> Unit,
    onDanmakuAreaChange: (Float) -> Unit,
    onDanmakuMaskChange: (Boolean) -> Unit = {},
    onSubtitleChange: (Subtitle, SubtitleRole) -> Unit,
    onAiAudioTranslationChange: (String) -> Unit,
    onSubtitleSizeChange: (TextUnit) -> Unit,
    onSubtitleBackgroundOpacityChange: (Float) -> Unit,
    onSubtitleBottomPadding: (Dp) -> Unit,
    onFocusStateChange: (MenuFocusState) -> Unit
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when (selectedNavMenu) {
            VideoPlayerMenuNavItem.Stats -> {
                PlayerStatsMenuList(
                    currentShowPlayerStats = uiState.showPlayerStats,
                    onShowPlayerStatsChange = onShowPlayerStatsChange,
                    onFocusStateChange = onFocusStateChange
                )
            }

            VideoPlayerMenuNavItem.Picture -> {
                PictureMenuList(
                    availableQuality = uiState.availableQuality,
                    availableAudio = uiState.availableAudio,
                    availableVideoCodec = uiState.availableVideoCodec,
                    currentResolution = uiState.mediaProfileState.qualityId,
                    currentVideoCodec = uiState.mediaProfileState.videoCodec,
                    currentVideoAspectRatio = uiState.aspectRatio,
                    currentAudio = uiState.mediaProfileState.audio,
                    aiAudioTranslations = uiState.aiAudioTranslations,
                    currentAiAudioLanguage = uiState.currentAiAudioLanguage,
                    onResolutionChange = onResolutionChange,
                    onCodecChange = onCodecChange,
                    onAspectRatioChange = onAspectRatioChange,
                    onAudioChange = onAudioChange,
                    onAiAudioTranslationChange = onAiAudioTranslationChange,
                    onFocusStateChange = onFocusStateChange,
                )
            }

            VideoPlayerMenuNavItem.PlaySpeed -> {
                PlaySpeedMenuList(
                    currentPlaySpeed = uiState.playSpeed,
                    onPlaySpeedChange = onPlaySpeedChange,
                    onFocusStateChange = onFocusStateChange,
                )
            }

            VideoPlayerMenuNavItem.Danmaku -> {
                DanmakuMenuList(
                    currentEnabledTypes = uiState.danmakuState.enabledTypes,
                    currentScale = uiState.danmakuState.scale,
                    currentOpacity = uiState.danmakuState.opacity,
                    currentSpeedFactor = uiState.danmakuState.speedFactor,
                    currentArea = uiState.danmakuState.area,
                    currentMaskEnabled = uiState.danmakuState.maskEnabled,
                    onDanmakuSwitchChange = onDanmakuSwitchChange,
                    onDanmakuSizeChange = onDanmakuSizeChange,
                    onDanmakuOpacityChange = onDanmakuOpacityChange,
                    onDanmakuSpeedFactorChange = onDanmakuSpeedFactorChange,
                    onDanmakuAreaChange = onDanmakuAreaChange,
                    onFocusStateChange = onFocusStateChange,
                    onDanmakuMaskChange = onDanmakuMaskChange,
                )
            }

            VideoPlayerMenuNavItem.ClosedCaption -> {
                ClosedCaptionMenuList(
                    currentSubtitleId = uiState.subtitleId,
                    currentSecondarySubtitleId = uiState.secondarySubtitleId,
                    currentSecondarySubtitleCustom = uiState.secondarySubtitleCustom,
                    availableSubtitleTracks = buildList {
                        add(
                            Subtitle(
                                id = -1,
                                lang = "",
                                langDoc = "关闭",
                                url = "",
                                type = SubtitleType.CC,
                                aiType = SubtitleAiType.Normal,
                                aiStatus = SubtitleAiStatus.None
                            )
                        )
                        addAll(uiState.subtitleList)
                        sortBy { it.id }
                    },
                    currentFontSize = uiState.subtitleState.fontSize,
                    currentOpacity = uiState.subtitleState.opacity,
                    currentPadding = uiState.subtitleState.bottomPadding,
                    bilingualSubtitleEnabled = Prefs.enableBilingualSubtitle,
                    subtitleTranslationConfig = readSubtitleTranslationConfigFromPrefs(),
                    preferCustomSecondarySubtitle = Prefs.preferCustomSecondarySubtitle,
                    onSubtitleChange = onSubtitleChange,
                    onSubtitleSizeChange = onSubtitleSizeChange,
                    onSubtitleBackgroundOpacityChange = onSubtitleBackgroundOpacityChange,
                    onSubtitleBottomPadding = onSubtitleBottomPadding,
                    onFocusStateChange = onFocusStateChange,
                )
            }
        }
    }
}


enum class VideoPlayerMenuNavItem(private val strRes: Int, val iconRes: Int) {
    PlaySpeed(R.string.video_player_menu_picture_play_speed, R.drawable.osd_play_speed_32),
    Picture(R.string.video_player_menu_nav_picture, R.drawable.osd_picture_32),
    Danmaku(R.string.video_player_menu_nav_danmaku, R.drawable.osd_danmaku_32),
    ClosedCaption(R.string.video_player_menu_nav_subtitle, R.drawable.osd_caption_32),
    Stats(R.string.video_player_menu_nav_stats, R.drawable.osd_stats_32);

    fun getDisplayName(context: Context) = context.getString(strRes)
}

enum class VideoPlayerPictureMenuItem(private val strRes: Int) {
    Resolution(R.string.video_player_menu_picture_resolution),
    Codec(R.string.video_player_menu_picture_codec),
    AspectRatio(R.string.video_player_menu_picture_aspect_ratio),

    //    PlaySpeed(R.string.video_player_menu_picture_play_speed),
    Audio(R.string.video_player_menu_picture_audio),
    AiAudioTranslation(R.string.video_player_menu_picture_ai_audio_translation);

    fun getDisplayName(context: Context) = context.getString(strRes)
}

enum class VideoPlayerDanmakuMenuItem(private val strRes: Int) {
    Switch(R.string.video_player_menu_danmaku_switch),
    Size(R.string.video_player_menu_danmaku_size),
    Opacity(R.string.video_player_menu_danmaku_opacity),
    SpeedFactor(R.string.video_player_menu_danmaku_speed_factor),
    Area(R.string.video_player_menu_danmaku_area),
    Mask(R.string.video_player_menu_danmaku_mask);

    fun getDisplayName(context: Context) = context.getString(strRes)
}

enum class VideoPlayerClosedCaptionMenuItem(private val strRes: Int) {
    Main(R.string.video_player_menu_subtitle_main),
    Secondary(R.string.video_player_menu_subtitle_secondary),
    Size(R.string.video_player_menu_subtitle_size),
    Opacity(R.string.video_player_menu_subtitle_background_opacity),
    Padding(R.string.video_player_menu_subtitle_bottom_padding);

    fun getDisplayName(context: Context) = context.getString(strRes)
}

enum class DanmakuType(private val strRes: Int) {
    All(R.string.video_player_menu_danmaku_type_all),
    Top(R.string.video_player_menu_danmaku_type_top),
    Rolling(R.string.video_player_menu_danmaku_type_cross),
    Bottom(R.string.video_player_menu_danmaku_type_bottom);

    fun getDisplayName(context: Context) = context.getString(strRes)
}

@Preview(device = "id:tv_1080p")
@Composable
fun MenuControllerPreview() {
    var currentResolution by remember { mutableIntStateOf(1) }
    var currentCodec by remember { mutableStateOf(VideoCodec.HEVC) }
    var currentVideoAspectRatio by remember { mutableStateOf(VideoAspectRatio.Default) }
    var currentPlaySpeed by remember { mutableFloatStateOf(1f) }
    var currentAudio by remember { mutableStateOf(Audio.A192K) }

    val currentDanmakuSwitch = remember { mutableStateListOf<DanmakuType>() }
    var currentDanmakuSize by remember { mutableFloatStateOf(1f) }
    var currentDanmakuSpeedFactor by remember { mutableFloatStateOf(1f) }
    var currentDanmakuOpacity by remember { mutableFloatStateOf(1f) }
    var currentDanmakuArea by remember { mutableFloatStateOf(1f) }
    var currentDanmakuMask by remember { mutableStateOf(false) }

    var currentSubtitleId by remember { mutableLongStateOf(-1L) }
    val currentSubtitleList = remember { mutableStateListOf<Subtitle>() }
    var currentSubtitleFontSize by remember { mutableStateOf(24.sp) }
    var currentSubtitleBackgroundOpacity by remember { mutableFloatStateOf(0.4f) }
    var currentSubtitleBottomPadding by remember { mutableStateOf(8.dp) }

    LaunchedEffect(Unit) {
        currentSubtitleList.apply {
            addAll(
                listOf(
                    Subtitle(
                        id = -1,
                        langDoc = "关闭",
                        lang = "",
                        url = "",
                        type = SubtitleType.CC,
                        aiType = SubtitleAiType.Normal,
                        aiStatus = SubtitleAiStatus.None
                    ),
                    Subtitle(
                        id = 1111,
                        langDoc = "ai-zh",
                        lang = "中文（自动翻译）",
                        url = "",
                        type = SubtitleType.CC,
                        aiType = SubtitleAiType.Normal,
                        aiStatus = SubtitleAiStatus.None
                    ),
                    Subtitle(
                        id = 222,
                        lang = "zh",
                        langDoc = "中文",
                        url = "",
                        type = SubtitleType.CC,
                        aiType = SubtitleAiType.Normal,
                        aiStatus = SubtitleAiStatus.None
                    ),
                    Subtitle(
                        id = 1333,
                        lang = "ai-en",
                        langDoc = "English",
                        url = "",
                        type = SubtitleType.CC,
                        aiType = SubtitleAiType.Normal,
                        aiStatus = SubtitleAiStatus.None
                    )
                )
            )
        }
    }

    BVTheme {
        Surface(
            colors = SurfaceDefaults.colors(
                containerColor = Color.White
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                MenuController(
                    modifier = Modifier
                        .align(Alignment.CenterEnd),
                    uiState = PlayerUiState(),
                    onResolutionChange = { currentResolution = it },
                    onCodecChange = { currentCodec = it },
                    onAspectRatioChange = { currentVideoAspectRatio = it },
                    onPlaySpeedChange = { currentPlaySpeed = it },
                    onShowPlayerStatsChange = { },
                    onAudioChange = { currentAudio = it },
                    onDanmakuSwitchChange = {
                        val a = currentDanmakuSwitch.toList()
                        currentDanmakuSwitch.swapList(it)
                        val b = currentDanmakuSwitch.toList()
                        println("a=$a")
                        println("b=$b")

                    },
                    onDanmakuSizeChange = { currentDanmakuSize = it },
                    onDanmakuOpacityChange = { currentDanmakuOpacity = it },
                    onDanmakuSpeedFactorChange = { currentDanmakuSpeedFactor = it },
                    onDanmakuAreaChange = { currentDanmakuArea = it },
                    onDanmakuMaskChange = { currentDanmakuMask = it },
                    onSubtitleChange = { subtitle, _ -> currentSubtitleId = subtitle.id },
                    onAiAudioTranslationChange = { },
                    onSubtitleSizeChange = { currentSubtitleFontSize = it },
                    onSubtitleBackgroundOpacityChange = {
                        currentSubtitleBackgroundOpacity = it
                    },
                    onSubtitleBottomPadding = { currentSubtitleBottomPadding = it }
                )
            }
        }
    }
}

enum class MenuFocusState {
    MenuNav, Menu, Items
}

data class MenuFocusStateData(
    val focusState: MenuFocusState = MenuFocusState.MenuNav
)

val LocalMenuFocusStateData = compositionLocalOf { MenuFocusStateData() }
