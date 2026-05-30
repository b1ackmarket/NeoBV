package dev.aaa1115910.bv.repository

import dev.aaa1115910.biliapi.repositories.VideoDetailRepository
import dev.aaa1115910.biliapi.repositories.VideoPlayRepository
import dev.aaa1115910.bv.entity.Audio
import dev.aaa1115910.bv.entity.Resolution
import dev.aaa1115910.bv.entity.VideoCodec
import dev.aaa1115910.bv.util.Prefs
import org.koin.core.annotation.Single

data class PreviewPlaybackUrls(
    val videoUrl: String,
    val audioUrl: String?
)

@Single
class VideoPreviewRepository(
    private val videoDetailRepository: VideoDetailRepository,
    private val videoPlayRepository: VideoPlayRepository
) {
    suspend fun resolvePreviewUrls(aid: Long, cid: Long? = null): PreviewPlaybackUrls? {
        if (aid <= 0L) return null
        val targetCid = cid?.takeIf { it > 0 }
            ?: videoDetailRepository.getUgcPages(aid = aid, preferApiType = Prefs.playbackApiType)
                .firstOrNull()
                ?.cid
            ?: return null

        val playData = videoPlayRepository.getPlayData(
            aid = aid,
            cid = targetCid,
            preferApiType = Prefs.playbackApiType
        )

        val preferredQuality = Resolution.R480P.code
        val videoItem = playData.dashVideos
            .filter { it.quality <= preferredQuality }
            .maxWithOrNull(compareBy({ it.quality }, { codecPriority(it.codecs) }))
            ?: playData.dashVideos.minByOrNull { it.quality }
            ?: return null

        val audioItem = playData.dashAudios.minByOrNull { audioPriority(Audio.fromCode(it.codecId)) }
            ?: playData.dashAudios.minByOrNull { it.codecId }

        return PreviewPlaybackUrls(
            videoUrl = videoItem.baseUrl,
            audioUrl = audioItem?.baseUrl
        )
    }

    private fun codecPriority(codec: String?): Int {
        return when (VideoCodec.fromCodecString(codec.orEmpty())) {
            VideoCodec.AVC -> 3
            VideoCodec.HEVC -> 2
            VideoCodec.AV1 -> 1
            else -> 0
        }
    }

    private fun audioPriority(audio: Audio): Int {
        return when (audio) {
            Audio.A64K -> 0
            Audio.A132K -> 1
            Audio.A192K -> 2
            Audio.ADolbyAtoms -> 3
            Audio.AHiRes -> 4
        }
    }
}
