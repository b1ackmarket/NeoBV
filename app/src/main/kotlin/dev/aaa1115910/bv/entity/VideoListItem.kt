package dev.aaa1115910.bv.entity

import dev.aaa1115910.biliapi.entity.video.VideoPage

data class VideoListItem(
    val aid: Long,
    val cid: Long,
    val epid: Int? = null,
    val seasonId: Int? = null,
    val title: String,
    val cover: String? = null,
    // 针对UGC合集内视频的分P
    val ugcPages: List<VideoPage>? = null
)
