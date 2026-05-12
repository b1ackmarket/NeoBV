package dev.aaa1115910.bv.screen.main.live

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import dev.aaa1115910.bv.component.TopNav
import dev.aaa1115910.bv.activities.live.LivePlayerActivity
import dev.aaa1115910.bv.component.TvLazyVerticalGrid
import dev.aaa1115910.bv.component.ifElse
import dev.aaa1115910.bv.component.videocard.SmallVideoCard
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.screen.main.LoginRequiredPlaceholder
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
    return 0.takeIf { roomCount > 0 }
}

@Composable
fun LiveContent(
    navFocusRequester: FocusRequester,
    onLogin: () -> Unit,
    liveViewModel: LiveViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val liveColumns = 4
    val gridState = rememberLazyGridState()
    var isRoomGridFocused by remember { mutableStateOf(false) }
    val roomFocusRequesters = remember(liveViewModel.rooms.map { it.roomId }) {
        List(liveViewModel.rooms.size) { FocusRequester() }
    }

    LaunchedEffect(liveViewModel.isLogin) {
        liveViewModel.onLoginStateChanged(liveViewModel.isLogin)
    }

    if (!liveViewModel.isLogin) {
        LoginRequiredPlaceholder(
            onLogin = onLogin,
            focusRequester = navFocusRequester
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp) return@onPreviewKeyEvent false
                if (event.key != Key.Menu) return@onPreviewKeyEvent false
                if (!shouldHandleLiveMenuKey(isRoomGridFocused)) return@onPreviewKeyEvent false
                if (shouldResetLiveRoomGridOnMenu(isRoomGridFocused)) {
                    scope.launch {
                        gridState.scrollToItem(0)
                        navFocusRequester.requestFocus()
                    }
                }
                true
            }
    ) {
        if (liveViewModel.categories.isNotEmpty()) {
            val categoryDownFocusRequester = targetLiveRoomIndexAfterCategoryDown(liveViewModel.rooms.size)
                ?.let { roomFocusRequesters.getOrNull(it) }
                ?: FocusRequester.Default
            TopNav(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .focusRequester(navFocusRequester)
                    .onFocusChanged {
                        if (it.hasFocus) {
                            isRoomGridFocused = false
                        }
                    },
                items = liveViewModel.categories,
                isLargePadding = true,
                downFocusRequester = categoryDownFocusRequester,
                onSelectedChanged = { nav ->
                    val index = liveViewModel.categories.indexOf(nav)
                    if (index >= 0 && index != liveViewModel.selectedCategoryIndex) {
                        liveViewModel.selectCategory(index)
                    }
                },
                onClick = { liveViewModel.refresh() }
            )
        }

        TvLazyVerticalGrid(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 72.dp),
            state = gridState,
            columns = GridCells.Fixed(liveColumns),
            contentPadding = PaddingValues(
                start = 24.dp,
                end = 24.dp,
                bottom = 24.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(liveViewModel.rooms, key = { _, room -> room.roomId }) { index, room ->
                val roomFocusRequester = roomFocusRequesters.getOrNull(index)
                SmallVideoCard(
                    modifier = Modifier
                        .then(
                            if (roomFocusRequester != null) {
                                Modifier.focusRequester(roomFocusRequester)
                            } else {
                                Modifier
                            }
                        )
                        .onFocusChanged {
                            if (it.hasFocus) {
                                isRoomGridFocused = true
                                liveViewModel.loadMoreIfNeeded(index)
                            }
                        }
                        .ifElse(
                            shouldRouteLiveRoomUpToCategory(index = index, columns = liveColumns),
                            Modifier.focusProperties { up = navFocusRequester }
                        )
                        .ifElse(
                            shouldRouteLiveRoomUpToPreviousRow(index = index, columns = liveColumns),
                            Modifier.focusProperties {
                                up = roomFocusRequesters.getOrNull(index - liveColumns)
                                    ?: FocusRequester.Default
                            }
                        ),
                    data = VideoCardData(
                        avid = room.roomId.toLong(),
                        title = room.title,
                        cover = room.cover,
                        upName = room.upName,
                        playString = room.online.toString(),
                        danmakuString = room.areaName
                    ),
                    onClick = {
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
    }
}
