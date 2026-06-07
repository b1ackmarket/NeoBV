package dev.aaa1115910.bv.component.controllers.playermenu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aaa1115910.biliapi.entity.video.Subtitle
import dev.aaa1115910.biliapi.entity.video.SubtitleAiStatus
import dev.aaa1115910.biliapi.entity.video.SubtitleAiType
import dev.aaa1115910.biliapi.entity.video.SubtitleType
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.component.controllers.LocalMenuFocusStateData
import dev.aaa1115910.bv.component.controllers.MenuFocusState
import dev.aaa1115910.bv.component.controllers.VideoPlayerClosedCaptionMenuItem
import dev.aaa1115910.bv.component.controllers.playermenu.component.MenuListItem
import dev.aaa1115910.bv.component.controllers.playermenu.component.RadioMenuList
import dev.aaa1115910.bv.component.controllers.playermenu.component.StepLessMenuItem
import dev.aaa1115910.bv.component.ifElse
import dev.aaa1115910.bv.subtitle.SecondarySubtitleOption
import dev.aaa1115910.bv.subtitle.buildSecondarySubtitleOptions
import dev.aaa1115910.bv.subtitle.translation.SubtitleTranslationConfig
import dev.aaa1115910.bv.viewmodel.player.CustomSubtitleTrackId
import dev.aaa1115910.bv.viewmodel.player.SubtitleRole
import java.text.NumberFormat

internal fun resolveSelectedSubtitleTrackIndex(
    currentSubtitleId: Long,
    tracks: List<Subtitle>
): Int {
    return tracks.indexOfFirst { it.id == currentSubtitleId }.takeIf { it >= 0 } ?: 0
}

@Composable
fun ClosedCaptionMenuList(
    modifier: Modifier = Modifier,
    currentSubtitleId: Long,
    currentSecondarySubtitleId: Long,
    currentSecondarySubtitleCustom: Boolean = false,
    availableSubtitleTracks: List<Subtitle>,
    currentFontSize: TextUnit,
    currentOpacity: Float,
    currentPadding: Dp,
    bilingualSubtitleEnabled: Boolean,
    subtitleTranslationConfig: SubtitleTranslationConfig,
    preferCustomSecondarySubtitle: Boolean,
    mainSubtitleContentAvailable: Boolean,
    onSubtitleChange: (Subtitle, SubtitleRole) -> Unit,
    onSubtitleSizeChange: (TextUnit) -> Unit,
    onSubtitleBackgroundOpacityChange: (Float) -> Unit,
    onSubtitleBottomPadding: (Dp) -> Unit,
    onFocusStateChange: (MenuFocusState) -> Unit
) {
    val context = LocalContext.current
    val focusState = LocalMenuFocusStateData.current
    val restorerFocusRequester = remember { FocusRequester() }

    val focusRequester = remember { FocusRequester() }
    var selectedClosedCaptionMenuItem by remember { mutableStateOf(VideoPlayerClosedCaptionMenuItem.Main) }
    val menuItemRequesters = remember {
        mutableStateListOf<FocusRequester>().apply {
            addAll(VideoPlayerClosedCaptionMenuItem.entries.map { FocusRequester() })
        }
    }
    val shouldFocusItems = focusState.focusState == MenuFocusState.Items
    val selectedSubtitleIndex = resolveSelectedSubtitleTrackIndex(
        currentSubtitleId = currentSubtitleId,
        tracks = availableSubtitleTracks
    )
    val secondarySubtitleOptions = remember(
        availableSubtitleTracks,
        currentSubtitleId,
        bilingualSubtitleEnabled,
        subtitleTranslationConfig,
        preferCustomSecondarySubtitle,
        mainSubtitleContentAvailable
    ) {
        if (!bilingualSubtitleEnabled) {
            emptyList()
        } else {
            buildSecondarySubtitleOptions(
                tracks = availableSubtitleTracks,
                currentMainSubtitleId = currentSubtitleId,
                config = subtitleTranslationConfig,
                preferCustom = preferCustomSecondarySubtitle,
                sourceSubtitleAvailable = mainSubtitleContentAvailable
            )
        }
    }
    val secondarySubtitleMenuOptions = remember(secondarySubtitleOptions) {
        buildSecondarySubtitleMenuOptions(secondarySubtitleOptions)
    }
    val secondarySubtitleItems = secondarySubtitleMenuOptions.map { it.toMenuName(context) }
    val selectedSecondaryIndex = resolveSelectedSecondarySubtitleMenuIndex(
        options = secondarySubtitleMenuOptions,
        currentSecondarySubtitleId = currentSecondarySubtitleId,
        currentSecondarySubtitleCustom = currentSecondarySubtitleCustom
    )
    val closedCaptionMenuItems = remember(bilingualSubtitleEnabled) {
        VideoPlayerClosedCaptionMenuItem.entries.filter { item ->
            bilingualSubtitleEnabled || item != VideoPlayerClosedCaptionMenuItem.Secondary
        }
    }

    LaunchedEffect(bilingualSubtitleEnabled, selectedClosedCaptionMenuItem) {
        if (!bilingualSubtitleEnabled && selectedClosedCaptionMenuItem == VideoPlayerClosedCaptionMenuItem.Secondary) {
            selectedClosedCaptionMenuItem = VideoPlayerClosedCaptionMenuItem.Main
            onFocusStateChange(MenuFocusState.Menu)
        }
    }

    LaunchedEffect(focusState.focusState, selectedClosedCaptionMenuItem) {
        if (focusState.focusState == MenuFocusState.Menu) {
            val index = resolveParentMenuFocusIndex(
                selectedIndex = selectedClosedCaptionMenuItem.ordinal,
                itemCount = menuItemRequesters.size
            )
            menuItemRequesters[index].requestFocus()
        }
    }

    Row(
        modifier = modifier.fillMaxHeight(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val menuItemsModifier = Modifier
            .width(216.dp)
            .padding(horizontal = 8.dp)
        AnimatedVisibility(visible = focusState.focusState != MenuFocusState.MenuNav) {
            when (selectedClosedCaptionMenuItem) {
                VideoPlayerClosedCaptionMenuItem.Main -> RadioMenuList(
                    modifier = menuItemsModifier,
                    items = availableSubtitleTracks.map { it.toSubtitleMenuName() },
                    selected = selectedSubtitleIndex,
                    requestFocusWhen = shouldFocusItems,
                    onSelectedChanged = { onSubtitleChange(availableSubtitleTracks[it], SubtitleRole.Main) },
                    onFocusBackToParent = {
                        onFocusStateChange(MenuFocusState.Menu)
                    },
                )

                VideoPlayerClosedCaptionMenuItem.Secondary -> RadioMenuList(
                    modifier = menuItemsModifier,
                    items = secondarySubtitleItems,
                    selected = selectedSecondaryIndex,
                    requestFocusWhen = shouldFocusItems,
                    onSelectedChanged = {
                        when (val option = secondarySubtitleMenuOptions.getOrNull(it)) {
                            SecondarySubtitleMenuOption.Off -> {
                                onSubtitleChange(SubtitleOffTrack, SubtitleRole.Secondary)
                            }

                            is SecondarySubtitleMenuOption.Option -> when (val subtitleOption = option.option) {
                                SecondarySubtitleOption.CustomTranslation -> {
                                    onSubtitleChange(SubtitleCustomTrack, SubtitleRole.Secondary)
                                }

                                is SecondarySubtitleOption.BiliTrack -> {
                                    val subtitle = subtitleOption.subtitle
                                    onSubtitleChange(subtitle, SubtitleRole.Secondary)
                                }
                            }

                            null -> Unit
                        }
                    },
                    onFocusBackToParent = {
                        onFocusStateChange(MenuFocusState.Menu)
                    },
                )

                VideoPlayerClosedCaptionMenuItem.Size -> StepLessMenuItem(
                    modifier = menuItemsModifier,
                    value = currentFontSize.value.toInt(),
                    step = 1,
                    range = 12..48,
                    text = "${currentFontSize.value.toInt()} SP",
                    requestFocusWhen = shouldFocusItems,
                    onValueChange = { onSubtitleSizeChange(it.sp) },
                    onFocusBackToParent = { onFocusStateChange(MenuFocusState.Menu) }
                )

                VideoPlayerClosedCaptionMenuItem.Opacity -> StepLessMenuItem(
                    modifier = menuItemsModifier,
                    value = currentOpacity,
                    step = 0.01f,
                    range = 0f..1f,
                    text = NumberFormat.getPercentInstance()
                        .apply { maximumFractionDigits = 0 }
                        .format(currentOpacity),
                    requestFocusWhen = shouldFocusItems,
                    onValueChange = onSubtitleBackgroundOpacityChange,
                    onFocusBackToParent = { onFocusStateChange(MenuFocusState.Menu) }
                )

                VideoPlayerClosedCaptionMenuItem.Padding -> StepLessMenuItem(
                    modifier = menuItemsModifier,
                    value = currentPadding.value.toInt(),
                    step = 1,
                    range = 0..48,
                    text = "${currentPadding.value.toInt()} DP",
                    requestFocusWhen = shouldFocusItems,
                    onValueChange = { onSubtitleBottomPadding(it.dp) },
                    onFocusBackToParent = { onFocusStateChange(MenuFocusState.Menu) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .focusRequester(focusRequester)
                .padding(horizontal = 8.dp)
                .onPreviewKeyEvent {
                    if (it.type == KeyEventType.KeyUp) {
                        if (listOf(Key.Enter, Key.DirectionCenter).contains(it.key)) {
                            return@onPreviewKeyEvent false
                        }
                        return@onPreviewKeyEvent true
                    }
                    when (it.key) {
                        Key.DirectionRight -> onFocusStateChange(MenuFocusState.MenuNav)
                        Key.DirectionLeft -> onFocusStateChange(MenuFocusState.Items)
                        else -> {}
                    }
                    false
                }
                .focusRestorer(restorerFocusRequester),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(8.dp)
        ) {
            itemsIndexed(closedCaptionMenuItems) { index, item ->
                val selectItem = {
                    val result = resolveParentMenuTouch(
                        current = selectedClosedCaptionMenuItem,
                        touched = item
                    )
                    selectedClosedCaptionMenuItem = result.selectedItem
                    onFocusStateChange(result.focusState)
                }
                MenuListItem(
                    modifier = Modifier
                        .ifElse(index == 0, Modifier.focusRequester(restorerFocusRequester))
                        .focusRequester(menuItemRequesters[item.ordinal]),
                    text = item.getClosedCaptionDisplayName(bilingualSubtitleEnabled, context),
                    selected = selectedClosedCaptionMenuItem == item,
                    onClick = selectItem,
                    onFocus = { selectedClosedCaptionMenuItem = item },
                )
            }
        }
    }
}

internal sealed interface SecondarySubtitleMenuOption {
    data object Off : SecondarySubtitleMenuOption
    data class Option(val option: SecondarySubtitleOption) : SecondarySubtitleMenuOption
}

internal fun buildSecondarySubtitleMenuOptions(
    options: List<SecondarySubtitleOption>
): List<SecondarySubtitleMenuOption> {
    return listOf(SecondarySubtitleMenuOption.Off) +
        options.map { SecondarySubtitleMenuOption.Option(it) }
}

internal fun resolveSelectedSecondarySubtitleMenuIndex(
    options: List<SecondarySubtitleMenuOption>,
    currentSecondarySubtitleId: Long,
    currentSecondarySubtitleCustom: Boolean
): Int {
    return options.indexOfFirst { option ->
        when (option) {
            SecondarySubtitleMenuOption.Off ->
                !currentSecondarySubtitleCustom && currentSecondarySubtitleId == -1L

            is SecondarySubtitleMenuOption.Option -> when (val subtitleOption = option.option) {
                SecondarySubtitleOption.CustomTranslation -> currentSecondarySubtitleCustom
                is SecondarySubtitleOption.BiliTrack -> !currentSecondarySubtitleCustom &&
                    subtitleOption.subtitle.id == currentSecondarySubtitleId
            }
        }
    }.takeIf { it >= 0 } ?: 0
}

val SubtitleOffTrack = Subtitle(
    id = -1L,
    lang = "",
    langDoc = "关闭",
    url = "",
    type = SubtitleType.CC,
    aiType = SubtitleAiType.Normal,
    aiStatus = SubtitleAiStatus.None
)

val SubtitleCustomTrack = Subtitle(
    id = CustomSubtitleTrackId,
    lang = "custom",
    langDoc = "自定义",
    url = "",
    type = SubtitleType.CC,
    aiType = SubtitleAiType.Normal,
    aiStatus = SubtitleAiStatus.None
)

private fun VideoPlayerClosedCaptionMenuItem.getClosedCaptionDisplayName(
    bilingualSubtitleEnabled: Boolean,
    context: android.content.Context
): String {
    if (this == VideoPlayerClosedCaptionMenuItem.Main && !bilingualSubtitleEnabled) {
        return context.getString(R.string.video_player_menu_subtitle_choose)
    }
    return getDisplayName(context)
}

private fun Subtitle.toSubtitleMenuName(): String {
    return langDoc
        .replace("（自动生成）", "")
        .replace("（自动翻译）", "")
        .trim() + if (type == SubtitleType.AI) "(AI)" else ""
}

private fun SecondarySubtitleMenuOption.toMenuName(context: android.content.Context): String {
    return when (this) {
        SecondarySubtitleMenuOption.Off -> "关闭"
        is SecondarySubtitleMenuOption.Option -> when (option) {
            SecondarySubtitleOption.CustomTranslation -> context.getString(R.string.video_player_menu_subtitle_custom)
            is SecondarySubtitleOption.BiliTrack -> option.subtitle.toSubtitleMenuName()
        }
    }
}
