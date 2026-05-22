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
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.activities.pgc.PgcIndexActivity
import dev.aaa1115910.bv.activities.pgc.PgcRankActivity
import dev.aaa1115910.bv.activities.pgc.anime.AnimeTimelineActivity
import dev.aaa1115910.bv.activities.user.FollowingSeasonActivity
import dev.aaa1115910.bv.tv.screens.main.pgc.PgcFeatureButtons
import dev.aaa1115910.bv.tv.screens.main.pgc.PgcScaffold
import dev.aaa1115910.bv.ui.theme.BVTheme
import dev.aaa1115910.bv.viewmodel.pgc.PgcAnimeViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun AnimeContent(
    modifier: Modifier = Modifier,
    lazyListState: LazyListState,
    pgcViewModel: PgcAnimeViewModel = koinViewModel()
) {
    val context = LocalContext.current

    val onOpenTimeline: () -> Unit = {
        context.startActivity(Intent(context, AnimeTimelineActivity::class.java))
    }
    val onOpenFollowing: () -> Unit = {
        context.startActivity(Intent(context, FollowingSeasonActivity::class.java))
    }
    val onOpenIndex: () -> Unit = {
        PgcIndexActivity.actionStart(context = context, pgcType = PgcType.Anime)
    }
    val onOpenRank: () -> Unit = {
        PgcRankActivity.actionStart(context = context, pgcType = PgcType.Anime)
    }

    PgcScaffold(
        lazyListState = lazyListState,
        pgcViewModel = pgcViewModel,
        pgcType = PgcType.Anime,
        featureButtons = {
            AnimeFeatureButtons(
                modifier = Modifier.padding(vertical = 24.dp),
                onOpenTimeline = onOpenTimeline,
                onOpenFollowing = onOpenFollowing,
                onOpenIndex = onOpenIndex,
                onOpenRank = onOpenRank
            )
        }
    )
}

@Composable
private fun AnimeFeatureButtons(
    modifier: Modifier = Modifier,
    onOpenTimeline: () -> Unit,
    onOpenFollowing: () -> Unit,
    onOpenIndex: () -> Unit,
    onOpenRank: () -> Unit = {}
) {
    val buttons = listOf(
        Triple(
            stringResource(R.string.anime_home_button_timeline),
            painterResource(R.drawable.pgc_timeline_24),
            onOpenTimeline
        ),
        Triple(
            stringResource(R.string.anime_home_button_following),
            painterResource(R.drawable.pgc_following_24),
            onOpenFollowing
        ),
        Triple(
            stringResource(R.string.anime_home_button_index),
            painterResource(R.drawable.pgc_index_24),
            onOpenIndex
        ),
        Triple(
            stringResource(R.string.pgc_home_button_rank),
            painterResource(R.drawable.pgc_rank_24),
            onOpenRank
        )
    )
    PgcFeatureButtons(
        modifier = modifier,
        buttons = buttons
    )
}

@Preview(device = "id:tv_1080p")
@Composable
private fun AnimeFeatureButtonsPreview() {
    BVTheme {
        AnimeFeatureButtons(
            modifier = Modifier,
            onOpenTimeline = {},
            onOpenFollowing = {},
            onOpenIndex = {},
            onOpenRank = {}
        )
    }
}
