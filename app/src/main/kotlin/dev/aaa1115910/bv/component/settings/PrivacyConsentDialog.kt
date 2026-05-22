package dev.aaa1115910.bv.component.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.focusable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import dev.aaa1115910.bv.telemetry.TelemetryConsentText
import dev.aaa1115910.bv.tv.component.TvAlertDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PrivacyConsentDialog(
    onAccept: () -> Unit,
    onDismissRequest: () -> Unit = {}
) {
    TvAlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = TelemetryConsentText.title) },
        text = {
            PrivacyPolicyText(requestInitialFocus = true)
        },
        confirmButton = {
            Button(onClick = onAccept) {
                Text(text = "同意并继续")
            }
        }
    )
}

@Composable
fun PrivacyPolicyDialog(
    onDismissRequest: () -> Unit
) {
    TvAlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = TelemetryConsentText.title) },
        text = {
            PrivacyPolicyText()
        },
        confirmButton = {
            OutlinedButton(onClick = onDismissRequest) {
                Text(text = "关闭")
            }
        }
    )
}

@Composable
private fun PrivacyPolicyText(
    requestInitialFocus: Boolean = false
) {
    val scrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val scrollStepPx = remember(density) { with(density) { 96.dp.roundToPx() } }
    var focused by remember { mutableStateOf(false) }

    LaunchedEffect(requestInitialFocus) {
        if (requestInitialFocus) {
            delay(120)
            runCatching { focusRequester.requestFocus() }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 360.dp)
            .clip(MaterialTheme.shapes.small)
            .border(
                border = BorderStroke(
                    width = 1.dp,
                    color = if (focused) {
                        MaterialTheme.colorScheme.border
                    } else {
                        Color.White.copy(alpha = 0.18f)
                    }
                ),
                shape = MaterialTheme.shapes.small
            )
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionDown -> {
                        if (scrollState.value >= scrollState.maxValue) {
                            false
                        } else {
                            scope.launch {
                                scrollState.animateScrollTo(
                                    (scrollState.value + scrollStepPx)
                                        .coerceAtMost(scrollState.maxValue)
                                )
                            }
                            true
                        }
                    }

                    Key.DirectionUp -> {
                        if (scrollState.value <= 0) {
                            false
                        } else {
                            scope.launch {
                                scrollState.animateScrollTo(
                                    (scrollState.value - scrollStepPx).coerceAtLeast(0)
                                )
                            }
                            true
                        }
                    }

                    else -> false
                }
            }
            .focusable()
            .verticalScroll(scrollState)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TelemetryConsentText.sections.forEach { section ->
            Text(text = section)
        }
    }
}
