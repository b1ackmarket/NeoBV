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
import androidx.compose.ui.unit.dp
import dev.aaa1115910.biliapi.entity.AiAudioTranslation
import dev.aaa1115910.bv.component.controllers.LocalMenuFocusStateData
import dev.aaa1115910.bv.component.controllers.MenuFocusState
import dev.aaa1115910.bv.component.controllers.VideoPlayerPictureMenuItem
import dev.aaa1115910.bv.component.controllers.playermenu.component.MenuListItem
import dev.aaa1115910.bv.component.controllers.playermenu.component.RadioMenuList
import dev.aaa1115910.bv.component.ifElse
import dev.aaa1115910.bv.entity.Audio
import dev.aaa1115910.bv.entity.Resolution
import dev.aaa1115910.bv.entity.VideoAspectRatio
import dev.aaa1115910.bv.entity.VideoCodec

@Composable
fun PictureMenuList(
    modifier: Modifier = Modifier,
    availableQuality: Map<Int, String>,
    availableAudio: List<Audio>,
    availableVideoCodec: List<VideoCodec>,
    currentResolution: Int?,
    currentVideoCodec: VideoCodec,
    currentVideoAspectRatio: VideoAspectRatio,
    currentAudio: Audio,
    aiAudioTranslations: List<AiAudioTranslation>,
    currentAiAudioLanguage: String,
    onResolutionChange: (Int) -> Unit,
    onCodecChange: (VideoCodec) -> Unit,
    onAspectRatioChange: (VideoAspectRatio) -> Unit,
    onAudioChange: (Audio) -> Unit,
    onAiAudioTranslationChange: (String) -> Unit,
    onFocusStateChange: (MenuFocusState) -> Unit
) {
    val context = LocalContext.current
    val focusState = LocalMenuFocusStateData.current
    val restorerFocusRequester = remember { FocusRequester() }

    val focusRequester = remember { FocusRequester() }
    var selectedPictureMenuItem by remember { mutableStateOf(VideoPlayerPictureMenuItem.Resolution) }
    val pictureMenuItems = remember(availableQuality, availableVideoCodec, availableAudio, aiAudioTranslations) {
        VideoPlayerPictureMenuItem.entries
            .filter { item ->
                when (item) {
                    VideoPlayerPictureMenuItem.Resolution -> availableQuality.isNotEmpty()
                    VideoPlayerPictureMenuItem.Codec -> availableVideoCodec.isNotEmpty()
                    VideoPlayerPictureMenuItem.Audio -> availableAudio.isNotEmpty()
                    VideoPlayerPictureMenuItem.AiAudioTranslation -> aiAudioTranslations.isNotEmpty()
                    VideoPlayerPictureMenuItem.AspectRatio -> true
                }
            }
    }
    val menuItemRequesters = remember(pictureMenuItems) {
        mutableStateListOf<FocusRequester>().apply {
            addAll(pictureMenuItems.map { FocusRequester() })
        }
    }
    val qualityIdList = remember(availableQuality) {
        availableQuality.keys
            .sortedByDescending {it}
    }
    val audioList = remember(availableAudio) {
        availableAudio.sortedBy { it.ordinal }
    }
    val shouldFocusItems = focusState.focusState == MenuFocusState.Items

    LaunchedEffect(pictureMenuItems) {
        if (selectedPictureMenuItem !in pictureMenuItems) {
            selectedPictureMenuItem = pictureMenuItems.firstOrNull() ?: VideoPlayerPictureMenuItem.AspectRatio
        }
    }

    LaunchedEffect(focusState.focusState, selectedPictureMenuItem) {
        if (focusState.focusState == MenuFocusState.Menu) {
            val index = resolveParentMenuFocusIndex(
                selectedIndex = pictureMenuItems.indexOf(selectedPictureMenuItem).takeIf { it >= 0 } ?: 0,
                itemCount = menuItemRequesters.size
            )
            menuItemRequesters.getOrNull(index)?.requestFocus()
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
            when (selectedPictureMenuItem) {
                VideoPlayerPictureMenuItem.Resolution -> RadioMenuList(
                    modifier = menuItemsModifier,
                    items = qualityIdList.map { resolutionCode ->
                        availableQuality[resolutionCode]
                            ?: runCatching {
                                Resolution.entries.find { it.code == resolutionCode }!!
                                    .getShortDisplayName(context)
                            }.getOrDefault("unknown: $resolutionCode")
                    },
                    selected = qualityIdList.indexOf(currentResolution),
                    requestFocusWhen = shouldFocusItems,
                    onSelectedChanged = { onResolutionChange(qualityIdList[it]) },
                    onFocusBackToParent = {
                        onFocusStateChange(MenuFocusState.Menu)
                    }
                )

                VideoPlayerPictureMenuItem.Codec -> RadioMenuList(
                    modifier = menuItemsModifier,
                    items = availableVideoCodec.map { it.getDisplayName(context) },
                    selected = availableVideoCodec.indexOf(currentVideoCodec),
                    requestFocusWhen = shouldFocusItems,
                    onSelectedChanged = { onCodecChange(availableVideoCodec[it]) },
                    onFocusBackToParent = {
                        onFocusStateChange(MenuFocusState.Menu)
                    }
                )

                VideoPlayerPictureMenuItem.AspectRatio -> RadioMenuList(
                    modifier = menuItemsModifier,
                    items = VideoAspectRatio.entries.map { it.getDisplayName(context) },
                    selected = VideoAspectRatio.entries.indexOf(currentVideoAspectRatio),
                    requestFocusWhen = shouldFocusItems,
                    onSelectedChanged = { onAspectRatioChange(VideoAspectRatio.entries[it]) },
                    onFocusBackToParent = {
                        onFocusStateChange(MenuFocusState.Menu)
                    }
                )

                VideoPlayerPictureMenuItem.Audio -> RadioMenuList(
                    modifier = menuItemsModifier,
                    items = audioList.map { audio -> audio.getDisplayName(context) },
                    selected = audioList.indexOf(currentAudio),
                    requestFocusWhen = shouldFocusItems,
                    onSelectedChanged = { onAudioChange(audioList[it]) },
                    onFocusBackToParent = {
                        onFocusStateChange(MenuFocusState.Menu)
                    }
                )

                VideoPlayerPictureMenuItem.AiAudioTranslation -> {
                    val translationItems = listOf(null) + aiAudioTranslations
                    RadioMenuList(
                        modifier = menuItemsModifier,
                        items = translationItems.map { it?.title ?: "关闭" },
                        selected = translationItems.indexOfFirst { it?.lang.orEmpty() == currentAiAudioLanguage }
                            .takeIf { it >= 0 } ?: 0,
                        requestFocusWhen = shouldFocusItems,
                        onSelectedChanged = { index ->
                            onAiAudioTranslationChange(translationItems[index]?.lang.orEmpty())
                        },
                        onFocusBackToParent = {
                            onFocusStateChange(MenuFocusState.Menu)
                        }
                    )
                }
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
            itemsIndexed(pictureMenuItems) { index, item ->
                val selectItem = {
                    val result = resolveParentMenuTouch(
                        current = selectedPictureMenuItem,
                        touched = item
                    )
                    selectedPictureMenuItem = result.selectedItem
                    onFocusStateChange(result.focusState)
                }
                MenuListItem(
                    modifier = Modifier
                        .ifElse(index == 0, Modifier.focusRequester(restorerFocusRequester))
                        .focusRequester(menuItemRequesters[index]),
                    text = item.getDisplayName(context),
                    selected = selectedPictureMenuItem == item,
                    onClick = selectItem,
                    onFocus = { selectedPictureMenuItem = item },
                )
            }
        }
    }
}
