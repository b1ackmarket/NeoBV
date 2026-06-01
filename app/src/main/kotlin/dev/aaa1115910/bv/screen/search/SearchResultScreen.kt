package dev.aaa1115910.bv.screen.search

import android.app.Activity
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.repositories.SearchFilterDuration
import dev.aaa1115910.biliapi.repositories.SearchFilterOrderType
import dev.aaa1115910.biliapi.repositories.SearchType
import dev.aaa1115910.biliapi.repositories.SearchTypeResult
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.activities.live.LivePlayerActivity
import dev.aaa1115910.bv.activities.video.SeasonInfoActivity
import dev.aaa1115910.bv.activities.video.UpInfoActivity
import dev.aaa1115910.bv.activities.video.VideoInfoActivity
import dev.aaa1115910.bv.component.SearchTypeTopNavItem
import dev.aaa1115910.bv.component.TopNav
import dev.aaa1115910.bv.component.TvLazyVerticalGrid
import dev.aaa1115910.bv.component.videocard.SeasonCard
import dev.aaa1115910.bv.component.videocard.SmallVideoCard
import dev.aaa1115910.bv.entity.carddata.SeasonCardData
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.repository.JumpModeQueueItem
import dev.aaa1115910.bv.repository.JumpModeRepository
import dev.aaa1115910.bv.repository.JumpModeSource
import dev.aaa1115910.bv.screen.user.UpCard
import dev.aaa1115910.bv.ui.effect.UiEffect
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.focusedScale
import dev.aaa1115910.bv.util.formatHourMinSec
import dev.aaa1115910.bv.util.removeHtmlTags
import dev.aaa1115910.bv.util.rememberAdaptiveGridCells
import dev.aaa1115910.bv.util.requestFocus
import dev.aaa1115910.bv.util.toWanString
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.viewmodel.search.SearchResultViewModel
import dev.aaa1115910.bv.viewmodel.user.ToViewViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel

internal data class SearchResultUpdateTrigger(
    val keyword: String,
    val type: SearchType,
    val order: SearchFilterOrderType,
    val duration: SearchFilterDuration,
    val partitionTid: Int?,
    val childPartitionTid: Int?
) {
    val isReady: Boolean get() = keyword.isNotBlank()
}

private fun List<SearchTypeResult.Video>.toJumpModeItems(): List<JumpModeQueueItem> {
    return filter { it.aid > 0 }
        .map { item ->
            JumpModeQueueItem(
                aid = item.aid,
                title = item.title.removeHtmlTags()
            )
        }
}

internal fun shouldRequestSearchResult(
    requestedTriggers: Map<SearchType, SearchResultRequestState>,
    nextTrigger: SearchResultUpdateTrigger
): Boolean {
    if (!nextTrigger.isReady) return false
    return when (val state = requestedTriggers[nextTrigger.type]) {
        is SearchResultRequestState.Loading,
        is SearchResultRequestState.Loaded -> state.trigger != nextTrigger

        is SearchResultRequestState.Failed,
        null -> true
    }
}

internal fun shouldLoadMoreSearchResults(
    lastVisibleIndex: Int?,
    resultCount: Int,
    preloadThreshold: Int = 20
): Boolean {
    return resultCount > 0 &&
        lastVisibleIndex != null &&
        lastVisibleIndex >= resultCount - preloadThreshold
}

internal sealed interface SearchResultRequestState {
    val trigger: SearchResultUpdateTrigger

    data class Loading(override val trigger: SearchResultUpdateTrigger) : SearchResultRequestState
    data class Loaded(override val trigger: SearchResultUpdateTrigger) : SearchResultRequestState
    data class Failed(override val trigger: SearchResultUpdateTrigger) : SearchResultRequestState
}

@Composable
fun SearchResultScreen(
    modifier: Modifier = Modifier,
    searchResultViewModel: SearchResultViewModel = koinViewModel(),
    toViewViewModel: ToViewViewModel = koinViewModel()
) {
    val gridState = rememberLazyGridState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val logger = KotlinLogging.logger { }
    val tabRowFocusRequester = remember { FocusRequester() }
    val jumpModeRepository = remember { BVApp.koinApplication.koin.get<JumpModeRepository>() }

    var rowSize by remember { mutableIntStateOf(4) }
    val gridCells = rememberAdaptiveGridCells(defaultColumns = rowSize)

    var searchKeyword by remember { mutableStateOf("") }
    val requestedSearchTriggers = remember { mutableStateMapOf<SearchType, SearchResultRequestState>() }

    val searchResult = when (searchResultViewModel.searchType) {
        SearchType.Video -> searchResultViewModel.videoSearchResult
        SearchType.MediaBangumi -> searchResultViewModel.mediaBangumiSearchResult
        SearchType.MediaFt -> searchResultViewModel.mediaFtSearchResult
        SearchType.Live -> searchResultViewModel.liveSearchResult
        SearchType.BiliUser -> searchResultViewModel.biliUserSearchResult
    }
    val searchResultItems = when (searchResult.type) {
        SearchType.Video -> searchResult.videos
        SearchType.MediaBangumi -> searchResult.mediaBangumis
        SearchType.MediaFt -> searchResult.mediaFts
        SearchType.Live -> searchResult.lives
        SearchType.BiliUser -> searchResult.biliUsers
    }
    var showFilter by remember { mutableStateOf(false) }
    var focusOnContent by remember { mutableStateOf(false) }

    val isVideoSearchViaWebApi = searchResultViewModel.searchType == SearchType.Video

    val selectedOrder = searchResultViewModel.selectedOrder
    val selectedDuration = searchResultViewModel.selectedDuration
    val selectedPartition = searchResultViewModel.selectedPartition
    val selectedChildPartition = searchResultViewModel.selectedChildPartition

    val onClickResult: (SearchTypeResult.SearchTypeResultItem) -> Unit = { resultItem ->
        when (resultItem) {
            is SearchTypeResult.Video -> {
                jumpModeRepository.setPendingQueue(
                    source = JumpModeSource.Search,
                    selectedAid = resultItem.aid,
                    items = searchResult.videos.toJumpModeItems()
                )
                VideoInfoActivity.actionStart(
                    context = context,
                    aid = resultItem.aid,
                    fromSeason = false
                )
            }

            is SearchTypeResult.Pgc -> {
                SeasonInfoActivity.actionStart(
                    context = context,
                    seasonId = resultItem.seasonId,
                    proxyArea = ProxyArea.checkProxyArea(resultItem.title)
                )
            }

            is SearchTypeResult.User -> {
                UpInfoActivity.actionStart(
                    context = context,
                    mid = resultItem.mid,
                    name = resultItem.name
                )
            }

            is SearchTypeResult.Live -> {
                LivePlayerActivity.actionStart(
                    context = context,
                    roomId = resultItem.roomId,
                    title = resultItem.title.removeHtmlTags(),
                    upName = resultItem.upName.removeHtmlTags(),
                    online = resultItem.online
                )
            }

            else -> {}
        }
    }

    val backToTabRow: () -> Unit = {
        tabRowFocusRequester.requestFocus(scope)
    }

    LaunchedEffect(Unit) {
        val intent = (context as Activity).intent
        if (intent.hasExtra("keyword")) {
            searchKeyword = intent.getStringExtra("keyword") ?: ""
            val enableProxy = intent.getBooleanExtra("enableProxy", false)
            if (searchKeyword == "") context.finish()
            searchResultViewModel.enableProxySearchResult = enableProxy
            searchResultViewModel.keyword = searchKeyword
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

    LaunchedEffect(searchResultViewModel.searchType) {
        rowSize = when (searchResultViewModel.searchType) {
            SearchType.Video -> 4
            SearchType.MediaBangumi, SearchType.MediaFt -> 6
            SearchType.Live -> 4
            SearchType.BiliUser -> 3
        }
    }

    val searchUpdateTrigger = SearchResultUpdateTrigger(
        keyword = searchResultViewModel.keyword,
        type = searchResultViewModel.searchType,
        order = selectedOrder,
        duration = selectedDuration,
        partitionTid = selectedPartition?.tid,
        childPartitionTid = selectedChildPartition?.tid
    )

    LaunchedEffect(searchUpdateTrigger) {
        if (!shouldRequestSearchResult(requestedSearchTriggers, searchUpdateTrigger)) {
            return@LaunchedEffect
        }
        requestedSearchTriggers[searchUpdateTrigger.type] = SearchResultRequestState.Loading(searchUpdateTrigger)
        logger.fInfo { "Start update search result because keyword, type or filter updated" }
        try {
            val loaded = searchResultViewModel.update(searchUpdateTrigger.type)
            requestedSearchTriggers[searchUpdateTrigger.type] = if (loaded) {
                SearchResultRequestState.Loaded(searchUpdateTrigger)
            } else {
                SearchResultRequestState.Failed(searchUpdateTrigger)
            }
        } catch (e: CancellationException) {
            withContext(NonCancellable) {
                if (requestedSearchTriggers[searchUpdateTrigger.type] == SearchResultRequestState.Loading(searchUpdateTrigger)) {
                    requestedSearchTriggers[searchUpdateTrigger.type] = SearchResultRequestState.Failed(searchUpdateTrigger)
                }
            }
            throw e
        }
    }


    LaunchedEffect(gridState, searchResult) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .distinctUntilChanged()
            .filter { index ->
                shouldLoadMoreSearchResults(index, searchResult.count)
            }
            .collect {
                searchResultViewModel.loadMore(searchResult.type)
            }
    }

    Scaffold(
        modifier = modifier.onKeyEvent {
            if (it.key == Key.Menu) {
                if (it.type == KeyEventType.KeyDown) return@onKeyEvent true
                if (isVideoSearchViaWebApi) {
                    showFilter = true
                    return@onKeyEvent true
                }
            }
            false
        },
        topBar = {
            Box(
                modifier = Modifier.padding(start = 48.dp, top = 24.dp, bottom = 8.dp, end = 48.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = searchKeyword,
                        fontSize = 24.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = (if (isVideoSearchViaWebApi) "菜单键打开筛选 | " else "") +
                                stringResource(R.string.load_data_count, searchResult.count),
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    ) { innerPadding ->
        BackHandler(focusOnContent) { backToTabRow() }

        Column(
            modifier = Modifier.padding(innerPadding)
        ) {
            TopNav(
                modifier = Modifier
                    .focusRequester(tabRowFocusRequester),
                items = SearchTypeTopNavItem.entries,
                isLargePadding = !focusOnContent,
                onSelectedChanged = { nav ->
                    when (nav) {
                        SearchTypeTopNavItem.Video -> searchResultViewModel.searchType =
                            SearchType.Video

                        SearchTypeTopNavItem.MediaBangumi -> searchResultViewModel.searchType =
                            SearchType.MediaBangumi

                        SearchTypeTopNavItem.MediaFt -> searchResultViewModel.searchType =
                            SearchType.MediaFt

                        SearchTypeTopNavItem.Live -> searchResultViewModel.searchType =
                            SearchType.Live

                        SearchTypeTopNavItem.BiliUser -> searchResultViewModel.searchType =
                            SearchType.BiliUser
                    }
                },
                onClick = { }
            )

            Spacer(modifier = Modifier.height(6.dp))

            TvLazyVerticalGrid(
                modifier = Modifier
                    .onFocusChanged { focusOnContent = it.hasFocus },
                state = gridState,
                columns = gridCells,
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                itemsIndexed(
                    items = searchResultItems
                ) { _, searchResultItem ->
                    SearchResultListItem(
                        searchResult = searchResultItem,
                        onClick = { onClickResult(searchResultItem) },
                        onAddWatchLater = { aid ->
                            toViewViewModel.addToView(aid)
                        },
                        onGoToDetailPage = { aid ->
                            jumpModeRepository.setPendingQueue(
                                source = JumpModeSource.Search,
                                selectedAid = aid,
                                items = searchResult.videos.toJumpModeItems()
                            )
                            VideoInfoActivity.actionStart(
                                context = context,
                                fromController = true,
                                aid = aid
                            )
                        },
                        onGoToUpPage = { mid, upName ->
                            UpInfoActivity.actionStart(context, mid, upName)
                        }
                    )
                }
            }
        }
    }

    SearchResultVideoFilter(
        show = showFilter,
        onHideFilter = { showFilter = false },
        selectedOrder = selectedOrder,
        selectedDuration = selectedDuration,
        selectedPartition = selectedPartition,
        selectedChildPartition = selectedChildPartition,
        onSelectedOrderChange = { searchResultViewModel.selectedOrder = it },
        onSelectedDurationChange = { searchResultViewModel.selectedDuration = it },
        onSelectedPartitionChange = { searchResultViewModel.selectedPartition = it },
        onSelectedChildPartitionChange = {
            searchResultViewModel.selectedChildPartition = it
        }
    )
}

@Composable
private fun SearchResultListItem(
    modifier: Modifier = Modifier,
    searchResult: SearchTypeResult.SearchTypeResultItem,
    onClick: () -> Unit,
    onAddWatchLater: ((Long) -> Unit),
    onGoToDetailPage: ((Long) -> Unit),
    onGoToUpPage: ((Long, String) -> Unit),
) {
    when (searchResult) {
        is SearchTypeResult.Video -> {
            SmallVideoCard(
                modifier = modifier,
                data = VideoCardData(
                    avid = searchResult.aid,
                    title = searchResult.title.removeHtmlTags(),
                    cover = searchResult.cover,
                    playString = searchResult.play.takeIf { it != -1 }.toWanString(),
                    danmakuString = searchResult.danmaku.takeIf { it != -1 }.toWanString(),
                    timeString = (searchResult.duration * 1000L).formatHourMinSec(),
                    upName = searchResult.author,
                    pubTime = searchResult.pubTime
                ),
                onClick = onClick,
                onAddWatchLater = { onAddWatchLater(searchResult.aid) },
                onGoToDetailPage = { onGoToDetailPage(searchResult.aid) },
                onGoToUpPage = { onGoToUpPage(searchResult.mid, searchResult.author) }
            )
        }

        is SearchTypeResult.Pgc -> {
            SeasonCard(
                modifier = modifier,
                data = SeasonCardData(
                    seasonId = searchResult.seasonId,
                    title = searchResult.title.removeHtmlTags(),
                    cover = searchResult.cover,
                    rating = String.format("%.1f", searchResult.star)
                ),
                onClick = onClick,
                onFocus = {}
            )
        }

        is SearchTypeResult.Live -> {
            SmallVideoCard(
                modifier = modifier,
                enableFocusPreview = false,
                data = VideoCardData(
                    avid = searchResult.roomId.toLong(),
                    title = searchResult.title.removeHtmlTags(),
                    cover = searchResult.cover,
                    playString = searchResult.online.toWanString(),
                    danmakuString = searchResult.areaName,
                    upName = searchResult.upName.removeHtmlTags()
                ),
                onClick = onClick
            )
        }

        is SearchTypeResult.User -> {
            UpCard(
                modifier = modifier.focusedScale(0.95f),
                face = searchResult.avatar,
                sign = searchResult.sign,
                username = searchResult.name,
                onFocusChange = { },
                onClick = onClick
            )
        }

        else -> {

        }
    }
}

fun SearchType.getDisplayName(context: Context) = when (this) {
    SearchType.Video -> context.getString(R.string.search_result_type_name_video)
    SearchType.MediaBangumi -> context.getString(R.string.search_result_type_name_media_bangumi)
    SearchType.MediaFt -> context.getString(R.string.search_result_type_name_media_ft)
    SearchType.Live -> "直播"
    SearchType.BiliUser -> context.getString(R.string.search_result_type_name_bili_user)
}
