package dev.aaa1115910.biliapi.http.entity.pgc

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PgcCinemaTabData(
    @SerialName("has_next")
    val hasNext: Int = 0,
    @SerialName("next_cursor")
    val nextCursor: String = "",
    val modules: List<Module> = emptyList()
) {
    @Serializable
    data class Module(
        val title: String = "",
        val style: String = "",
        val items: List<Item> = emptyList()
    )

    @Serializable
    data class Item(
        val cover: String = "",
        val desc: String = "",
        @SerialName("episode_id")
        val episodeId: Int? = null,
        val items: List<Item> = emptyList(),
        val link: String = "",
        @SerialName("new_ep")
        val newEp: NewEp? = null,
        val score: Float? = null,
        @SerialName("season_id")
        val seasonId: Int? = null,
        @SerialName("season_type")
        val seasonType: Int? = null,
        val title: String = ""
    ) {
        @Serializable
        data class NewEp(
            @SerialName("index_show")
            val indexShow: String = ""
        )
    }
}
