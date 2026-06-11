package dev.aaa1115910.bv.component.controllers

import dev.aaa1115910.biliapi.entity.video.VideoPage
import dev.aaa1115910.biliapi.entity.video.season.UgcSeason
import dev.aaa1115910.bv.entity.VideoListItem

internal enum class VideoListPanelMode {
    Pages,
    Collection,
    Single
}

internal data class VideoListPanelEntry(
    val aid: Long,
    val cid: Long,
    val title: String
)

internal data class VideoListPanelState(
    val buttonLabel: String,
    val headerText: String,
    val mode: VideoListPanelMode,
    val items: List<VideoListPanelEntry>
)

internal fun resolveVideoListPanelState(
    currentCid: Long,
    videoList: List<VideoListItem>
): VideoListPanelState {
    val currentVideo = videoList.firstOrNull { video ->
        video.cid == currentCid || video.ugcPages?.any { it.cid == currentCid } == true
    } ?: videoList.firstOrNull()

    val currentPages = currentVideo?.ugcPages.orEmpty()
    if (currentPages.isNotEmpty() && currentVideo != null) {
        return VideoListPanelState(
            buttonLabel = "分P",
            headerText = "视频分P",
            mode = VideoListPanelMode.Pages,
            items = currentPages.map { page ->
                VideoListPanelEntry(
                    aid = currentVideo.aid,
                    cid = page.cid,
                    title = page.title
                )
            }
        )
    }

    if (videoList.size > 1) {
        return VideoListPanelState(
            buttonLabel = "合集",
            headerText = "视频合集",
            mode = VideoListPanelMode.Collection,
            items = videoList.map { video ->
                VideoListPanelEntry(
                    aid = video.aid,
                    cid = video.cid,
                    title = video.title
                )
            }
        )
    }

    return VideoListPanelState(
        buttonLabel = "视频",
        headerText = "视频",
        mode = VideoListPanelMode.Single,
        items = listOfNotNull(
            currentVideo?.let { video ->
                VideoListPanelEntry(
                    aid = video.aid,
                    cid = video.cid,
                    title = video.title
                )
            }
        )
    )
}

internal fun resolvePlaybackVideoList(
    aid: Long,
    currentCid: Long,
    title: String,
    pages: List<VideoPage>,
    ugcSeason: UgcSeason?
): List<VideoListItem> {
    if (pages.size > 1) {
        return listOf(
            VideoListItem(
                aid = aid,
                cid = currentCid,
                title = title,
                ugcPages = pages
            )
        )
    }

    val collectionEpisodes = ugcSeason
        ?.sections
        ?.firstOrNull { section ->
            section.episodes.any { episode -> episode.cid == currentCid || episode.aid == aid }
        }
        ?.episodes
        .orEmpty()
    if (collectionEpisodes.isNotEmpty()) {
        return collectionEpisodes.map { episode ->
            VideoListItem(
                aid = episode.aid,
                cid = episode.cid,
                title = episode.title,
                cover = episode.cover
            )
        }
    }

    return listOf(
        VideoListItem(
            aid = aid,
            cid = currentCid,
            title = title
        )
    )
}
