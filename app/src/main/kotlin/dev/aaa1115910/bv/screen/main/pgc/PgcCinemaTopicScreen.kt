package dev.aaa1115910.bv.screen.main.pgc

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.aaa1115910.bv.activities.video.SeasonInfoActivity
import dev.aaa1115910.bv.component.TvLazyVerticalGrid
import dev.aaa1115910.bv.component.videocard.SeasonCard
import dev.aaa1115910.bv.entity.PgcCinemaTopicPayload
import dev.aaa1115910.bv.entity.carddata.SeasonCardData
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.util.rememberAdaptiveGridCells
import dev.aaa1115910.bv.util.requestFocus

@Composable
fun PgcCinemaTopicScreen(
    modifier: Modifier = Modifier,
    payload: PgcCinemaTopicPayload?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val firstCardFocusRequester = remember { FocusRequester() }
    val items = remember(payload) { payload?.items?.map { it.toPgcItem() }.orEmpty() }

    LaunchedEffect(items.isNotEmpty()) {
        if (items.isNotEmpty()) firstCardFocusRequester.requestFocus(scope)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onKeyEvent {
                if (it.key == Key.Back || it.key == Key.Menu) {
                    if (it.type == KeyEventType.KeyDown) return@onKeyEvent true
                    (context as Activity).finish()
                    return@onKeyEvent true
                }
                false
            }
    ) {
        if (payload == null) {
            Text(
                modifier = Modifier.align(Alignment.Center),
                text = "专题数据无效",
                style = MaterialTheme.typography.titleLarge
            )
            return@Box
        }

        TvLazyVerticalGrid(
            modifier = Modifier.fillMaxSize(),
            columns = rememberAdaptiveGridCells(defaultColumns = 6),
            contentPadding = PaddingValues(start = 48.dp, top = 32.dp, end = 48.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = payload.title,
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 1
                    )
                    Text(
                        text = "${items.size} 部",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                        fontSize = 18.sp
                    )
                }
            }

            items.forEachIndexed { index, pgcItem ->
                item {
                    SeasonCard(
                        modifier = if (index == 0) {
                            Modifier.focusRequester(firstCardFocusRequester)
                        } else {
                            Modifier
                        },
                        data = SeasonCardData.fromPgcItem(pgcItem),
                        onClick = {
                            SeasonInfoActivity.actionStart(
                                context = context,
                                seasonId = pgcItem.seasonId,
                                proxyArea = ProxyArea.checkProxyArea(pgcItem.title)
                            )
                        }
                    )
                }
            }
        }
    }
}
