package dev.aaa1115910.bv.screen.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import dev.aaa1115910.bv.activities.video.UpInfoActivity
import dev.aaa1115910.bv.activities.video.VideoInfoActivity
import dev.aaa1115910.bv.component.HomeTopNavItem
import dev.aaa1115910.bv.component.TopNav
import dev.aaa1115910.bv.screen.main.home.HomeRankingScreen
import dev.aaa1115910.bv.screen.main.home.PopularScreen
import dev.aaa1115910.bv.screen.main.home.RecommendScreen
import dev.aaa1115910.bv.screen.main.ugc.UgcRegionScaffold
import dev.aaa1115910.bv.util.LayoutConfig
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.viewmodel.UserViewModel
import dev.aaa1115910.bv.viewmodel.home.HomeRegionState
import dev.aaa1115910.bv.viewmodel.home.HomeRegionViewModel
import dev.aaa1115910.bv.viewmodel.home.HomeRankingViewModel
import dev.aaa1115910.bv.viewmodel.home.PopularViewModel
import dev.aaa1115910.bv.viewmodel.home.RecommendViewModel
import dev.aaa1115910.bv.viewmodel.player.HomeStartDestination
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeContent(
    navFocusRequester: FocusRequester,
    startupTab: HomeStartDestination = HomeStartDestination.Recommend,
    recommendViewModel: RecommendViewModel = koinViewModel(),
    popularViewModel: PopularViewModel = koinViewModel(),
    rankingViewModel: HomeRankingViewModel = koinViewModel(),
    userViewModel: UserViewModel = koinViewModel(),
    homeRegionViewModel: HomeRegionViewModel = koinViewModel()
) {
    val scope = rememberCoroutineScope()
    val logger = KotlinLogging.logger("HomeContent")
    val context = LocalContext.current

    val firstTab = remember(startupTab) {
        when (startupTab) {
            HomeStartDestination.Popular -> HomeTopNavItem.Popular
            HomeStartDestination.Recommend -> HomeTopNavItem.Recommend
        }
    }
    var selectedTab by remember { mutableStateOf(firstTab) }
    var focusOnContent by remember { mutableStateOf(false) }

    val reorderedItems = remember {
        LayoutConfig.applyHome(resolveHomeTopNavOrder(firstTab))
    }

    LaunchedEffect(reorderedItems, selectedTab) {
        if (selectedTab !in reorderedItems) {
            selectedTab = reorderedItems.firstOrNull() ?: firstTab
        }
    }
    val regionGridStates = remember {
        HomeTopNavItem.entries
            .filter {
                it != HomeTopNavItem.Recommend &&
                    it != HomeTopNavItem.Popular &&
                    it != HomeTopNavItem.Ranking
            }
            .associateWith { LazyGridState() }
    }

    //启动时刷新数据
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            recommendViewModel.loadMore()
        }
        scope.launch(Dispatchers.IO) {
            popularViewModel.loadMore()
        }
        scope.launch(Dispatchers.IO) {
            userViewModel.updateUserInfo()
        }
    }

    //监听登录变化
    LaunchedEffect(userViewModel.isLogin) {
        if (userViewModel.isLogin) {
            //login
            userViewModel.updateUserInfo()
        } else {
            //logout
            userViewModel.clearUserInfo()
        }
    }

    Scaffold(
        topBar = {
            TopNav(
                modifier = Modifier.focusRequester(navFocusRequester),
                items = reorderedItems,
                isLargePadding = !focusOnContent,
                onSelectedChanged = { nav ->
                    selectedTab = nav as HomeTopNavItem
                    when (nav) {
                        HomeTopNavItem.Recommend -> {}
                        HomeTopNavItem.Popular -> {}
                        HomeTopNavItem.Ranking -> {
                            scope.launch(Dispatchers.IO) { rankingViewModel.ensureLoaded() }
                        }
                        else -> {
                            if (homeRegionViewModel.regionStateMap[nav] == null) {
                                homeRegionViewModel.addState(
                                    nav,
                                    HomeRegionState(
                                        lazyGridState = regionGridStates.getValue(nav),
                                        ugcType = nav.toUgcType()
                                    )
                                )
                            }
                        }
                    }
                },
                onClick = { nav ->
                    when ((nav as HomeTopNavItem).toClickAction()) {
                        HomeTabAction.RefreshRecommend -> {
                            logger.fInfo { "clear recommend data" }
                            recommendViewModel.clearData()
                            logger.fInfo { "reload recommend data" }
                            scope.launch(Dispatchers.IO) { recommendViewModel.loadMore() }
                        }

                        HomeTabAction.RefreshPopular -> {
                            logger.fInfo { "clear popular data" }
                            popularViewModel.clearData()
                            logger.fInfo { "reload popular data" }
                            scope.launch(Dispatchers.IO) { popularViewModel.loadMore() }
                        }

                        HomeTabAction.RefreshRanking -> {
                            scope.launch(Dispatchers.IO) { rankingViewModel.refresh() }
                        }

                        HomeTabAction.RefreshRegion -> {
                            homeRegionViewModel.reloadAll(nav)
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .onFocusChanged { focusOnContent = it.hasFocus }
                .onPreviewKeyEvent {
                    if (it.key == Key.Menu) {
                        if (it.type == KeyEventType.KeyDown) return@onPreviewKeyEvent true
                        when (resolveHomeMenuAction(selectedTab)) {
                            HomeTabAction.RefreshRecommend -> {
                                recommendViewModel.clearData()
                                scope.launch(Dispatchers.IO) { recommendViewModel.loadMore() }
                            }

                            HomeTabAction.RefreshPopular -> {
                                popularViewModel.clearData()
                                scope.launch(Dispatchers.IO) { popularViewModel.loadMore() }
                            }

                            HomeTabAction.RefreshRegion -> {
                                homeRegionViewModel.reloadAll(selectedTab)
                            }

                            HomeTabAction.RefreshRanking,
                            null -> {}
                        }
                        navFocusRequester.requestFocus()
                        return@onPreviewKeyEvent true
                    }
                    return@onPreviewKeyEvent false
                },
        ) {
            AnimatedContent(
                targetState = selectedTab,
                label = "home animated content",
                transitionSpec = {
                    val coefficient = 10
                    if (reorderedItems.indexOf(targetState) < reorderedItems.indexOf(initialState)) {
                        fadeIn() + slideInHorizontally { -it / coefficient } togetherWith
                                fadeOut() + slideOutHorizontally { it / coefficient }
                    } else {
                        fadeIn() + slideInHorizontally { it / coefficient } togetherWith
                                fadeOut() + slideOutHorizontally { -it / coefficient }
                    }
                }
            ) { screen ->
                when (screen) {
                    HomeTopNavItem.Recommend -> RecommendScreen()
                    HomeTopNavItem.Popular -> PopularScreen()
                    HomeTopNavItem.Ranking -> HomeRankingScreen(rankingViewModel = rankingViewModel)
                    else -> {
                        val state = homeRegionViewModel.regionStateMap[screen]
                        if (state != null) {
                            UgcRegionScaffold(
                                state = dev.aaa1115910.bv.screen.main.ugc.UgcScaffoldState(
                                    lazyGridState = state.lazyGridState,
                                    ugcType = state.ugcType,
                                    ugcItems = state.items,
                                    nextPage = state.nextPage,
                                    hasMore = state.hasMore,
                                    updating = state.updating
                                ),
                                onLoadMore = { homeRegionViewModel.loadMore(screen) },
                                onAddWatchLater = { },
                                onGoToDetailPage = { aid ->
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
        }
    }
}

private fun HomeTopNavItem.toUgcType() = when (this) {
    HomeTopNavItem.Douga -> dev.aaa1115910.biliapi.entity.ugc.UgcTypeV2.Douga
    HomeTopNavItem.Game -> dev.aaa1115910.biliapi.entity.ugc.UgcTypeV2.Game
    HomeTopNavItem.Kichiku -> dev.aaa1115910.biliapi.entity.ugc.UgcTypeV2.Kichiku
    HomeTopNavItem.Music -> dev.aaa1115910.biliapi.entity.ugc.UgcTypeV2.Music
    HomeTopNavItem.Dance -> dev.aaa1115910.biliapi.entity.ugc.UgcTypeV2.Dance
    HomeTopNavItem.Cinephile -> dev.aaa1115910.biliapi.entity.ugc.UgcTypeV2.Cinephile
    HomeTopNavItem.Ent -> dev.aaa1115910.biliapi.entity.ugc.UgcTypeV2.Ent
    HomeTopNavItem.Knowledge -> dev.aaa1115910.biliapi.entity.ugc.UgcTypeV2.Knowledge
    HomeTopNavItem.Tech -> dev.aaa1115910.biliapi.entity.ugc.UgcTypeV2.Tech
    HomeTopNavItem.Information -> dev.aaa1115910.biliapi.entity.ugc.UgcTypeV2.Information
    HomeTopNavItem.Food -> dev.aaa1115910.biliapi.entity.ugc.UgcTypeV2.Food
    HomeTopNavItem.Life -> dev.aaa1115910.biliapi.entity.ugc.UgcTypeV2.LifeJoy
    HomeTopNavItem.Car -> dev.aaa1115910.biliapi.entity.ugc.UgcTypeV2.Car
    HomeTopNavItem.Fashion -> dev.aaa1115910.biliapi.entity.ugc.UgcTypeV2.Fashion
    HomeTopNavItem.Sports -> dev.aaa1115910.biliapi.entity.ugc.UgcTypeV2.Sports
    HomeTopNavItem.Animal -> dev.aaa1115910.biliapi.entity.ugc.UgcTypeV2.Animal
    HomeTopNavItem.Recommend,
    HomeTopNavItem.Popular,
    HomeTopNavItem.Ranking -> dev.aaa1115910.biliapi.entity.ugc.UgcTypeV2.Douga
}

enum class HomeTabAction {
    RefreshRecommend,
    RefreshPopular,
    RefreshRanking,
    RefreshRegion
}

fun resolveHomeTopNavOrder(firstTab: HomeTopNavItem): List<HomeTopNavItem> {
    val stableHead = listOf(HomeTopNavItem.Recommend, HomeTopNavItem.Popular, HomeTopNavItem.Ranking)
    if (firstTab in stableHead) return stableHead + HomeTopNavItem.entries.filterNot { it in stableHead }

    val allItems = HomeTopNavItem.entries
    val startIndex = allItems.indexOf(firstTab)
    return if (startIndex == -1) stableHead else {
        allItems.drop(startIndex) + allItems.take(startIndex)
    }
}

fun HomeTopNavItem.toClickAction(): HomeTabAction {
    return when (this) {
        HomeTopNavItem.Recommend -> HomeTabAction.RefreshRecommend
        HomeTopNavItem.Popular -> HomeTabAction.RefreshPopular
        HomeTopNavItem.Ranking -> HomeTabAction.RefreshRanking
        else -> HomeTabAction.RefreshRegion
    }
}

fun resolveHomeMenuAction(tab: HomeTopNavItem): HomeTabAction? {
    return when (tab) {
        HomeTopNavItem.Ranking -> null
        else -> tab.toClickAction()
    }
}
