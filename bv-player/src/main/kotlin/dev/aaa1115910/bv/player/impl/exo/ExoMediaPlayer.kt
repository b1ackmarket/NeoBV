package dev.aaa1115910.bv.player.impl.exo

import android.content.Context
import android.media.MediaFormat
import android.os.Handler
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.C
import androidx.media3.common.VideoSize
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
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.exoplayer.video.VideoFrameMetadataListener
import androidx.media3.exoplayer.video.VideoRendererEventListener
import dev.aaa1115910.bv.player.AbstractVideoPlayer
import dev.aaa1115910.bv.player.OkHttpUtil
import dev.aaa1115910.bv.player.VideoPlayerOptions
import dev.aaa1115910.bv.player.formatMinSec
import java.util.ArrayDeque
import java.util.Locale
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.net.URI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
class ExoMediaPlayer(
    private val context: Context,
    private val options: VideoPlayerOptions
) : AbstractVideoPlayer(), Player.Listener, AnalyticsListener, VideoFrameMetadataListener {
    var mPlayer: ExoPlayer? = null
    protected var mMediaSource: MediaSource? = null
    private var droppedVideoFrames = 0
    private val bandwidthMeter = DefaultBandwidthMeter.Builder(context).build()
    private val realtimeSpeedSamples = ArrayDeque<TransferSample>()
    private val realtimeSpeedLock = Any()
    private var latestRealtimeSpeed = 0L
    private val renderedVideoFrameSamples = ArrayDeque<Long>()
    private val renderedVideoFrameLock = Any()
    private var latestRenderedVideoFps = 0f
    private var currentMediaUrl: String = ""
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
            override fun buildVideoRenderers(
                context: Context,
                extensionRendererMode: Int,
                mediaCodecSelector: MediaCodecSelector,
                enableDecoderFallback: Boolean,
                eventHandler: Handler,
                eventListener: VideoRendererEventListener,
                allowedVideoJoiningTimeMs: Long,
                out: ArrayList<Renderer>
            ) {
                // The bundled ffmpeg AAR includes a video renderer that crashes after interface-method desugaring.
                super.buildVideoRenderers(
                    context,
                    EXTENSION_RENDERER_MODE_OFF,
                    mediaCodecSelector,
                    enableDecoderFallback,
                    eventHandler,
                    eventListener,
                    allowedVideoJoiningTimeMs,
                    out
                )
            }

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
            .setVideoChangeFrameRateStrategy(C.VIDEO_CHANGE_FRAME_RATE_STRATEGY_OFF)
            .setSeekForwardIncrementMs(1000 * 10)
            .setSeekBackIncrementMs(1000 * 5)
            .build()

        initListener()
    }

    private fun initListener() {
        mPlayer?.addListener(this)
        mPlayer?.addAnalyticsListener(this)
        mPlayer?.setVideoFrameMetadataListener(this)
    }

    @OptIn(UnstableApi::class)
    override fun setHeader(headers: Map<String, String>) {

    }

    @OptIn(UnstableApi::class)
    override fun playUrl(videoUrl: String?, audioUrl: String?) {
        currentMediaUrl = videoUrl.orEmpty()
        if (audioUrl == null && videoUrl?.contains(".m3u8") == true) {
            playHls(videoUrl)
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
    override fun playDash(
        mpdUrl: String,
        drmLicenseUrl: String?,
        drmRequestHeaders: Map<String, String>
    ) {
        currentMediaUrl = mpdUrl
        val mediaItemBuilder = MediaItem.Builder()
            .setUri(mpdUrl)
            .setMimeType(MimeTypes.APPLICATION_MPD)
        if (!drmLicenseUrl.isNullOrBlank()) {
            mediaItemBuilder.setDrmConfiguration(
                MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
                    .setLicenseUri(drmLicenseUrl)
                    .setLicenseRequestHeaders(drmRequestHeaders)
                    .setMultiSession(true)
                    .build()
            )
        }
        val mediaItem = mediaItemBuilder.build()
        mMediaSource = DashMediaSource.Factory(dataSourceFactory)
            .createMediaSource(mediaItem)
    }

    @OptIn(UnstableApi::class)
    override fun playHls(hlsUrl: String) {
        currentMediaUrl = hlsUrl
        val mediaItem = MediaItem.Builder()
            .setUri(hlsUrl)
            .setMimeType(MimeTypes.APPLICATION_M3U8)
            .build()
        mMediaSource = HlsMediaSource.Factory(dataSourceFactory)
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
        mPlayer?.clearVideoFrameMetadataListener(this)
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
    override var volume: Float
        get() = mPlayer?.volume ?: 1f
        set(value) {
            mPlayer?.volume = value.coerceIn(0f, 1f)
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
            val selectedVideoFormat = player?.currentTracks?.selectedFormatFor(C.TRACK_TYPE_VIDEO)
            val selectedAudioFormat = player?.currentTracks?.selectedFormatFor(C.TRACK_TYPE_AUDIO)
            return buildString {
                selectedVideoFormat?.sampleMimeType?.let { mimeType ->
                    val codecs = listOfNotNull(
                        selectedVideoFormat.codecs?.takeIf { it.isNotBlank() },
                        selectedAudioFormat?.codecs?.takeIf { it.isNotBlank() }
                    ).joinToString(",")
                    appendLine("mime type: $mimeType${codecs.takeIf { it.isNotBlank() }?.let { "; codecs=\"$it\"" }.orEmpty()}")
                }
                selectedVideoFormat?.toVideoCodecLine()?.let { appendLine(it) }
                selectedVideoFormat?.toVideoInfoLine()?.let { appendLine(it) }
                getRenderedVideoFps().takeIf { it > 0f }?.let { fps ->
                    appendLine("video fps: ${formatFrameRate(fps)}")
                }
                selectedAudioFormat?.toAudioCodecLine()?.let { appendLine(it) }
                selectedAudioFormat?.toAudioInfoLine()?.let { appendLine(it) }
                currentMediaUrl.toHostOrNull()?.let { appendLine("stream host: $it") }
                appendLine("player: ${androidx.media3.common.MediaLibraryInfo.VERSION_SLASHY}")
                appendLine("time: ${currentPosition.formatMinSec()} / ${duration.formatMinSec()}")
                appendLine("buffered: $bufferedPercentage%")
                player?.bufferedPosition?.let { bufferedPosition ->
                    appendLine("buffer length: ${formatBufferLength(bufferedPosition - currentPosition)}")
                }
                appendLine("download bitrate: ${formatBitrate(tcpSpeed)}")
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

    override fun onVideoSizeChanged(videoSize: VideoSize) {
        mPlayerEventListener?.onVideoSizeChanged(videoSize.width, videoSize.height)
    }

    override fun onDroppedVideoFrames(
        eventTime: AnalyticsListener.EventTime,
        droppedFrames: Int,
        elapsedMs: Long
    ) {
        droppedVideoFrames += droppedFrames
    }

    override fun onVideoFrameAboutToBeRendered(
        presentationTimeUs: Long,
        releaseTimeNs: Long,
        format: Format,
        mediaFormat: MediaFormat?
    ) {
        recordRenderedVideoFrame()
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

    private fun recordRenderedVideoFrame() {
        val now = SystemClock.elapsedRealtime()
        synchronized(renderedVideoFrameLock) {
            renderedVideoFrameSamples.addLast(now)
            latestRenderedVideoFps = calculateRenderedVideoFpsLocked(now)
        }
    }

    private fun getRenderedVideoFps(): Float {
        val now = SystemClock.elapsedRealtime()
        return synchronized(renderedVideoFrameLock) {
            latestRenderedVideoFps = calculateRenderedVideoFpsLocked(now)
            latestRenderedVideoFps
        }
    }

    private fun calculateRenderedVideoFpsLocked(now: Long): Float {
        while (renderedVideoFrameSamples.isNotEmpty()) {
            val firstSample = renderedVideoFrameSamples.peekFirst() ?: break
            if (now - firstSample <= RenderedFpsWindowMs) break
            renderedVideoFrameSamples.removeFirst()
        }
        val firstSample = renderedVideoFrameSamples.peekFirst()
        val lastSample = renderedVideoFrameSamples.peekLast()
        if (firstSample == null || lastSample == null || now - lastSample > RenderedFpsIdleTimeoutMs) {
            renderedVideoFrameSamples.clear()
            return 0f
        }
        val elapsedMs = (lastSample - firstSample).coerceAtLeast(1L)
        return (renderedVideoFrameSamples.size - 1).coerceAtLeast(0) * 1000f / elapsedMs
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

    private fun formatBitrate(bitrate: Long): String {
        if (bitrate <= 0L) return "0 kbps"
        return if (bitrate >= 1_000_000) {
            String.format(Locale.US, "%.2f Mbps", bitrate / 1_000_000f)
        } else {
            "${bitrate / 1000} kbps"
        }
    }

    private fun formatBufferLength(bufferLengthMs: Long): String {
        return String.format(Locale.US, "%.3fs", bufferLengthMs.coerceAtLeast(0L) / 1000f)
    }

    private data class TransferSample(
        val timeMs: Long,
        val bytes: Long
    )

    private companion object {
        const val RealtimeSpeedWindowMs = 1_000L
        const val RealtimeSpeedIdleTimeoutMs = 1_200L
        const val RenderedFpsWindowMs = 1_000L
        const val RenderedFpsIdleTimeoutMs = 1_500L
    }
}

@OptIn(UnstableApi::class)
private fun Tracks.selectedFormatFor(trackType: Int): Format? {
    return groups
        .asSequence()
        .filter { it.type == trackType && it.isSelected }
        .flatMap { group ->
            (0 until group.length).asSequence()
                .filter { group.isTrackSelected(it) }
                .map { group.getTrackFormat(it) }
        }
        .firstOrNull()
}

private fun Format.toVideoInfoLine(): String? {
    val resolution = if (width > 0 && height > 0) "${width}x$height" else ""
    val frameRateText = frameRate.takeIf { it > 0f }?.let {
        "${String.format(Locale.US, "%.0f", it)}FPS"
    }.orEmpty()
    val bitrateText = debugBitrate.takeIf { it > 0 }?.let { formatFormatBitrate(it.toLong()) }.orEmpty()
    val value = listOf(resolution, frameRateText, bitrateText)
        .filter { it.isNotBlank() }
        .joinToString(", ")
    return value.takeIf { it.isNotBlank() }?.let { "video info: $it" }
}

private fun Format.toVideoCodecLine(): String? =
    resolveCodecName(sampleMimeType, codecs)
        ?.let { "video codec: $it" }

private fun Format.toAudioCodecLine(): String? =
    resolveCodecName(sampleMimeType, codecs)
        ?.let { "audio codec: $it" }

private fun resolveCodecName(mimeType: String?, codecs: String?): String? {
    val codec = codecs
        ?.split(',')
        ?.map { it.trim() }
        ?.firstOrNull { it.isNotBlank() }
    val normalized = listOfNotNull(codec, mimeType).joinToString(separator = " ").lowercase(Locale.US)
    return when {
        normalized.contains("hev1") || normalized.contains("hvc1") ||
            normalized.contains("h265") || normalized.contains("hevc") -> "hevc"
        normalized.contains("avc1") || normalized.contains("avc3") ||
            normalized.contains("h264") || normalized.contains("avc") -> "avc"
        normalized.contains("av01") || normalized.contains("av1") -> "av1"
        normalized.contains("mp4a") || normalized.contains("aac") -> "aac"
        normalized.contains("opus") -> "opus"
        normalized.contains("ec-3") || normalized.contains("eac3") -> "eac3"
        normalized.contains("ac-3") || normalized.contains("ac3") -> "ac3"
        codec != null -> codec.substringBefore('.').takeIf { it.isNotBlank() }
        mimeType != null -> mimeType.substringAfterLast('/').takeIf { it.isNotBlank() }
        else -> null
    }
}

private fun formatFrameRate(frameRate: Float): String =
    if (abs(frameRate - frameRate.roundToInt()) < 0.05f) {
        frameRate.roundToInt().toString()
    } else {
        String.format(Locale.US, "%.1f", frameRate)
    }

private fun Format.toAudioInfoLine(): String? {
    val sampleRateText = sampleRate.takeIf { it > 0 }?.let { "${it / 1000}KHz" }.orEmpty()
    val channelText = channelCount.takeIf { it > 0 }?.let {
        when (it) {
            1 -> "Mono"
            2 -> "Stereo"
            else -> "${it}ch"
        }
    }.orEmpty()
    val bitrateText = debugBitrate.takeIf { it > 0 }?.let { formatFormatBitrate(it.toLong()) }.orEmpty()
    val value = listOf(sampleRateText, channelText, bitrateText)
        .filter { it.isNotBlank() }
        .joinToString(", ")
    return value.takeIf { it.isNotBlank() }?.let { "audio info: $it" }
}

private fun String.toHostOrNull(): String? =
    runCatching { URI(this).host }.getOrNull()?.takeIf { it.isNotBlank() }

private fun formatFormatBitrate(bitrate: Long): String {
    return if (bitrate >= 1_000_000) {
        String.format(Locale.US, "%.2f Mbps", bitrate / 1_000_000f)
    } else {
        "${bitrate / 1000} kbps"
    }
}

private val Format.debugBitrate: Int
    get() = averageBitrate.takeIf { it > 0 } ?: peakBitrate

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
