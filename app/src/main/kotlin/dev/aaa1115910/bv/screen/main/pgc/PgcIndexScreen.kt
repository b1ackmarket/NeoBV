package dev.aaa1115910.bv.screen.main.pgc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.entity.pgc.PgcType
import dev.aaa1115910.biliapi.entity.pgc.index.IndexOrder
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.activities.video.SeasonInfoActivity
import dev.aaa1115910.bv.component.TvLazyVerticalGrid
import dev.aaa1115910.bv.component.pgc.IndexFilter
import dev.aaa1115910.bv.component.videocard.SeasonCard
import dev.aaa1115910.bv.entity.carddata.SeasonCardData
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.util.getDisplayName
import dev.aaa1115910.bv.util.rememberAdaptiveGridCells
import dev.aaa1115910.bv.util.requestFocus
import dev.aaa1115910.bv.viewmodel.index.PgcIndexViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel

internal data class PgcIndexLaunchState(
    val pgcType: PgcType,
    val indexOrder: IndexOrder
)

internal fun resolvePgcIndexLaunchState(pgcType: PgcType): PgcIndexLaunchState =
    PgcIndexLaunchState(
        pgcType = pgcType,
        indexOrder = IndexOrder.getList(pgcType).first()
    )

@Composable
fun PgcIndexScreen(
    modifier: Modifier = Modifier,
    initialPgcType: PgcType = PgcType.Anime,
    pgcIndexViewModel: PgcIndexViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launchState = remember(initialPgcType) { resolvePgcIndexLaunchState(initialPgcType) }
    val firstCardFocusRequester = remember { FocusRequester() }

    var currentSeasonIndex by remember { mutableIntStateOf(0) }
    var filterReloadsEnabled by remember(initialPgcType) { mutableStateOf(false) }

    val pgcItems = pgcIndexViewModel.indexResultItems
    val noMore = pgcIndexViewModel.noMore
    var showFilter by remember { mutableStateOf(false) }

    val reloadData: suspend () -> Unit = {
        withContext(Dispatchers.IO) {
            pgcIndexViewModel.reload()
        }
    }

    LaunchedEffect(launchState) {
        filterReloadsEnabled = false
        pgcIndexViewModel.changePgcType(launchState.pgcType)
        reloadData()
        filterReloadsEnabled = true
    }

    LaunchedEffect(
        pgcIndexViewModel.indexOrder,
        pgcIndexViewModel.indexOrderType,
        pgcIndexViewModel.seasonVersion,
        pgcIndexViewModel.spokenLanguage,
        pgcIndexViewModel.area,
        pgcIndexViewModel.isFinish,
        pgcIndexViewModel.copyright,
        pgcIndexViewModel.seasonStatus,
        pgcIndexViewModel.seasonMonth,
        pgcIndexViewModel.producer,
        pgcIndexViewModel.year,
        pgcIndexViewModel.releaseDate,
        pgcIndexViewModel.style,
    ) {
        if (!filterReloadsEnabled) return@LaunchedEffect
        reloadData()
    }

    LaunchedEffect(pgcItems.isNotEmpty()) {
        if (pgcItems.isNotEmpty()) firstCardFocusRequester.requestFocus(scope)
    }

    Scaffold(
        modifier = modifier.onKeyEvent {
            if (it.key == Key.Menu) {
                if (it.type == KeyEventType.KeyDown) return@onKeyEvent true
                showFilter = true
                return@onKeyEvent true
            }
            false
        },
        topBar = {
            Box(
                modifier = Modifier.padding(start = 48.dp, top = 24.dp, bottom = 8.dp, end = 48.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(id = R.string.title_activity_pgc_index) +
                                " - " + launchState.pgcType.getDisplayName(context),
                        fontSize = 24.sp,
                    )
                    Text(
                        text = stringResource(R.string.filter_dialog_open_tip),
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    ) { innerPadding ->
        TvLazyVerticalGrid(
            modifier = Modifier.padding(innerPadding),
            columns = rememberAdaptiveGridCells(defaultColumns = 6),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            itemsIndexed(items = pgcItems) { index, pgcItem ->
                SeasonCard(
                    modifier = if (index == 0) {
                        Modifier.focusRequester(firstCardFocusRequester)
                    } else {
                        Modifier
                    },
                    data = SeasonCardData.fromPgcItem(pgcItem),
                    onFocus = {
                        currentSeasonIndex = index
                        if (index + 30 > pgcItems.size) {
                            println("load more by focus")
                            scope.launch(Dispatchers.IO) { pgcIndexViewModel.loadMore() }
                        }
                    },
                    onClick = {
                        SeasonInfoActivity.actionStart(
                            context = context,
                            seasonId = pgcItem.seasonId,
                            proxyArea = ProxyArea.checkProxyArea(pgcItem.title)
                        )
                    }
                )
            }
            if (pgcItems.isEmpty() && noMore) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = stringResource(R.string.no_data))
                            OutlinedButton(onClick = { showFilter = true }) {
                                Text(text = stringResource(R.string.filter_dialog_open_tip_click))
                            }
                        }
                    }
                }
            }
        }
    }

    IndexFilter(
        type = launchState.pgcType,
        show = showFilter,
        onDismissRequest = { showFilter = false },
        order = pgcIndexViewModel.indexOrder,
        orderType = pgcIndexViewModel.indexOrderType,
        seasonVersion = pgcIndexViewModel.seasonVersion,
        spokenLanguage = pgcIndexViewModel.spokenLanguage,
        area = pgcIndexViewModel.area,
        isFinish = pgcIndexViewModel.isFinish,
        copyright = pgcIndexViewModel.copyright,
        seasonStatus = pgcIndexViewModel.seasonStatus,
        seasonMonth = pgcIndexViewModel.seasonMonth,
        producer = pgcIndexViewModel.producer,
        year = pgcIndexViewModel.year,
        releaseDate = pgcIndexViewModel.releaseDate,
        style = pgcIndexViewModel.style,
        onOrderChange = { pgcIndexViewModel.indexOrder = it },
        onOrderTypeChange = { pgcIndexViewModel.indexOrderType = it },
        onSeasonVersionChange = { pgcIndexViewModel.seasonVersion = it },
        onSpokenLanguageChange = { pgcIndexViewModel.spokenLanguage = it },
        onAreaChange = { pgcIndexViewModel.area = it },
        onIsFinishChange = { pgcIndexViewModel.isFinish = it },
        onCopyrightChange = { pgcIndexViewModel.copyright = it },
        onSeasonStatusChange = { pgcIndexViewModel.seasonStatus = it },
        onSeasonMonthChange = { pgcIndexViewModel.seasonMonth = it },
        onProducerChange = { pgcIndexViewModel.producer = it },
        onYearChange = { pgcIndexViewModel.year = it },
        onReleaseDateChange = { pgcIndexViewModel.releaseDate = it },
        onStyleChange = { pgcIndexViewModel.style = it }
    )
}
