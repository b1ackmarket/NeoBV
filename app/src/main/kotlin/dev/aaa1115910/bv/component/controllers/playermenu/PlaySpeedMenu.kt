package dev.aaa1115910.bv.component.controllers.playermenu

import android.content.Context
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
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.component.controllers.LocalMenuFocusStateData
import dev.aaa1115910.bv.component.controllers.MenuFocusState
import dev.aaa1115910.bv.component.controllers.playermenu.component.StepLessMenuItem
import dev.aaa1115910.bv.component.controllers.playermenu.component.MenuListItem
import dev.aaa1115910.bv.component.ifElse
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

internal const val MIN_CUSTOM_PLAY_SPEED = 0.5f
internal const val MAX_CUSTOM_PLAY_SPEED = 2.0f

sealed interface PlaySpeedMenuItem {
    data object Custom : PlaySpeedMenuItem
    data class Preset(val preset: PlaySpeedPreset) : PlaySpeedMenuItem
}

typealias PlaySpeedPreset = PlaySpeedItem

internal fun clampCustomPlaySpeed(speed: Float): Float {
    return ((speed * 10).roundToInt() / 10f).coerceIn(MIN_CUSTOM_PLAY_SPEED, MAX_CUSTOM_PLAY_SPEED)
}

private fun findPresetBySpeed(speed: Float): PlaySpeedPreset? {
    return PlaySpeedPreset.entries.find { abs(it.speed - speed) < 0.001f }
}

private fun isAlignedToTenth(speed: Float): Boolean {
    return abs(speed * 10f - (speed * 10f).roundToInt()) < 0.001f
}

internal fun resolveCustomPlaySpeedDisplay(speed: Float): Float {
    return findPresetBySpeed(speed)?.speed ?: clampCustomPlaySpeed(speed)
}

internal fun stepCustomPlaySpeed(currentSpeed: Float, direction: Int): Float {
    if (direction == 0) return resolveCustomPlaySpeedDisplay(currentSpeed)
    val normalizedDirection = if (direction > 0) 1 else -1
    val currentDisplaySpeed = resolveCustomPlaySpeedDisplay(currentSpeed)
    if (!isAlignedToTenth(currentDisplaySpeed)) {
        val snapped = if (normalizedDirection > 0) {
            ceil(currentDisplaySpeed * 10f) / 10f
        } else {
            floor(currentDisplaySpeed * 10f) / 10f
        }
        return snapped.coerceIn(MIN_CUSTOM_PLAY_SPEED, MAX_CUSTOM_PLAY_SPEED)
    }
    return clampCustomPlaySpeed(currentDisplaySpeed + normalizedDirection * 0.1f)
}

internal fun resolvePlaySpeedMenuItem(speed: Float): PlaySpeedMenuItem {
    val preset = findPresetBySpeed(speed)
    return if (preset != null) {
        PlaySpeedMenuItem.Preset(preset)
    } else {
        PlaySpeedMenuItem.Custom
    }
}

internal fun syncPlaySpeedMenuItem(
    currentSelection: PlaySpeedMenuItem,
    currentSpeed: Float
): PlaySpeedMenuItem {
    return if (currentSelection == PlaySpeedMenuItem.Custom) {
        PlaySpeedMenuItem.Custom
    } else {
        resolvePlaySpeedMenuItem(currentSpeed)
    }
}

internal fun resolveDisplayedPlaySpeedMenuItem(
    appliedSelection: PlaySpeedMenuItem,
    focusedSelection: PlaySpeedMenuItem,
    showFocusedSelection: Boolean
): PlaySpeedMenuItem {
    return if (showFocusedSelection) focusedSelection else appliedSelection
}

internal fun resolveConfirmedPresetSpeed(
    focusedItem: PlaySpeedMenuItem
): Float? {
    return (focusedItem as? PlaySpeedMenuItem.Preset)?.preset?.speed
}

private fun formatPlaySpeed(speed: Float): String = "${resolveCustomPlaySpeedDisplay(speed)}x"

@Composable
fun PlaySpeedMenuList(
    modifier: Modifier = Modifier,
    currentPlaySpeed: Float,
    onPlaySpeedChange: (Float) -> Unit,
    onFocusStateChange: (MenuFocusState) -> Unit
) {
    val context = LocalContext.current
    val focusState = LocalMenuFocusStateData.current
    val restorerFocusRequester = remember { FocusRequester() }
    val focusRequester = remember { FocusRequester() }
    var focusedPlaySpeedMenuItem by remember { mutableStateOf(resolvePlaySpeedMenuItem(currentPlaySpeed)) }
    var customSelectionLocked by remember {
        mutableStateOf(resolvePlaySpeedMenuItem(currentPlaySpeed) == PlaySpeedMenuItem.Custom)
    }
    val menuItemRequesters = remember {
        mutableStateListOf<FocusRequester>().apply {
            addAll(List(PlaySpeedItem.entries.size + 1) { FocusRequester() })
        }
    }
    val appliedPlaySpeedMenuItem = if (customSelectionLocked) {
        syncPlaySpeedMenuItem(
            currentSelection = PlaySpeedMenuItem.Custom,
            currentSpeed = currentPlaySpeed
        )
    } else {
        resolvePlaySpeedMenuItem(currentPlaySpeed)
    }
    val shouldFocusItems = focusState.focusState == MenuFocusState.Items &&
            focusedPlaySpeedMenuItem == PlaySpeedMenuItem.Custom
    LaunchedEffect(currentPlaySpeed) {
        if (focusedPlaySpeedMenuItem != PlaySpeedMenuItem.Custom) {
            focusedPlaySpeedMenuItem = resolvePlaySpeedMenuItem(currentPlaySpeed)
        }
    }

    LaunchedEffect(focusState.focusState, focusedPlaySpeedMenuItem) {
        if (focusState.focusState == MenuFocusState.Menu) {
            val focusedIndex = when (val item = focusedPlaySpeedMenuItem) {
                PlaySpeedMenuItem.Custom -> 0
                is PlaySpeedMenuItem.Preset -> PlaySpeedItem.entries.indexOf(item.preset) + 1
            }
            val index = resolveParentMenuFocusIndex(
                selectedIndex = focusedIndex,
                itemCount = menuItemRequesters.size
            )
            menuItemRequesters[index].requestFocus()
        }
    }

    Row(
        modifier = modifier
            .fillMaxHeight(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val menuItemsModifier = Modifier
            .width(216.dp)
            .padding(horizontal = 8.dp)
        AnimatedVisibility(visible = focusState.focusState != MenuFocusState.MenuNav) {
            when (focusedPlaySpeedMenuItem) {
                PlaySpeedMenuItem.Custom -> StepLessMenuItem(
                    modifier = menuItemsModifier,
                    value = resolveCustomPlaySpeedDisplay(currentPlaySpeed),
                    step = 0.1f,
                    range = MIN_CUSTOM_PLAY_SPEED..MAX_CUSTOM_PLAY_SPEED,
                    text = formatPlaySpeed(currentPlaySpeed),
                    requestFocusWhen = shouldFocusItems,
                    stepValueResolver = { currentValue, direction ->
                        stepCustomPlaySpeed(currentValue, direction)
                    },
                    onValueChange = {
                        customSelectionLocked = true
                        onPlaySpeedChange(it)
                    },
                    onFocusBackToParent = {
                        onFocusStateChange(MenuFocusState.Menu)
                    }
                )

                is PlaySpeedMenuItem.Preset -> Unit
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
                        Key.DirectionRight -> {
                            onFocusStateChange(MenuFocusState.MenuNav)
                            return@onPreviewKeyEvent true
                        }

                        Key.DirectionLeft -> {
                            if (focusedPlaySpeedMenuItem == PlaySpeedMenuItem.Custom) {
                                onFocusStateChange(MenuFocusState.Items)
                                return@onPreviewKeyEvent true
                            }
                        }

                        Key.Enter, Key.DirectionCenter -> {
                            if (focusedPlaySpeedMenuItem == PlaySpeedMenuItem.Custom) {
                                onFocusStateChange(MenuFocusState.Items)
                                return@onPreviewKeyEvent true
                            }
                        }
                    }
                    false
                }
                .focusRestorer(restorerFocusRequester),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(8.dp),
        ) {
            item {
                MenuListItem(
                    modifier = Modifier
                        .focusRequester(restorerFocusRequester)
                        .focusRequester(menuItemRequesters[0]),
                    text = "自定义",
                    selected = appliedPlaySpeedMenuItem == PlaySpeedMenuItem.Custom,
                    onClick = {
                        onFocusStateChange(MenuFocusState.Items)
                    },
                    onFocus = { focusedPlaySpeedMenuItem = PlaySpeedMenuItem.Custom }
                )
            }
            itemsIndexed(PlaySpeedItem.entries.toMutableList()) { index, item ->
                MenuListItem(
                    modifier = Modifier.focusRequester(menuItemRequesters[index + 1]),
                    text = item.getDisplayName(context),
                    selected = appliedPlaySpeedMenuItem == PlaySpeedMenuItem.Preset(item),
                    onClick = {
                        customSelectionLocked = false
                        onPlaySpeedChange(item.speed)
                    },
                    onFocus = { focusedPlaySpeedMenuItem = PlaySpeedMenuItem.Preset(item) }
                )
            }
        }
    }
}

enum class PlaySpeedItem(val code: Int, private val strRes: Int, val speed: Float) {
    x2(4, R.string.play_speed_x2, 2f),
    x1_5(3, R.string.play_speed_x1_5, 1.5f),
    x1_25(2, R.string.play_speed_x1_25, 1.25f),
    x1(1, R.string.play_speed_x1, 1f),
    x0_5(0, R.string.play_speed_x0_5, 0.5f);

    companion object {
        fun fromCode(code: Int): PlaySpeedItem {
            return entries.find { it.code == code } ?: x1
        }

        fun fromSpeed(speed: Float): PlaySpeedItem {
            return entries.find { it.speed == speed } ?: x1
        }
    }

    fun getDisplayName(context: Context) = context.getString(strRes)
}
