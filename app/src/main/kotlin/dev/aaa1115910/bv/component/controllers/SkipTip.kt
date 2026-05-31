package dev.aaa1115910.bv.component.controllers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.aaa1115910.bv.util.touchClick

@Composable
fun SkipTips(
    modifier: Modifier = Modifier,
    showBackToStart: Boolean,
    showSkipToNextEp: Boolean,
    showPreviewTip: Boolean,
    showOnlineCount: Boolean = false,
    onlineCountText: String? = null,
    pluginTipMessage: String? = null,
    onPluginTipClick: (() -> Unit)? = null,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PlayerTip(
                show = showPreviewTip,
                text = "视频需付费，当前为试看片段",
                icon = Icons.Outlined.Info,
            )
            PlayerTip(
                show = !pluginTipMessage.isNullOrBlank(),
                text = pluginTipMessage.orEmpty(),
                icon = Icons.Outlined.Info,
                onClick = onPluginTipClick,
            )
            PlayerTip(
                show = showSkipToNextEp,
                text = "播放结束，即将播放下一集",
                icon = Icons.Outlined.SkipNext,
            )
            PlayerTip(
                show = showBackToStart,
                text = "从上次播放位置继续，按确认键从头播放",
                icon = Icons.Outlined.Replay,
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 80.dp)
        ) {
            PlayerTip(
                show = showOnlineCount && !onlineCountText.isNullOrBlank(),
                text = onlineCountText.orEmpty(),
                icon = Icons.Outlined.People,
                anchoredEnd = true,
            )
        }
    }
}

@Composable
fun PlayerTip(
    show: Boolean,
    text: String,
    icon: ImageVector,
    anchoredEnd: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    AnimatedVisibility(
        visible = show,
        enter = expandVertically(
            expandFrom = Alignment.Bottom,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn(tween(400)),
        exit = shrinkVertically(
            shrinkTowards = Alignment.Bottom,
            animationSpec = tween(350)
        ) + fadeOut(tween(280))
    ) {
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .then(if (onClick != null) Modifier.touchClick(onClick) else Modifier)
                .clip(
                    if (anchoredEnd) {
                        RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp)
                    } else {
                        RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp)
                    }
                )
                .background(Color.Black.copy(alpha = 0.6f)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
    }
}

@Preview
@Composable
private fun SkipTipsPreview() {
    SkipTips(
        showBackToStart = true,
        showSkipToNextEp = true,
        showPreviewTip = true,
        showOnlineCount = true,
        onlineCountText = "256 人一起看",
        pluginTipMessage = "跳过片头"
    )
}
