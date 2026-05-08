package dev.aaa1115910.bv.screen.main.live

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import dev.aaa1115910.bv.activities.live.LivePlayerActivity
import dev.aaa1115910.bv.component.TopNav
import dev.aaa1115910.bv.component.TvLazyVerticalGrid
import dev.aaa1115910.bv.component.ifElse
import dev.aaa1115910.bv.component.videocard.SmallVideoCard
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.screen.main.LoginRequiredPlaceholder
import dev.aaa1115910.bv.viewmodel.live.LiveViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun LiveContent(
    navFocusRequester: FocusRequester,
    onLogin: () -> Unit,
    liveViewModel: LiveViewModel = koinViewModel()
) {
    val context = LocalContext.current
    var focusOnContent by remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()
    val firstRoomFocusRequester = remember { FocusRequester() }

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

    Scaffold(
        topBar = {
            if (liveViewModel.categories.isNotEmpty()) {
                TopNav(
                    modifier = Modifier
                        .focusRequester(navFocusRequester)
                        .focusProperties {
                            down = firstRoomFocusRequester
                        },
                    items = liveViewModel.categories,
                    isLargePadding = !focusOnContent,
                    onSelectedChanged = { item ->
                        val index = liveViewModel.categories.indexOf(item)
                        if (index >= 0) liveViewModel.selectCategory(index)
                    },
                    onClick = {
                        liveViewModel.refresh()
                    }
                )
            }
        }
    ) { innerPadding ->
        TvLazyVerticalGrid(
            modifier = Modifier.focusProperties {
                up = navFocusRequester
            },
            state = gridState,
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(
                start = 24.dp,
                end = 24.dp,
                top = innerPadding.calculateTopPadding(),
                bottom = 24.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(liveViewModel.rooms, key = { _, room -> room.roomId }) { index, room ->
                SmallVideoCard(
                    modifier = Modifier
                        .ifElse(index == 0, Modifier.focusRequester(firstRoomFocusRequester))
                        .focusProperties {
                            up = navFocusRequester
                        },
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
                            title = room.title
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
