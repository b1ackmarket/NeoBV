package dev.aaa1115910.bv.component.controllers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.DenseListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import dev.aaa1115910.bv.entity.VideoListItem

@Composable
internal fun VideoListController(
    modifier: Modifier = Modifier,
    show: Boolean,
    currentCid: Long,
    panelState: VideoListPanelState,
    onPlayNewVideo: (VideoListItem) -> Unit,
) {
    val listState = rememberLazyListState()
    val itemFocusRequester = remember { FocusRequester() }

    // 自动定位到当前分P
    LaunchedEffect(show, panelState.items, currentCid) {
        if (show) {
            val currentIndex = panelState.items.indexOfFirst { item -> item.cid == currentCid }

            if (currentIndex != -1) {
                listState.animateScrollToItem(currentIndex)
                itemFocusRequester.requestFocus()
            }
        }
    }

    AnimatedVisibility(
        visible = show,
        enter = expandHorizontally(),
        exit = shrinkHorizontally()
    ) {
        Surface(
            modifier = modifier,
            colors = SurfaceDefaults.colors(
                containerColor = Color.Black.copy(alpha = 0.5f)
            )
        ) {
            Box(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column {
                    Text(
                        modifier = Modifier
                            .padding(start = 24.dp, top = 20.dp, bottom = 8.dp),
                        text = panelState.headerText,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge
                    )
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 60.dp)
                    ) {
                        items(
                            items = panelState.items,
                            key = { it.cid }
                        ) { item ->
                            val isSelected = item.cid == currentCid

                            MenuListItem(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .then(
                                        if (isSelected) {
                                            Modifier.focusRequester(itemFocusRequester)
                                        } else {
                                            Modifier
                                        }
                                    ),
                                text = item.title,
                                selected = isSelected,
                                textAlign = TextAlign.Start
                            ) {
                                if (!isSelected) {
                                    onPlayNewVideo(
                                        VideoListItem(
                                            aid = item.aid,
                                            cid = item.cid,
                                            title = item.title
                                        )
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
@Composable
fun MenuListItem(
    modifier: Modifier = Modifier,
    text: String,
    selected: Boolean,
    textAlign: TextAlign = TextAlign.Center,
    onFocus: () -> Unit = {},
    onClick: () -> Unit
) {
    DenseListItem(
        modifier = modifier
            .onFocusChanged { if (it.hasFocus) onFocus() },
        selected = selected,
        onClick = onClick,
        headlineContent = {
            Text(
                text = text,
                textAlign = textAlign
            )
        }
    )
}
