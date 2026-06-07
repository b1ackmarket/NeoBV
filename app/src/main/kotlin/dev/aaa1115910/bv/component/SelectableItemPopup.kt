package dev.aaa1115910.bv.component

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import dev.aaa1115910.bv.util.requestFocus
import dev.aaa1115910.bv.util.touchClick

internal const val SelectableItemPopupColumns = 4
internal const val SelectableItemPopupWidthFraction = 0.68f

@Composable
fun <T> SelectableItemPopup(
    show: Boolean,
    title: String,
    items: List<T>,
    selectedItem: T?,
    label: (T) -> String,
    onDismiss: () -> Unit,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!show) return

    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    val focusRequester = remember { FocusRequester() }
    val maxHeight = with(density) { (windowInfo.containerSize.height * 0.72f).toDp() }
    val focusTargetIndex = items.indexOfFirst { it == selectedItem }.takeIf { it >= 0 } ?: 0

    LaunchedEffect(show, items.size, selectedItem) {
        if (!show || items.isEmpty()) return@LaunchedEffect
        gridState.scrollToItem(focusTargetIndex)
        focusRequester.requestFocus(scope)
    }

    Popup(
        alignment = Alignment.Center,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .focusRequester(focusRequester)
                .onPreviewKeyEvent {
                    if (it.key == Key.Back || it.key == Key.Menu) {
                        if (it.type == KeyEventType.KeyUp) onDismiss()
                        return@onPreviewKeyEvent true
                    }
                    false
                }
                .pointerInput(onDismiss) {
                    detectTapGestures(onTap = { onDismiss() })
                },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(SelectableItemPopupWidthFraction)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {})
                    },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge
                    )
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(SelectableItemPopupColumns),
                        modifier = Modifier.heightIn(max = maxHeight),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(
                            items = items
                        ) { index, item ->
                            val selected = item == selectedItem
                            val select = {
                                onSelect(item)
                                onDismiss()
                            }
                            OutlinedButton(
                                modifier = (if (index == focusTargetIndex) {
                                    Modifier.focusRequester(focusRequester)
                                } else {
                                    Modifier
                                }).touchClick(select),
                                onClick = select
                            ) {
                                Text(
                                    modifier = Modifier.basicMarquee(),
                                    text = label(item),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Clip
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
