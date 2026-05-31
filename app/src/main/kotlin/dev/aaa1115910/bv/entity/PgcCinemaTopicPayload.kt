package dev.aaa1115910.bv.entity

import dev.aaa1115910.biliapi.entity.pgc.PgcCinemaTabData
import dev.aaa1115910.biliapi.entity.pgc.PgcItem
import dev.aaa1115910.biliapi.http.SeasonIndexType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class PgcCinemaTopicPayload(
    val title: String,
    val cover: String,
    val link: String,
    val items: List<Item>
) {
    @Serializable
    data class Item(
        val cover: String,
        val title: String,
        val subTitle: String,
        val seasonId: Int,
        val episodeId: Int,
        val seasonTypeId: Int,
        val rating: String
    ) {
        fun toPgcItem(): PgcItem = PgcItem(
            cover = cover,
            title = title,
            subTitle = subTitle,
            seasonId = seasonId,
            episodeId = episodeId,
            seasonType = runCatching { SeasonIndexType.fromId(seasonTypeId) }
                .getOrDefault(SeasonIndexType.Movie),
            rating = rating
        )

        companion object {
            fun fromPgcItem(item: PgcItem): Item = Item(
                cover = item.cover,
                title = item.title,
                subTitle = item.subTitle,
                seasonId = item.seasonId,
                episodeId = item.episodeId,
                seasonTypeId = item.seasonType.id,
                rating = item.rating
            )
        }
    }

    fun toJson(): String = json.encodeToString(this)

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        fun fromTopic(topic: PgcCinemaTabData.CinemaTopic): PgcCinemaTopicPayload =
            PgcCinemaTopicPayload(
                title = topic.title,
                cover = topic.cover,
                link = topic.link,
                items = topic.items.map(Item::fromPgcItem)
            )

        fun fromJson(value: String): PgcCinemaTopicPayload =
            json.decodeFromString(value)
    }
}
