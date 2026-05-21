package dev.aaa1115910.bv.screen.main.pgc

import android.content.Intent
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.aaa1115910.biliapi.entity.pgc.PgcType
import dev.aaa1115910.biliapi.entity.season.TimelineFilter
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.activities.pgc.PgcIndexActivity
import dev.aaa1115910.bv.activities.pgc.anime.AnimeTimelineActivity
import dev.aaa1115910.bv.tv.screens.main.pgc.PgcFeatureButtons
import dev.aaa1115910.bv.tv.screens.main.pgc.PgcScaffold
import dev.aaa1115910.bv.tv.screens.main.pgc.showPlaceholderToast
import dev.aaa1115910.bv.ui.theme.BVTheme
import dev.aaa1115910.bv.viewmodel.pgc.PgcGuoChuangViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun GuoChuangContent(
    modifier: Modifier = Modifier,
    lazyListState: LazyListState,
    pgcViewModel: PgcGuoChuangViewModel = koinViewModel()
) {
    val context = LocalContext.current

    val onOpenIndex: () -> Unit = {
        PgcIndexActivity.actionStart(context = context, pgcType = PgcType.GuoChuang)
    }
    val onOpenTimeline: () -> Unit = {
        context.startActivity(
            Intent(context, AnimeTimelineActivity::class.java).apply {
                putExtra(AnimeTimelineActivity.ExtraTimelineFilter, TimelineFilter.GuoChuang.ordinal)
            }
        )
    }

    PgcScaffold(
        lazyListState = lazyListState,
        pgcViewModel = pgcViewModel,
        pgcType = PgcType.GuoChuang,
        featureButtons = {
            GuoChuangFeatureButtons(
                modifier = Modifier.padding(vertical = 24.dp),
                onOpenIndex = onOpenIndex,
                onOpenTimeline = onOpenTimeline
            )
        }
    )
}

@Composable
private fun GuoChuangFeatureButtons(
    modifier: Modifier = Modifier,
    onOpenIndex: () -> Unit,
    onOpenTimeline: () -> Unit
) {
    val buttons = listOf(
        Triple(
            stringResource(R.string.anime_home_button_index),
            painterResource(R.drawable.pgc_index_24),
            onOpenIndex
        ),
        Triple(
            stringResource(R.string.pgc_home_button_unknown),
            painterResource(R.drawable.pgc_placeholder_24),
            showPlaceholderToast
        ),
        Triple(
            stringResource(R.string.pgc_home_button_unknown),
            painterResource(R.drawable.pgc_placeholder_24),
            showPlaceholderToast
        ),
        Triple(
            stringResource(R.string.anime_home_button_timeline),
            painterResource(R.drawable.pgc_timeline_24),
            onOpenTimeline
        )
    )
    PgcFeatureButtons(
        modifier = modifier,
        buttons = buttons
    )
}

@Preview(device = "id:tv_1080p")
@Composable
private fun GuoChuangFeatureButtonsPreview() {
    BVTheme {
        GuoChuangFeatureButtons(
            modifier = Modifier,
            onOpenIndex = {},
            onOpenTimeline = {}
        )
    }
}
