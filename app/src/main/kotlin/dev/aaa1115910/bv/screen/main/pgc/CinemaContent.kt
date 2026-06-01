package dev.aaa1115910.bv.screen.main.pgc

import android.view.KeyEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import dev.aaa1115910.biliapi.entity.pgc.PgcCinemaTabData
import dev.aaa1115910.biliapi.entity.pgc.PgcItem
import dev.aaa1115910.bv.activities.pgc.PgcCinemaTopicActivity
import dev.aaa1115910.bv.activities.video.SeasonInfoActivity
import dev.aaa1115910.bv.component.PgcCarousel
import dev.aaa1115910.bv.component.videocard.SeasonCard
import dev.aaa1115910.bv.entity.carddata.SeasonCardData
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.util.ImageSize
import dev.aaa1115910.bv.util.resizedImageUrl
import dev.aaa1115910.bv.viewmodel.pgc.PgcCinemaViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun CinemaContent(
    modifier: Modifier = Modifier,
    lazyListState: LazyListState,
    pgcViewModel: PgcCinemaViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val carouselFocusRequester = remember { FocusRequester() }
    val carouselItems = pgcViewModel.carouselItems
    val rows = pgcViewModel.rows

    LazyColumn(
        modifier = modifier,
        state = lazyListState
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.Center
            ) {
                PgcCarousel(
                    modifier = Modifier
                        .width(880.dp)
                        .padding(32.dp, 0.dp)
                        .focusRequester(carouselFocusRequester),
                    data = carouselItems,
                    onClick = { item ->
                        SeasonInfoActivity.actionStart(
                            context = context,
                            epId = item.episodeId,
                            seasonId = item.seasonId,
                            proxyArea = ProxyArea.checkProxyArea(item.title)
                        )
                    }
                )
            }
        }

        item {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
            )
        }

        itemsIndexed(items = rows) { index, row ->
            CinemaRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .onFocusChanged {
                        if (it.hasFocus && index + 6 > rows.size) {
                            pgcViewModel.loadMore()
                        }
                    },
                row = row
            )
        }
    }
}

@Composable
private fun CinemaRow(
    modifier: Modifier = Modifier,
    row: PgcCinemaTabData.Row
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        row.topics.forEach { topic ->
            CinemaTopicStrip(topic = topic)
        }
    }
}

@Composable
private fun CinemaTopicStrip(
    modifier: Modifier = Modifier,
    topic: PgcCinemaTabData.CinemaTopic
) {
    val context = LocalContext.current
    val previewItems = topic.items

    Row(
        modifier = modifier.padding(start = 16.dp, end = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        CinemaTopicHeader(
            modifier = Modifier.padding(6.dp),
            title = topic.title,
            backgroundCover = topic.cover.resizedImageUrl(ImageSize.SeasonCoverThumbnail),
            onClick = { PgcCinemaTopicActivity.actionStart(context, topic) }
        )

        LazyRow(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 8.dp, top = 6.dp, end = 24.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            itemsIndexed(previewItems) { index, item ->
                val cardModifier = if (index == previewItems.lastIndex) {
                    Modifier.onPreviewKeyEvent {
                        when (it.nativeKeyEvent.keyCode) {
                            KeyEvent.KEYCODE_DPAD_RIGHT -> return@onPreviewKeyEvent true
                        }
                        false
                    }
                } else {
                    Modifier
                }
                CinemaSeasonCard(
                    modifier = cardModifier,
                    item = item
                )
            }
        }
    }
}

@Composable
private fun CinemaTopicHeader(
    modifier: Modifier = Modifier,
    title: String,
    backgroundCover: String?,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .width(196.dp)
            .height(180.dp),
        onClick = onClick,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            pressedContainerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.large),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(width = 3.dp, color = Color.White),
                shape = MaterialTheme.shapes.large
            )
        ),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (backgroundCover != null) {
                AsyncImage(
                    modifier = Modifier.fillMaxSize(),
                    model = backgroundCover,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alpha = 0.4f
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.12f),
                                Color.Black.copy(alpha = 0.82f)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = "专题",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CinemaSeasonCard(
    modifier: Modifier = Modifier,
    item: PgcItem
) {
    val context = LocalContext.current
    SeasonCard(
        modifier = modifier,
        coverHeight = 180.dp,
        data = SeasonCardData(
            seasonId = item.seasonId,
            title = item.title,
            subTitle = item.subTitle,
            cover = item.cover.resizedImageUrl(ImageSize.SeasonCoverThumbnail),
            rating = item.rating
        ),
        onClick = {
            SeasonInfoActivity.actionStart(
                context = context,
                seasonId = item.seasonId,
                proxyArea = ProxyArea.checkProxyArea(item.title)
            )
        }
    )
}
