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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
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
import org.koin.androidx.compose.koinViewModel

internal fun shouldRouteLiveRoomUpToCategory(index: Int, columns: Int): Boolean {
    return columns > 0 && index in 0 until columns
}

internal fun shouldRouteLiveRoomUpToPreviousRow(index: Int, columns: Int): Boolean {
    return columns > 0 && index >= columns
}

@Composable
fun LiveContent(
    navFocusRequester: FocusRequester,
    onLogin: () -> Unit,
    liveViewModel: LiveViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val liveColumns = 4
    val gridState = rememberLazyGridState()
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
                liveViewModel.refresh()
                navFocusRequester.requestFocus()
                true
            }
    ) {
        if (liveViewModel.categories.isNotEmpty()) {
            TopNav(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .focusRequester(navFocusRequester)
                    .focusProperties {
                        down = roomFocusRequesters.firstOrNull() ?: FocusRequester.Default
                    },
                items = liveViewModel.categories,
                isLargePadding = true,
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
                SmallVideoCard(
                    modifier = Modifier
                        .focusRequester(roomFocusRequesters[index])
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
