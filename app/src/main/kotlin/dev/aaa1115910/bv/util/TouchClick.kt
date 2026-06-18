package dev.aaa1115910.bv.util

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext

private fun Context.isTv(): Boolean {
    val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
    return uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
}

fun Modifier.touchClick(onClick: () -> Unit): Modifier = composed {
    val context = LocalContext.current
    if (context.isTv()) {
        this.pointerInput(onClick) {
            detectTapGestures(onTap = { onClick() })
        }
    } else {
        val interactionSource = remember { MutableInteractionSource() }
        this.clickable(
            onClick = onClick,
            indication = null,
            interactionSource = interactionSource
        )
    }
}

fun Modifier.touchLongClick(
    onClick: () -> Unit,
    onLongClick: () -> Unit
): Modifier = composed {
    val context = LocalContext.current
    if (context.isTv()) {
        this.pointerInput(onClick, onLongClick) {
            detectTapGestures(
                onTap = { onClick() },
                onLongPress = { onLongClick() }
            )
        }
    } else {
        val interactionSource = remember { MutableInteractionSource() }
        this.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick,
            indication = null,
            interactionSource = interactionSource
        )
    }
}
