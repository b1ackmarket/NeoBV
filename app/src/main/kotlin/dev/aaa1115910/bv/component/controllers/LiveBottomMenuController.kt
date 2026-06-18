package dev.aaa1115910.bv.component.controllers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.aaa1115910.bv.util.LiveBottomOsdControl
import dev.aaa1115910.bv.util.touchClick
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.input.InputMode

data class LiveBottomMenuItem(
    val control: LiveBottomOsdControl,
    val iconRes: Int,
    val label: String,
    val onClick: () -> Unit
)

@Composable
fun LiveBottomMenuController(
    modifier: Modifier = Modifier,
    show: Boolean,
    items: List<LiveBottomMenuItem>,
    quality: String? = null,
    onDismiss: () -> Unit
) {
    val itemFocusRequesters = remember(items.size) {
        List(items.size) { FocusRequester() }
    }

    val inputModeManager = LocalInputModeManager.current

    LaunchedEffect(show) {
        if (show && items.isNotEmpty()) {
            delay(80)
            if (inputModeManager.inputMode != InputMode.Touch) {
                runCatching { itemFocusRequesters.firstOrNull()?.requestFocus() }
            }
        }
    }

    AnimatedVisibility(
        visible = show && items.isNotEmpty(),
        modifier = modifier,
        enter = fadeIn() + slideInVertically { it },
        exit = fadeOut() + slideOutVertically { it }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.52f))
                .padding(horizontal = 24.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.Start),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    val clickItem = item.onClick
                    Surface(
                        modifier = Modifier
                            .focusRequester(itemFocusRequesters[index])
                            .onPreviewKeyEvent {
                                if (it.type == KeyEventType.KeyDown && it.key == Key.DirectionUp) {
                                    onDismiss()
                                    return@onPreviewKeyEvent true
                                }
                                false
                            }
                            .onKeyEvent {
                                if (it.type == KeyEventType.KeyUp) {
                                    if (it.key == Key.DirectionLeft || it.key == Key.DirectionRight) return@onKeyEvent true
                                    return@onKeyEvent false
                                }
                                when (it.key) {
                                    Key.DirectionLeft -> {
                                        if (index == 0) {
                                            itemFocusRequesters.lastOrNull()?.requestFocus()
                                            true
                                        } else false
                                    }
                                    Key.DirectionRight -> {
                                        if (index == items.lastIndex) {
                                            itemFocusRequesters.firstOrNull()?.requestFocus()
                                            true
                                        } else false
                                    }
                                    else -> false
                                }
                            }
                            .touchClick(clickItem),
                        onClick = clickItem
                     ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                modifier = Modifier.size(24.dp),
                                painter = painterResource(id = item.iconRes),
                                contentDescription = item.label
                            )
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            if (!quality.isNullOrBlank()) {
                val formattedQuality = quality
                    .replace("（", "\n（")
                    .replace("(", "\n(")
                Text(
                    text = formattedQuality,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}
