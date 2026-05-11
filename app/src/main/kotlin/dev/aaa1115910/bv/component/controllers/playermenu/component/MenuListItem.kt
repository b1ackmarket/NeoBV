package dev.aaa1115910.bv.component.controllers.playermenu.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.DenseListItem
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.aaa1115910.bv.ui.theme.BVTheme

@Composable
fun MenuListItem(
    modifier: Modifier = Modifier,
    text: String,
    icon: ImageVector? = null,
    expanded: Boolean = true,
    selected: Boolean,
    textAlign: TextAlign = TextAlign.Center,
    onFocus: () -> Unit = {},
    onClick: () -> Unit
) {
    val itemWidth by animateDpAsState(
        targetValue = if (expanded) 200.dp else 66.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "MenuListItem width [$text]"
    )
    var hasFocus by remember { mutableStateOf(false) }

    DenseListItem(
        modifier = modifier
            .width(itemWidth)
            .onFocusChanged {
                hasFocus = it.hasFocus
                if (it.hasFocus) onFocus()
            }
            .then(
                if (selected && !hasFocus) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(18.dp)
                    )
                } else {
                    Modifier
                }
            ),
        selected = selected,
        onClick = onClick,
        headlineContent = {
            Box {
                Row(
                    modifier = Modifier
                        .padding(
                            vertical = 0.dp,
                            horizontal = 6.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedVisibility(
                        visible = expanded,
                        label = "MenuListItem text [$text]",
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp),
                            text = text,
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = textAlign,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    AnimatedVisibility(
                        visible = !expanded,
                        label = "MenuListItem icon [$text]",
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        if (icon == null) {
                            Box(modifier = Modifier.size(32.dp))
                        } else {
                            Icon(
                                modifier = Modifier.size(32.dp),
                                imageVector = icon,
                                contentDescription = null
                            )
                        }
                    }
                }
            }
        },
        colors = ListItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.4f),
        )
    )
}

@Preview
@Composable
fun MenuListItemPreview() {
    var expanded by remember { mutableStateOf(true) }
    BVTheme {
        MenuListItem(
            text = "MenuListItemAAAAAAAAAAAAA",
            icon = Icons.Default.Home,
            expanded = expanded,
            selected = true,
            textAlign = TextAlign.Center,
            onFocus = {},
            onClick = { expanded = !expanded }
        )
    }
}
