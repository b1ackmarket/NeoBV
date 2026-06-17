package dev.aaa1115910.bv.component.controllers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import dev.aaa1115910.bv.component.controllers.playermenu.DanmakuMenuList
import dev.aaa1115910.bv.component.controllers.playermenu.component.MenuListItem
import dev.aaa1115910.bv.component.controllers.playermenu.component.RadioMenuList
import dev.aaa1115910.bv.repository.LiveLineOption
import dev.aaa1115910.bv.repository.LiveQualityOption
import dev.aaa1115910.bv.component.ifElse
import kotlinx.coroutines.delay

enum class LiveMenuNavItem {
    Quality,
    Line,
    Danmaku,
    BitrateBoost
}

@Composable
fun LiveMenuController(
    modifier: Modifier = Modifier,
    show: Boolean,
    qualityOptions: List<LiveQualityOption>,
    currentQuality: Int,
    lineOptions: List<LiveLineOption>,
    currentLineIndex: Int,
    danmakuState: LiveDanmakuMenuState,
    preferHighBitrate: Boolean,
    onQualitySelected: (LiveQualityOption) -> Unit,
    onLineSelected: (Int) -> Unit,
    onDanmakuStateChange: (LiveDanmakuMenuState) -> Unit,
    onPreferHighBitrateChange: (Boolean) -> Unit
) {
    var selectedNav by remember { mutableStateOf(LiveMenuNavItem.Quality) }
    var focusState by remember { mutableStateOf(MenuFocusState.MenuNav) }
    var openGeneration by remember { mutableStateOf(0) }
    val navFocusRequester = remember { FocusRequester() }
    val firstItemFocusRequester = remember { FocusRequester() }
    val navItemRequesters = remember {
        mutableStateListOf<FocusRequester>().apply {
            addAll(LiveMenuNavItem.entries.map { FocusRequester() })
        }
    }

    LaunchedEffect(show) {
        if (show) {
            openGeneration++
            selectedNav = LiveMenuNavItem.Quality
            focusState = MenuFocusState.MenuNav
            delay(80)
            runCatching { navItemRequesters[LiveMenuNavItem.Quality.ordinal].requestFocus() }
        } else {
            selectedNav = LiveMenuNavItem.Quality
            focusState = MenuFocusState.MenuNav
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.CenterEnd
    ) {
        AnimatedVisibility(
            visible = show,
            enter = fadeIn() + slideInHorizontally { it },
            exit = fadeOut() + slideOutHorizontally { it }
        ) {
            key(openGeneration) {
                Surface(
                    modifier = Modifier.fillMaxHeight(),
                    colors = SurfaceDefaults.colors(
                        containerColor = Color.Black.copy(alpha = 0.5f)
                    )
                ) {
                    CompositionLocalProvider(
                        LocalMenuFocusStateData provides MenuFocusStateData(focusState = focusState)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            when (selectedNav) {
                                LiveMenuNavItem.Quality -> RadioMenuList(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    items = qualityOptions.map { it.desc },
                                    selected = qualityOptions.indexOfFirst { it.qn == currentQuality },
                                    requestFocusWhen = focusState == MenuFocusState.Items,
                                    onSelectedChanged = { index -> onQualitySelected(qualityOptions[index]) },
                                    onFocusBackToParent = {
                                        focusState = MenuFocusState.MenuNav
                                        navItemRequesters[selectedNav.ordinal].requestFocus()
                                    }
                                )

                                LiveMenuNavItem.Line -> RadioMenuList(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    items = lineOptions.map { it.label },
                                    selected = currentLineIndex,
                                    requestFocusWhen = focusState == MenuFocusState.Items,
                                    onSelectedChanged = onLineSelected,
                                    onFocusBackToParent = {
                                        focusState = MenuFocusState.MenuNav
                                        navItemRequesters[selectedNav.ordinal].requestFocus()
                                    }
                                )

                                LiveMenuNavItem.Danmaku -> DanmakuMenuList(
                                    currentEnabledTypes = danmakuState.enabledTypes,
                                    currentScale = danmakuState.scale,
                                    currentOpacity = danmakuState.opacity,
                                    currentSpeedFactor = danmakuState.speedFactor,
                                    currentArea = danmakuState.area,
                                    currentMaskEnabled = danmakuState.maskEnabled,
                                    onDanmakuSwitchChange = {
                                        onDanmakuStateChange(danmakuState.copy(enabledTypes = it))
                                    },
                                    onDanmakuSizeChange = {
                                        onDanmakuStateChange(danmakuState.copy(scale = it))
                                    },
                                    onDanmakuOpacityChange = {
                                        onDanmakuStateChange(danmakuState.copy(opacity = it))
                                    },
                                    onDanmakuSpeedFactorChange = {
                                        onDanmakuStateChange(danmakuState.copy(speedFactor = it))
                                    },
                                    onDanmakuAreaChange = {
                                        onDanmakuStateChange(danmakuState.copy(area = it))
                                    },
                                    onDanmakuMaskChange = {
                                        onDanmakuStateChange(danmakuState.copy(maskEnabled = it))
                                    },
                                    onFocusStateChange = {
                                        focusState = when (it) {
                                            MenuFocusState.MenuNav -> {
                                                navItemRequesters[selectedNav.ordinal].requestFocus()
                                                MenuFocusState.MenuNav
                                            }

                                            else -> it
                                        }
                                    }
                                )

                                LiveMenuNavItem.BitrateBoost -> RadioMenuList(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    items = listOf("关闭", "开启"),
                                    selected = if (preferHighBitrate) 1 else 0,
                                    requestFocusWhen = focusState != MenuFocusState.MenuNav,
                                    onSelectedChanged = { index -> onPreferHighBitrateChange(index == 1) },
                                    onFocusBackToParent = {
                                        focusState = MenuFocusState.MenuNav
                                        navItemRequesters[selectedNav.ordinal].requestFocus()
                                    }
                                )
                            }

                            LazyColumn(
                                modifier = Modifier
                                    .focusRequester(navFocusRequester)
                                    .focusRestorer(firstItemFocusRequester)
                                    .padding(horizontal = 8.dp)
                                    .onPreviewKeyEvent {
                                        if (it.type == KeyEventType.KeyUp) {
                                            if (listOf(Key.Enter, Key.DirectionCenter).contains(it.key)) {
                                                return@onPreviewKeyEvent false
                                            }
                                            return@onPreviewKeyEvent true
                                        }
                                        when (it.key) {
                                            Key.DirectionLeft,
                                            Key.DirectionCenter,
                                            Key.Enter -> {
                                                focusState = if (
                                                    selectedNav.opensMenuPanel()
                                                ) {
                                                    MenuFocusState.Menu
                                                } else {
                                                    MenuFocusState.Items
                                                }
                                                true
                                            }

                                            else -> false
                                        }
                                    },
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(16.dp)
                            ) {
                                itemsIndexed(LiveMenuNavItem.entries) { index, item ->
                                    MenuListItem(
                                        modifier = Modifier
                                            .ifElse(index == 0, Modifier.focusRequester(firstItemFocusRequester))
                                            .focusRequester(navItemRequesters[index])
                                            .onKeyEvent {
                                                if (it.type == KeyEventType.KeyUp) {
                                                    if (it.key == Key.DirectionUp || it.key == Key.DirectionDown) return@onKeyEvent true
                                                    return@onKeyEvent false
                                                }
                                                when (it.key) {
                                                    Key.DirectionUp -> {
                                                        if (index == 0) {
                                                            navItemRequesters.lastOrNull()?.requestFocus()
                                                            true
                                                        } else false
                                                    }
                                                    Key.DirectionDown -> {
                                                        if (index == LiveMenuNavItem.entries.lastIndex) {
                                                            navItemRequesters.firstOrNull()?.requestFocus()
                                                            true
                                                        } else false
                                                    }
                                                    else -> false
                                                }
                                            },
                                        text = item.toDisplayName(),
                                        selected = focusState == MenuFocusState.MenuNav && selectedNav == item,
                                        onClick = {
                                            selectedNav = item
                                            focusState =
                                                if (item.opensMenuPanel()) {
                                                    MenuFocusState.Menu
                                                } else {
                                                    MenuFocusState.Items
                                                }
                                        },
                                        onFocus = { selectedNav = item }
                                    )
                                }

                            }
                        }
                    }
                }
            }
        }
    }
}

data class LiveDanmakuMenuState(
    val enabledTypes: List<DanmakuType>,
    val scale: Float,
    val opacity: Float,
    val speedFactor: Float,
    val area: Float,
    val maskEnabled: Boolean
)

private fun LiveMenuNavItem.toDisplayName(): String = when (this) {
    LiveMenuNavItem.Quality -> "画质"
    LiveMenuNavItem.Line -> "线路"
    LiveMenuNavItem.Danmaku -> "弹幕"
    LiveMenuNavItem.BitrateBoost -> "码率增强"
}

private fun LiveMenuNavItem.opensMenuPanel(): Boolean =
    this == LiveMenuNavItem.Danmaku ||
        this == LiveMenuNavItem.BitrateBoost
