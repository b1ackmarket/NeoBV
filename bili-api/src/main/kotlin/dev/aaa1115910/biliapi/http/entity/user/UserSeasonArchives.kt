package dev.aaa1115910.biliapi.http.entity.user

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserSeasonArchivesData(
    val archives: List<Archive> = emptyList(),
    val page: Page? = null
) {
    @Serializable
    data class Archive(
        val aid: Long = 0L,
        val bvid: String = "",
        val title: String = "",
        val pic: String = "",
        val duration: Int = 0,
        val pubdate: Long = 0L,
        val stat: Stat = Stat(),
        @SerialName("upMid")
        val upMid: Long = 0L
    )

    @Serializable
    data class Stat(
        val view: Int = 0,
        val danmaku: Int = 0
    )

    @Serializable
    data class Page(
        val total: Int = 0
    )
}
