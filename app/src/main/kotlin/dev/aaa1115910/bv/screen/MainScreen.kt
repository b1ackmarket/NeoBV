package dev.aaa1115910.bv.screen

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.DrawerValue
import androidx.tv.material3.NavigationDrawer
import androidx.tv.material3.rememberDrawerState
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.activities.settings.SettingsActivity
import dev.aaa1115910.bv.activities.user.LoginActivity
import dev.aaa1115910.bv.activities.user.UserSwitchActivity
import dev.aaa1115910.bv.component.UserPanel
import dev.aaa1115910.bv.screen.main.HomeContent
import dev.aaa1115910.bv.screen.main.LeftNaviContent
import dev.aaa1115910.bv.screen.main.LeftNaviItem
import dev.aaa1115910.bv.screen.main.PersonalContent
import dev.aaa1115910.bv.screen.main.PgcContent
import dev.aaa1115910.bv.screen.main.home.DynamicsScreen
import dev.aaa1115910.bv.screen.main.live.LiveContent
import dev.aaa1115910.bv.screen.search.SearchInputScreen
import dev.aaa1115910.bv.telemetry.FirebaseTelemetry
import dev.aaa1115910.bv.telemetry.TelemetryScreen
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.fException
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.viewmodel.UserViewModel
import dev.aaa1115910.bv.viewmodel.home.DynamicViewModel
import dev.aaa1115910.bv.viewmodel.live.LiveViewModel
import dev.aaa1115910.bv.viewmodel.player.HomeEntryResolver
import dev.aaa1115910.bv.viewmodel.player.toHomeStartDestination
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.androidx.compose.koinViewModel

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    userViewModel: UserViewModel = koinViewModel(),
    dynamicViewModel: DynamicViewModel = koinViewModel(),
    liveViewModel: LiveViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val logger = KotlinLogging.logger("MainScreen")
    var showUserPanel by remember { mutableStateOf(false) }
    var lastPressBack: Long by remember { mutableLongStateOf(0L) }
    var selectedDrawerItem by remember {
        mutableStateOf(
            HomeEntryResolver.resolve(Prefs.firstHomeTopNavItem.toHomeStartDestination()).leftNaviItem
                .takeIf { Prefs.homeLeftNaviItem == LeftNaviItem.Home } ?: Prefs.homeLeftNaviItem
        )
    }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val personalFocusRequester = remember { FocusRequester() }
    val mainFocusRequester = remember { FocusRequester() }
    val pgcFocusRequester = remember { FocusRequester() }
    val searchFocusRequester = remember { FocusRequester() }
    val dynamicFocusRequester = remember { FocusRequester() }
    val liveFocusRequester = remember { FocusRequester() }

    val handleBack = {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastPressBack < 1000 * 3) {
            logger.fInfo { "Exiting bug video" }
            (context as Activity).finish()
        } else {
            lastPressBack = currentTime
            R.string.home_press_back_again_to_exit.toast(context)
        }
    }

    val onFocusToContent: () -> Unit = {
        when (selectedDrawerItem) {
            LeftNaviItem.Home -> mainFocusRequester.requestFocus()
            LeftNaviItem.PGC -> pgcFocusRequester.requestFocus()
            LeftNaviItem.Search -> searchFocusRequester.requestFocus()
            LeftNaviItem.Personal -> personalFocusRequester.requestFocus()
            LeftNaviItem.Dynamic -> dynamicFocusRequester.requestFocus()
            LeftNaviItem.Live -> liveFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(Unit) {
        runCatching {
            onFocusToContent()
        }.onFailure {
            logger.fException(it) { "request default focus requester failed" }
        }
    }

    LaunchedEffect(selectedDrawerItem) {
        FirebaseTelemetry.setLastScreen(
            when (selectedDrawerItem) {
                LeftNaviItem.Home -> TelemetryScreen.Home
                LeftNaviItem.PGC -> TelemetryScreen.Pgc
                LeftNaviItem.Search -> TelemetryScreen.Search
                LeftNaviItem.Personal -> TelemetryScreen.Personal
                LeftNaviItem.Dynamic -> TelemetryScreen.Dynamic
                LeftNaviItem.Live -> TelemetryScreen.Live
            }
        )
    }

    LaunchedEffect(userViewModel.isLogin) {
        dynamicViewModel.onLoginStateChanged(userViewModel.isLogin)
        liveViewModel.onLoginStateChanged(userViewModel.isLogin)
    }

    BackHandler {
        handleBack()
    }

    NavigationDrawer(
        modifier = modifier,
        drawerContent = {
            LeftNaviContent(
                isLogin = userViewModel.isLogin,
                avatar = userViewModel.face,
                //avatar = "https://i2.hdslb.com/bfs/face/ef0457addb24141e15dfac6fbf45293ccf1e32ab.jpg",
                //username = "碧诗",
                selectedItem = selectedDrawerItem,
                onLeftNaviItemChanged = { selectedDrawerItem = it },
                onOpenSettings = {
                    context.startActivity(Intent(context, SettingsActivity::class.java))
                },
                onFocusToContent = onFocusToContent,
                onShowUserPanel = {
                    showUserPanel = true
                },
                onLogin = {
                    context.startActivity(Intent(context, LoginActivity::class.java))
                }
            )
        },
        drawerState = drawerState
    ) {
        Box(
            modifier = Modifier
        ) {
            AnimatedContent(
                targetState = selectedDrawerItem,
                label = "main animated content",
                transitionSpec = {
                    val coefficient = 20
                    if (targetState.ordinal < initialState.ordinal) {
                        fadeIn() + slideInVertically { -it / coefficient } togetherWith
                                fadeOut() + slideOutVertically { it / coefficient }
                    } else {
                        fadeIn() + slideInVertically { it / coefficient } togetherWith
                                fadeOut() + slideOutVertically { -it / coefficient }
                    }
                }
            ) { screen ->
                when (screen) {
                    LeftNaviItem.Search -> SearchInputScreen(defaultFocusRequester = searchFocusRequester)
                    LeftNaviItem.Personal -> PersonalContent(navFocusRequester = personalFocusRequester)
                    LeftNaviItem.Home -> HomeContent(
                        navFocusRequester = mainFocusRequester,
                        startupTab = Prefs.firstHomeTopNavItem.toHomeStartDestination()
                    )
                    LeftNaviItem.Dynamic -> DynamicsScreen(
                        dynamicViewModel = dynamicViewModel,
                        defaultFocusRequester = dynamicFocusRequester
                    )
                    LeftNaviItem.PGC -> PgcContent(navFocusRequester = pgcFocusRequester)
                    LeftNaviItem.Live -> LiveContent(
                        navFocusRequester = liveFocusRequester,
                        onLogin = {
                            context.startActivity(Intent(context, LoginActivity::class.java))
                        },
                        liveViewModel = liveViewModel
                    )
                }
            }

            AnimatedVisibility(
                visible = showUserPanel,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    UserPanel(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(12.dp),
                        username = userViewModel.username,
                        face = userViewModel.face,
                        level = userViewModel.responseData?.level ?: 0,
                        currentExp = userViewModel.responseData?.levelExp?.currentExp ?: 0,
                        nextLevelExp = with(userViewModel.responseData?.levelExp?.nextExp) {
                            if (this == null) {
                                1
                            } else if (this <= 0) {
                                userViewModel.responseData?.levelExp?.currentExp ?: 1
                            } else {
                                (userViewModel.responseData?.levelExp?.currentExp ?: 1)
                                +(userViewModel.responseData?.levelExp?.nextExp ?: 0)
                            }
                        },
                        onHide = { showUserPanel = false },
                        onGoUserSwitch = {
                            context.startActivity(Intent(context, UserSwitchActivity::class.java))
                        }
                    )
                }
            }
        }
    }
}
