package dev.aaa1115910.bv.screen.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.LocalMovies
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import coil.compose.AsyncImage
import dev.aaa1115910.bv.ui.theme.BVTheme
import dev.aaa1115910.bv.util.isDpadRight
import dev.aaa1115910.bv.util.isKeyDown

@Composable
fun LeftNaviContent(
    modifier: Modifier = Modifier,
    isLogin: Boolean = false,
    avatar: String = "",
    selectedItem: LeftNaviItem,
    onLeftNaviItemChanged: (LeftNaviItem) -> Unit,
    onOpenSettings: () -> Unit,
    onShowUserPanel: () -> Unit,
    onFocusToContent: () -> Unit,
    onLogin: () -> Unit
) {
    val railItems = remember {
        listOf(
            LeftNaviItem.Search,
            LeftNaviItem.Personal,
            LeftNaviItem.Home,
            LeftNaviItem.Dynamic,
            LeftNaviItem.PGC,
            LeftNaviItem.Live,
        )
    }
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val useScrollableRail = remember(screenHeightDp, railItems.size) {
        LeftNaviLayoutPolicy.shouldUseScrollableRail(
            screenHeightDp = screenHeightDp,
            itemCount = railItems.size + 2
        )
    }

    NavigationRail(
        modifier = modifier
            .fillMaxHeight()
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.isDpadRight()) {
                    if (keyEvent.isKeyDown()) {
                        onFocusToContent()
                        return@onPreviewKeyEvent true
                    }
                }
                false
            },
        containerColor = Color.White.copy(alpha = 0.05f),
    ) {
        var userIsFocused by remember { mutableStateOf(false) }
        NavigationRailItem(
            modifier = Modifier.onFocusChanged {
                userIsFocused = it.hasFocus
            },
            onClick = {
                if (isLogin) {
                    onShowUserPanel()
                } else {
                    onLogin()
                }
            },
            selected = userIsFocused,
            icon = {
                if (isLogin) {
                    Surface(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        colors = SurfaceDefaults.colors(
                            containerColor = Color.Gray
                        )
                    ) {
                        AsyncImage(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            model = avatar,
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        )
        if (useScrollableRail) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Top
            ) {
                railItems.forEach { item ->
                    RailItem(
                        item = item,
                        selectedItem = selectedItem,
                        onLeftNaviItemChanged = onLeftNaviItemChanged
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                railItems.forEach { item ->
                    RailItem(
                        item = item,
                        selectedItem = selectedItem,
                        onLeftNaviItemChanged = onLeftNaviItemChanged
                    )
                }
            }
        }
        var settingsIsFocused by remember { mutableStateOf(false) }
        NavigationRailItem(
            modifier = Modifier.onFocusChanged {
                settingsIsFocused = it.hasFocus
            },
            onClick = onOpenSettings,
            selected = settingsIsFocused,
            icon = {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
        )
    }
}

enum class LeftNaviItem(
    val displayIcon: ImageVector,
    val displayName: String,
    val displayIconSizeDp: Int = 24
) {
    Search(displayIcon = Icons.Default.Search, displayName = "搜索"),
    Personal(displayIcon = Icons.Default.Person, displayName = "个人"),
    Home(displayIcon = Icons.Default.Home, displayName = "主页"),
    Dynamic(displayIcon = Icons.Default.Autorenew, displayName = "动态"),
    PGC(displayIcon = Icons.Default.LocalMovies, displayName = "影视", displayIconSizeDp = 22),
    Live(displayIcon = Icons.Default.LiveTv, displayName = "直播"),
}

object LeftNaviLayoutPolicy {
    private const val compactScreenHeightThresholdDp = 520

    fun shouldUseScrollableRail(
        screenHeightDp: Int,
        itemCount: Int = 8
    ): Boolean {
        return screenHeightDp in 1 until compactScreenHeightThresholdDp && itemCount >= 8
    }

    fun iconSizeDp(item: LeftNaviItem): Int = item.displayIconSizeDp
}

@Composable
private fun RailItem(
    item: LeftNaviItem,
    selectedItem: LeftNaviItem,
    onLeftNaviItemChanged: (LeftNaviItem) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val indicatorColor by animateColorAsState(
        targetValue = if (item == selectedItem) {
            MaterialTheme.colorScheme.border
        } else {
            Color.Transparent
        },
        label = "selectionIndicatorColor"
    )

    NavigationRailItem(
        modifier = Modifier
            .onFocusChanged { isFocused = it.hasFocus }
            .selectionIndicator(indicatorColor),
        onClick = { onLeftNaviItemChanged(item) },
        selected = isFocused,
        icon = {
            Icon(
                imageVector = item.displayIcon,
                contentDescription = null,
                modifier = Modifier.size(LeftNaviLayoutPolicy.iconSizeDp(item).dp)
            )
        }
    )
}

fun Modifier.selectionIndicator(color: Color): Modifier {
    return this.drawBehind {
        val strokeWidth = 4.dp.toPx()
        drawRect(
            color = color,
            topLeft = Offset.Zero,
            size = Size(width = strokeWidth, height = size.height)
        )
    }
}

@Preview(device = "id:tv_1080p")
@Composable
private fun LeftNaviContentPreview() {
    BVTheme {
        LeftNaviContent(
            selectedItem = LeftNaviItem.Home,
            onLeftNaviItemChanged = {},
            onOpenSettings = {},
            onShowUserPanel = {},
            onFocusToContent = {},
            onLogin = {},
        )
    }
}
