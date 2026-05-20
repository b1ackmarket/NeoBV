package dev.aaa1115910.biliapi.http.entity.user

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserSeasonsSeriesData(
    @SerialName("items_lists")
    val itemsLists: ItemsLists = ItemsLists()
) {
    @Serializable
    data class ItemsLists(
        @SerialName("seasons_list")
        val seasonsList: List<SeasonItem> = emptyList(),
        @SerialName("series_list")
        val seriesList: List<SeasonItem> = emptyList()
    )

    @Serializable
    data class SeasonItem(
        val meta: Meta = Meta()
    )

    @Serializable
    data class Meta(
        @SerialName("season_id")
        val seasonId: Long = 0L,
        @SerialName("series_id")
        val seriesId: Long = 0L,
        val title: String = "",
        val name: String = "",
        val cover: String = "",
        val total: Int = 0,
        val description: String = ""
    )
}
