package dev.aaa1115910.bv.component.controllers.playermenu

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.aaa1115910.bv.component.controllers.LocalMenuFocusStateData
import dev.aaa1115910.bv.component.controllers.MenuFocusState
import dev.aaa1115910.bv.component.controllers.playermenu.component.RadioMenuList

internal fun resolvePlayerStatsMenuSelectedIndex(showPlayerStats: Boolean): Int {
    return if (showPlayerStats) 1 else 0
}

@Composable
fun PlayerStatsMenuList(
    modifier: Modifier = Modifier,
    currentShowPlayerStats: Boolean,
    onShowPlayerStatsChange: (Boolean) -> Unit,
    onFocusStateChange: (MenuFocusState) -> Unit
) {
    val focusState = LocalMenuFocusStateData.current
    RadioMenuList(
        modifier = modifier.padding(horizontal = 8.dp),
        items = listOf("关闭", "开启"),
        selected = resolvePlayerStatsMenuSelectedIndex(currentShowPlayerStats),
        requestFocusWhen = focusState.focusState != MenuFocusState.MenuNav,
        onSelectedChanged = { onShowPlayerStatsChange(it == 1) },
        onFocusBackToParent = { onFocusStateChange(MenuFocusState.MenuNav) }
    )
}
