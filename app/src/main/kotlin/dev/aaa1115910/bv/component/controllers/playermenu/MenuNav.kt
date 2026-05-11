package dev.aaa1115910.bv.component.controllers.playermenu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.aaa1115910.bv.component.controllers.VideoPlayerMenuNavItem
import dev.aaa1115910.bv.component.controllers.playermenu.component.MenuListItem
import dev.aaa1115910.bv.component.ifElse

@Composable
fun MenuNavList(
    modifier: Modifier = Modifier,
    focusedMenu: VideoPlayerMenuNavItem,
    selectedMenu: VideoPlayerMenuNavItem,
    onSelectedChanged: (VideoPlayerMenuNavItem) -> Unit,
    onItemClick: (VideoPlayerMenuNavItem) -> Unit,
    isFocusing: Boolean
) {
    val context = LocalContext.current
    val restorerFocusRequester = remember { FocusRequester() }
    val focusRequester = remember { FocusRequester() }
    val itemRequesters = remember {
        mutableStateListOf<FocusRequester>().apply {
            addAll(VideoPlayerMenuNavItem.entries.map { FocusRequester() })
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(isFocusing) {
        if (isFocusing) {
            itemRequesters[focusedMenu.ordinal].requestFocus()
        }
    }

    LazyColumn(
        modifier = modifier
            .focusRestorer(restorerFocusRequester)
            .focusRequester(focusRequester),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        itemsIndexed(VideoPlayerMenuNavItem.entries) { index, item ->
            MenuListItem(
                modifier = Modifier
                    .ifElse(index == 0, Modifier.focusRequester(restorerFocusRequester))
                    .focusRequester(itemRequesters[index]),
                text = item.getDisplayName(context),
                icon = item.icon,
                expanded = isFocusing,
                selected = if (isFocusing) focusedMenu == item else selectedMenu == item,
                onClick = { onItemClick(item) },
                onFocus = { onSelectedChanged(item) },
            )
        }
    }
}
