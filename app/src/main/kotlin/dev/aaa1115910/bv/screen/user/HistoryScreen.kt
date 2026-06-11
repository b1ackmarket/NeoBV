package dev.aaa1115910.bv.screen.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.activities.live.LivePlayerActivity
import dev.aaa1115910.bv.activities.video.UpInfoActivity
import dev.aaa1115910.bv.activities.video.VideoInfoActivity
import dev.aaa1115910.bv.component.TvLazyVerticalGrid
import dev.aaa1115910.bv.component.ifElse
import dev.aaa1115910.bv.component.videocard.SmallVideoCard
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.repository.JumpModeRepository
import dev.aaa1115910.bv.repository.JumpModeSource
import dev.aaa1115910.bv.repository.LiveJumpModeRepository
import dev.aaa1115910.bv.repository.toLiveJumpModeItems
import dev.aaa1115910.bv.repository.toJumpModeItems
import dev.aaa1115910.bv.ui.effect.UiEffect
import dev.aaa1115910.bv.util.touchClick
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.viewmodel.user.HistoryContentType
import dev.aaa1115910.bv.viewmodel.user.HistoryViewModel
import dev.aaa1115910.bv.viewmodel.user.ToViewViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import org.koin.androidx.compose.koinViewModel

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    firstCardFocusRequester: FocusRequester? = null,
    historyViewModel: HistoryViewModel = koinViewModel(),
    toViewViewModel: ToViewViewModel = koinViewModel()
) {
    val gridState = rememberLazyGridState()
    val context = LocalContext.current
    val jumpModeRepository = remember { BVApp.koinApplication.koin.get<JumpModeRepository>() }
    val liveJumpModeRepository = remember { BVApp.koinApplication.koin.get<LiveJumpModeRepository>() }
    val typeTabsFocusRequester = remember { FocusRequester() }
    val resolvedFirstCardFocusRequester = firstCardFocusRequester ?: remember { FocusRequester() }
    val columns = 4

    // 监听可见区最后一个 item 的 index，距离尾部 20 个就翻页
    LaunchedEffect(gridState, historyViewModel.selectedType) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .distinctUntilChanged()
            .filter { index ->
                index != null && index >= historyViewModel.currentItemCount() - 20
            }
            .collect {
                historyViewModel.update()
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

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        HistoryTypeTabs(
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(typeTabsFocusRequester),
            selectedType = historyViewModel.selectedType,
            onFocusSelect = historyViewModel::selectType,
            onClickSelect = historyViewModel::refreshType
        )
        Spacer(modifier = Modifier.height(6.dp))
        TvLazyVerticalGrid(
            modifier = Modifier.fillMaxSize(),
            state = gridState,
            columns = GridCells.Fixed(columns),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            when (historyViewModel.selectedType) {
                HistoryContentType.Video -> {
                    if (historyViewModel.histories.isNotEmpty()) {
                        itemsIndexed(historyViewModel.histories) { index, history ->
                            Box(contentAlignment = Alignment.Center) {
                                HistoryVideoCard(
                                    modifier = Modifier.historyCardFocus(
                                        index = index,
                                        columns = columns,
                                        firstCardFocusRequester = resolvedFirstCardFocusRequester,
                                        tabsFocusRequester = typeTabsFocusRequester
                                    ),
                                    history = history,
                                    onClick = {
                                        jumpModeRepository.setPendingQueue(
                                            source = JumpModeSource.Personal,
                                            selectedAid = history.avid,
                                            items = historyViewModel.histories.toJumpModeItems()
                                        )
                                        VideoInfoActivity.actionStart(
                                            context = context,
                                            aid = history.avid,
                                            epid = history.epId,
                                            proxyArea = ProxyArea.checkProxyArea(history.title)
                                        )
                                    },
                                    onAddWatchLater = { toViewViewModel.addToView(history.avid) },
                                    onGoToDetailPage = {
                                        jumpModeRepository.setPendingQueue(
                                            source = JumpModeSource.Personal,
                                            selectedAid = history.avid,
                                            items = historyViewModel.histories.toJumpModeItems()
                                        )
                                        VideoInfoActivity.actionStart(
                                            context = context,
                                            fromController = true,
                                            aid = history.avid,
                                            epid = history.epId
                                        )
                                    },
                                    onGoToUpPage = history.upMid?.let {
                                        { UpInfoActivity.actionStart(context, it, history.upName) }
                                    }
                                )
                            }
                        }
                    } else {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            EmptyTip(
                                text = if (historyViewModel.refreshingType == HistoryContentType.Video) {
                                    "加载中…"
                                } else {
                                    "空空如也"
                                }
                            )
                        }
                    }
                }

                HistoryContentType.Live -> {
                    if (historyViewModel.liveHistories.isNotEmpty()) {
                        itemsIndexed(historyViewModel.liveHistories, key = { _, room -> room.roomId }) { index, room ->
                            SmallVideoCard(
                                modifier = Modifier.historyCardFocus(
                                    index = index,
                                    columns = columns,
                                    firstCardFocusRequester = resolvedFirstCardFocusRequester,
                                    tabsFocusRequester = typeTabsFocusRequester
                                ),
                                data = VideoCardData(
                                    avid = room.roomId.toLong(),
                                    title = room.title,
                                    cover = room.cover,
                                    upName = room.upName,
                                    playString = room.badges.firstOrNull().orEmpty(),
                                    danmakuString = room.areaName,
                                    badges = room.badges
                                ),
                                onClick = {
                                    liveJumpModeRepository.setPendingQueue(
                                        selectedRoomId = room.roomId,
                                        items = historyViewModel.liveHistories.toLiveJumpModeItems()
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
                    } else {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            EmptyTip(
                                text = if (historyViewModel.refreshingType == HistoryContentType.Live) {
                                    "加载中…"
                                } else {
                                    "空空如也"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryTypeTabs(
    modifier: Modifier = Modifier,
    selectedType: HistoryContentType,
    onFocusSelect: (HistoryContentType) -> Unit,
    onClickSelect: (HistoryContentType) -> Unit
) {
    val tabs = HistoryContentType.entries
    val currentTabIndex by remember(selectedType) {
        derivedStateOf { tabs.indexOf(selectedType).coerceAtLeast(0) }
    }
    val focusRequesters = remember(tabs) {
        tabs.map { FocusRequester() }
    }

    TabRow(
        modifier = modifier
            .padding(horizontal = 12.dp)
            .focusRestorer(focusRequesters[currentTabIndex]),
        selectedTabIndex = currentTabIndex,
        separator = { Spacer(modifier = Modifier.width(12.dp)) },
    ) {
        tabs.forEachIndexed { index, type ->
            Tab(
                modifier = Modifier
                    .focusRequester(focusRequesters[index])
                    .touchClick { onClickSelect(type) },
                selected = selectedType == type,
                onFocus = { onFocusSelect(type) },
                onClick = { onClickSelect(type) }
            ) {
                Box(
                    modifier = Modifier.height(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        text = type.displayName,
                        color = LocalContentColor.current,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryVideoCard(
    modifier: Modifier,
    history: VideoCardData,
    onClick: () -> Unit,
    onAddWatchLater: () -> Unit,
    onGoToDetailPage: () -> Unit,
    onGoToUpPage: (() -> Unit)?
) {
    SmallVideoCard(
        modifier = modifier,
        data = history,
        onClick = onClick,
        onAddWatchLater = onAddWatchLater,
        onGoToDetailPage = onGoToDetailPage,
        onGoToUpPage = onGoToUpPage
    )
}

private fun Modifier.historyCardFocus(
    index: Int,
    columns: Int,
    firstCardFocusRequester: FocusRequester,
    tabsFocusRequester: FocusRequester
): Modifier {
    return this
        .ifElse(index == 0, Modifier.focusRequester(firstCardFocusRequester))
        .ifElse(index in 0 until columns, Modifier.focusProperties { up = tabsFocusRequester })
}
