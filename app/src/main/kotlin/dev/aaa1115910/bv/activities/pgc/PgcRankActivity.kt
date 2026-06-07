package dev.aaa1115910.bv.activities.pgc

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import dev.aaa1115910.biliapi.entity.pgc.PgcType
import dev.aaa1115910.bv.activities.ImmersiveComponentActivity
import dev.aaa1115910.bv.screen.main.pgc.PgcRankScreen
import dev.aaa1115910.bv.ui.theme.BVTheme

class PgcRankActivity : ImmersiveComponentActivity() {
    companion object {
        private const val ExtraPgcType = "pgc_type"

        fun actionStart(
            context: Context,
            pgcType: PgcType
        ) {
            context.startActivity(
                Intent(context, PgcRankActivity::class.java).apply {
                    putExtra(ExtraPgcType, pgcType.ordinal)
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pgcType = PgcType.entries.getOrElse(
            intent.getIntExtra(ExtraPgcType, PgcType.Anime.ordinal)
        ) {
            PgcType.Anime
        }
        setContent {
            BVTheme {
                PgcRankScreen(pgcType = pgcType)
            }
        }
    }
}
