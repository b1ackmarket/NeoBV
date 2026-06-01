package dev.aaa1115910.bv.component.controllers.playermenu.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import dev.aaa1115910.bv.component.ifElse

@Composable
fun CheckBoxMenuList(
    modifier: Modifier = Modifier,
    items: List<String>,
    selected: List<Int> = listOf(),
    requestFocusWhen: Boolean = false,
    onSelectedChanged: (indexes: List<Int>) -> Unit,
    onFocusBackToParent: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val itemFocusRequesters = remember(items) {
        mutableStateListOf<FocusRequester>().apply {
            addAll(items.map { FocusRequester() })
        }
    }
    LaunchedEffect(requestFocusWhen, selected, items) {
        if (requestFocusWhen && items.isNotEmpty()) {
            val targetIndex = selected.firstOrNull()
                ?.coerceIn(0, items.lastIndex)
                ?: 0
            itemFocusRequesters.getOrNull(targetIndex)?.requestFocus()
                ?: focusRequester.requestFocus()
        }
    }
    LazyColumn(
        modifier = modifier
            .onPreviewKeyEvent {
                println(it)
                if (it.type == KeyEventType.KeyUp) {
                    if (listOf(Key.Enter, Key.DirectionCenter).contains(it.key)) {
                        return@onPreviewKeyEvent false
                    }
                    return@onPreviewKeyEvent true
                }
                val result = it.key == Key.DirectionRight
                if (result) onFocusBackToParent()
                result
            }
            .focusRestorer(focusRequester),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 120.dp, horizontal = 8.dp)
    ) {
        itemsIndexed(items) { index, item ->
            val selectItem = {
                itemFocusRequesters.getOrNull(index)?.requestFocus()
                val newSelectedIndexes = selected.toMutableList()
                if (newSelectedIndexes.contains(index)) newSelectedIndexes.remove(index)
                else newSelectedIndexes.add(index)
                onSelectedChanged(newSelectedIndexes)
            }
            MenuListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(itemFocusRequesters[index])
                    .ifElse(index == 0, Modifier.focusRequester(focusRequester)),
                text = item,
                selected = selected.contains(index),
                onClick = selectItem
            )
        }
    }
}
