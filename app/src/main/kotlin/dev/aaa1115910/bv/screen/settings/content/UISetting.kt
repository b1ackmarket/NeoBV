package dev.aaa1115910.bv.screen.settings.content

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ArrowDropUp
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.component.HomePageSettingItem
import dev.aaa1115910.bv.component.PersonalTopNavItem
import dev.aaa1115910.bv.component.settings.SettingListItem
import dev.aaa1115910.bv.component.settings.SettingSwitchListItem
import dev.aaa1115910.bv.network.HttpServer
import dev.aaa1115910.bv.screen.main.LeftNaviItem
import dev.aaa1115910.bv.screen.settings.SettingsMenuNavItem
import dev.aaa1115910.bv.ui.theme.BVTheme
import dev.aaa1115910.bv.util.LayoutConfig
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.requestFocus
import dev.aaa1115910.bv.util.touchClick
import dev.aaa1115910.bv.util.resolveDefaultDensity
import kotlin.math.roundToInt

@Composable
fun UISetting(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var showDensityDialog by remember { mutableStateOf(false) }
    var showStartupPageDialog by remember { mutableStateOf(false) }
    var showHomepageDialog by remember { mutableStateOf(false) }
    var showPersonalPageDialog by remember { mutableStateOf(false) }

    var showVideoInfo by remember { mutableStateOf(Prefs.showVideoInfo) }
    var showPersistentSeek by remember { mutableStateOf(Prefs.showPersistentSeek) }
    var showChapterBar by remember { mutableStateOf(Prefs.showChapterBar) }
    var enableFocusPreview by remember { mutableStateOf(Prefs.enableFocusPreview) }
    var enableFocusPreviewMuted by remember { mutableStateOf(Prefs.enableFocusPreviewMuted) }
    var enableLayoutWebConfig by remember { mutableStateOf(Prefs.enableLayoutWebConfig) }

    val density by Prefs.densityFlow.collectAsState(
        resolveDefaultDensity(
            widthPx = context.resources.displayMetrics.widthPixels,
            heightPx = context.resources.displayMetrics.heightPixels
        )
    )
    val densityDialogState = remember { UIDensityDialogState(density) }
    var selectedLeftNavItem by remember { mutableStateOf(Prefs.homeLeftNaviItem) }
    var selectedFirstHomeTopNavItem by remember { mutableStateOf(Prefs.firstHomeTopNavItem) }
    var selectedFirstPersonalTopNavItem by remember { mutableStateOf(Prefs.firstPersonalTopNavItem) }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = SettingsMenuNavItem.UI.getDisplayName(context),
                style = MaterialTheme.typography.displaySmall
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    SettingListItem(
                        title = stringResource(R.string.settings_ui_startup_page_title),
                        supportText = "当前：${selectedLeftNavItem.displayName}",
                        onClick = { showStartupPageDialog = true }
                    )
                }
                if (!enableLayoutWebConfig) {
                    item {
                        SettingListItem(
                            title = stringResource(R.string.settings_ui_homepage_title),
                            supportText = stringResource(R.string.settings_ui_homepage_text),
                            onClick = { showHomepageDialog = true }
                        )
                    }
                    item {
                        SettingListItem(
                            title = stringResource(R.string.settings_ui_personal_page_title),
                            supportText = stringResource(R.string.settings_ui_personal_page_text),
                            onClick = { showPersonalPageDialog = true }
                        )
                    }
                }
                item {
                    SettingSwitchListItem(
                        title = stringResource(R.string.settings_ui_show_video_info_title),
                        supportText = stringResource(R.string.settings_ui_show_video_info_text),
                        checked = showVideoInfo,
                        onCheckedChange = {
                            showVideoInfo = it
                            Prefs.showVideoInfo = it
                        }
                    )
                }
                item {
                    SettingSwitchListItem(
                        title = stringResource(R.string.settings_ui_show_persistent_seek_title),
                        supportText = stringResource(R.string.settings_ui_show_persistent_seek_text),
                        checked = showPersistentSeek,
                        onCheckedChange = {
                            showPersistentSeek = it
                            Prefs.showPersistentSeek = it
                        }
                    )
                }
                item {
                    SettingSwitchListItem(
                        title = stringResource(R.string.settings_ui_show_chapter_bar_title),
                        supportText = stringResource(R.string.settings_ui_show_chapter_bar_text),
                        checked = showChapterBar,
                        onCheckedChange = {
                            showChapterBar = it
                            Prefs.showChapterBar = it
                        }
                    )
                }
                item {
                    SettingSwitchListItem(
                        title = "焦点视频自动预览",
                        supportText = "视频卡片获得焦点 1 秒后预览，预览不会计入播放历史",
                        checked = enableFocusPreview,
                        onCheckedChange = {
                            enableFocusPreview = it
                            Prefs.enableFocusPreview = it
                        }
                    )
                }
                item {
                    SettingSwitchListItem(
                        title = "预览静音",
                        supportText = "开启后焦点预览默认静音",
                        checked = enableFocusPreviewMuted,
                        onCheckedChange = {
                            enableFocusPreviewMuted = it
                            Prefs.enableFocusPreviewMuted = it
                        }
                    )
                }
                item {
                    SettingSwitchListItem(
                        title = "布局自定义",
                        supportText = "开启后可到 ${HttpServer.getServerAddress("/layout")} 调整各页面 tab 顺序与隐藏；关闭后恢复默认布局，置顶设置保留",
                        checked = enableLayoutWebConfig,
                        onCheckedChange = {
                            enableLayoutWebConfig = it
                            Prefs.enableLayoutWebConfig = it
                            if (!it) {
                                LayoutConfig.reset()
                            }
                        }
                    )
                }
                item {
                    SettingListItem(
                        title = stringResource(R.string.settings_ui_density_title),
                        supportText = stringResource(R.string.settings_ui_density_text),
                        onClick = {
                            densityDialogState.open(density)
                            showDensityDialog = true
                        }
                    )
                }
            }
        }
    }

    UIDensityDialog(
        show = showDensityDialog,
        onHideDialog = { showDensityDialog = false },
        densityState = densityDialogState,
        onDensityChange = { Prefs.density = it }
    )

    if (showStartupPageDialog) {
        OptionDialog(
            options = LeftNaviItem.entries.toTypedArray(),
            selectedOption = selectedLeftNavItem,
            onDismiss = { showStartupPageDialog = false },
            onSelect = {
                Prefs.homeLeftNaviItem = it
                selectedLeftNavItem = it
            },
            getDisplayName = { it.displayName }
        )
    }

    if (showHomepageDialog) {
        OptionDialog(
            options = HomePageSettingItem.entries.toTypedArray(),
            selectedOption = selectedFirstHomeTopNavItem,
            onDismiss = { showHomepageDialog = false },
            onSelect = {
                Prefs.firstHomeTopNavItem = it
                selectedFirstHomeTopNavItem = it
            },
            getDisplayName = { it.getDisplayName(context) }
        )
    }

    if (showPersonalPageDialog) {
        OptionDialog(
            options = PersonalTopNavItem.entries.toTypedArray(),
            selectedOption = selectedFirstPersonalTopNavItem,
            onDismiss = { showPersonalPageDialog = false },
            onSelect = {
                Prefs.firstPersonalTopNavItem = it
                selectedFirstPersonalTopNavItem = it
            },
            getDisplayName = { it.getDisplayName(context) }
        )
    }
}

internal class UIDensityDialogState(initialDensity: Float) {
    var displayDensity by mutableFloatStateOf(sanitizeDensity(initialDensity))
        private set

    fun open(density: Float) {
        displayDensity = sanitizeDensity(density)
    }

    fun step(direction: Int): Float {
        displayDensity = sanitizeDensity(displayDensity + DENSITY_STEP * direction)
        return displayDensity
    }

    private fun sanitizeDensity(density: Float): Float {
        return (density * 10)
            .roundToInt()
            .div(10f)
            .coerceIn(MIN_DENSITY, MAX_DENSITY)
    }
}

private const val MIN_DENSITY = 0.5f
private const val MAX_DENSITY = 5f
private const val DENSITY_STEP = 0.1f

@Composable
private fun UIDensityDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    onHideDialog: () -> Unit,
    densityState: UIDensityDialogState,
    onDensityChange: (Float) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val defaultDensity by remember {
        mutableFloatStateOf(
            resolveDefaultDensity(
                widthPx = context.resources.displayMetrics.widthPixels,
                heightPx = context.resources.displayMetrics.heightPixels
            )
        )
    }

    LaunchedEffect(show) {
        if (show) {
            focusRequester.requestFocus(scope)
        }
    }

    // 这里得采用固定的 Density，否则会导致更改 Density 时，对话框反复重新加载
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = defaultDensity,
            fontScale = LocalDensity.current.fontScale
        )
    ) {
        if (show) {
            AlertDialog(
                modifier = modifier,
                onDismissRequest = { onHideDialog() },
                title = { Text(text = stringResource(R.string.settings_ui_density_title)) },
                text = {
                    Column(
                        modifier = Modifier
                            .focusRequester(focusRequester)
                            .focusable()
                            .fillMaxWidth()
                            .onPreviewKeyEvent {
                                if (it.key == Key.DirectionUp || it.key == Key.DirectionDown) {
                                    if (it.type == KeyEventType.KeyDown) {
                                        val newDensity = densityState.step(
                                            direction = if (it.key == Key.DirectionUp) 1 else -1
                                        )
                                        onDensityChange(newDensity)
                                    }
                                    return@onPreviewKeyEvent true
                                }
                                false
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .touchClick {
                                    onDensityChange(densityState.step(direction = 1))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Rounded.ArrowDropUp, contentDescription = null)
                        }
                        Text(text = "${densityState.displayDensity}")
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .touchClick {
                                    onDensityChange(densityState.step(direction = -1))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Rounded.ArrowDropDown, contentDescription = null)
                        }
                    }
                },
                confirmButton = {}
            )
        }
    }
}

@Preview
@Composable
fun UIDensityDialogPreview() {
    val show by remember { mutableStateOf(true) }
    var density by remember { mutableFloatStateOf(1.0f) }
    val densityState = remember { UIDensityDialogState(density) }

    BVTheme {
        UIDensityDialog(
            show = show,
            onHideDialog = {},
            densityState = densityState,
            onDensityChange = { density = it }
        )
    }
}
