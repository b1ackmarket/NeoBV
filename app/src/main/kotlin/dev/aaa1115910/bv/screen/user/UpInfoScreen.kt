package dev.aaa1115910.bv.screen.user

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.activities.video.VideoInfoActivity
import dev.aaa1115910.bv.component.TvLazyVerticalGrid
import dev.aaa1115910.bv.component.videocard.SmallVideoCard
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.repository.JumpModeRepository
import dev.aaa1115910.bv.repository.JumpModeSource
import dev.aaa1115910.bv.repository.toJumpModeItems
import dev.aaa1115910.bv.ui.effect.UiEffect
import dev.aaa1115910.bv.util.requestFocus
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.viewmodel.user.ToViewViewModel
import dev.aaa1115910.bv.viewmodel.user.UpInfoViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

@Composable
fun UpSpaceScreen(
    modifier: Modifier = Modifier,
    upInfoViewModel: UpInfoViewModel = koinViewModel(),
    toViewViewModel: ToViewViewModel = koinViewModel()
) {
    val gridState = rememberLazyGridState()
    val context = LocalContext.current
    val jumpModeRepository = remember { BVApp.koinApplication.koin.get<JumpModeRepository>() }
    val firstVideoFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        val intent = (context as Activity).intent
        if (intent.hasExtra("mid")) {
            val mid = intent.getLongExtra("mid", 0)
            val name = intent.getStringExtra("name") ?: ""
            upInfoViewModel.upMid = mid
            upInfoViewModel.upName = name
            upInfoViewModel.update()
        } else {
            context.finish()
        }
    }

    LaunchedEffect(Unit) {
        toViewViewModel.uiEvent.collect { event ->
            when (event) {
                is UiEffect.ShowToast -> {
                    event.message.toast(context)
                }
            }
        }
    }

    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .distinctUntilChanged()
            .filter { index ->
                index != null && index >= upInfoViewModel.spaceVideos.size - 16
            }
            .collect {
                upInfoViewModel.update()
            }
    }

    LaunchedEffect(upInfoViewModel.spaceVideos.size) {
        if (upInfoViewModel.spaceVideos.isNotEmpty()) {
            delay(50)
            runCatching { gridState.scrollToItem(0) }
            firstVideoFocusRequester.requestFocus(this)
        }
    }

    Scaffold(modifier = modifier) { innerPadding ->
        TvLazyVerticalGrid(
            modifier = Modifier.padding(innerPadding),
            columns = GridCells.Fixed(4),
            state = gridState,
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                UpProfileHeader(upInfoViewModel = upInfoViewModel)
            }
            if (upInfoViewModel.spaceVideos.isNotEmpty()) {
                itemsIndexed(
                    items = upInfoViewModel.spaceVideos,
                    key = { _, video -> video.avid }
                ) { index, video ->
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        SmallVideoCard(
                            modifier = if (index == 0) {
                                Modifier.focusRequester(firstVideoFocusRequester)
                            } else {
                                Modifier
                            },
                            data = video,
                            onClick = {
                                jumpModeRepository.setPendingQueue(
                                    source = JumpModeSource.Personal,
                                    selectedAid = video.avid,
                                    items = upInfoViewModel.spaceVideos.toJumpModeItems()
                                )
                                VideoInfoActivity.actionStart(
                                    context = context,
                                    aid = video.avid,
                                    proxyArea = ProxyArea.checkProxyArea(video.title)
                                )
                            },
                            onAddWatchLater = {
                                toViewViewModel.addToView(video.avid)
                            },
                            onGoToDetailPage = {
                                jumpModeRepository.setPendingQueue(
                                    source = JumpModeSource.Personal,
                                    selectedAid = video.avid,
                                    items = upInfoViewModel.spaceVideos.toJumpModeItems()
                                )
                                VideoInfoActivity.actionStart(
                                    context = context,
                                    fromController = true,
                                    aid = video.avid
                                )
                            },
                        )
                    }
                }
            } else if (!upInfoViewModel.videosLoading) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyTip()
                }
            } else {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyTip(text = "加载中…")
                }
            }
        }
    }
}

@Composable
private fun UpProfileHeader(upInfoViewModel: UpInfoViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (upInfoViewModel.upFace.isNotBlank()) {
                    AsyncImage(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape),
                        model = upInfoViewModel.upFace,
                        contentDescription = null,
                        contentScale = ContentScale.Crop
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = upInfoViewModel.upName,
                        fontSize = 24.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        UpProfileStat("粉丝", upInfoViewModel.followerText)
                        UpProfileStat("获赞", upInfoViewModel.likeText)
                        UpProfileStat("投稿", upInfoViewModel.archiveText)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(
                        R.string.load_data_count,
                        upInfoViewModel.spaceVideos.size
                    ),
                    color = Color.White.copy(alpha = 0.6f)
                )
                AnimatedVisibility(visible = upInfoViewModel.noMore) {
                    Text(
                        text = stringResource(R.string.load_data_no_more),
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
        if (upInfoViewModel.upSign.isNotBlank()) {
            Text(
                text = upInfoViewModel.upSign,
                color = Color.White.copy(alpha = 0.62f),
                maxLines = 3,
                fontSize = 13.sp
            )
        }
        if (upInfoViewModel.seriesSummaryText.isNotBlank()) {
            Text(
                text = upInfoViewModel.seriesSummaryText,
                color = Color.White.copy(alpha = 0.72f),
                maxLines = 2,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun UpProfileStat(label: String, value: String) {
    if (value.isBlank()) return
    Text(
        text = "$label $value",
        color = Color.White.copy(alpha = 0.72f),
        fontSize = 13.sp
    )
}
