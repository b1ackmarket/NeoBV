package dev.aaa1115910.biliapi.entity.pgc

import dev.aaa1115910.biliapi.http.SeasonIndexType

data class PgcItem(
    var cover: String,
    var title: String,
    var subTitle: String,
    var seasonId: Int,
    var episodeId: Int,
    var seasonType: SeasonIndexType,
    var rating: String
) {
    companion object {
        fun fromFeedSubItem(feedSubItem: dev.aaa1115910.biliapi.http.entity.pgc.PgcFeedData.FeedSubItem): PgcItem? {
            val seasonId = feedSubItem.seasonId ?: return null
            val seasonType = feedSubItem.seasonType ?: return null
            return PgcItem(
                cover = feedSubItem.cover,
                title = feedSubItem.title,
                subTitle = feedSubItem.subTitle,
                seasonId = seasonId,
                episodeId = feedSubItem.episodeId,
                seasonType = SeasonIndexType.fromId(seasonType),
                rating = feedSubItem.rating ?: "0"
            )
        }

        fun fromFeedSubItem(feedSubItem: dev.aaa1115910.biliapi.http.entity.pgc.PgcFeedV3Data.FeedItem.FeedSubItem): PgcItem? {
            val seasonId = feedSubItem.seasonId ?: return null
            val seasonType = feedSubItem.seasonType ?: return null
            val episodeId = feedSubItem.episodeId ?: feedSubItem.inline?.epId ?: return null
            return PgcItem(
                cover = feedSubItem.cover,
                title = feedSubItem.title,
                subTitle = feedSubItem.subTitle,
                seasonId = seasonId,
                episodeId = episodeId,
                seasonType = SeasonIndexType.fromId(seasonType),
                rating = feedSubItem.rating ?: "0"
            )
        }

        fun fromIndexResultItem(indexResultItem: dev.aaa1115910.biliapi.http.entity.index.IndexResultData.IndexResultItem): PgcItem {
            return PgcItem(
                cover = indexResultItem.cover,
                title = indexResultItem.title,
                subTitle = indexResultItem.subTitle,
                seasonId = indexResultItem.seasonId,
                episodeId = indexResultItem.firstEp.epId,
                seasonType = SeasonIndexType.fromId(indexResultItem.seasonType),
                rating = indexResultItem.score
            )
        }

        fun fromRankItem(
            rankItem: dev.aaa1115910.biliapi.http.entity.pgc.PgcRankItem,
            fallbackSeasonType: SeasonIndexType
        ): PgcItem {
            val resolvedSeasonType = runCatching {
                SeasonIndexType.fromId(rankItem.seasonType)
            }.getOrDefault(fallbackSeasonType)
            return PgcItem(
                cover = rankItem.cover,
                title = rankItem.title,
                subTitle = rankItem.newEp?.indexShow.orEmpty(),
                seasonId = rankItem.seasonId.takeIf { it > 0 } ?: rankItem.url
                    .substringAfter("ss", "")
                    .substringBefore("?", "")
                    .toIntOrNull()
                    .orZero(),
                episodeId = 0,
                seasonType = resolvedSeasonType,
                rating = ""
            )
        }
    }
}

private fun Int?.orZero(): Int = this ?: 0
