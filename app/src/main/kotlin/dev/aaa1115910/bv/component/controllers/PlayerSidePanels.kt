package dev.aaa1115910.bv.component.controllers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Border
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.viewmodel.player.PlayerSidePanel

internal fun shouldCloseSidePanelForPreviewKey(
    eventType: KeyEventType,
    key: Key,
    headerHasFocus: Boolean
): Boolean {
    if (eventType != KeyEventType.KeyDown) return false
    if (key == Key.Back) return true
    return key == Key.DirectionLeft && !headerHasFocus
}

data class PlayerUpPanelUiState(
    val upName: String = "",
    val upFace: String = "",
    val latestSelected: Boolean = true,
    val isFollowing: Boolean = false,
    val videos: List<VideoCardData> = emptyList()
)

@Composable
fun PlayerSidePanels(
    activePanel: PlayerSidePanel,
    relatedVideos: List<VideoCardData>,
    upPanelUiState: PlayerUpPanelUiState,
    onClose: () -> Unit,
    onRelatedVideoClicked: (VideoCardData) -> Unit,
    onUpVideoClicked: (VideoCardData) -> Unit,
    onToggleUpSort: () -> Unit,
    onToggleUpFollow: () -> Unit
) {
    var headerHasFocus by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = activePanel != PlayerSidePanel.None,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(420.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(Color(0xD9131824))
            ) {
                Box(
                    modifier = Modifier.onPreviewKeyEvent {
                        if (shouldCloseSidePanelForPreviewKey(it.type, it.key, headerHasFocus)) {
                            onClose()
                            true
                        } else {
                            false
                        }
                    }
                ) {
                    when (activePanel) {
                        PlayerSidePanel.RelatedVideos -> PlayerRelatedPanel(
                            videos = relatedVideos,
                            onVideoClicked = onRelatedVideoClicked
                        )

                        PlayerSidePanel.UpSpace -> PlayerUpSpacePanel(
                            state = upPanelUiState,
                            onHeaderFocusChanged = { headerHasFocus = it },
                            onVideoClicked = onUpVideoClicked,
                            onToggleSort = onToggleUpSort,
                            onToggleFollow = onToggleUpFollow
                        )

                        PlayerSidePanel.None -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerRelatedPanel(
    videos: List<VideoCardData>,
    onVideoClicked: (VideoCardData) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(videos) {
        if (videos.isNotEmpty()) focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .focusRequester(focusRequester)
            .padding(horizontal = 18.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = R.drawable.related_videos_24px),
                contentDescription = null,
                tint = Color.White
            )
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = "更多视频",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(videos) { _, video ->
                PlayerSidePanelVideoItem(
                    modifier = if (video == videos.firstOrNull()) {
                        Modifier.focusRequester(focusRequester)
                    } else {
                        Modifier
                    },
                    video = video,
                    onClick = { onVideoClicked(video) }
                )
            }
        }
    }
}

@Composable
private fun PlayerUpSpacePanel(
    state: PlayerUpPanelUiState,
    onHeaderFocusChanged: (Boolean) -> Unit,
    onVideoClicked: (VideoCardData) -> Unit,
    onToggleSort: () -> Unit,
    onToggleFollow: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(state.videos) {
        if (state.videos.isNotEmpty()) focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .focusRequester(focusRequester)
            .padding(horizontal = 18.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(MaterialTheme.shapes.large),
                    model = state.upFace,
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
                Column(modifier = Modifier.padding(start = 10.dp)) {
                    Text(
                        text = state.upName,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        modifier = Modifier.padding(top = 2.dp),
                        text = "UP 主页",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Row(
                modifier = Modifier
                    .onFocusChanged { onHeaderFocusChanged(it.hasFocus) }
                    .focusTarget(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PlayerPanelChip(
                    text = if (state.latestSelected) "最新" else "最热",
                    emphasized = false,
                    onClick = onToggleSort
                )
                PlayerPanelChip(
                    text = if (state.isFollowing) "已关注" else "+ 关注",
                    emphasized = !state.isFollowing,
                    onClick = onToggleFollow
                )
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(state.videos) { _, video ->
                PlayerSidePanelVideoItem(
                    modifier = if (video == state.videos.firstOrNull()) {
                        Modifier.focusRequester(focusRequester)
                    } else {
                        Modifier
                    },
                    video = video,
                    onClick = { onVideoClicked(video) }
                )
            }
        }
    }
}

@Composable
private fun PlayerSidePanelVideoItem(
    modifier: Modifier = Modifier,
    video: VideoCardData,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.medium),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.02f),
            contentColor = Color.White,
            focusedContainerColor = Color.White.copy(alpha = 0.1f),
            focusedContentColor = Color.White
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = androidx.compose.foundation.BorderStroke(2.dp, Color.White.copy(alpha = 0.9f)),
                shape = MaterialTheme.shapes.medium
            ),
            border = Border(
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                shape = MaterialTheme.shapes.medium
            )
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                modifier = Modifier
                    .width(150.dp)
                    .height(88.dp)
                    .clip(MaterialTheme.shapes.medium),
                model = video.cover,
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = video.title,
                    maxLines = 2,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Normal)
                )
                Text(
                    text = listOf(video.upName, video.playString, video.pubTime)
                        .filter { !it.isNullOrBlank() }
                        .joinToString("  "),
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun PlayerPanelChip(
    text: String,
    emphasized: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (emphasized) Color(0xFFF4529B) else Color.White.copy(alpha = 0.12f),
            contentColor = Color.White,
            focusedContainerColor = if (emphasized) Color(0xFFFF6EAF) else Color.White.copy(alpha = 0.24f),
            focusedContentColor = Color.White
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = androidx.compose.foundation.BorderStroke(2.dp, Color.White.copy(alpha = 0.85f)),
                shape = MaterialTheme.shapes.large
            ),
            border = Border(
                border = androidx.compose.foundation.BorderStroke(0.dp, Color.Transparent),
                shape = MaterialTheme.shapes.large
            )
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.large)
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
            text = text,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )
    }
}
