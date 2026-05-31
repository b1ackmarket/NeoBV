package dev.aaa1115910.biliapi.entity.pgc

import dev.aaa1115910.biliapi.entity.CarouselData
import dev.aaa1115910.biliapi.http.SeasonIndexType
import dev.aaa1115910.biliapi.http.entity.pgc.PgcCinemaTabData as HttpPgcCinemaTabData

data class PgcCinemaTabData(
    val hasNext: Boolean,
    val nextCursor: String,
    val carouselItems: List<CarouselData.CarouselItem>,
    val rows: List<Row>
) {
    data class Row(
        val title: String,
        val topics: List<CinemaTopic>
    )

    data class CinemaTopic(
        val title: String,
        val cover: String,
        val link: String,
        val items: List<PgcItem>
    )

    companion object {
        fun fromHttpData(data: HttpPgcCinemaTabData): PgcCinemaTabData {
            val carouselItems = data.modules
                .firstOrNull { it.style == "banner_v3" }
                ?.items
                ?.mapNotNull { item ->
                    val seasonId = item.seasonId ?: return@mapNotNull null
                    val episodeId = item.episodeId
                    CarouselData.CarouselItem(
                        cover = item.cover.withHttpsScheme(),
                        title = item.title,
                        seasonId = seasonId,
                        episodeId = episodeId
                    )
                }
                .orEmpty()

            val rows = data.modules
                .filterNot { it.style == "banner_v3" || it.style == "function" }
                .mapNotNull { module ->
                    val topics = module.toCinemaTopics()
                    if (topics.isEmpty()) null else Row(
                        title = module.title.takeIf { it.isNotBlank() } ?: "影视推荐",
                        topics = topics
                    )
                }

            return PgcCinemaTabData(
                hasNext = data.hasNext == 1,
                nextCursor = data.nextCursor,
                carouselItems = carouselItems,
                rows = rows
            )
        }
    }
}

private fun HttpPgcCinemaTabData.Module.toCinemaTopics(): List<PgcCinemaTabData.CinemaTopic> {
    val moduleTitle = title.takeIf { it.isNotBlank() } ?: "影视专题"
    val nestedTopics = items
        .filter { it.items.isNotEmpty() }
        .mapNotNull { item ->
            val topicItems = item.items
                .mapNotNull { it.toPgcItemOrNull() }
                .distinctBy { it.seasonId }
            if (topicItems.isEmpty()) return@mapNotNull null

            PgcCinemaTabData.CinemaTopic(
                title = item.title.takeIf { it.isNotBlank() } ?: moduleTitle,
                cover = item.cover.takeIf { it.isNotBlank() }?.withHttpsScheme()
                    ?: topicItems.firstOrNull()?.cover.orEmpty(),
                link = item.link,
                items = topicItems
            )
        }
    if (nestedTopics.isNotEmpty()) return nestedTopics

    val topicItems = items
        .mapNotNull { it.toPgcItemOrNull() }
        .distinctBy { it.seasonId }
    if (topicItems.isEmpty()) return emptyList()

    return listOf(
        PgcCinemaTabData.CinemaTopic(
            title = moduleTitle,
            cover = items.firstOrNull { it.cover.isNotBlank() }?.cover?.withHttpsScheme()
                ?: topicItems.firstOrNull()?.cover.orEmpty(),
            link = items.firstOrNull { it.link.isNotBlank() }?.link.orEmpty(),
            items = topicItems
        )
    )
}

private fun HttpPgcCinemaTabData.Item.toPgcItemOrNull(): PgcItem? {
    val seasonId = seasonId ?: return null
    val resolvedSeasonType = seasonType
        ?.let { id -> runCatching { SeasonIndexType.fromId(id) }.getOrNull() }
        ?: SeasonIndexType.Movie
    return PgcItem(
        cover = cover.withHttpsScheme(),
        title = title,
        subTitle = newEp?.indexShow?.takeIf { it.isNotBlank() }
            ?: desc.takeIf { it.isNotBlank() }
            ?: "",
        seasonId = seasonId,
        episodeId = episodeId ?: newEp?.let { 0 } ?: 0,
        seasonType = resolvedSeasonType,
        rating = score?.takeIf { it > 0f }?.let { String.format("%.1f", it) }.orEmpty()
    )
}

private fun String.withHttpsScheme(): String = when {
    startsWith("//") -> "https:$this"
    else -> this
}
