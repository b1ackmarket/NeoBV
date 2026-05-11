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
import dev.aaa1115910.bv.component.controllers.DanmakuType
import dev.aaa1115910.bv.component.controllers.LocalMenuFocusStateData
import dev.aaa1115910.bv.component.controllers.MenuFocusState
import dev.aaa1115910.bv.component.controllers.VideoPlayerDanmakuMenuItem
import dev.aaa1115910.bv.component.controllers.playermenu.component.CheckBoxMenuList
import dev.aaa1115910.bv.component.controllers.playermenu.component.MenuListItem
import dev.aaa1115910.bv.component.controllers.playermenu.component.RadioMenuList
import dev.aaa1115910.bv.component.controllers.playermenu.component.StepLessMenuItem
import dev.aaa1115910.bv.component.ifElse
import dev.aaa1115910.bv.entity.DanmakuSpeedFactor
import java.text.NumberFormat

@Composable
fun DanmakuMenuList(
    modifier: Modifier = Modifier,
    currentEnabledTypes: List<DanmakuType>,
    currentScale: Float,
    currentOpacity: Float,
    currentSpeedFactor: Float,
    currentArea: Float,
    currentMaskEnabled: Boolean,
    onDanmakuSwitchChange: (List<DanmakuType>) -> Unit,
    onDanmakuSizeChange: (Float) -> Unit,
    onDanmakuOpacityChange: (Float) -> Unit,
    onDanmakuSpeedFactorChange: (Float) -> Unit,
    onDanmakuAreaChange: (Float) -> Unit,
    onDanmakuMaskChange: (Boolean) -> Unit,
    onFocusStateChange: (MenuFocusState) -> Unit
) {
    val context = LocalContext.current
    val focusState = LocalMenuFocusStateData.current
    val restorerFocusRequester = remember { FocusRequester() }

    val focusRequester = remember { FocusRequester() }
    var selectedDanmakuMenuItem by remember { mutableStateOf(VideoPlayerDanmakuMenuItem.Switch) }
    val menuItemRequesters = remember {
        mutableStateListOf<FocusRequester>().apply {
            addAll(VideoPlayerDanmakuMenuItem.entries.map { FocusRequester() })
        }
    }
    val shouldFocusItems = focusState.focusState == MenuFocusState.Items

    LaunchedEffect(focusState.focusState, selectedDanmakuMenuItem) {
        if (focusState.focusState == MenuFocusState.Menu) {
            val index = resolveParentMenuFocusIndex(
                selectedIndex = selectedDanmakuMenuItem.ordinal,
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
            when (selectedDanmakuMenuItem) {
                VideoPlayerDanmakuMenuItem.Switch -> CheckBoxMenuList(
                    modifier = menuItemsModifier,
                    items = DanmakuType.entries.map { it.getDisplayName(context) },
                    selected = currentEnabledTypes.map { it.ordinal },
                    onSelectedChanged = { indices ->
                        val newSelection = indices
                            .map { index -> DanmakuType.entries[index] }
                            .toMutableList()

                        val allType = DanmakuType.All
                        val allEntries = DanmakuType.entries

                        val isAllInOld = currentEnabledTypes.contains(allType)
                        val isAllInNew = newSelection.contains(allType)

                        val realItemsCount = allEntries.size - 1

                        when {
                            // ---------------------------------------------------------
                            // 场景 1: 用户直接点击了 [全选] 框
                            // ---------------------------------------------------------

                            // 1.1 从无到有：点击全选 -> 选中所有
                            !isAllInOld && isAllInNew -> {
                                onDanmakuSwitchChange(allEntries)
                            }

                            // 1.2 从有到无：取消全选 -> 清空所有
                            isAllInOld && !isAllInNew -> {
                                onDanmakuSwitchChange(emptyList())
                            }

                            // ---------------------------------------------------------
                            // 场景 2: 用户点击了 [子选项] (触发联动)
                            // ---------------------------------------------------------

                            else -> {
                                // 先计算当前选中了多少个“真实子项”(排除 All)
                                val currentRealItemsCount = newSelection.count { it != allType }

                                if (currentRealItemsCount == realItemsCount) {
                                    // 触发自动 All：
                                    // 如果所有子项都齐了，但列表中没有 All，手动加上 All
                                    if (!newSelection.contains(allType)) {
                                        newSelection.add(allType)
                                    }
                                    onDanmakuSwitchChange(newSelection)
                                } else {
                                    // 触发自动移除 All：
                                    // 如果子项不齐（用户取消了某一项），但列表里还有 All，必须移除 All
                                    if (newSelection.contains(allType)) {
                                        newSelection.remove(allType)
                                    }
                                    onDanmakuSwitchChange(newSelection)
                                }
                            }
                        }
                    },
                    requestFocusWhen = shouldFocusItems,
                    onFocusBackToParent = {
                        onFocusStateChange(MenuFocusState.Menu)
                    }
                )

                VideoPlayerDanmakuMenuItem.Size -> StepLessMenuItem(
                    modifier = menuItemsModifier,
                    value = currentScale,
                    step = 0.01f,
                    range = 0.5f..4f,
                    text = NumberFormat.getPercentInstance()
                        .apply { maximumFractionDigits = 0 }
                        .format(currentScale),
                    requestFocusWhen = shouldFocusItems,
                    onValueChange = onDanmakuSizeChange,
                    onFocusBackToParent = { onFocusStateChange(MenuFocusState.Menu) }
                )

                VideoPlayerDanmakuMenuItem.Opacity -> StepLessMenuItem(
                    modifier = menuItemsModifier,
                    value = currentOpacity,
                    step = 0.01f,
                    range = 0f..1f,
                    text = NumberFormat.getPercentInstance()
                        .apply { maximumFractionDigits = 0 }
                        .format(currentOpacity),
                    requestFocusWhen = shouldFocusItems,
                    onValueChange = onDanmakuOpacityChange,
                    onFocusBackToParent = { onFocusStateChange(MenuFocusState.Menu) }
                )

                VideoPlayerDanmakuMenuItem.SpeedFactor -> RadioMenuList(
                    modifier = menuItemsModifier,
                    items = DanmakuSpeedFactor.entries.map { it.getDisplayName(context) },
                    selected = DanmakuSpeedFactor.getIndexByFactor(currentSpeedFactor),
                    requestFocusWhen = shouldFocusItems,
                    onSelectedChanged = {
                        onDanmakuSpeedFactorChange(DanmakuSpeedFactor.entries[it].factor) },
                    onFocusBackToParent = {
                        onFocusStateChange(MenuFocusState.Menu)
                    }
                )

                VideoPlayerDanmakuMenuItem.Area -> StepLessMenuItem(
                    modifier = menuItemsModifier,
                    value = currentArea,
                    step = 0.01f,
                    range = 0f..1f,
                    text = NumberFormat.getPercentInstance()
                        .apply { maximumFractionDigits = 0 }
                        .format(currentArea),
                    requestFocusWhen = shouldFocusItems,
                    onValueChange = onDanmakuAreaChange,
                    onFocusBackToParent = { onFocusStateChange(MenuFocusState.Menu) }
                )

                VideoPlayerDanmakuMenuItem.Mask -> RadioMenuList(
                    modifier = menuItemsModifier,
                    items = listOf("关闭", "开启"),
                    selected = if (currentMaskEnabled) 1 else 0,
                    requestFocusWhen = shouldFocusItems,
                    onSelectedChanged = { onDanmakuMaskChange(it == 1) },
                    onFocusBackToParent = {
                        onFocusStateChange(MenuFocusState.Menu)
                    }
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
            itemsIndexed(VideoPlayerDanmakuMenuItem.entries) { index, item ->
                MenuListItem(
                    modifier = Modifier
                        .ifElse(index == 0, Modifier.focusRequester(restorerFocusRequester))
                        .focusRequester(menuItemRequesters[index]),
                    text = item.getDisplayName(context),
                    selected = selectedDanmakuMenuItem == item,
                    onClick = {},
                    onFocus = { selectedDanmakuMenuItem = item },
                )
            }
        }
    }
}
