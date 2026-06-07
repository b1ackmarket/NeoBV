package dev.aaa1115910.bv.component.controllers.playermenu.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ArrowDropUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import dev.aaa1115910.bv.util.touchClick

@Composable
fun StepLessMenuItem(
    modifier: Modifier = Modifier,
    value: Float = 1f,
    text: String,
    step: Float = 0.01f,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
    requestFocusWhen: Boolean = false,
    stepValueResolver: ((currentValue: Float, direction: Int) -> Float)? = null,
    onValueChange: (Float) -> Unit,
    onFocusBackToParent: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val increaseValue = {
        val nextValue = stepValueResolver?.invoke(value, 1) ?: if (value >= range.endInclusive - step) {
            range.endInclusive
        } else {
            value + step
        }
        onValueChange(nextValue)
    }
    val decreaseValue = {
        val nextValue = stepValueResolver?.invoke(value, -1) ?: if (value - step <= range.start) {
            range.start
        } else {
            value - step
        }
        onValueChange(nextValue)
    }
    LaunchedEffect(requestFocusWhen, text) {
        if (requestFocusWhen) {
            focusRequester.requestFocus()
        }
    }
    Box(
        modifier = modifier
            .fillMaxHeight()
            .onPreviewKeyEvent {
                println(it)
                if (it.type == KeyEventType.KeyUp) return@onPreviewKeyEvent true
                if (it.key == Key.DirectionRight) onFocusBackToParent()
                false
            }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .touchClick(increaseValue),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowDropUp,
                    contentDescription = null
                )
            }
            MenuListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onPreviewKeyEvent {
                        when (it.key) {
                            Key.DirectionUp -> {
                                if (it.type == KeyEventType.KeyUp) return@onPreviewKeyEvent true
                                increaseValue()
                                return@onPreviewKeyEvent true
                            }

                            Key.DirectionDown -> {
                                if (it.type == KeyEventType.KeyUp) return@onPreviewKeyEvent true
                                decreaseValue()
                                return@onPreviewKeyEvent true
                            }
                        }
                        false
                    },
                text = text,
                selected = false
            ) { }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .touchClick(decreaseValue),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowDropDown,
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
fun StepLessMenuItem(
    modifier: Modifier = Modifier,
    value: Int = 100,
    text: String,
    step: Int = 1,
    range: IntRange = 0..100,
    requestFocusWhen: Boolean = false,
    onValueChange: (Int) -> Unit,
    onFocusBackToParent: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val increaseValue = {
        if (value >= range.last - step) {
            onValueChange(range.last)
        } else {
            onValueChange(value + step)
        }
    }
    val decreaseValue = {
        if (value - step <= range.first) {
            onValueChange(range.first)
        } else {
            onValueChange(value - step)
        }
    }
    LaunchedEffect(requestFocusWhen, text) {
        if (requestFocusWhen) {
            focusRequester.requestFocus()
        }
    }
    Box(
        modifier = modifier
            .fillMaxHeight()
            .onPreviewKeyEvent {
                println(it)
                if (it.type == KeyEventType.KeyUp) return@onPreviewKeyEvent true
                if (it.key == Key.DirectionRight) onFocusBackToParent()
                false
            }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .touchClick(increaseValue),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowDropUp,
                    contentDescription = null
                )
            }
            MenuListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onPreviewKeyEvent {
                        when (it.key) {
                            Key.DirectionUp -> {
                                if (it.type == KeyEventType.KeyUp) return@onPreviewKeyEvent true
                                increaseValue()
                                return@onPreviewKeyEvent true
                            }

                            Key.DirectionDown -> {
                                if (it.type == KeyEventType.KeyUp) return@onPreviewKeyEvent true
                                decreaseValue()
                                return@onPreviewKeyEvent true
                            }
                        }
                        false
                    },
                text = text,
                selected = false
            ) { }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .touchClick(decreaseValue),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowDropDown,
                    contentDescription = null
                )
            }
        }
    }
}
