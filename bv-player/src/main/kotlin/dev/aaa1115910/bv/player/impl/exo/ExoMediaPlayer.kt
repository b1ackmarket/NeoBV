package dev.aaa1115910.bv.player.impl.exo

import android.content.Context
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import dev.aaa1115910.bv.player.AbstractVideoPlayer
import dev.aaa1115910.bv.player.OkHttpUtil
import dev.aaa1115910.bv.player.VideoPlayerOptions
import dev.aaa1115910.bv.player.formatMinSec
import java.util.ArrayDeque
import java.util.Locale
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
class ExoMediaPlayer(
    private val context: Context,
    private val options: VideoPlayerOptions
) : AbstractVideoPlayer(), Player.Listener, AnalyticsListener {
    var mPlayer: ExoPlayer? = null
    protected var mMediaSource: MediaSource? = null
    private var droppedVideoFrames = 0
    private val bandwidthMeter = DefaultBandwidthMeter.Builder(context).build()
    private val realtimeSpeedSamples = ArrayDeque<TransferSample>()
    private val realtimeSpeedLock = Any()
    private var latestRealtimeSpeed = 0L
    private val transferListener = object : TransferListener {
        override fun onTransferInitializing(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) {
            bandwidthMeter.onTransferInitializing(source, dataSpec, isNetwork)
        }

        override fun onTransferStart(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) {
            bandwidthMeter.onTransferStart(source, dataSpec, isNetwork)
        }

        override fun onBytesTransferred(
            source: DataSource,
            dataSpec: DataSpec,
            isNetwork: Boolean,
            bytesTransferred: Int
        ) {
            bandwidthMeter.onBytesTransferred(source, dataSpec, isNetwork, bytesTransferred)
            if (isNetwork && bytesTransferred > 0) {
                recordRealtimeSpeedSample(bytesTransferred)
            }
        }

        override fun onTransferEnd(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) {
            bandwidthMeter.onTransferEnd(source, dataSpec, isNetwork)
        }
    }

    @OptIn(UnstableApi::class)
    private val dataSourceFactory =
        OkHttpDataSource.Factory(OkHttpUtil.generateCustomSslOkHttpClient(context)).apply {
            options.userAgent?.let { setUserAgent(it) }
            options.referer?.let { setDefaultRequestProperties(mapOf("referer" to it)) }
            setTransferListener(transferListener)
        }

    init {
        initPlayer()
    }

    @OptIn(UnstableApi::class)
    override fun initPlayer() {
        val renderersFactory = object : DefaultRenderersFactory(context) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): androidx.media3.exoplayer.audio.AudioSink {
                if (!options.enableVolumeNormalization) {
                    return super.buildAudioSink(
                        context,
                        enableFloatOutput,
                        enableAudioTrackPlaybackParams
                    ) ?: DefaultAudioSink.Builder(context).build()
                }
                return DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .setAudioProcessors(arrayOf(SimpleVolumeNormalizerAudioProcessor()))
                    .build()
            }
        }.apply {
            setExtensionRendererMode(
                when (options.enableFfmpegAudioRenderer) {
                    true -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
                    false -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
                }
            )
            if (options.enableSoftwareVideoDecoder) {
                // 强制软件解码
                setMediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
                    val allDecoders = MediaCodecUtil.getDecoderInfos(
                        mimeType,
                        requiresSecureDecoder,
                        requiresTunnelingDecoder
                    )
                    val softwareDecoders = allDecoders.filter {
                        it.name.startsWith("OMX.google.") || it.name.startsWith("c2.android.")
                    }
                    // 兜底回退
                    softwareDecoders.ifEmpty { allDecoders }
                }

            } else {
                // 默认硬件解码
                setMediaCodecSelector(MediaCodecSelector.DEFAULT)
            }
        }
        mPlayer = ExoPlayer
            .Builder(context)
            .setRenderersFactory(renderersFactory)
            .setBandwidthMeter(bandwidthMeter)
            .setSeekForwardIncrementMs(1000 * 10)
            .setSeekBackIncrementMs(1000 * 5)
            .build()

        initListener()
    }

    private fun initListener() {
        mPlayer?.addListener(this)
        mPlayer?.addAnalyticsListener(this)
    }

    @OptIn(UnstableApi::class)
    override fun setHeader(headers: Map<String, String>) {

    }

    @OptIn(UnstableApi::class)
    override fun playUrl(videoUrl: String?, audioUrl: String?) {
        if (audioUrl == null && videoUrl?.contains(".m3u8") == true) {
            mMediaSource = HlsMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(videoUrl))
            return
        }

        val videoMediaSource = videoUrl?.let {
            ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(it))
        }
        val audioMediaSource = audioUrl?.let {
            ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(it))
        }

        val mediaSources = listOfNotNull(videoMediaSource, audioMediaSource)
        mMediaSource = MergingMediaSource(*mediaSources.toTypedArray())
    }

    @OptIn(UnstableApi::class)
    override fun playDash(mpdUrl: String) {
        val mediaItem = MediaItem.Builder()
            .setUri(mpdUrl)
            .setMimeType(MimeTypes.APPLICATION_MPD)
            .build()
        mMediaSource = DashMediaSource.Factory(dataSourceFactory)
            .createMediaSource(mediaItem)
    }

    @OptIn(UnstableApi::class)
    override fun prepare() {
        mPlayer?.setMediaSource(mMediaSource!!)
        mPlayer?.prepare()
    }

    override fun start() {
        mPlayer?.play()
    }

    override fun pause() {
        mPlayer?.pause()
    }

    override fun stop() {
        mPlayer?.stop()
    }

    override fun reset() {
        TODO("Not yet implemented")
    }

    override val isPlaying: Boolean
        get() = mPlayer?.isPlaying == true

    override fun seekTo(time: Long) {
        mPlayer?.seekTo(time)
    }

    override fun release() {
        mPlayer?.release()
    }

    override val currentPosition: Long
        get() = mPlayer?.currentPosition ?: 0
    override val duration: Long
        get() = mPlayer?.duration ?: 0
    override val bufferedPercentage: Int
        get() = mPlayer?.bufferedPercentage ?: 0

    override fun setOptions() {
        mPlayer?.playWhenReady = true
    }

    override var speed: Float
        get() = mPlayer?.playbackParameters?.speed ?: 1f
        set(value) {
            mPlayer?.setPlaybackSpeed(value)
        }
    override val tcpSpeed: Long
        get() = getRealtimeNetworkSpeed()

    override fun onPlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
            Player.STATE_IDLE -> {}
            Player.STATE_BUFFERING -> mPlayerEventListener?.onBuffering()
            Player.STATE_READY -> mPlayerEventListener?.onReady()
            Player.STATE_ENDED -> mPlayerEventListener?.onEnd()
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (isPlaying) {
            mPlayerEventListener?.onPlay()
        } else {
            mPlayerEventListener?.onPause()
        }
    }

    override fun onSeekBackIncrementChanged(seekBackIncrementMs: Long) {
        mPlayerEventListener?.onSeekBack(seekBackIncrementMs)
    }

    override fun onSeekForwardIncrementChanged(seekForwardIncrementMs: Long) {
        mPlayerEventListener?.onSeekForward(seekForwardIncrementMs)
    }

    override val debugInfo: String
        get() {
            val player = mPlayer
            return buildString {
                appendLine("player: ${androidx.media3.common.MediaLibraryInfo.VERSION_SLASHY}")
                appendLine("time: ${currentPosition.formatMinSec()} / ${duration.formatMinSec()}")
                appendLine("buffered: $bufferedPercentage%")
                appendLine("speed: ${formatSpeed(speed)}")
                appendLine("network speed: ${formatBitrate(tcpSpeed)}")
                appendLine("dropped frames: $droppedVideoFrames")
                append("track groups: ${player?.currentTracks?.groups?.size ?: 0}")
            }
        }

    override val videoWidth: Int
        get() = mPlayer?.videoSize?.width ?: 0
    override val videoHeight: Int
        get() = mPlayer?.videoSize?.height ?: 0

    override fun onPlayerError(error: PlaybackException) {
        mPlayerEventListener?.onError(error)
    }

    override fun onDroppedVideoFrames(
        eventTime: AnalyticsListener.EventTime,
        droppedFrames: Int,
        elapsedMs: Long
    ) {
        droppedVideoFrames += droppedFrames
    }

    private fun recordRealtimeSpeedSample(bytesTransferred: Int) {
        val now = SystemClock.elapsedRealtime()
        synchronized(realtimeSpeedLock) {
            realtimeSpeedSamples.addLast(TransferSample(now, bytesTransferred.toLong()))
            latestRealtimeSpeed = calculateRealtimeNetworkSpeedLocked(now)
        }
    }

    private fun getRealtimeNetworkSpeed(): Long {
        val now = SystemClock.elapsedRealtime()
        return synchronized(realtimeSpeedLock) {
            latestRealtimeSpeed = calculateRealtimeNetworkSpeedLocked(now)
            latestRealtimeSpeed
        }
    }

    private fun calculateRealtimeNetworkSpeedLocked(now: Long): Long {
        while (realtimeSpeedSamples.isNotEmpty()) {
            val firstSample = realtimeSpeedSamples.peekFirst() ?: break
            if (now - firstSample.timeMs <= RealtimeSpeedWindowMs) break
            realtimeSpeedSamples.removeFirst()
        }
        val lastSample = realtimeSpeedSamples.peekLast()
        if (lastSample == null || now - lastSample.timeMs > RealtimeSpeedIdleTimeoutMs) {
            realtimeSpeedSamples.clear()
            return 0L
        }

        val bytesInWindow = realtimeSpeedSamples.sumOf { it.bytes }
        return bytesInWindow * 8_000L / RealtimeSpeedWindowMs
    }

    private fun formatSpeed(speed: Float): String {
        return String.format(Locale.US, "%.2fx", speed)
    }

    private fun formatBitrate(bitrate: Long): String {
        if (bitrate <= 0L) return "0 kbps"
        return if (bitrate >= 1_000_000) {
            String.format(Locale.US, "%.2f Mbps", bitrate / 1_000_000f)
        } else {
            "${bitrate / 1000} kbps"
        }
    }

    private data class TransferSample(
        val timeMs: Long,
        val bytes: Long
    )

    private companion object {
        const val RealtimeSpeedWindowMs = 1_000L
        const val RealtimeSpeedIdleTimeoutMs = 1_200L
    }
}

@OptIn(UnstableApi::class)
private class SimpleVolumeNormalizerAudioProcessor : BaseAudioProcessor() {
    private var gain = 1f

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != androidx.media3.common.C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val inputSize = inputBuffer.remaining()
        if (inputSize == 0) return
        val outputBuffer = replaceOutputBuffer(inputSize).order(ByteOrder.nativeOrder())
        val duplicate = inputBuffer.slice().order(ByteOrder.nativeOrder())
        var peak = 0
        while (duplicate.remaining() >= 2) {
            peak = max(peak, abs(duplicate.short.toInt()))
        }
        val targetGain = if (peak > 0) {
            (TargetPcmPeak / peak.toFloat()).coerceIn(MinGain, MaxGain)
        } else {
            gain
        }
        gain = gain * Smoothing + targetGain * (1f - Smoothing)
        val samples = inputBuffer.slice().order(ByteOrder.nativeOrder())
        while (samples.remaining() >= 2) {
            val normalized = (samples.short * gain)
                .roundToInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            outputBuffer.putShort(normalized.toShort())
        }
        if (samples.hasRemaining()) {
            outputBuffer.put(samples.get())
        }
        inputBuffer.position(inputBuffer.limit())
        outputBuffer.flip()
    }

    private companion object {
        const val TargetPcmPeak = 12_000f
        const val MinGain = 0.35f
        const val MaxGain = 3.0f
        const val Smoothing = 0.96f
    }
}
