package dev.aaa1115910.biliapi.http.entity.video

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeeklySeriesListData(
    val list: List<WeeklySeriesItem> = emptyList()
)

@Serializable
data class WeeklySeriesItem(
    val number: Int,
    val subject: String = "",
    val name: String = ""
)

@Serializable
data class WeeklySeriesOneData(
    val config: WeeklySeriesConfig? = null,
    val list: List<VideoInfo> = emptyList()
)

@Serializable
data class WeeklySeriesConfig(
    val number: Int = 0,
    val name: String = "",
    val label: String = "",
    val subject: String = ""
)

@Serializable
data class RankingV2Data(
    val list: List<VideoInfo> = emptyList(),
    val note: String = ""
)

@Serializable
data class MusicTopListData(
    val list: List<MusicTopListItem> = emptyList()
)

@Serializable
data class MusicTopListItem(
    val rank: Int = 0,
    @SerialName("creation_aid")
    val creationAid: Long = 0,
    @SerialName("creation_bvid")
    val creationBvid: String = "",
    @SerialName("creation_title")
    val creationTitle: String = "",
    @SerialName("creation_cover")
    val creationCover: String = "",
    @SerialName("creation_up")
    val creationUp: Long = 0,
    @SerialName("creation_nickname")
    val creationNickname: String = "",
    @SerialName("creation_duration")
    val creationDuration: Int = 0,
    @SerialName("creation_play")
    val creationPlay: Int = 0,
    val heat: Int = 0,
    @SerialName("arc_list")
    val arcList: List<MusicTopListArchive>? = null
)

@Serializable
data class MusicTopListArchive(
    val aid: Long = 0,
    val bvid: String = "",
    val title: String = "",
    val cover: String = "",
    @SerialName("up_name")
    val upName: String = "",
    val mid: Long = 0,
    val play: Int = 0,
    @SerialName("first_cid")
    val firstCid: Long = 0
)

@Serializable
data class MusicTopListPeriodData(
    val list: Map<String, List<MusicTopListPeriodItem>> = emptyMap()
)

@Serializable
data class MusicTopListPeriodItem(
    @SerialName("ID")
    val id: Int,
    @SerialName("priod")
    val period: Int,
    @SerialName("publish_time")
    val publishTime: Long = 0
)
