package dev.aaa1115910.bv.screen.user

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.activities.video.VideoInfoActivity
import dev.aaa1115910.bv.component.TvLazyVerticalGrid
import dev.aaa1115910.bv.component.ifElse
import dev.aaa1115910.bv.component.videocard.CardCover
import dev.aaa1115910.bv.component.videocard.SmallVideoCard
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.repository.JumpModeRepository
import dev.aaa1115910.bv.repository.JumpModeSource
import dev.aaa1115910.bv.repository.toJumpModeItems
import dev.aaa1115910.bv.ui.effect.UiEffect
import dev.aaa1115910.bv.util.requestFocus
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.viewmodel.user.ToViewViewModel
import dev.aaa1115910.bv.viewmodel.user.UpFavoriteGroup
import dev.aaa1115910.bv.viewmodel.user.UpInfoViewModel
import dev.aaa1115910.bv.viewmodel.user.UpSeasonSeriesGroup
import dev.aaa1115910.bv.viewmodel.user.UpSpaceTab
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import org.koin.androidx.compose.koinViewModel

@Composable
fun UpSpaceScreen(
    modifier: Modifier = Modifier,
    upInfoViewModel: UpInfoViewModel = koinViewModel(),
    toViewViewModel: ToViewViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val jumpModeRepository = remember { BVApp.koinApplication.koin.get<JumpModeRepository>() }
    val profileFocusRequester = remember { FocusRequester() }
    var initialFocusRequested by remember { mutableStateOf(false) }

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
                is UiEffect.ShowToast -> event.message.toast(context)
            }
        }
    }

    LaunchedEffect(upInfoViewModel.profileLoaded) {
        if (!initialFocusRequested && upInfoViewModel.profileLoaded) {
            initialFocusRequested = true
            profileFocusRequester.requestFocus(scope)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        UpProfileHeader(
            modifier = Modifier.focusRequester(profileFocusRequester),
            upInfoViewModel = upInfoViewModel
        )
        UpTabs(
            tabs = upInfoViewModel.visibleTabs,
            selectedTab = upInfoViewModel.selectedTab,
            onSelect = upInfoViewModel::selectTab
        )
        when (upInfoViewModel.selectedTab) {
            UpSpaceTab.Videos -> UpVideosGrid(
                upInfoViewModel = upInfoViewModel,
                toViewViewModel = toViewViewModel,
                jumpModeRepository = jumpModeRepository
            )

            UpSpaceTab.SeasonsSeries -> UpSeasonSeriesContent(
                groups = upInfoViewModel.seasonsSeries,
                loading = upInfoViewModel.seasonsSeriesLoading && !upInfoViewModel.seasonsSeriesLoaded,
                onVideoClicked = { video ->
                    jumpModeRepository.setPendingQueue(
                        source = JumpModeSource.Personal,
                        selectedAid = video.avid,
                        items = upInfoViewModel.seasonsSeries.flatMap { it.videos }.toJumpModeItems()
                    )
                    VideoInfoActivity.actionStart(
                        context = context,
                        aid = video.avid,
                        proxyArea = ProxyArea.checkProxyArea(video.title)
                    )
                },
                onAddWatchLater = toViewViewModel::addToView
            )

            UpSpaceTab.Favorites -> UpFavoritesContent(
                groups = upInfoViewModel.favorites,
                loading = upInfoViewModel.favoritesLoading && !upInfoViewModel.favoritesLoaded,
                onVideoClicked = { video ->
                    jumpModeRepository.setPendingQueue(
                        source = JumpModeSource.Personal,
                        selectedAid = video.avid,
                        items = upInfoViewModel.favorites.flatMap { it.videos }.toJumpModeItems()
                    )
                    VideoInfoActivity.actionStart(
                        context = context,
                        aid = video.avid,
                        proxyArea = ProxyArea.checkProxyArea(video.title)
                    )
                },
                onAddWatchLater = toViewViewModel::addToView
            )
        }
    }
}

@Composable
private fun UpVideosGrid(
    upInfoViewModel: UpInfoViewModel,
    toViewViewModel: ToViewViewModel,
    jumpModeRepository: JumpModeRepository
) {
    val context = LocalContext.current
    val gridState = rememberLazyGridState()
    val firstVideoFocusRequester = remember { FocusRequester() }

    LaunchedEffect(gridState, upInfoViewModel.spaceVideos.size) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .distinctUntilChanged()
            .filter { index ->
                index != null &&
                    upInfoViewModel.spaceVideos.isNotEmpty() &&
                    index >= upInfoViewModel.spaceVideos.size - 8 &&
                    !upInfoViewModel.videosLoading &&
                    !upInfoViewModel.noMore
            }
            .collect {
                upInfoViewModel.update()
            }
    }

    TvLazyVerticalGrid(
        modifier = Modifier.fillMaxSize(),
        columns = GridCells.Fixed(4),
        state = gridState,
        contentPadding = PaddingValues(bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        if (upInfoViewModel.spaceVideos.isNotEmpty()) {
            itemsIndexed(
                items = upInfoViewModel.spaceVideos,
                key = { _, video -> video.avid }
            ) { index, video ->
                SmallVideoCard(
                    modifier = Modifier.ifElse(index == 0, Modifier.focusRequester(firstVideoFocusRequester)),
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
                    onAddWatchLater = { toViewViewModel.addToView(video.avid) },
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
        } else {
            item {
                EmptyTip(
                    text = if (upInfoViewModel.videosLoaded && !upInfoViewModel.videosLoading) {
                        "空空如也"
                    } else {
                        "加载中…"
                    }
                )
            }
        }
    }
}

@Composable
private fun UpProfileHeader(
    modifier: Modifier = Modifier,
    upInfoViewModel: UpInfoViewModel
) {
    var showDescriptionDialog by remember { mutableStateOf(false) }
    val description = upInfoViewModel.upSign.ifBlank { "这个 UP 主还没有填写个人简介" }

    Surface(
        onClick = { showDescriptionDialog = true },
        modifier = modifier.fillMaxWidth(),
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.medium),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.02f),
            contentColor = Color.White,
            focusedContainerColor = Color.White.copy(alpha = 0.08f),
            focusedContentColor = Color.White
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, Color.White.copy(alpha = 0.85f)),
                shape = MaterialTheme.shapes.medium
            )
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
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
                                .size(72.dp)
                                .clip(CircleShape),
                            model = upInfoViewModel.upFace,
                            contentDescription = null,
                            contentScale = ContentScale.Crop
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = upInfoViewModel.upName,
                            fontSize = 26.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
            Text(
                text = description,
                color = Color.White.copy(alpha = if (upInfoViewModel.upSign.isNotBlank()) 0.78f else 0.42f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontSize = 13.sp
            )
        }
    }

    UpDescriptionDialog(
        show = showDescriptionDialog,
        description = description,
        onDismiss = { showDescriptionDialog = false }
    )
}

@Composable
private fun UpTabs(
    tabs: List<UpSpaceTab>,
    selectedTab: UpSpaceTab,
    onSelect: (UpSpaceTab) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)

    if (tabs.isEmpty()) return

    TabRow(
        modifier = Modifier.focusRestorer(focusRequester),
        selectedTabIndex = selectedIndex,
        separator = { Spacer(modifier = Modifier.width(16.dp)) }
    ) {
        tabs.forEachIndexed { index, tab ->
            Tab(
                modifier = Modifier.ifElse(index == 0, Modifier.focusRequester(focusRequester)),
                selected = tab == selectedTab,
                onFocus = { onSelect(tab) },
                onClick = { onSelect(tab) }
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    text = tab.displayName,
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
private fun UpSeasonSeriesContent(
    groups: List<UpSeasonSeriesGroup>,
    loading: Boolean,
    onVideoClicked: (VideoCardData) -> Unit,
    onAddWatchLater: (Long) -> Unit
) {
    if (groups.isEmpty()) {
        EmptyTip(text = if (loading) "加载中…" else "空空如也")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(bottom = 48.dp)
    ) {
        itemsIndexed(groups, key = { _, group -> "${group.type}-${group.id}" }) { _, group ->
            UpVideoGroupRow(
                title = "${group.type.displayName} · ${group.title}",
                subtitle = if (group.total > 0) "${group.total} 个视频" else null,
                cover = group.cover,
                videos = group.videos,
                onVideoClicked = onVideoClicked,
                onAddWatchLater = onAddWatchLater
            )
        }
    }
}

@Composable
private fun UpFavoritesContent(
    groups: List<UpFavoriteGroup>,
    loading: Boolean,
    onVideoClicked: (VideoCardData) -> Unit,
    onAddWatchLater: (Long) -> Unit
) {
    if (groups.isEmpty()) {
        EmptyTip(text = if (loading) "加载中…" else "空空如也")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(bottom = 48.dp)
    ) {
        itemsIndexed(groups, key = { _, group -> group.id }) { _, group ->
            UpVideoGroupRow(
                title = group.title,
                subtitle = if (group.total > 0) "${group.total} 个视频" else null,
                cover = group.cover.orEmpty(),
                videos = group.videos,
                onVideoClicked = onVideoClicked,
                onAddWatchLater = onAddWatchLater
            )
        }
    }
}

@Composable
private fun UpVideoGroupRow(
    title: String,
    subtitle: String?,
    cover: String,
    videos: List<VideoCardData>,
    onVideoClicked: (VideoCardData) -> Unit,
    onAddWatchLater: (Long) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    var showAllVideos by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 4.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                fontSize = 21.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            subtitle?.let {
                Text(
                    text = it,
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 13.sp
                )
            }
        }
        LazyRow(
            modifier = Modifier.focusRestorer(focusRequester),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(end = 48.dp)
        ) {
            item {
                UpCollectionCoverCard(
                    modifier = Modifier.focusRequester(focusRequester),
                    title = title,
                    cover = cover,
                    countText = subtitle,
                    onClick = { showAllVideos = true }
                )
            }
            itemsIndexed(videos, key = { _, video -> video.avid }) { _, video ->
                SmallVideoCard(
                    modifier = Modifier.width(210.dp),
                    data = video,
                    onClick = { onVideoClicked(video) },
                    onAddWatchLater = { onAddWatchLater(video.avid) },
                    onGoToDetailPage = { onVideoClicked(video) }
                )
            }
        }
    }

    UpGroupVideosDialog(
        show = showAllVideos,
        title = title,
        videos = videos,
        onDismiss = { showAllVideos = false },
        onVideoClicked = {
            showAllVideos = false
            onVideoClicked(it)
        },
        onAddWatchLater = onAddWatchLater
    )
}

@Composable
private fun UpCollectionCoverCard(
    modifier: Modifier = Modifier,
    title: String,
    cover: String,
    countText: String?,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.width(210.dp),
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.large),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.03f),
            contentColor = Color.White,
            focusedContainerColor = Color.White.copy(alpha = 0.1f),
            focusedContentColor = Color.White
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(3.dp, MaterialTheme.colorScheme.border),
                shape = MaterialTheme.shapes.large
            )
        )
    ) {
        Column {
            CardCover(
                modifier = Modifier.aspectRatio(1.6f),
                cover = cover,
                play = "",
                danmaku = "",
                time = countText.orEmpty()
            )
            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "全部",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.65f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun UpGroupVideosDialog(
    show: Boolean,
    title: String,
    videos: List<VideoCardData>,
    onDismiss: () -> Unit,
    onVideoClicked: (VideoCardData) -> Unit,
    onAddWatchLater: (Long) -> Unit
) {
    if (!show) return

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            val gridState = rememberLazyGridState()
            TvLazyVerticalGrid(
                modifier = Modifier.height(420.dp),
                columns = GridCells.Fixed(3),
                state = gridState,
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                if (videos.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptyTip()
                    }
                } else {
                    itemsIndexed(videos, key = { _, video -> video.avid }) { _, video ->
                        SmallVideoCard(
                            data = video,
                            onClick = { onVideoClicked(video) },
                            onAddWatchLater = { onAddWatchLater(video.avid) },
                            onGoToDetailPage = { onVideoClicked(video) }
                        )
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun UpDescriptionDialog(
    show: Boolean,
    description: String,
    onDismiss: () -> Unit
) {
    if (!show) return

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "个人简介",
                color = Color.White
            )
        },
        text = {
            LazyColumn {
                item {
                    Text(text = description)
                }
            }
        },
        confirmButton = {}
    )
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
