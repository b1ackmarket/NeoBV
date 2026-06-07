package dev.aaa1115910.bv.activities.pgc

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import dev.aaa1115910.biliapi.entity.pgc.PgcType
import dev.aaa1115910.bv.activities.ImmersiveComponentActivity
import dev.aaa1115910.bv.screen.main.pgc.PgcIndexScreen
import dev.aaa1115910.bv.ui.theme.BVTheme

class PgcIndexActivity : ImmersiveComponentActivity() {
    companion object {
        private const val ExtraPgcType = "pgcType"

        fun actionStart(
            context: Context,
            pgcType: PgcType
        ) {
            context.startActivity(
                Intent(context, PgcIndexActivity::class.java).apply {
                    putExtra(ExtraPgcType, pgcType.ordinal)
                }
            )
        }

        private fun resolvePgcType(intent: Intent): PgcType =
            PgcType.entries.getOrElse(intent.getIntExtra(ExtraPgcType, PgcType.Anime.ordinal)) {
                PgcType.Anime
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialPgcType = resolvePgcType(intent)
        setContent {
            BVTheme {
                PgcIndexScreen(initialPgcType = initialPgcType)
            }
        }
    }
}
