package dev.aaa1115910.bv.screen.user

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.focusable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Done
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.component.SelectableItemPopupWidthFraction
import dev.aaa1115910.bv.activities.video.VideoInfoActivity
import dev.aaa1115910.bv.component.TvLazyVerticalGrid
import dev.aaa1115910.bv.component.videocard.CardCover
import dev.aaa1115910.bv.component.videocard.SmallVideoCard
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.repository.JumpModeRepository
import dev.aaa1115910.bv.repository.JumpModeSource
import dev.aaa1115910.bv.repository.toJumpModeItems
import dev.aaa1115910.bv.ui.effect.UiEffect
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.requestFocus
import dev.aaa1115910.bv.util.touchClick
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.viewmodel.user.ToViewViewModel
import dev.aaa1115910.bv.viewmodel.user.UpFavoriteGroup
import dev.aaa1115910.bv.viewmodel.user.UpInfoViewModel
import dev.aaa1115910.bv.viewmodel.user.UpSeasonSeriesGroup
import dev.aaa1115910.bv.viewmodel.user.UpSpaceTab
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
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
    val tabsFocusRequester = remember { FocusRequester() }
    var initialFocusRequested by remember { mutableStateOf(false) }
    var tabFocusRequestId by remember { mutableIntStateOf(0) }
    val onTabSelected: (UpSpaceTab) -> Unit = { tab ->
        if (tab != upInfoViewModel.selectedTab) tabFocusRequestId++
        upInfoViewModel.selectTab(tab)
    }

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

    LaunchedEffect(
        upInfoViewModel.profileLoaded,
        upInfoViewModel.selectedTab
    ) {
        if (
            !initialFocusRequested &&
            upInfoViewModel.profileLoaded &&
            upInfoViewModel.selectedTab == UpSpaceTab.Videos
        ) {
            initialFocusRequested = true
            profileFocusRequester.requestFocus(scope)
        }
    }

    when (upInfoViewModel.selectedTab) {
        UpSpaceTab.Videos -> UpVideosGrid(
            modifier = modifier,
            upInfoViewModel = upInfoViewModel,
            toViewViewModel = toViewViewModel,
            jumpModeRepository = jumpModeRepository,
            profileFocusRequester = profileFocusRequester,
            tabsFocusRequester = tabsFocusRequester,
            tabFocusRequestId = tabFocusRequestId,
            onTabSelected = onTabSelected
        )

        UpSpaceTab.SeasonsSeries -> UpSeasonSeriesContent(
            modifier = modifier,
            upInfoViewModel = upInfoViewModel,
            groups = upInfoViewModel.seasonsSeries,
            loading = upInfoViewModel.seasonsSeriesLoading && !upInfoViewModel.seasonsSeriesLoaded,
            profileFocusRequester = profileFocusRequester,
            tabsFocusRequester = tabsFocusRequester,
            tabFocusRequestId = tabFocusRequestId,
            onTabSelected = onTabSelected,
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
            modifier = modifier,
            upInfoViewModel = upInfoViewModel,
            groups = upInfoViewModel.favorites,
            loading = upInfoViewModel.favoritesLoading && !upInfoViewModel.favoritesLoaded,
            profileFocusRequester = profileFocusRequester,
            tabsFocusRequester = tabsFocusRequester,
            tabFocusRequestId = tabFocusRequestId,
            onTabSelected = onTabSelected,
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

@Composable
private fun UpVideosGrid(
    modifier: Modifier,
    upInfoViewModel: UpInfoViewModel,
    toViewViewModel: ToViewViewModel,
    jumpModeRepository: JumpModeRepository,
    profileFocusRequester: FocusRequester,
    tabsFocusRequester: FocusRequester,
    tabFocusRequestId: Int,
    onTabSelected: (UpSpaceTab) -> Unit
) {
    val context = LocalContext.current
    val gridState = rememberLazyGridState()

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
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 24.dp),
        columns = GridCells.Fixed(4),
        state = gridState,
        contentPadding = PaddingValues(bottom = 72.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            UpProfileHeader(
                modifier = Modifier.focusRequester(profileFocusRequester),
                upInfoViewModel = upInfoViewModel
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            UpTabs(
                modifier = Modifier.focusRequester(tabsFocusRequester),
                tabs = upInfoViewModel.visibleTabs,
                selectedTab = upInfoViewModel.selectedTab,
                focusRequestId = tabFocusRequestId,
                onSelect = onTabSelected
            )
        }
        if (upInfoViewModel.spaceVideos.isNotEmpty()) {
            itemsIndexed(
                items = upInfoViewModel.spaceVideos,
                key = { _, video -> video.avid }
            ) { _, video ->
                SmallVideoCard(
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
            if (upInfoViewModel.noMore) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        text = stringResource(R.string.load_data_no_more),
                        color = Color.White.copy(alpha = 0.42f)
                    )
                }
            }
        } else {
            item(span = { GridItemSpan(maxLineSpan) }) {
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
    val descriptionFocusRequester = remember { FocusRequester() }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .background(
                    color = Color.White.copy(alpha = 0.02f),
                    shape = MaterialTheme.shapes.medium
                )
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
                    AnimatedVisibility(
                        visible = shouldShowUpFollowButton(Prefs.isLogin, upInfoViewModel.upMid)
                    ) {
                        UpFollowButton(
                            modifier = Modifier
                                .align(Alignment.Top)
                                .focusProperties { down = descriptionFocusRequester },
                            followed = upInfoViewModel.isFollowing,
                            onToggleFollow = { upInfoViewModel.setFollow(!upInfoViewModel.isFollowing) }
                        )
                    }
                }
                UpProfileDescriptionCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 2.dp)
                        .focusRequester(descriptionFocusRequester),
                    description = description,
                    dimmed = upInfoViewModel.upSign.isBlank(),
                    onClick = { showDescriptionDialog = true }
                )
            }
        }
    }

    UpDescriptionDialog(
        show = showDescriptionDialog,
        description = description,
        onDismiss = { showDescriptionDialog = false }
    )
}

internal fun shouldShowUpFollowButton(isLogin: Boolean, upMid: Long): Boolean =
    isLogin && upMid > 0L

@Composable
private fun UpFollowButton(
    modifier: Modifier = Modifier,
    followed: Boolean,
    onToggleFollow: () -> Unit
) {
    val shape = RoundedCornerShape(50)

    Surface(
        modifier = modifier
            .padding(2.dp)
            .height(40.dp)
            .width(88.dp)
            .touchClick(onToggleFollow),
        onClick = onToggleFollow,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.16f),
            focusedContainerColor = Color.White.copy(alpha = 0.28f),
            pressedContainerColor = Color.White.copy(alpha = 0.28f)
        ),
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(width = 2.dp, color = Color.White),
                shape = shape
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (followed) Icons.Rounded.Done else Icons.Rounded.Add,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = stringResource(
                    if (followed) R.string.video_info_followed else R.string.video_info_follow
                ),
                color = Color.White,
                maxLines = 1,
                softWrap = false,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun UpProfileDescriptionCard(
    modifier: Modifier = Modifier,
    description: String,
    dimmed: Boolean,
    onClick: () -> Unit
) {
    val shape = MaterialTheme.shapes.medium

    Surface(
        modifier = modifier
            .height(64.dp)
            .touchClick(onClick),
        onClick = onClick,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.04f),
            focusedContainerColor = Color.White.copy(alpha = 0.08f),
            pressedContainerColor = Color.White.copy(alpha = 0.08f)
        ),
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(width = 2.dp, color = Color.White),
                shape = shape
            )
        )
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp),
            text = description,
            color = Color.White.copy(alpha = if (dimmed) 0.42f else 0.78f),
            fontSize = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun UpTabs(
    modifier: Modifier = Modifier,
    tabs: List<UpSpaceTab>,
    selectedTab: UpSpaceTab,
    focusRequestId: Int = 0,
    onSelect: (UpSpaceTab) -> Unit
) {
    val selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)
    val tabFocusRequesters = remember(tabs) {
        tabs.map { FocusRequester() }
    }

    if (tabs.isEmpty()) return

    LaunchedEffect(focusRequestId) {
        if (focusRequestId > 0) {
            runCatching { tabFocusRequesters[selectedIndex].requestFocus() }
        }
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        TabRow(
            modifier = modifier.focusRestorer(tabFocusRequesters[selectedIndex]),
            selectedTabIndex = selectedIndex,
            separator = { Spacer(modifier = Modifier.width(16.dp)) }
        ) {
            tabs.forEachIndexed { index, tab ->
                val selectTab = { onSelect(tab) }
                Tab(
                    modifier = Modifier
                        .focusRequester(tabFocusRequesters[index])
                        .touchClick(selectTab),
                    selected = tab == selectedTab,
                    onFocus = selectTab,
                    onClick = selectTab
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
}

@Composable
private fun UpSeasonSeriesContent(
    modifier: Modifier = Modifier,
    upInfoViewModel: UpInfoViewModel,
    groups: List<UpSeasonSeriesGroup>,
    loading: Boolean,
    profileFocusRequester: FocusRequester,
    tabsFocusRequester: FocusRequester,
    tabFocusRequestId: Int,
    onTabSelected: (UpSpaceTab) -> Unit,
    onVideoClicked: (VideoCardData) -> Unit,
    onAddWatchLater: (Long) -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(bottom = 72.dp)
    ) {
        item {
            UpProfileHeader(
                modifier = Modifier.focusRequester(profileFocusRequester),
                upInfoViewModel = upInfoViewModel
            )
        }
        item {
            UpTabs(
                modifier = Modifier.focusRequester(tabsFocusRequester),
                tabs = upInfoViewModel.visibleTabs,
                selectedTab = upInfoViewModel.selectedTab,
                focusRequestId = tabFocusRequestId,
                onSelect = onTabSelected
            )
        }
        if (groups.isEmpty()) {
            item {
                EmptyTip(text = if (loading) "加载中…" else "空空如也")
            }
        } else {
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
}

@Composable
private fun UpFavoritesContent(
    modifier: Modifier = Modifier,
    upInfoViewModel: UpInfoViewModel,
    groups: List<UpFavoriteGroup>,
    loading: Boolean,
    profileFocusRequester: FocusRequester,
    tabsFocusRequester: FocusRequester,
    tabFocusRequestId: Int,
    onTabSelected: (UpSpaceTab) -> Unit,
    onVideoClicked: (VideoCardData) -> Unit,
    onAddWatchLater: (Long) -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(bottom = 72.dp)
    ) {
        item {
            UpProfileHeader(
                modifier = Modifier.focusRequester(profileFocusRequester),
                upInfoViewModel = upInfoViewModel
            )
        }
        item {
            UpTabs(
                modifier = Modifier.focusRequester(tabsFocusRequester),
                tabs = upInfoViewModel.visibleTabs,
                selectedTab = upInfoViewModel.selectedTab,
                focusRequestId = tabFocusRequestId,
                onSelect = onTabSelected
            )
        }
        if (groups.isEmpty()) {
            item {
                EmptyTip(text = if (loading) "加载中…" else "空空如也")
            }
        } else {
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
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(
                start = 18.dp,
                top = 18.dp,
                end = 48.dp,
                bottom = 12.dp
            )
        ) {
            item {
                UpCollectionCoverCard(
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
    Column(
        modifier = Modifier.width(210.dp)
    ) {
        Card(
            onClick = onClick,
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(1.6f)
                .touchClick(onClick),
            shape = CardDefaults.shape(MaterialTheme.shapes.large),
            border = CardDefaults.border(
                focusedBorder = Border(
                    border = BorderStroke(3.dp, MaterialTheme.colorScheme.border),
                    shape = MaterialTheme.shapes.large
                )
            )
        ) {
            CardCover(
                cover = cover,
                play = "",
                danmaku = "",
                time = countText.orEmpty()
            )
        }
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
        modifier = Modifier.fillMaxWidth(SelectableItemPopupWidthFraction),
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
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
                contentPadding = PaddingValues(top = 12.dp, bottom = 8.dp),
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

    val scrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val scrollStepPx = remember(density) { with(density) { 96.dp.roundToPx() } }
    var focused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus(scope)
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "个人简介",
                color = Color.White
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .clip(MaterialTheme.shapes.small)
                    .border(
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (focused) {
                                MaterialTheme.colorScheme.border
                            } else {
                                Color.White.copy(alpha = 0.18f)
                            }
                        ),
                        shape = MaterialTheme.shapes.small
                    )
                    .focusRequester(focusRequester)
                    .onFocusChanged { focused = it.isFocused }
                    .onPreviewKeyEvent {
                        if (it.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when (it.key) {
                            Key.DirectionDown -> {
                                if (scrollState.value >= scrollState.maxValue) {
                                    false
                                } else {
                                    scope.launch {
                                        scrollState.animateScrollTo(
                                            (scrollState.value + scrollStepPx)
                                                .coerceAtMost(scrollState.maxValue)
                                        )
                                    }
                                    true
                                }
                            }

                            Key.DirectionUp -> {
                                if (scrollState.value <= 0) {
                                    false
                                } else {
                                    scope.launch {
                                        scrollState.animateScrollTo(
                                            (scrollState.value - scrollStepPx)
                                                .coerceAtLeast(0)
                                        )
                                    }
                                    true
                                }
                            }

                            else -> false
                        }
                    }
                    .focusable()
                    .verticalScroll(scrollState)
                    .padding(12.dp)
            ) {
                Text(
                    text = description,
                    color = Color.White.copy(alpha = if (focused) 0.92f else 0.82f)
                )
            }
        },
        confirmButton = {
            androidx.tv.material3.OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.touchClick(onDismiss)
            ) {
                Text(text = "关闭")
            }
        }
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
