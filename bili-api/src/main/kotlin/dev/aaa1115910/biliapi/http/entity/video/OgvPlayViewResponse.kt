package dev.aaa1115910.biliapi.http.entity.video

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class OgvPlayViewData(
    @SerialName("play_video_type")
    val playVideoType: String = "",
    @SerialName("video_info")
    val videoInfo: PlayUrlData,
    @SerialName("video_extra")
    val videoExtra: VideoExtra = VideoExtra(),
    val arc: Arc = Arc(),
    @SerialName("user_status")
    val userStatus: UserStatus = UserStatus(),
    @SerialName("watch_progress")
    val watchProgress: WatchProgress = WatchProgress(),
    val supplement: Supplement = Supplement(),
    val plugins: List<JsonElement> = emptyList(),
    @SerialName("exp_info")
    val expInfo: JsonElement? = null
) {
    @Serializable
    data class VideoExtra(
        @SerialName("clip_info")
        val clipInfo: List<JsonElement> = emptyList()
    )

    @Serializable
    data class Arc(
        @SerialName("biz_type")
        val bizType: Int = 0,
        val aid: Long = 0,
        val cid: Long = 0,
        val bvid: String = "",
        @SerialName("is_drm")
        val isDrm: Boolean = false
    )

    @Serializable
    data class UserStatus(
        @SerialName("is_login")
        val isLogin: Boolean = false,
        @SerialName("follow_info")
        val followInfo: JsonElement? = null,
        @SerialName("vip_info")
        val vipInfo: JsonElement? = null
    )

    @Serializable
    data class WatchProgress(
        @SerialName("current_progress")
        val currentProgress: Int = 0
    )

    @Serializable
    data class Supplement(
        @SerialName("ogv_episode_info")
        val ogvEpisodeInfo: JsonElement? = null,
        @SerialName("record_number")
        val recordNumber: JsonElement? = null,
        @SerialName("ogv_season_watch_progress")
        val ogvSeasonWatchProgress: JsonElement? = null,
        @SerialName("ogv_season_info")
        val ogvSeasonInfo: JsonElement? = null,
        @SerialName("ogv_pay_tip")
        val ogvPayTip: JsonElement? = null
    )
}

