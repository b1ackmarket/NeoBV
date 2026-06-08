package dev.aaa1115910.bv.cast

import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.repositories.SearchFilterDuration
import dev.aaa1115910.biliapi.repositories.SearchFilterOrderType
import dev.aaa1115910.biliapi.repositories.SearchRepository
import dev.aaa1115910.biliapi.repositories.SearchType
import dev.aaa1115910.biliapi.repositories.SearchTypePage
import dev.aaa1115910.biliapi.repositories.VideoDetailRepository
import dev.aaa1115910.bv.cast.protocol.CastClientHint
import dev.aaa1115910.bv.cast.protocol.CastContent
import io.github.oshai.kotlinlogging.KotlinLogging

class PiliPlusUgcCastResolver(
    private val searchVideos: suspend (String) -> List<PiliPlusUgcSearchCandidate>,
    private val loadVideoDetail: suspend (Long) -> PiliPlusUgcVideoDetail?
) {
    private val logger = KotlinLogging.logger("PiliPlusUgcCastResolver")

    constructor(
        searchRepository: SearchRepository,
        videoDetailRepository: VideoDetailRepository,
        preferApiType: () -> ApiType,
        enableProxy: () -> Boolean
    ) : this(
        searchVideos = { title ->
            searchRepository.searchType(
                keyword = title,
                type = SearchType.Video,
                tid = null,
                order = SearchFilterOrderType.ComprehensiveSort,
                duration = SearchFilterDuration.All,
                page = SearchTypePage(),
                preferApiType = preferApiType(),
                enableProxy = enableProxy()
            ).videos.map { video ->
                PiliPlusUgcSearchCandidate(
                    aid = video.aid,
                    title = video.title
                )
            }
        },
        loadVideoDetail = { aid ->
            videoDetailRepository.getVideoDetail(aid = aid, preferApiType = preferApiType()).let { detail ->
                PiliPlusUgcVideoDetail(
                    aid = detail.aid,
                    title = detail.title,
                    pages = detail.pages.map { page ->
                        PiliPlusUgcVideoPage(
                            cid = page.cid,
                            title = page.title
                        )
                    }
                )
            }
        }
    )

    suspend fun resolve(content: CastContent): PiliPlusResolvedVideo? {
        if (content.clientHint != CastClientHint.PiliPlus) return null
        if (!content.hasDirectMedia) return null

        val title = content.title?.normalizeCastTitle()?.takeIf { it.isNotBlank() } ?: return null
        val cid = content.cid ?: content.directMediaUrl?.extractBiliCidFromMediaUrl() ?: return null

        val searchResult = runCatching {
            searchVideos(title)
        }.onFailure { error ->
            logger.warn(error) { "Search PiliPlus cast title failed: $title" }
        }.getOrNull() ?: return null

        val candidates = searchResult
            .filter { video -> video.title.normalizeCastTitle() == title }
            .distinctBy { it.aid }
            .take(MAX_CANDIDATES_TO_CHECK + 1)

        if (candidates.isEmpty()) return null

        val matches = mutableListOf<PiliPlusResolvedVideo>()
        for (candidate in candidates.take(MAX_CANDIDATES_TO_CHECK)) {
            val detail = loadDetail(candidate.aid) ?: continue
            val page = detail.pages.firstOrNull { it.cid == cid } ?: continue
            matches += PiliPlusResolvedVideo(
                aid = detail.aid,
                cid = page.cid,
                title = detail.title,
                partTitle = page.title
            )
            if (matches.size > 1) break
        }

        return matches.singleOrNull()
    }

    private suspend fun loadDetail(aid: Long): PiliPlusUgcVideoDetail? =
        runCatching {
            loadVideoDetail(aid)
        }.onFailure { error ->
            logger.warn(error) { "Resolve PiliPlus UGC cast detail failed: aid=$aid" }
        }.getOrNull()

    private companion object {
        const val MAX_CANDIDATES_TO_CHECK = 5
    }
}

data class PiliPlusUgcSearchCandidate(
    val aid: Long,
    val title: String
)

data class PiliPlusUgcVideoDetail(
    val aid: Long,
    val title: String,
    val pages: List<PiliPlusUgcVideoPage>
)

data class PiliPlusUgcVideoPage(
    val cid: Long,
    val title: String
)

data class PiliPlusResolvedVideo(
    val aid: Long,
    val cid: Long,
    val title: String,
    val partTitle: String
)

internal fun String.normalizeCastTitle(): String =
    replace(Regex("<[^>]+>"), "")
        .decodeBasicHtmlEntities()
        .replace(Regex("\\s+"), " ")
        .trim()

internal fun String.extractBiliCidFromMediaUrl(): Long? {
    val lower = lowercase()
    Regex("""(?:^|[?&])cid=(\d+)""")
        .find(lower)
        ?.groupValues
        ?.getOrNull(1)
        ?.toLongOrNull()
        ?.let { return it }

    Regex("""/(\d+)(?:[-_/][^/?#]*)?\.(?:m4s|mp4|flv)(?:[?#]|$)""")
        .find(lower)
        ?.groupValues
        ?.getOrNull(1)
        ?.toLongOrNull()
        ?.let { return it }

    Regex("""/(\d{6,})(?:[/?#]|$)""")
        .findAll(lower)
        .lastOrNull()
        ?.groupValues
        ?.getOrNull(1)
        ?.toLongOrNull()
        ?.let { return it }

    return null
}

private fun String.decodeBasicHtmlEntities(): String =
    replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#34;", "\"")
        .replace("&apos;", "'")
        .replace("&#39;", "'")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
