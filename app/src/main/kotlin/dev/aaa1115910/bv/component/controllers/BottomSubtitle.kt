package dev.aaa1115910.bv.component.controllers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.aaa1115910.bilisubtitle.entity.SubtitleItem
import dev.aaa1115910.bv.BuildConfig

internal fun resolveSubtitleBottomPadding(
    basePadding: Dp,
    liftForBottomController: Boolean
): Dp {
    return basePadding + if (liftForBottomController) 96.dp else 0.dp
}

@Composable
fun BottomSubtitle(
    modifier: Modifier = Modifier,
    subtitleData: List<SubtitleItem>,
    secondarySubtitleData: List<SubtitleItem> = emptyList(),
    currentTime: Long,
    fontSize: TextUnit,
    opacity: Float,
    padding: Dp,
) {
    var currentText by remember { mutableStateOf("") }
    var currentSecondaryText by remember { mutableStateOf("") }

    val updateCurrentText: () -> Unit = {
        runCatching {
            currentText = subtitleData.find { it.isShowing(currentTime) }?.content
                ?: if (BuildConfig.DEBUG) "【DEBUG】无内容" else ""
            currentSecondaryText = secondarySubtitleData.find { it.isShowing(currentTime) }?.content.orEmpty()
        }
    }

    LaunchedEffect(subtitleData, secondarySubtitleData, currentTime) {
        updateCurrentText()
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        if (currentText != "" || currentSecondaryText != "") {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = padding)
                    .clip(MaterialTheme.shapes.small)
                    .background(Color.Black.copy(alpha = opacity))
                    .padding(vertical = 4.dp, horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (currentText != "") {
                    Text(
                        text = currentText,
                        fontSize = fontSize,
                        textAlign = TextAlign.Center
                    )
                }
                if (currentSecondaryText != "") {
                    Text(
                        text = currentSecondaryText,
                        fontSize = fontSize * 0.78f,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
