package dev.aaa1115910.bv.cast

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import dev.aaa1115910.biliapi.repositories.VideoDetailRepository
import dev.aaa1115910.biliapi.util.AvBvConverter
import dev.aaa1115910.bv.activities.live.LivePlayerActivity
import dev.aaa1115910.bv.activities.video.VideoPlayerV3Activity
import dev.aaa1115910.bv.cast.protocol.CastContent
import dev.aaa1115910.bv.util.Prefs
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.getKoin

class CastPlaybackLauncher(private val appContext: Context) {
    private val logger = KotlinLogging.logger("CastPlaybackLauncher")
    private var lastLaunch: LastLaunch? = null

    suspend fun launch(content: CastContent): Boolean = withContext(Dispatchers.Main) {
        if (isDuplicateLaunch(content)) {
            logger.info { "Skip duplicate cast launch: $content" }
            return@withContext true
        }

        when {
            content.hasLiveIdentity -> {
                launchLive(content)
                true
            }

            content.hasVideoIdentity -> launchVideo(content)

            else -> false
        }
    }

    private fun launchLive(content: CastContent) {
        val roomId = content.roomId ?: return
        logger.info { "Launch live from cast: roomId=$roomId" }
        appContext.startActivity(
            Intent(appContext, LivePlayerActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("room_id", roomId)
                putExtra("title", content.title ?: "投屏直播 $roomId")
                putExtra("up_name", "")
                putExtra("online", 0)
                content.danmakuEnabled?.let { putExtra("danmaku_enabled", it) }
            }
        )
    }

    private suspend fun launchVideo(content: CastContent): Boolean {
        val repository = getKoin().get<VideoDetailRepository>()
        val resolved = withContext(Dispatchers.IO) {
            resolveVideo(content, repository)
        } ?: return false

        logger.info {
            "Launch video from cast: aid=${resolved.aid}, cid=${resolved.cid}, epid=${resolved.epid}, seasonId=${resolved.seasonId}"
        }
        appContext.startActivity(
            Intent(appContext, VideoPlayerV3Activity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("avid", resolved.aid)
                putExtra("cid", resolved.cid)
                putExtra("title", resolved.title)
                putExtra("partTitle", resolved.partTitle)
                putExtra("played", content.seekSeconds.toPlayedMillis())
                putExtra("fromSeason", resolved.epid != null || resolved.seasonId != null)
                putExtra("epid", resolved.epid ?: 0)
                putExtra("seasonId", resolved.seasonId ?: 0)
                content.playSpeed?.let { putExtra("play_speed", it) }
                content.danmakuEnabled?.let { putExtra("danmaku_enabled", it) }
            }
        )
        return true
    }

    private suspend fun resolveVideo(
        content: CastContent,
        repository: VideoDetailRepository
    ): ResolvedVideo? {
        val aid = content.aid
            ?: content.bvid?.let { runCatching { AvBvConverter.bv2av(it) }.getOrNull() }

        if (aid != null && aid > 0L && content.cid != null && content.cid > 0L) {
            return ResolvedVideo(
                aid = aid,
                cid = content.cid,
                epid = content.epid,
                seasonId = content.seasonId,
                title = content.title ?: "投屏视频",
                partTitle = content.partTitle ?: ""
            )
        }

        if (content.epid != null || content.seasonId != null) {
            val season = runCatching {
                repository.getPgcVideoDetail(
                    epid = content.epid,
                    seasonId = content.seasonId,
                    preferApiType = Prefs.playbackApiType
                )
            }.onFailure {
                logger.warn(it) { "Resolve PGC cast content failed: epid=${content.epid}, seasonId=${content.seasonId}" }
            }.getOrNull()
            val episode = season?.episodes?.firstOrNull { episode ->
                (content.epid != null && episode.epid == content.epid) ||
                    (aid != null && episode.aid == aid) ||
                    (content.cid != null && episode.cid == content.cid)
            } ?: season?.episodes?.firstOrNull()
            if (season != null && episode != null) {
                return ResolvedVideo(
                    aid = episode.aid,
                    cid = episode.cid,
                    epid = episode.epid ?: content.epid,
                    seasonId = season.seasonId,
                    title = content.title ?: season.title,
                    partTitle = content.partTitle ?: episode.longTitle.ifBlank { episode.title }
                )
            }
        }

        if (aid != null && aid > 0L) {
            val detail = runCatching {
                repository.getVideoDetail(aid = aid, preferApiType = Prefs.playbackApiType)
            }.onFailure {
                logger.warn(it) { "Resolve UGC cast content failed: aid=$aid" }
            }.getOrNull()
            if (detail != null) {
                val page = content.cid?.let { cid -> detail.pages.firstOrNull { it.cid == cid } }
                    ?: detail.pages.firstOrNull { it.cid == detail.cid }
                    ?: detail.pages.firstOrNull()
                return ResolvedVideo(
                    aid = detail.aid,
                    cid = page?.cid ?: detail.cid,
                    epid = content.epid ?: detail.epid,
                    seasonId = content.seasonId,
                    title = content.title ?: detail.title,
                    partTitle = content.partTitle ?: page?.title.orEmpty()
                )
            }
        }

        return null
    }

    private fun isDuplicateLaunch(content: CastContent): Boolean {
        val now = SystemClock.elapsedRealtime()
        val key = listOf(
            content.aid,
            content.bvid,
            content.cid,
            content.epid,
            content.seasonId,
            content.roomId,
            content.seekSeconds,
            content.playSpeed,
            content.danmakuEnabled
        ).joinToString(separator = ":")
        val duplicate = lastLaunch?.let { it.key == key && now - it.atMillis < DUPLICATE_LAUNCH_WINDOW_MS } == true
        if (!duplicate) lastLaunch = LastLaunch(key, now)
        return duplicate
    }

    private data class ResolvedVideo(
        val aid: Long,
        val cid: Long,
        val epid: Int?,
        val seasonId: Int?,
        val title: String,
        val partTitle: String
    )

    private data class LastLaunch(
        val key: String,
        val atMillis: Long
    )

    private companion object {
        const val DUPLICATE_LAUNCH_WINDOW_MS = 2_000L
    }
}

internal fun Int?.toPlayedMillis(): Int =
    ((this ?: 0).coerceAtLeast(0).toLong() * 1000L)
        .coerceAtMost(Int.MAX_VALUE.toLong())
        .toInt()
