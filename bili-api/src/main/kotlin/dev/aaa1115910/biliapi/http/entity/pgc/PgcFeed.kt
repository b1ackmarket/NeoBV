package dev.aaa1115910.biliapi.http.entity.pgc

import dev.aaa1115910.biliapi.http.entity.web.Hover
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray

@Serializable
data class PgcFeedData(
    @Suppress("SpellCheckingInspection")
    val coursor: Int = 0,
    @SerialName("has_next")
    val hasNext: Boolean = false,
    var items: List<FeedSubItem> = emptyList()
) {
    @Serializable
    data class FeedSubItem(
        val cover: String = "",
        @SerialName("episode_id")
        val episodeId: Int = 0,
        val hover: Hover? = null,
        val link: String? = null,
        @SerialName("rank_id")
        val rankId: Int = 0,
        val rating: String? = null,
        @SerialName("season_id")
        val seasonId: Int? = null,
        @SerialName("season_type")
        val seasonType: Int? = null,
        val stat: Stat? = null,
        @SerialName("sub_title")
        val subTitle: String = "",
        val text: JsonArray? = null,
        val title: String = "",
        @SerialName("user_status")
        val userStatus: UserStatus? = null
    ) {
        @Serializable
        data class Stat(
            val danmaku: Int = 0,
            val duration: Int = 0,
            val view: Long = 0
        )

        @Serializable
        data class UserStatus(
            val follow: Int = 0
        )
    }
}
