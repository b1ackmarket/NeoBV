package dev.aaa1115910.bv.screen.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.entity.season.FollowingSeasonStatus
import dev.aaa1115910.biliapi.entity.season.FollowingSeasonType
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.activities.video.SeasonInfoActivity
import dev.aaa1115910.bv.component.TvLazyVerticalGrid
import dev.aaa1115910.bv.component.videocard.SeasonCard
import dev.aaa1115910.bv.entity.carddata.SeasonCardData
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.util.ImageSize
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.requestFocus
import dev.aaa1115910.bv.util.resizedImageUrl
import dev.aaa1115910.bv.viewmodel.user.FollowingSeasonViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.androidx.compose.koinViewModel

internal fun shouldRequestFollowingSeasonInitialFocus(itemCount: Int): Boolean = itemCount > 0

@Composable
fun FollowingSeasonScreen(
    modifier: Modifier = Modifier,
    followingSeasonViewModel: FollowingSeasonViewModel = koinViewModel(),
    requestInitialFocus: Boolean = true
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val logger = KotlinLogging.logger { }
    val firstCardFocusRequester = remember { FocusRequester() }
    val inputModeManager = LocalInputModeManager.current
    val gridState = rememberLazyGridState()

    var currentIndex by remember { mutableIntStateOf(0) }
    var showFilter by remember { mutableStateOf(false) }

    val followingSeasons = followingSeasonViewModel.followingSeasons
    var followingSeasonType by remember { mutableStateOf(followingSeasonViewModel.followingSeasonType) }
    var followingSeasonStatus by remember { mutableStateOf(followingSeasonViewModel.followingSeasonStatus) }
    val noMore = followingSeasonViewModel.noMore

    // 用 derivedStateOf 监听滚动位置，兼容触控和遥控两种输入方式触发分页
    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()
            val total = gridState.layoutInfo.totalItemsCount
            lastVisible != null && followingSeasons.isNotEmpty() && !noMore &&
                    lastVisible.index + 18 >= total
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            followingSeasonViewModel.loadMore()
        }
    }

    val updateType: (FollowingSeasonType) -> Unit = {
        followingSeasonType = it
        followingSeasonViewModel.followingSeasonType = it
    }

    val updateStatus: (FollowingSeasonStatus) -> Unit = {
        followingSeasonStatus = it
        followingSeasonViewModel.followingSeasonStatus = it
    }

    LaunchedEffect(followingSeasonType, followingSeasonStatus) {
        logger.fInfo { "Start update search result because filter updated" }
        followingSeasonViewModel.clearData()
        followingSeasonViewModel.loadMore()
    }

    // 非触控模式下，列表加载后自动请求焦点
    LaunchedEffect(followingSeasons.size) {
        if (requestInitialFocus && shouldRequestFollowingSeasonInitialFocus(followingSeasons.size) && inputModeManager.inputMode != InputMode.Touch) {
            firstCardFocusRequester.requestFocus(scope)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .onKeyEvent {
                if (it.key == Key.Menu) {
                    if (it.type == KeyEventType.KeyDown) return@onKeyEvent true
                    showFilter = true
                    return@onKeyEvent true
                }
                false
            },
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            text = stringResource(R.string.filter_dialog_open_tip),
            color = Color.White.copy(alpha = 0.6f)
        )
        TvLazyVerticalGrid(
            modifier = Modifier,
            state = gridState,
            columns = GridCells.Fixed(6),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (followingSeasons.isNotEmpty()) {
                itemsIndexed(items = followingSeasons) { index, followingSeason ->
                    SeasonCard(
                        modifier = if (index == 0) {
                            Modifier.focusRequester(firstCardFocusRequester)
                        } else {
                            Modifier
                        },
                        data = SeasonCardData(
                            seasonId = followingSeason.seasonId,
                            title = followingSeason.title,
                            cover = followingSeason.cover.resizedImageUrl(ImageSize.SeasonCoverThumbnail),
                            rating = null
                        ),
                        onFocus = {
                            currentIndex = index
                        },
                        onClick = {
                            SeasonInfoActivity.actionStart(
                                context = context,
                                seasonId = followingSeason.seasonId,
                                proxyArea = ProxyArea.checkProxyArea(followingSeason.title)
                            )
                        }
                    )
                }
            } else {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyTip()
                }
            }

            if (followingSeasons.isEmpty() && noMore) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        OutlinedButton(onClick = { showFilter = true }) {
                            Text(text = stringResource(R.string.filter_dialog_open_tip_click))
                        }
                    }
                }
            }
        }
    }

    FollowingSeasonFilter(
        show = showFilter,
        onHideFilter = { showFilter = false },
        selectedType = followingSeasonType,
        selectedStatus = followingSeasonStatus,
        onSelectedTypeChange = updateType,
        onSelectedStatusChange = updateStatus
    )
}
