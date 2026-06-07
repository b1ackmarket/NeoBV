package dev.aaa1115910.bv.activities.live

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import dev.aaa1115910.bv.activities.ImmersiveComponentActivity
import dev.aaa1115910.bv.cast.CastPlaybackSession
import dev.aaa1115910.bv.cast.CastPlaybackSessionRegistry
import dev.aaa1115910.bv.cast.CastPlaybackSnapshot
import dev.aaa1115910.bv.cast.CastTransportState
import dev.aaa1115910.bv.screen.live.LivePlayerScreen
import dev.aaa1115910.bv.ui.theme.BVTheme

class LivePlayerActivity : ImmersiveComponentActivity() {
    private val castPlaybackSession = object : CastPlaybackSession {
        override fun play() = Unit

        override fun pause() = Unit

        override fun stop() {
            finish()
        }

        override fun seekTo(positionMs: Long) = Unit

        override fun setSpeed(speed: Float) = Unit

        override fun setQuality(qualityId: Int) = Unit

        override fun setDanmakuEnabled(enabled: Boolean) = Unit

        override fun snapshot(): CastPlaybackSnapshot =
            CastPlaybackSnapshot(
                state = CastTransportState.PLAYING,
                roomId = intent.getIntExtra("room_id", 0).toLong(),
                title = intent.getStringExtra("title").orEmpty(),
                danmakuEnabled = intent.getBooleanExtra("danmaku_enabled", true)
            )
    }

    companion object {
        fun actionStart(
            context: Context,
            roomId: Int,
            title: String,
            upName: String,
            online: Int
        ) {
            context.startActivity(
                Intent(context, LivePlayerActivity::class.java).apply {
                    putExtra("room_id", roomId)
                    putExtra("title", title)
                    putExtra("up_name", upName)
                    putExtra("online", online)
                    putExtra("danmaku_enabled", true)
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        keepScreenAwake()
        setContent {
            BVTheme {
                LivePlayerScreen()
            }
        }
        CastPlaybackSessionRegistry.register(castPlaybackSession)
    }

    override fun onResume() {
        super.onResume()
        keepScreenAwake()
    }

    override fun onDestroy() {
        super.onDestroy()
        CastPlaybackSessionRegistry.unregister(castPlaybackSession)
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun keepScreenAwake() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
