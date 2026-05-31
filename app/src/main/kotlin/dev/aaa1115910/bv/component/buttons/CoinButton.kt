package dev.aaa1115910.bv.component.buttons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import dev.aaa1115910.bv.util.touchClick

@Composable
fun CoinButton(
    modifier: Modifier = Modifier,
    isCoined: Boolean,
    onClick: () -> Unit
) {
    Button(
        modifier = modifier.touchClick(onClick),
        onClick = onClick,
    ) {
        Icon(
            imageVector = if (isCoined) Icons.Rounded.Paid else Icons.Outlined.Paid,
            contentDescription = null
        )
    }
}

@Preview
@Composable
fun CoinButtonPreview() {
    CoinButton(
        isCoined = false,
        onClick = {}
    )
}
