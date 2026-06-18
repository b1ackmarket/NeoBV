package dev.aaa1115910.bv.screen.main.live

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.activities.live.LivePlayerActivity
import dev.aaa1115910.bv.component.FilterChip
import dev.aaa1115910.bv.component.FilterChipDefaults
import dev.aaa1115910.bv.component.SelectableItemPopup
import dev.aaa1115910.bv.component.TopNav
import dev.aaa1115910.bv.component.TvLazyVerticalGrid
import dev.aaa1115910.bv.component.ifElse
import dev.aaa1115910.bv.component.videocard.SmallVideoCard
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.entity.live.LiveCategory
import dev.aaa1115910.bv.entity.live.LiveCategoryType
import dev.aaa1115910.bv.repository.LiveJumpModeRepository
import dev.aaa1115910.bv.repository.toLiveJumpModeItems
import dev.aaa1115910.bv.screen.main.LoginRequiredPlaceholder
import dev.aaa1115910.bv.util.LayoutConfig
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.rememberAdaptiveGridCells
import dev.aaa1115910.bv.viewmodel.live.LiveViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

internal fun shouldRouteLiveRoomUpToCategory(index: Int, columns: Int): Boolean {
    return columns > 0 && index in 0 until columns
}

internal fun shouldRouteLiveRoomUpToPreviousRow(index: Int, columns: Int): Boolean {
    return false
}

internal fun shouldLoadMoreLiveRooms(
    focusedIndex: Int,
    roomCount: Int,
    preloadThreshold: Int = 8
): Boolean {
    return focusedIndex >= 0 && roomCount > 0 && focusedIndex + preloadThreshold >= roomCount
}

internal fun shouldHandleLiveMenuKey(isRoomGridFocused: Boolean): Boolean {
    return isRoomGridFocused
}

internal fun shouldResetLiveRoomGridOnMenu(isRoomGridFocused: Boolean): Boolean {
    return isRoomGridFocused
}

internal fun targetLiveRoomIndexAfterCategoryDown(roomCount: Int): Int? {
    return null
}

internal fun shouldShowLiveLoginPlaceholder(
    isLogin: Boolean,
    selectedCategory: LiveCategory?
): Boolean {
    return !isLogin && selectedCategory?.type == LiveCategoryType.Following
}

@Composable
fun LiveContent(
    navFocusRequester: FocusRequester,
    onLogin: () -> Unit,
    liveViewModel: LiveViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val liveJumpModeRepository = remember { BVApp.koinApplication.koin.get<LiveJumpModeRepository>() }
    val liveColumns = 4
    val liveGridCells = rememberAdaptiveGridCells(defaultColumns = liveColumns)
    val gridState = rememberLazyGridState()
    var isRoomGridFocused by remember { mutableStateOf(false) }
    var isSubCategoryRowFocused by remember { mutableStateOf(false) }
    var showSubCategoryPopup by remember { mutableStateOf(false) }
    val loginFocusRequester = remember { FocusRequester() }
    val firstRoomFocusRequester = remember { FocusRequester() }

    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItem = gridState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem != null && liveViewModel.rooms.isNotEmpty() &&
                    lastVisibleItem.index + 8 >= gridState.layoutInfo.totalItemsCount
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            val lastVisibleItem = gridState.layoutInfo.visibleItemsInfo.lastOrNull()
            if (lastVisibleItem != null) {
                liveViewModel.loadMoreIfNeeded(lastVisibleItem.index)
            }
        }
    }

    LaunchedEffect(liveViewModel.isLogin) {
        liveViewModel.onLoginStateChanged(liveViewModel.isLogin)
    }

    LaunchedEffect(liveViewModel.selectedCategoryIndex) {
        gridState.scrollToItem(0)
        isRoomGridFocused = false
        isSubCategoryRowFocused = false
        showSubCategoryPopup = false
    }

    LaunchedEffect(gridState, liveViewModel.rooms.size) {
        snapshotFlow { gridState.firstVisibleItemIndex }
            .collect { firstVisibleIndex ->
                if (liveViewModel.rooms.isNotEmpty() && firstVisibleIndex >= liveViewModel.rooms.size) {
                    gridState.scrollToItem(0)
                }
            }
    }

    val categoryKeys = liveViewModel.categories.joinToString("|") { it.key }
    val visibleCategories = remember(categoryKeys, Prefs.layoutConfigJson) {
        LayoutConfig.applyLive(liveViewModel.categories)
    }
    val selectedCategory = liveViewModel.categories.getOrNull(liveViewModel.selectedCategoryIndex)
    val showLoginPlaceholder = shouldShowLiveLoginPlaceholder(liveViewModel.isLogin, selectedCategory)
    val childCategories = selectedCategory
        ?.takeIf { it.type == LiveCategoryType.Partition }
        ?.children
        .orEmpty()

    val visibleCategoryKeys = visibleCategories.joinToString("|") { it.key }
    LaunchedEffect(visibleCategoryKeys, liveViewModel.selectedCategoryIndex) {
        val current = liveViewModel.categories.getOrNull(liveViewModel.selectedCategoryIndex)
        if (current != null && visibleCategories.none { it.key == current.key }) {
            val firstVisible = visibleCategories.firstOrNull()
            val index = firstVisible?.let { liveViewModel.categories.indexOf(it) } ?: -1
            if (index >= 0) liveViewModel.selectCategory(index)
        }
    }

    Scaffold(
        topBar = {
            if (visibleCategories.isNotEmpty()) {
                TopNav(
                    modifier = Modifier
                        .focusRequester(navFocusRequester)
                        .onFocusChanged {
                            if (it.hasFocus) {
                                isRoomGridFocused = false
                                isSubCategoryRowFocused = false
                            }
                        },
                    items = visibleCategories,
                    isLargePadding = !(isRoomGridFocused || isSubCategoryRowFocused),
                    downFocusRequester = loginFocusRequester.takeIf { showLoginPlaceholder }
                        ?: firstRoomFocusRequester.takeIf { liveViewModel.rooms.isNotEmpty() }
                        ?: FocusRequester.Default,
                    onSelectedChanged = { nav ->
                        val index = liveViewModel.categories.indexOf(nav)
                        if (index >= 0 && index != liveViewModel.selectedCategoryIndex) {
                            liveViewModel.selectCategory(index)
                        }
                    },
                    onClick = { liveViewModel.refresh() }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyUp) return@onPreviewKeyEvent false
                    if (event.key != Key.Menu) return@onPreviewKeyEvent false
                    if (!shouldHandleLiveMenuKey(isRoomGridFocused || isSubCategoryRowFocused)) {
                        return@onPreviewKeyEvent false
                    }
                    if (shouldResetLiveRoomGridOnMenu(isRoomGridFocused || isSubCategoryRowFocused)) {
                        scope.launch {
                            gridState.scrollToItem(0)
                            navFocusRequester.requestFocus()
                        }
                    }
                    true
                }
        ) {
            if (showLoginPlaceholder) {
                LoginRequiredPlaceholder(
                    onLogin = onLogin,
                    focusRequester = loginFocusRequester
                )
            } else {
                TvLazyVerticalGrid(
                    modifier = Modifier.align(Alignment.TopStart),
                    state = gridState,
                    columns = liveGridCells,
                    contentPadding = PaddingValues(
                        start = 24.dp,
                        top = if (childCategories.isEmpty()) 24.dp else 0.dp,
                        end = 24.dp,
                        bottom = 24.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (childCategories.isNotEmpty()) {
                        item(span = { GridItemSpan(liveColumns) }) {
                            LiveSubCategoryRow(
                                modifier = Modifier.onFocusChanged {
                                    if (it.hasFocus) {
                                        isSubCategoryRowFocused = true
                                        isRoomGridFocused = false
                                    }
                                },
                                selectedSubCategory = liveViewModel.selectedSubCategory,
                                categories = childCategories,
                                onShowSelector = { showSubCategoryPopup = true },
                                onSelect = { liveViewModel.selectSubCategory(it) }
                            )
                        }
                    }

                    itemsIndexed(liveViewModel.rooms, key = { _, room -> room.roomId }) { index, room ->
                        SmallVideoCard(
                            modifier = Modifier
                                .ifElse(index == 0, Modifier.focusRequester(firstRoomFocusRequester))
                                .onFocusChanged {
                                    if (it.hasFocus) {
                                        isRoomGridFocused = true
                                        isSubCategoryRowFocused = false
                                    }
                                }
                                .ifElse(
                                    index == 0 && childCategories.isNotEmpty(),
                                    Modifier.focusProperties { up = FocusRequester.Default }
                                )
                                .ifElse(
                                    childCategories.isEmpty() &&
                                        shouldRouteLiveRoomUpToCategory(index = index, columns = liveColumns),
                                    Modifier.focusProperties { up = navFocusRequester }
                                )
                                .ifElse(
                                    shouldRouteLiveRoomUpToPreviousRow(index = index, columns = liveColumns),
                                    Modifier.focusProperties {
                                        up = FocusRequester.Default
                                    }
                                ),
                            data = VideoCardData(
                                avid = room.roomId.toLong(),
                                title = room.title,
                                upName = room.upName,
                                cover = room.cover,
                                playString = room.online.toString(),
                                danmakuString = room.areaName,
                                badges = room.badges
                            ),
                            onClick = {
                                liveJumpModeRepository.setPendingQueue(
                                    selectedRoomId = room.roomId,
                                    items = liveViewModel.rooms.toLiveJumpModeItems()
                                )
                                LivePlayerActivity.actionStart(
                                    context = context,
                                    roomId = room.roomId,
                                    title = room.title,
                                    upName = room.upName,
                                    online = room.online
                                )
                            }
                        )
                    }
                }

                if (liveViewModel.rooms.isEmpty() && !liveViewModel.loading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "暂无直播内容")
                    }
                }

                val allCategory = childCategories.firstOrNull { it.areaId == 0 } ?: childCategories.firstOrNull()
                SelectableItemPopup(
                    show = showSubCategoryPopup,
                    title = selectedCategory?.label ?: "选择直播分区",
                    items = childCategories,
                    selectedItem = liveViewModel.selectedSubCategory ?: allCategory,
                    label = { it.label },
                    onDismiss = { showSubCategoryPopup = false },
                    onSelect = { category ->
                        liveViewModel.selectSubCategory(category.takeUnless { it.areaId == 0 })
                    }
                )
            }
        }
    }
}

@Composable
private fun LiveSubCategoryRow(
    modifier: Modifier = Modifier,
    selectedSubCategory: LiveCategory?,
    categories: List<LiveCategory>,
    onShowSelector: () -> Unit,
    onSelect: (LiveCategory?) -> Unit
) {
    val allCategory = categories.firstOrNull { it.areaId == 0 } ?: categories.firstOrNull()

    LazyRow(
        modifier = modifier,
        contentPadding = FilterChipDefaults.RowContentPadding,
        horizontalArrangement = Arrangement.spacedBy(FilterChipDefaults.RowSpacing)
    ) {
        item {
            FilterChip(
                text = selectedSubCategory?.label ?: allCategory?.label ?: "全部",
                icon = Icons.Rounded.Tune,
                maxWidth = FilterChipDefaults.SelectorMaxWidth,
                onClick = onShowSelector
            )
        }
        items(categories, key = { it.key }) { category ->
            val selectCategory = { onSelect(category.takeUnless { it.areaId == 0 }) }
            FilterChip(
                text = category.label,
                onClick = selectCategory
            )
        }
    }
}
