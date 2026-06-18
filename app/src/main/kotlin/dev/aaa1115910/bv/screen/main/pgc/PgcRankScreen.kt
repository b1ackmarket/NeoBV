package dev.aaa1115910.bv.screen.main.pgc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.entity.pgc.PgcItem
import dev.aaa1115910.biliapi.entity.pgc.PgcType
import dev.aaa1115910.biliapi.repositories.PgcRepository
import dev.aaa1115910.biliapi.repositories.displayName
import dev.aaa1115910.bv.activities.video.SeasonInfoActivity
import dev.aaa1115910.bv.component.TvLazyVerticalGrid
import dev.aaa1115910.bv.component.videocard.SeasonCard
import dev.aaa1115910.bv.entity.carddata.SeasonCardData
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.util.ImageSize
import dev.aaa1115910.bv.util.rememberAdaptiveGridCells
import dev.aaa1115910.bv.util.requestFocus
import dev.aaa1115910.bv.util.resizedImageUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.koin.compose.getKoin

@Composable
fun PgcRankScreen(
    modifier: Modifier = Modifier,
    pgcType: PgcType,
    pgcRepository: PgcRepository = getKoin().get()
) {
    val context = LocalContext.current
    val gridState = rememberLazyGridState()
    val firstItemFocusRequester = remember { FocusRequester() }
    val inputModeManager = LocalInputModeManager.current
    val items = remember { mutableStateListOf<PgcItem>() }
    var tip by remember { mutableStateOf("加载中…") }

    LaunchedEffect(pgcType) {
        val result = withContext(Dispatchers.IO) {
            runCatching { pgcRepository.getRank(pgcType) }
        }
        result
            .onSuccess { rank ->
                items.clear()
                items.addAll(rank.items.filter { it.seasonId > 0 })
                tip = if (items.isEmpty()) "暂无排行榜内容" else ""
            }
            .onFailure {
                tip = it.localizedMessage ?: "加载排行榜失败"
            }
    }

    LaunchedEffect(items.size) {
        if (items.isNotEmpty()) {
            delay(50)
            runCatching { gridState.scrollToItem(0) }
            if (inputModeManager.inputMode != InputMode.Touch) {
                firstItemFocusRequester.requestFocus(this)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            Box(modifier = Modifier.padding(start = 48.dp, top = 24.dp, bottom = 8.dp, end = 48.dp)) {
                Text(text = "${pgcType.displayName}排行榜")
            }
        }
    ) { innerPadding ->
        if (items.isEmpty()) {
            Box(modifier = Modifier.padding(innerPadding).padding(48.dp)) {
                Text(text = tip)
            }
        } else {
            TvLazyVerticalGrid(
                modifier = Modifier.padding(innerPadding),
                columns = rememberAdaptiveGridCells(defaultColumns = 5),
                state = gridState,
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                itemsIndexed(items, key = { _, item -> item.seasonId }) { index, item ->
                    SeasonCard(
                        modifier = if (index == 0) {
                            Modifier.focusRequester(firstItemFocusRequester)
                        } else {
                            Modifier
                        },
                        coverHeight = 180.dp,
                        data = SeasonCardData(
                            seasonId = item.seasonId,
                            title = "${index + 1}. ${item.title}",
                            subTitle = item.subTitle,
                            cover = item.cover.resizedImageUrl(ImageSize.SeasonCoverThumbnail),
                            rating = item.rating
                        ),
                        onClick = {
                            SeasonInfoActivity.actionStart(
                                context = context,
                                seasonId = item.seasonId,
                                proxyArea = ProxyArea.checkProxyArea(item.title)
                            )
                        }
                    )
                }
            }
        }
    }
}
