package dev.aaa1115910.bv.screen.main.home

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.entity.user.DynamicVideo
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.activities.user.LoginActivity
import dev.aaa1115910.bv.activities.video.UpInfoActivity
import dev.aaa1115910.bv.activities.video.VideoInfoActivity
import dev.aaa1115910.bv.component.LoadingTip
import dev.aaa1115910.bv.component.TvLazyVerticalGrid
import dev.aaa1115910.bv.component.ifElse
import dev.aaa1115910.bv.component.videocard.SmallVideoCard
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.screen.main.LoginRequiredPlaceholder
import dev.aaa1115910.bv.ui.effect.UiEffect
import dev.aaa1115910.bv.util.formatHourMinSec
import dev.aaa1115910.bv.util.toWanString
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.viewmodel.home.DynamicViewModel
import dev.aaa1115910.bv.viewmodel.user.ToViewViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun DynamicsScreen(
    modifier: Modifier = Modifier,
    dynamicViewModel: DynamicViewModel = koinViewModel(),
    toViewViewModel: ToViewViewModel = koinViewModel(),
    defaultFocusRequester: FocusRequester
) {
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val firstGridItemFocusRequester = remember { FocusRequester() }
    val selectedAuthor = dynamicViewModel.selectedAuthor
    val filteredDynamics = dynamicViewModel.filteredDynamicList
    val onLogin = remember(context) {
        { context.startActivity(Intent(context, LoginActivity::class.java)) }
    }

    val onClickVideo: (DynamicVideo) -> Unit = { dynamic ->
        VideoInfoActivity.actionStart(
            context = context,
            aid = dynamic.aid,
            epid = dynamic.epid,
            proxyArea = ProxyArea.checkProxyArea(dynamic.title)
        )
    }

    LaunchedEffect(Unit) {
        toViewViewModel.uiEvent.collect { event ->
            when (event) {
                is UiEffect.ShowToast -> event.message.toast(context)
            }
        }
    }

    LaunchedEffect(dynamicViewModel.isLogin) {
        dynamicViewModel.onLoginStateChanged(dynamicViewModel.isLogin)
    }

    LaunchedEffect(selectedAuthor) {
        gridState.scrollToItem(0)
    }

    LaunchedEffect(gridState, selectedAuthor, dynamicViewModel.dynamicList.size) {
        snapshotFlow {
            gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index to
                dynamicViewModel.filteredDynamicList.size
        }
            .distinctUntilChanged()
            .filter { (index, filteredSize) ->
                index != null &&
                    filteredSize > 0 &&
                    index >= filteredSize - 12
            }
            .collect {
                scope.launch(Dispatchers.IO) {
                    dynamicViewModel.loadMore()
                }
            }
    }

    if (!dynamicViewModel.isLogin) {
        LoginRequiredPlaceholder(
            modifier = modifier,
            onLogin = onLogin,
            focusRequester = defaultFocusRequester
        )
        return
    }

    Scaffold(modifier = modifier) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .width(220.dp)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(dynamicViewModel.authorFilters) { _, author ->
                    var isFocused by remember(author) { mutableStateOf(false) }
                    val isAllAuthors = author == DynamicViewModel.ALL_UP_AUTHORS_FILTER
                    val isSelected = if (isAllAuthors) {
                        selectedAuthor == null
                    } else {
                        selectedAuthor == author
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .ifElse(isSelected, Modifier.focusRequester(defaultFocusRequester))
                            .onFocusChanged { isFocused = it.isFocused }
                            .focusProperties {
                                right = firstGridItemFocusRequester
                            },
                        onClick = {
                            dynamicViewModel.selectAuthor(if (isAllAuthors) null else author)
                        },
                        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.large),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.surface
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            },
                            focusedContainerColor = MaterialTheme.colorScheme.inverseSurface,
                            pressedContainerColor = MaterialTheme.colorScheme.inverseSurface,
                            focusedContentColor = MaterialTheme.colorScheme.inverseOnSurface,
                            pressedContentColor = MaterialTheme.colorScheme.inverseOnSurface
                        )
                    ) {
                        Text(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                            text = author,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isFocused) {
                                MaterialTheme.colorScheme.inverseOnSurface
                            } else if (isSelected) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }

            TvLazyVerticalGrid(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f),
                state = gridState,
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(
                    items = filteredDynamics,
                    key = { _, item -> "${item.authorMid}_${item.aid}_${item.cid}" }
                ) { index, item ->
                    SmallVideoCard(
                        modifier = Modifier
                            .ifElse(index == 0, Modifier.focusRequester(firstGridItemFocusRequester))
                            .focusProperties {
                                left = defaultFocusRequester
                            },
                        data = remember(item) {
                            VideoCardData(
                                avid = item.aid,
                                title = item.title,
                                cover = item.cover,
                                playString = item.play.takeIf { it != -1 }.toWanString(),
                                danmakuString = item.danmaku.takeIf { it != -1 }.toWanString(),
                                upName = item.author,
                                timeString = (item.duration * 1000L).formatHourMinSec(),
                                pubTime = item.pubTime
                            )
                        },
                        onClick = { onClickVideo(item) },
                        onAddWatchLater = {
                            toViewViewModel.addToView(item.aid)
                        },
                        onGoToDetailPage = {
                            VideoInfoActivity.actionStart(
                                context = context,
                                fromController = true,
                                aid = item.aid,
                                epid = item.epid,
                            )
                        },
                        onGoToUpPage = {
                            UpInfoActivity.actionStart(context, item.authorMid, item.author)
                        }
                    )
                }

                if (dynamicViewModel.loading) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        LoadingTip()
                    }
                }

                if (filteredDynamics.isEmpty() && !dynamicViewModel.loading) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            text = stringResource(R.string.dynamic_empty),
                            color = Color.White
                        )
                    }
                }

                if (!dynamicViewModel.hasMore && filteredDynamics.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            text = stringResource(R.string.dynamic_no_more),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
