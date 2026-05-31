package dev.aaa1115910.bv.activities.pgc

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.aaa1115910.biliapi.entity.pgc.PgcCinemaTabData
import dev.aaa1115910.bv.entity.PgcCinemaTopicPayload
import dev.aaa1115910.bv.screen.main.pgc.PgcCinemaTopicScreen
import dev.aaa1115910.bv.ui.theme.BVTheme
import io.github.oshai.kotlinlogging.KotlinLogging

class PgcCinemaTopicActivity : ComponentActivity() {
    companion object {
        private const val ExtraTopicPayload = "topic_payload"
        private val logger = KotlinLogging.logger("PgcCinemaTopicActivity")

        fun actionStart(
            context: Context,
            topic: PgcCinemaTabData.CinemaTopic
        ) {
            context.startActivity(
                Intent(context, PgcCinemaTopicActivity::class.java).apply {
                    putExtra(ExtraTopicPayload, PgcCinemaTopicPayload.fromTopic(topic).toJson())
                }
            )
        }

        private fun resolvePayload(intent: Intent): PgcCinemaTopicPayload? {
            val payloadJson = intent.getStringExtra(ExtraTopicPayload) ?: return null
            return runCatching { PgcCinemaTopicPayload.fromJson(payloadJson) }
                .onFailure { logger.warn(it) { "Failed to parse PGC cinema topic payload" } }
                .getOrNull()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val payload = resolvePayload(intent)
        setContent {
            BVTheme {
                PgcCinemaTopicScreen(payload = payload)
            }
        }
    }
}
