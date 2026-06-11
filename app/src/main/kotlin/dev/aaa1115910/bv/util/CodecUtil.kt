package dev.aaa1115910.bv.util

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Build
import android.util.Range
import androidx.annotation.RequiresApi
import androidx.core.util.toRange

object CodecUtil {
    fun parseCodecs(): List<CodecInfoData> {
        return MediaCodecList(MediaCodecList.ALL_CODECS)
            .codecInfos.toList()
            .mapNotNull { codecInfo ->
                runCatching { CodecInfoData.fromCodecInfo(codecInfo) }.getOrNull()
            }
    }

    fun readPlaybackCapabilityInfo(): PlaybackCapabilityInfo {
        val codecInfos = runCatching {
            MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos.toList()
        }.getOrDefault(emptyList())
        val decoders = codecInfos.filterNot { it.isEncoder }
        val profileNames = decoders.flatMap { codecInfo ->
            codecInfo.supportedTypes.flatMap { type ->
                runCatching {
                    codecInfo.getCapabilitiesForType(type).profileLevels.mapNotNull { profileLevel ->
                        MediaCodecInfo.CodecProfileLevel::class.java.fields
                            .firstOrNull { field ->
                                field.name.contains("Profile") &&
                                    runCatching { field.getInt(null) == profileLevel.profile }.getOrDefault(false)
                            }
                            ?.name
                    }
                }.getOrDefault(emptyList())
            }
        }

        fun supportsMime(mime: String): Boolean {
            return decoders.any { codecInfo ->
                codecInfo.supportedTypes.any { it.equals(mime, ignoreCase = true) }
            }
        }

        fun supportsProfile(keyword: String): Boolean {
            return profileNames.any { it.contains(keyword, ignoreCase = true) }
        }

        return PlaybackCapabilityInfo(
            hdr10 = if (supportsProfile("HDR10")) "支持" else "未知",
            hdr10Plus = if (supportsProfile("HDR10Plus") || supportsProfile("HDR10_PLUS")) "支持" else "未知",
            hlg = if (supportsProfile("HLG")) "支持" else "未知",
            dolbyVision = if (supportsMime("video/dolby-vision")) "支持" else "未知",
            hdrVivid = "系统未提供统一公开能力字段",
            dolbyAtmos = if (supportsMime("audio/eac3-joc") || supportsMime("audio/ac4")) "支持" else "未知"
        )
    }
}

data class PlaybackCapabilityInfo(
    val hdr10: String,
    val hdr10Plus: String,
    val hlg: String,
    val dolbyVision: String,
    val hdrVivid: String,
    val dolbyAtmos: String
) {
    fun toDisplayText(): String {
        return listOf(
            "HDR10：$hdr10",
            "HDR10+：$hdr10Plus",
            "HLG：$hlg",
            "杜比视界：$dolbyVision",
            "HDR Vivid：$hdrVivid",
            "杜比全景声：$dolbyAtmos"
        ).joinToString("\n")
    }
}

data class CodecInfoData(
    val name: String,
    val mimeType: String,
    val type: CodecType,
    val mode: CodecMode,
    val media: CodecMedia,
    //val codecProvider: CodecProvider,
    val maxSupportedInstances: Int?,
    val colorFormats: List<Int>,
    val audioBitrateRange: IntRange?,
    val videoBitrateRange: IntRange?,
    val videoFrame: IntRange?,
    val supportedFrameRates: List<SupportedFrameRate>,
    val achievableFrameRates: List<SupportedFrameRate>
) {
    companion object {
        fun fromCodecInfo(codecInfo: MediaCodecInfo): CodecInfoData {
            val capabilities = codecInfo.getCapabilitiesForType(codecInfo.supportedTypes.first())
            return CodecInfoData(
                name = codecInfo.name,
                mimeType = capabilities.mimeType,
                type = CodecType.fromMediaCodecInfo(codecInfo),
                mode = CodecMode.fromMediaCodecInfo(codecInfo),
                media = CodecMedia.fromMediaCodecInfo(codecInfo),
                maxSupportedInstances = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    capabilities.maxSupportedInstances else null,
                colorFormats = capabilities.colorFormats.toList(),
                audioBitrateRange = capabilities.audioCapabilities?.bitrateRange?.let { it.lower..it.upper },
                videoBitrateRange = capabilities.videoCapabilities?.bitrateRange?.let { it.lower..it.upper },
                videoFrame = capabilities.videoCapabilities?.supportedFrameRates?.let { it.lower..it.upper },
                supportedFrameRates = runCatching { codecInfo.getSupportedFrameRates() }
                    .getOrDefault(emptyList()),
                achievableFrameRates = runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) codecInfo.getAchievableFrameRates() else emptyList()
                }.getOrDefault(emptyList())
            )
        }
    }
}

enum class CodecType {
    Encoder,
    Decoder;

    companion object {
        fun fromMediaCodecInfo(mediaCodecInfo: MediaCodecInfo): CodecType {
            return if (mediaCodecInfo.isEncoder) Encoder else Decoder
        }

    }
}

enum class CodecMedia {
    Audio,
    Video;

    companion object {
        fun fromMediaCodecInfo(mediaCodecInfo: MediaCodecInfo): CodecMedia {
            return if (mediaCodecInfo.isAudioCodec()) Audio else Video
        }
    }
}

enum class CodecMode {
    Hardware,
    Software;

    companion object {
        fun fromMediaCodecInfo(mediaCodecInfo: MediaCodecInfo): CodecMode {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                return if (mediaCodecInfo.isSoftwareOnly) Software else Hardware
            }

            if (mediaCodecInfo.isAudioCodec()) return Software

            val name = mediaCodecInfo.name
            if (name.contains("omx.brcm.video", true)
                && name.contains("hw", true)
            ) return Hardware
            if (name.startsWith("omx.marvell.video.hw", true)) return Hardware
            if (name.startsWith("omx.intel.hw_vd", true)) return Hardware
            if (name.startsWith("omx.qcom", true) && name.endsWith("hw")) return Hardware
            if (name.startsWith("c2.vda.arc", true)
                || name.startsWith("arc.")
            ) return Hardware

            return if (
                name.startsWith("omx.google.", true)
                || name.contains("ffmpeg", true)
                || (name.startsWith("omx.sec.", true) && name.contains(".sw.", true))
                || name.equals("omx.qcom.video.decoder.hevcswvdec", true)
                || name.startsWith("c2.android.", true)
                || name.startsWith("c2.google.", true)
                || name.startsWith("omx.sprd.soft.", true)
                || name.startsWith("omx.avcodec.", true)
                || name.startsWith("omx.pv", true)
                || name.endsWith("sw", true)
                || name.endsWith("sw.dec", true)
                || name.endsWith("sw_vd", true)
                || (!name.startsWith("omx.", true) && !name.startsWith("c2.", true))
            ) Software else Hardware
        }
    }
}

private fun MediaCodecInfo.isAudioCodec(): Boolean {
    return supportedTypes.joinToString().contains("audio")
}

private val resolutions = mapOf(
    480 to 360,
    720 to 480,
    1280 to 720,
    1920 to 1080,
    2560 to 1440,
    3840 to 2160,
    7680 to 4320
)

data class SupportedFrameRate(
    val resolution: Pair<Int, Int>,
    val frameRate: Range<Double>,
    val unsupported: Boolean
)

private fun MediaCodecInfo.getSupportedFrameRates(): List<SupportedFrameRate> {
    return resolutions.map { (width, height) ->
        val frameRates = runCatching {
            val videoCapabilities = getCapabilitiesForType(supportedTypes.first()).videoCapabilities
            videoCapabilities?.getSupportedFrameRatesFor(width, height)
        }.getOrNull()
        SupportedFrameRate(
            resolution = width to height,
            frameRate = frameRates ?: ((0.0..0.0).toRange()),
            unsupported = frameRates == null
        )
    }
}

@RequiresApi(Build.VERSION_CODES.M)
private fun MediaCodecInfo.getAchievableFrameRates(): List<SupportedFrameRate> {
    return resolutions.map { (width, height) ->
        val frameRates = runCatching {
            val videoCapabilities = getCapabilitiesForType(supportedTypes.first()).videoCapabilities
            videoCapabilities?.getAchievableFrameRatesFor(width, height)
        }.getOrNull()
        SupportedFrameRate(
            resolution = width to height,
            frameRate = frameRates ?: ((0.0..0.0).toRange()),
            unsupported = frameRates == null
        )
    }
}
