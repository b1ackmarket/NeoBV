package dev.aaa1115910.bv.component

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.aaa1115910.bv.util.touchClick

object FilterChipDefaults {
    val RowContentPadding = PaddingValues(vertical = 4.dp)
    val RowSpacing = 12.dp
    val IconSize = 18.dp
    val MinWidth = 0.dp
    val MaxWidth = 184.dp
    val SelectorMaxWidth = 224.dp
    val PopupWidth = 96.dp
    val PopupHorizontalPadding = 8.dp
}

internal fun shortenSelectablePopupLabel(
    label: String,
    maxDisplayWidth: Float = 5.5f,
    ellipsisPrefixWidth: Float = 5f
): String {
    if (maxDisplayWidth <= 0f) return ""
    if (label.displayWidth() <= maxDisplayWidth) return label

    var width = 0f
    val builder = StringBuilder()
    label.forEach { char ->
        val charWidth = char.selectablePopupCharWidth()
        if (width + charWidth > ellipsisPrefixWidth) {
            return builder.append('…').toString()
        }
        builder.append(char)
        width += charWidth
    }
    return builder.toString()
}

private fun String.displayWidth(): Float = fold(0f) { acc, char ->
    acc + char.selectablePopupCharWidth()
}

private fun Char.selectablePopupCharWidth(): Float = when {
    code <= 0x007F -> 0.5f
    else -> 1f
}

@Composable
fun FilterChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    minWidth: Dp = FilterChipDefaults.MinWidth,
    maxWidth: Dp = FilterChipDefaults.MaxWidth,
    horizontalPadding: Dp = 16.dp
) {
    val fixedWidth = minWidth == maxWidth
    Surface(
        modifier = modifier
            .then(
                if (fixedWidth) Modifier.width(maxWidth)
                else Modifier.widthIn(min = minWidth, max = maxWidth)
            )
            .touchClick(onClick),
        onClick = onClick,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.inverseSurface,
            pressedContainerColor = MaterialTheme.colorScheme.inverseSurface,
            focusedContentColor = MaterialTheme.colorScheme.inverseOnSurface,
            pressedContentColor = MaterialTheme.colorScheme.inverseOnSurface
        ),
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.extraLarge),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f)
    ) {
        Row(
            modifier = Modifier
                .then(if (fixedWidth) Modifier.fillMaxWidth() else Modifier)
                .height(32.dp)
                .padding(horizontal = horizontalPadding, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                    imageVector = icon,
                    contentDescription = null
                )
            }
            Text(
                modifier = (if (fixedWidth) Modifier.weight(1f, fill = false) else Modifier)
                    .basicMarquee(),
                text = text,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
