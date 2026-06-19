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
import dev.aaa1115910.bv.util.Prefs

class LivePlayerActivity : ImmersiveComponentActivity() {
    @Volatile
    var castController: LiveCastController? = null

    private val castPlaybackSession = object : CastPlaybackSession {
        override fun play() {
            castController?.play()
        }

        override fun pause() {
            castController?.pause()
        }

        override fun stop() {
            castController?.stop() ?: finish()
        }

        override fun seekTo(positionMs: Long) = Unit

        override fun setSpeed(speed: Float) {
            castController?.setSpeed(speed)
        }

        override fun setQuality(qualityId: Int) {
            castController?.setQuality(qualityId)
        }

        override fun setDanmakuEnabled(enabled: Boolean) {
            castController?.setDanmakuEnabled(enabled)
        }

        override fun snapshot(): CastPlaybackSnapshot =
            castController?.snapshot() ?: CastPlaybackSnapshot(
                state = CastTransportState.PLAYING,
                roomId = intent.getIntExtra("room_id", 0).toLong(),
                title = intent.getStringExtra("title").orEmpty(),
                danmakuEnabled = Prefs.defaultLiveDanmakuEnabled
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

    override fun onPause() {
        super.onPause()
        castController?.pause()
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

interface LiveCastController {
    fun play()
    fun pause()
    fun stop()
    fun setSpeed(speed: Float)
    fun setQuality(qualityId: Int)
    fun setDanmakuEnabled(enabled: Boolean)
    fun snapshot(): CastPlaybackSnapshot
}
