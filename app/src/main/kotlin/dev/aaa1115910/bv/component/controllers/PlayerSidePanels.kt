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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.focusable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Icon
import androidx.tv.material3.Border
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.entity.PlayerCommentEmote
import dev.aaa1115910.bv.entity.PlayerCommentItem
import dev.aaa1115910.bv.entity.PlayerCommentSort
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.tv.component.TvAlertDialog
import dev.aaa1115910.bv.util.touchClick
import dev.aaa1115910.bv.util.touchLongClick
import dev.aaa1115910.bv.viewmodel.player.PlayerSidePanel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

internal fun shouldCloseSidePanelForPreviewKey(
    eventType: KeyEventType,
    key: Key,
    headerHasFocus: Boolean
): Boolean {
    if (eventType != KeyEventType.KeyDown) return false
    if (key == Key.Back) return true
    return key == Key.DirectionLeft && !headerHasFocus
}

internal const val UP_PANEL_NAME_MAX_DISPLAY_UNITS = 14

internal fun truncateUpPanelName(
    name: String,
    maxDisplayUnits: Int = UP_PANEL_NAME_MAX_DISPLAY_UNITS
): String {
    if (name.isBlank()) return name
    val totalUnits = name.fold(0) { acc: Int, char ->
        acc + if (char.code <= 0x7F) 1 else 2
    }
    if (totalUnits <= maxDisplayUnits) return name

    val ellipsisUnits = 1
    val targetUnits = (maxDisplayUnits - ellipsisUnits).coerceAtLeast(0)
    var units = 0
    val builder = StringBuilder()
    for (char in name) {
        val charUnits = if (char.code <= 0x7F) 1 else 2
        if (units + charUnits > targetUnits) {
            return builder.append('…').toString()
        }
        builder.append(char)
        units += charUnits
    }
    return builder.toString()
}

data class PlayerUpPanelUiState(
    val upMid: Long = 0L,
    val upName: String = "",
    val upFace: String = "",
    val latestSelected: Boolean = true,
    val isFollowing: Boolean = false,
    val videos: List<VideoCardData> = emptyList()
)

data class PlayerCommentPanelUiState(
    val title: String = "评论",
    val emptyText: String = "暂无评论",
    val sort: PlayerCommentSort = PlayerCommentSort.Latest,
    val totalCountText: String = "",
    val showSortToggle: Boolean = true,
    val loading: Boolean = false,
    val canLoadMore: Boolean = false,
    val errorMessage: String? = null,
    val comments: List<PlayerCommentItem> = emptyList(),
    val focusLatest: Boolean = false,
    val rememberedFirstVisibleItemIndex: Int = 0,
    val rememberedFirstVisibleItemScrollOffset: Int = 0,
    val detailRootComment: PlayerCommentItem? = null,
    val detailReplies: List<PlayerCommentItem> = emptyList(),
    val detailLoading: Boolean = false,
    val detailErrorMessage: String? = null
)

@Composable
fun PlayerSidePanels(
    activePanel: PlayerSidePanel,
    relatedVideos: List<VideoCardData>,
    upPanelUiState: PlayerUpPanelUiState,
    commentPanelUiState: PlayerCommentPanelUiState = PlayerCommentPanelUiState(showSortToggle = false),
    onClose: () -> Unit,
    onRelatedVideoClicked: (VideoCardData) -> Unit,
    onUpVideoClicked: (VideoCardData) -> Unit,
    onOpenUpPage: (Long, String) -> Unit = { _, _ -> },
    onToggleUpSort: () -> Unit,
    onToggleUpFollow: () -> Unit,
    onToggleCommentSort: () -> Unit = {},
    onLoadMoreComments: () -> Unit = {},
    onCommentListPositionChanged: (Int, Int) -> Unit = { _, _ -> },
    onOpenCommentDetail: (PlayerCommentItem) -> Unit = {},
    onCloseCommentDetail: () -> Unit = {},
    onCommentLike: (PlayerCommentItem) -> Unit = {},
    onCommentDislike: (PlayerCommentItem) -> Unit = {}
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
                        if (
                            activePanel == PlayerSidePanel.Comments &&
                            commentPanelUiState.detailRootComment != null &&
                            (it.key == Key.Back || it.key == Key.DirectionLeft)
                        ) {
                            if (it.type == KeyEventType.KeyDown) onCloseCommentDetail()
                            true
                        } else if (shouldCloseSidePanelForPreviewKey(it.type, it.key, headerHasFocus)) {
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
                            onOpenUpPage = onOpenUpPage,
                            onVideoClicked = onUpVideoClicked,
                            onToggleSort = onToggleUpSort,
                            onToggleFollow = onToggleUpFollow
                        )

                        PlayerSidePanel.Comments -> PlayerCommentsPanel(
                            state = commentPanelUiState,
                            onHeaderFocusChanged = { headerHasFocus = it },
                            onToggleSort = onToggleCommentSort,
                            onLoadMore = onLoadMoreComments,
                            onListPositionChanged = onCommentListPositionChanged,
                            onOpenCommentDetail = onOpenCommentDetail,
                            onCloseCommentDetail = onCloseCommentDetail,
                            onOpenUpPage = onOpenUpPage,
                            onCommentLike = onCommentLike,
                            onCommentDislike = onCommentDislike
                        )

                        PlayerSidePanel.None -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerCommentsPanel(
    state: PlayerCommentPanelUiState,
    onHeaderFocusChanged: (Boolean) -> Unit,
    onToggleSort: () -> Unit,
    onLoadMore: () -> Unit,
    onListPositionChanged: (Int, Int) -> Unit,
    onOpenCommentDetail: (PlayerCommentItem) -> Unit,
    onCloseCommentDetail: () -> Unit,
    onOpenUpPage: (Long, String) -> Unit,
    onCommentLike: (PlayerCommentItem) -> Unit,
    onCommentDislike: (PlayerCommentItem) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val sortFocusRequester = remember { FocusRequester() }
    val latestFocusRequester = remember { FocusRequester() }
    val restoreCommentFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = state.rememberedFirstVisibleItemIndex,
        initialFirstVisibleItemScrollOffset = state.rememberedFirstVisibleItemScrollOffset
    )
    var restoreCommentId by remember { mutableStateOf<String?>(null) }
    var commentDetailWasOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        if (state.showSortToggle) {
            sortFocusRequester.requestFocus()
        } else {
            focusRequester.requestFocus()
        }
    }
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .collect { (index, offset) -> onListPositionChanged(index, offset) }
    }
    LaunchedEffect(state.focusLatest, state.comments.size) {
        if (state.focusLatest && state.comments.isNotEmpty() && state.detailRootComment == null) {
            listState.animateScrollToItem(state.comments.lastIndex)
            latestFocusRequester.requestFocus()
        }
    }
    LaunchedEffect(state.detailRootComment, state.comments.size, restoreCommentId) {
        if (state.detailRootComment != null) {
            commentDetailWasOpen = true
            return@LaunchedEffect
        }
        if (!commentDetailWasOpen) return@LaunchedEffect
        val targetCommentId = restoreCommentId ?: return@LaunchedEffect
        if (state.comments.isEmpty()) return@LaunchedEffect
        val targetIndex = state.comments.indexOfFirst { it.id == targetCommentId }
        if (targetIndex >= 0) {
            listState.scrollToItem(targetIndex)
            requestFocusWithRetry(restoreCommentFocusRequester)
        } else {
            requestFocusWithRetry(focusRequester)
        }
        restoreCommentId = null
        commentDetailWasOpen = false
    }
    LaunchedEffect(state.comments.size, state.canLoadMore, state.loading) {
        if (!state.canLoadMore || state.loading) return@LaunchedEffect
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .distinctUntilChanged()
            .filter { index ->
                index != null && index >= state.comments.lastIndex - 3
            }
            .collect {
                onLoadMore()
            }
    }

    Column(
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(id = R.drawable.comment_24px),
                    contentDescription = null,
                    tint = Color.White
                )
                Text(
                    modifier = Modifier.padding(start = 8.dp),
                    text = listOf(state.title, state.totalCountText)
                        .filter(String::isNotBlank)
                        .joinToString(" · "),
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
            if (state.showSortToggle) {
                PlayerPanelChip(
                    modifier = Modifier.focusRequester(sortFocusRequester),
                    text = if (state.sort == PlayerCommentSort.Latest) "最新" else "最热",
                    emphasized = false,
                    onClick = onToggleSort,
                    onFocusChanged = onHeaderFocusChanged
                )
            }
        }

        if (state.detailRootComment != null) {
            PlayerCommentDetailPanel(
                rootComment = state.detailRootComment,
                replies = state.detailReplies,
                loading = state.detailLoading,
                errorMessage = state.detailErrorMessage,
                onBack = onCloseCommentDetail,
                onOpenUpPage = onOpenUpPage,
                onLike = onCommentLike,
                onDislike = onCommentDislike
            )
            return@Column
        }

        when {
            state.loading && state.comments.isEmpty() -> {
                Text(
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .focusable(),
                    text = "加载中…",
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            state.errorMessage != null && state.comments.isEmpty() -> {
                Text(
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .focusable(),
                    text = state.errorMessage,
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            state.comments.isEmpty() -> {
                Text(
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .focusable(),
                    text = state.emptyText,
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.onPreviewKeyEvent {
                        if (
                            state.showSortToggle &&
                            it.type == KeyEventType.KeyDown &&
                            it.key == Key.Menu
                        ) {
                            scope.launch {
                                listState.scrollToItem(0)
                                sortFocusRequester.requestFocus()
                            }
                            return@onPreviewKeyEvent true
                        }
                        false
                    },
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(state.comments) { index, comment ->
                        PlayerCommentListItem(
                            modifier = when {
                                comment.id == restoreCommentId -> Modifier.focusRequester(restoreCommentFocusRequester)
                                state.focusLatest && index == state.comments.lastIndex -> Modifier.focusRequester(latestFocusRequester)
                                index == 0 -> Modifier.focusRequester(focusRequester)
                                else -> Modifier
                            },
                            comment = comment,
                            onClick = {
                                restoreCommentId = comment.id
                                onOpenCommentDetail(comment)
                            },
                            onOpenActions = { onOpenUpPage(comment.mid, comment.username) }
                        )
                    }
                    if (state.loading) {
                        item {
                            Text(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                text = "加载中…",
                                color = Color.White.copy(alpha = 0.62f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }

}

private suspend fun requestFocusWithRetry(focusRequester: FocusRequester) {
    runCatching { focusRequester.requestFocus() }.onFailure {
        delay(100)
        runCatching { focusRequester.requestFocus() }
    }
}

@Composable
private fun PlayerCommentListItem(
    modifier: Modifier = Modifier,
    comment: PlayerCommentItem,
    onClick: () -> Unit,
    forceExpanded: Boolean = false,
    onOpenActions: () -> Unit = {}
) {
    var isFocused by remember { mutableStateOf(false) }
    var expanded by remember(comment.id) { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        onLongClick = onOpenActions,
        modifier = modifier
            .fillMaxWidth()
            .touchLongClick(onClick = onClick, onLongClick = onOpenActions)
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
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (comment.avatar.isNotBlank()) {
                AsyncImage(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape),
                    model = comment.avatar,
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color.White.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = comment.username.ifBlank { "?" }.take(1),
                        color = Color.White.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = comment.username.ifBlank { "匿名用户" },
                        color = comment.color?.let { Color(0xff000000.toInt() or (it and 0x00ffffff)) }
                            ?: Color.White.copy(alpha = 0.86f),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Normal
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val meta = listOf(comment.badgeText, comment.likeText, comment.replyText, comment.timeText)
                        .filter { !it.isNullOrBlank() }
                        .joinToString("  ")
                    if (meta.isNotBlank()) {
                        Text(
                            modifier = Modifier.padding(start = 10.dp),
                            text = meta,
                            color = Color.White.copy(alpha = 0.58f),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                PlayerCommentMessage(
                    comment = comment,
                    forceExpanded = forceExpanded,
                    expanded = expanded
                )
                if (comment.pictures.isNotEmpty()) {
                    if (forceExpanded || expanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            comment.pictures.forEach { picture ->
                                AsyncImage(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(132.dp)
                                        .clip(MaterialTheme.shapes.medium),
                                    model = picture.url,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "${comment.pictures.size}张图片，进入详情展开",
                            color = Color.White.copy(alpha = 0.58f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                if (comment.ipLocation.isNotBlank()) {
                    Text(
                        modifier = Modifier.align(Alignment.End),
                        text = comment.ipLocation,
                        color = Color.White.copy(alpha = 0.46f),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerCommentMessage(
    comment: PlayerCommentItem,
    forceExpanded: Boolean,
    expanded: Boolean
) {
    val maxLines = if (forceExpanded || expanded) Int.MAX_VALUE else 4
    val overflow = if (forceExpanded || expanded) TextOverflow.Clip else TextOverflow.Ellipsis
    val emotes = remember(comment.emotes) {
        comment.emotes
            .filter { it.text.isNotBlank() && it.url.isNotBlank() }
            .distinctBy { it.text }
            .sortedByDescending { it.text.length }
    }
    if (emotes.isEmpty()) {
        Text(
            text = comment.message,
            color = Color.White.copy(alpha = 0.9f),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = maxLines,
            overflow = overflow
        )
        return
    }

    val inlineContent = remember(emotes) {
        emotes.mapIndexed { index, emote ->
            val textSize = if (emote.size > 1) 30.sp else 22.sp
            val imageSize = if (emote.size > 1) 30.dp else 22.dp
            emote.inlineId(index) to InlineTextContent(
                placeholder = Placeholder(
                    width = textSize,
                    height = textSize,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                )
            ) {
                AsyncImage(
                    modifier = Modifier.size(imageSize),
                    model = emote.url,
                    contentDescription = emote.text,
                    contentScale = ContentScale.Fit
                )
            }
        }.toMap()
    }
    val annotatedMessage = remember(comment.message, emotes) {
        buildAnnotatedString {
            var cursor = 0
            while (cursor < comment.message.length) {
                val next = emotes.mapIndexedNotNull { index, emote ->
                    val start = comment.message.indexOf(emote.text, startIndex = cursor)
                    if (start >= 0) Triple(start, index, emote) else null
                }.minByOrNull { it.first }

                if (next == null) {
                    append(comment.message.substring(cursor))
                    cursor = comment.message.length
                } else {
                    val (start, index, emote) = next
                    if (start > cursor) append(comment.message.substring(cursor, start))
                    appendInlineContent(emote.inlineId(index), emote.text)
                    cursor = start + emote.text.length
                }
            }
        }
    }

    Text(
        text = annotatedMessage,
        inlineContent = inlineContent,
        color = Color.White.copy(alpha = 0.9f),
        style = MaterialTheme.typography.bodyMedium,
        maxLines = maxLines,
        overflow = overflow
    )
}

private fun PlayerCommentEmote.inlineId(index: Int): String = "comment_emote_$index"

@Composable
private fun PlayerCommentActionDialog(
    comment: PlayerCommentItem,
    onDismiss: () -> Unit,
    onOpenUpPage: (Long, String) -> Unit,
    onLike: (PlayerCommentItem) -> Unit,
    onDislike: (PlayerCommentItem) -> Unit
) {
    TvAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = comment.username.ifBlank { "评论操作" })
        },
        confirmButton = {},
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        ),
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PlayerPanelChip(
                    text = "点赞",
                    emphasized = false,
                    onFocusChanged = {},
                    onClick = {
                        onDismiss()
                        onLike(comment)
                    }
                )
                PlayerPanelChip(
                    text = "主页",
                    emphasized = true,
                    onFocusChanged = {},
                    onClick = {
                        onDismiss()
                        onOpenUpPage(comment.mid, comment.username)
                    }
                )
                PlayerPanelChip(
                    text = "点踩",
                    emphasized = false,
                    onFocusChanged = {},
                    onClick = {
                        onDismiss()
                        onDislike(comment)
                    }
                )
            }
        }
    )
}

@Composable
private fun PlayerCommentDetailPanel(
    rootComment: PlayerCommentItem,
    replies: List<PlayerCommentItem>,
    loading: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onOpenUpPage: (Long, String) -> Unit,
    onLike: (PlayerCommentItem) -> Unit,
    onDislike: (PlayerCommentItem) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(rootComment.id) {
        focusRequester.requestFocus()
    }
    LazyColumn(
        modifier = Modifier.onPreviewKeyEvent {
            if (it.key == Key.Back || it.key == Key.DirectionLeft) {
                if (it.type == KeyEventType.KeyDown) onBack()
                true
            } else {
                false
            }
        },
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            PlayerCommentListItem(
                modifier = Modifier.focusRequester(focusRequester),
                comment = rootComment,
                onClick = {},
                forceExpanded = true,
                onOpenActions = { onOpenUpPage(rootComment.mid, rootComment.username) }
            )
        }
        item {
            Text(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                text = "回复",
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.titleSmall
            )
        }
        if (loading) {
            item {
                Text(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    text = "加载中…",
                    color = Color.White.copy(alpha = 0.62f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        } else if (errorMessage != null) {
            item {
                Text(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    text = errorMessage,
                    color = Color.White.copy(alpha = 0.62f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        } else if (replies.isEmpty()) {
            item {
                Text(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    text = "暂无回复",
                    color = Color.White.copy(alpha = 0.62f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        } else {
            itemsIndexed(replies) { _, reply ->
                PlayerCommentListItem(
                    comment = reply,
                    onClick = {},
                    forceExpanded = true,
                    onOpenActions = { onOpenUpPage(reply.mid, reply.username) }
                )
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
    onOpenUpPage: (Long, String) -> Unit,
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
                Surface(
                    onClick = { onOpenUpPage(state.upMid, state.upName) },
                    modifier = Modifier
                        .size(50.dp)
                        .touchClick { onOpenUpPage(state.upMid, state.upName) },
                    shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.large),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        focusedContainerColor = Color.White.copy(alpha = 0.14f),
                        focusedContentColor = Color.White
                    ),
                    border = ClickableSurfaceDefaults.border(
                        focusedBorder = Border(
                            border = androidx.compose.foundation.BorderStroke(2.dp, Color.White.copy(alpha = 0.9f)),
                            shape = MaterialTheme.shapes.large
                        )
                    ),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1f)
                ) {
                    AsyncImage(
                        modifier = Modifier
                            .size(46.dp)
                            .padding(2.dp)
                            .clip(MaterialTheme.shapes.large),
                        model = state.upFace,
                        contentDescription = null,
                        contentScale = ContentScale.Crop
                    )
                }
                Column(
                    modifier = Modifier
                        .padding(start = 10.dp, end = 12.dp)
                        .weight(1f, fill = false)
                ) {
                    Text(
                        text = truncateUpPanelName(state.upName),
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Clip
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
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PlayerPanelChip(
                    text = if (state.latestSelected) "最新" else "最热",
                    emphasized = false,
                    onClick = onToggleSort,
                    onFocusChanged = onHeaderFocusChanged
                )
                PlayerPanelChip(
                    text = if (state.isFollowing) "已关注" else "+ 关注",
                    emphasized = !state.isFollowing,
                    onClick = onToggleFollow,
                    onFocusChanged = onHeaderFocusChanged
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
            .touchClick(onClick)
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
    modifier: Modifier = Modifier,
    text: String,
    emphasized: Boolean,
    onFocusChanged: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        modifier = modifier
            .touchClick(onClick)
            .onFocusChanged {
                isFocused = it.hasFocus
                onFocusChanged(it.hasFocus)
            }
            .focusTarget(),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (emphasized) Color(0xFFF4529B) else Color.White.copy(alpha = 0.12f),
            contentColor = Color.White,
            focusedContainerColor = if (emphasized) Color(0xFFFF6EAF) else Color.White.copy(alpha = 0.24f),
            focusedContentColor = Color.White
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = androidx.compose.foundation.BorderStroke(2.dp, Color.White.copy(alpha = 0.95f)),
                shape = MaterialTheme.shapes.large
            ),
            border = Border(
                border = androidx.compose.foundation.BorderStroke(
                    if (isFocused) 2.dp else 1.dp,
                    if (isFocused) Color.White.copy(alpha = 0.95f) else Color.White.copy(alpha = 0.08f)
                ),
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
