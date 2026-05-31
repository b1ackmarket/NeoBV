package dev.aaa1115910.bv.util

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

fun Modifier.touchClick(onClick: () -> Unit): Modifier = pointerInput(onClick) {
    detectTapGestures(onTap = { onClick() })
}

fun Modifier.touchLongClick(
    onClick: () -> Unit,
    onLongClick: () -> Unit
): Modifier = pointerInput(onClick, onLongClick) {
    detectTapGestures(
        onTap = { onClick() },
        onLongPress = { onLongClick() }
    )
}
