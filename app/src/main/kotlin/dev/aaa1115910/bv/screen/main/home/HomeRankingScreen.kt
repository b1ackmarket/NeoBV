package dev.aaa1115910.bv.screen.main.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.repositories.HomeRankingPeriod
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.activities.video.VideoInfoActivity
import dev.aaa1115910.bv.component.LoadingTip
import dev.aaa1115910.bv.component.SelectableItemPopup
import dev.aaa1115910.bv.component.TvLazyVerticalGrid
import dev.aaa1115910.bv.component.videocard.SmallVideoCard
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.repository.JumpModeRepository
import dev.aaa1115910.bv.repository.JumpModeSource
import dev.aaa1115910.bv.repository.toJumpModeItems
import dev.aaa1115910.bv.util.formatHourMinSec
import dev.aaa1115910.bv.util.rememberAdaptiveGridCells
import dev.aaa1115910.bv.util.touchClick
import dev.aaa1115910.bv.util.toWanString
import dev.aaa1115910.bv.viewmodel.home.HomeRankingType
import dev.aaa1115910.bv.viewmodel.home.HomeRankingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeRankingScreen(
    modifier: Modifier = Modifier,
    rankingViewModel: HomeRankingViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    val jumpModeRepository = remember { BVApp.koinApplication.koin.get<JumpModeRepository>() }
    var showTypePopup by remember { mutableStateOf(false) }
    var showPeriodPopup by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        TvLazyVerticalGrid(
            state = gridState,
            columns = rememberAdaptiveGridCells(defaultColumns = 4),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                HomeRankingFilterRow(
                    selectedType = rankingViewModel.selectedType,
                    periods = rankingViewModel.periods,
                    selectedPeriodId = rankingViewModel.selectedPeriodId,
                    onShowTypePopup = { showTypePopup = true },
                    onShowPeriodPopup = { showPeriodPopup = true },
                    onSelectType = { type ->
                        scope.launch(Dispatchers.IO) { rankingViewModel.selectType(type) }
                    }
                )
            }

            items(rankingViewModel.items, key = { it.aid }) { item ->
                SmallVideoCard(
                    data = remember(item) {
                        VideoCardData(
                            avid = item.aid,
                            cid = null,
                            title = item.title,
                            cover = item.cover,
                            playString = item.play.takeIf { it != -1 }.toWanString(),
                            danmakuString = item.danmaku.takeIf { it != -1 }.toWanString(),
                            timeString = (item.duration * 1000L).formatHourMinSec(),
                            upName = item.author,
                            pubTime = item.pubTime
                        )
                    },
                    onClick = {
                        jumpModeRepository.setPendingQueue(
                            source = JumpModeSource.Home,
                            selectedAid = item.aid,
                            items = rankingViewModel.items.toJumpModeItems()
                        )
                        VideoInfoActivity.actionStart(context, item.aid)
                    },
                    onGoToDetailPage = {
                        VideoInfoActivity.actionStart(
                            context = context,
                            fromController = true,
                            aid = item.aid
                        )
                    }
                )
            }

            if (rankingViewModel.loading) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) { LoadingTip() }
                }
            } else if (rankingViewModel.errorMessage != null || rankingViewModel.items.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp),
                        textAlign = TextAlign.Center,
                        text = rankingViewModel.errorMessage ?: "暂无排行榜内容"
                    )
                }
            }
        }

        SelectableItemPopup(
            show = showTypePopup,
            title = "选择榜单",
            items = HomeRankingType.entries,
            selectedItem = rankingViewModel.selectedType,
            label = { it.displayName },
            onDismiss = { showTypePopup = false },
            onSelect = { type ->
                scope.launch(Dispatchers.IO) { rankingViewModel.selectType(type) }
            }
        )

        SelectableItemPopup(
            show = showPeriodPopup,
            title = "选择期数",
            items = rankingViewModel.periods,
            selectedItem = rankingViewModel.periods.firstOrNull {
                it.id == rankingViewModel.selectedPeriodId
            },
            label = { it.label },
            onDismiss = { showPeriodPopup = false },
            onSelect = { period ->
                scope.launch(Dispatchers.IO) { rankingViewModel.selectPeriod(period) }
            }
        )
    }
}

@Composable
private fun HomeRankingFilterRow(
    selectedType: HomeRankingType,
    periods: List<HomeRankingPeriod>,
    selectedPeriodId: Int?,
    onShowTypePopup: () -> Unit,
    onShowPeriodPopup: () -> Unit,
    onSelectType: (HomeRankingType) -> Unit
) {
    val selectedPeriod = periods.firstOrNull { it.id == selectedPeriodId }
    LazyRow(
        contentPadding = PaddingValues(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedButton(
                modifier = Modifier.touchClick(onShowTypePopup),
                onClick = onShowTypePopup
            ) {
                Icon(
                    modifier = Modifier.size(18.dp),
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = null
                )
                Text(text = selectedType.displayName)
            }
        }
        if (selectedType.hasPeriods && periods.isNotEmpty()) {
            item {
                OutlinedButton(
                    modifier = Modifier.touchClick(onShowPeriodPopup),
                    onClick = onShowPeriodPopup
                ) {
                    Text(text = selectedPeriod?.label ?: "最新一期")
                }
            }
        }
        items(HomeRankingType.entries, key = { it.name }) { type ->
            val selectType = { onSelectType(type) }
            OutlinedButton(
                modifier = Modifier
                    .widthIn(max = 192.dp)
                    .touchClick(selectType),
                onClick = selectType
            ) {
                Text(
                    text = type.displayName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
