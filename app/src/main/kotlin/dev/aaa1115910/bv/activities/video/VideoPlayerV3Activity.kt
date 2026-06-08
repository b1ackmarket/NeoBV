package dev.aaa1115910.bv.activities.video

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import dev.aaa1115910.biliapi.entity.user.Author
import dev.aaa1115910.bv.activities.ImmersiveComponentActivity
import dev.aaa1115910.bv.cast.CastPlaybackSession
import dev.aaa1115910.bv.cast.CastPlaybackSessionRegistry
import dev.aaa1115910.bv.cast.CastPlaybackSnapshot
import dev.aaa1115910.bv.cast.CastTransportState
import dev.aaa1115910.bv.component.controllers.DanmakuType
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.screen.VideoPlayerV3Screen
import dev.aaa1115910.bv.ui.state.PlayerState
import dev.aaa1115910.bv.ui.theme.BVTheme
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.viewmodel.player.DanmakuSettingAction
import dev.aaa1115910.bv.viewmodel.player.MediaProfileSettingAction
import dev.aaa1115910.bv.viewmodel.player.VideoPlayerV3ViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.androidx.viewmodel.ext.android.viewModel

class VideoPlayerV3Activity : ImmersiveComponentActivity() {
    private val playerViewModel: VideoPlayerV3ViewModel by viewModel()
    private val castPlaybackSession = object : CastPlaybackSession {
        private var commandedState: CastTransportState? = null
        private var commandedStateAtMs: Long = 0L
        private var commandedPositionMs: Long? = null
        private var commandedPositionAtMs: Long = 0L

        override fun play() {
            commandedState = CastTransportState.PLAYING
            commandedStateAtMs = System.currentTimeMillis()
            playerViewModel.videoPlayer?.start()
        }

        override fun pause() {
            commandedState = CastTransportState.PAUSED
            commandedStateAtMs = System.currentTimeMillis()
            playerViewModel.videoPlayer?.pause()
            playerViewModel.danmakuPlayer?.pause()
        }

        override fun stop() {
            commandedState = CastTransportState.STOPPED
            commandedStateAtMs = System.currentTimeMillis()
            playerViewModel.videoPlayer?.stop()
            playerViewModel.danmakuPlayer?.pause()
            if (!isFinishing) {
                finish()
            }
        }

        override fun seekTo(positionMs: Long) {
            val durationMs = (playerViewModel.videoPlayer?.duration
                ?: playerViewModel.seekerState.value.totalDuration)
                .coerceAtLeast(0L)
            val targetMs = if (durationMs > 0L) {
                positionMs.coerceIn(0L, durationMs)
            } else {
                positionMs.coerceAtLeast(0L)
            }
            commandedPositionMs = targetMs
            commandedPositionAtMs = System.currentTimeMillis()
            playerViewModel.seekToTime(targetMs)
        }

        override fun setSpeed(speed: Float) {
            playerViewModel.updatePlaySpeed(speed = speed)
        }

        override fun setQuality(qualityId: Int) {
            playerViewModel.updateMediaProfile(MediaProfileSettingAction.SetQuality(qualityId))
        }

        override fun setDanmakuEnabled(enabled: Boolean) {
            val currentEnabled = playerViewModel.uiState.value.danmakuState.enabledTypes.isNotEmpty()
            if (currentEnabled != enabled) {
                playerViewModel.updateDanmakuState(DanmakuSettingAction.ToggleEnabled)
            }
        }

        override fun snapshot(): CastPlaybackSnapshot {
            val uiState = playerViewModel.uiState.value
            val seekerState = playerViewModel.seekerState.value
            val player = playerViewModel.videoPlayer
            val actualTransportState = when {
                uiState.isBuffering -> CastTransportState.TRANSITIONING
                player?.isPlaying == true -> CastTransportState.PLAYING
                uiState.playerState == PlayerState.Playing -> CastTransportState.PLAYING
                uiState.playerState == PlayerState.Paused || uiState.playerState == PlayerState.Ready -> CastTransportState.PAUSED
                else -> CastTransportState.STOPPED
            }
            val nowMs = System.currentTimeMillis()
            if (commandedState != null && actualTransportState == commandedState) {
                commandedState = null
            }
            val transportState = commandedState
                ?.takeIf { nowMs - commandedStateAtMs <= CAST_COMMAND_OPTIMISTIC_WINDOW_MS }
                ?: actualTransportState
            val actualPositionMs = (player?.currentPosition ?: seekerState.currentTime).coerceAtLeast(0L)
            val pendingStartPositionMs = uiState.lastPlayed.toLong().takeIf { it > 0L }
            if (commandedPositionMs != null &&
                kotlin.math.abs(actualPositionMs - commandedPositionMs!!) <= CAST_SEEK_POSITION_TOLERANCE_MS
            ) {
                commandedPositionMs = null
            }
            val positionMs = commandedPositionMs
                ?.takeIf { nowMs - commandedPositionAtMs <= CAST_COMMAND_OPTIMISTIC_WINDOW_MS }
                ?: pendingStartPositionMs
                ?: actualPositionMs
            return CastPlaybackSnapshot(
                state = transportState,
                positionMs = positionMs,
                durationMs = (player?.duration ?: seekerState.totalDuration).coerceAtLeast(0L),
                speed = uiState.playSpeed,
                aid = uiState.aid,
                cid = uiState.cid,
                epid = uiState.epid,
                seasonId = uiState.seasonId,
                title = uiState.title,
                qualityId = uiState.mediaProfileState.qualityId,
                availableQuality = uiState.availableQuality,
                danmakuEnabled = uiState.danmakuState.enabledTypes.isNotEmpty()
            )
        }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
        private var currentInstance: VideoPlayerV3Activity? = null
        private const val CAST_COMMAND_OPTIMISTIC_WINDOW_MS = 3_000L
        private const val CAST_SEEK_POSITION_TOLERANCE_MS = 1_500L

        fun actionStart(
            context: Context,
            avid: Long,
            cid: Long,
            title: String,
            partTitle: String,
            played: Int,
            fromSeason: Boolean,
            subType: Int? = null,
            epid: Int? = null,
            seasonId: Int? = null,
            proxyArea: ProxyArea = ProxyArea.MainLand,
            author: Author? = null
        ) {
            currentInstance?.finish()
            context.startActivity(
                Intent(context, VideoPlayerV3Activity::class.java).apply {
                    putExtra("avid", avid)
                    putExtra("cid", cid)
                    putExtra("title", title)
                    putExtra("partTitle", partTitle)
                    putExtra("played", played)
                    putExtra("fromSeason", fromSeason)
                    putExtra("subType", subType)
                    putExtra("epid", epid)
                    putExtra("seasonId", seasonId)
                    putExtra("proxy_area", proxyArea.ordinal)
                    putExtra("author_mid", author?.mid)
                    putExtra("author_name", author?.name)
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        currentInstance = this

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            BVTheme {
                VideoPlayerV3Screen()
            }
        }

        // 初始化viewmodel参数
        initViewModelFromIntent()
        // 初始化播放器
        playerViewModel.initVideoPlayer(applicationContext)
        CastPlaybackSessionRegistry.register(castPlaybackSession)
        // 初始化弹幕播放器
        playerViewModel.initDanmakuPlayer()
        // 加载视频资源并播放
        playerViewModel.loadVideoWithResources()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()

        playerViewModel.videoPlayer?.pause()
        playerViewModel.danmakuPlayer?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (currentInstance === this) {
            currentInstance = null
        }
        CastPlaybackSessionRegistry.unregister(castPlaybackSession)
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (isFinishing) {
            playerViewModel.detachPlayer()
            playerViewModel.releaseDanmakuPlayer()
        }
    }

    private fun initViewModelFromIntent() {
        if (intent.hasExtra("external_media_url")) {
            val mediaUrl = intent.getStringExtra("external_media_url").orEmpty()
            val title = intent.getStringExtra("title") ?: "投屏视频"
            val played = intent.getIntExtra("played", 0)
            val playSpeed = intent.getFloatExtra("play_speed", 0f)
            val isBilibiliMedia = intent.getBooleanExtra("external_media_bilibili", false)
            logger.fInfo { "Launch external cast media: [$mediaUrl]" }

            playerViewModel.initExternalMedia(
                mediaUrl = mediaUrl,
                title = title,
                lastPlayed = played,
                isBilibiliMedia = isBilibiliMedia
            )
            if (playSpeed > 0f) {
                playerViewModel.updatePlaySpeed(speed = playSpeed)
            }
        } else if (intent.hasExtra("avid")) {
            val aid = intent.getLongExtra("avid", 170001)
            val cid = intent.getLongExtra("cid", 170001)
            val title = intent.getStringExtra("title") ?: "Unknown Title"
            val played = intent.getIntExtra("played", 0)
            val fromSeason = intent.getBooleanExtra("fromSeason", false)
            val subType = intent.getIntExtra("subType", 0)
            val epid = intent.getIntExtra("epid", 0)
            val seasonId = intent.getIntExtra("seasonId", 0)
            val playSpeed = intent.getFloatExtra("play_speed", 0f)
            val castQuality = intent.getIntExtra("cast_quality", 0)
            val proxyArea = ProxyArea.entries[intent.getIntExtra("proxy_area", 0)]
            val author_mid = intent.getLongExtra("author_mid", 0)
            val author_name = intent.getStringExtra("author_name")
            logger.fInfo { "Launch parameter: [aid=$aid, cid=$cid]" }

            playerViewModel.init(
                aid = aid,
                cid = cid,
                epid = epid.takeIf { it != 0 },
                title = title,
                lastPlayed = played,
                fromSeason = fromSeason,
                subType = subType,
                seasonId = seasonId,
                proxyArea = proxyArea,
                authorMid = author_mid,
                authorName = author_name ?: ""
            )
            if (castQuality > 0) {
                playerViewModel.updateMediaProfile(MediaProfileSettingAction.SetQuality(castQuality))
            }
            if (playSpeed > 0f) {
                playerViewModel.updatePlaySpeed(speed = playSpeed)
            }
            if (intent.hasExtra("danmaku_enabled")) {
                applyCastDanmakuState(intent.getBooleanExtra("danmaku_enabled", true))
            }
        } else {
            logger.fInfo { "Null launch parameter" }
        }
    }

    private fun applyCastDanmakuState(enabled: Boolean) {
        val currentState = playerViewModel.uiState.value.danmakuState
        val enabledTypes = if (enabled) {
            currentState.enabledTypes.takeIf { it.isNotEmpty() }
                ?: currentState.lastEnabledTypes.takeIf { it.isNotEmpty() }
                ?: DanmakuType.entries
        } else {
            emptyList()
        }
        playerViewModel.updateDanmakuState(
            DanmakuSettingAction.SetEnabledTypes(
                types = enabledTypes,
                persist = false
            )
        )
    }
}
