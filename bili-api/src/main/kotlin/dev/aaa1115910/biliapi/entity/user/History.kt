package dev.aaa1115910.biliapi.entity.user

import bilibili.app.interfaces.v1.CursorItem

data class HistoryData(
    val cursor: Long,
    val data: List<HistoryItem>
) {
    companion object {
        fun fromHistoryResponse(
            data: dev.aaa1115910.biliapi.http.entity.history.HistoryData,
            business: HistoryBusiness = HistoryBusiness.Video
        ) =
            HistoryData(
                cursor = data.cursor.viewAt,
                data = data.list
                    .filter { business.accepts(it.history.business) }
                    .map { HistoryItem.fromHistoryItem(it) }
            )

        fun fromHistoryResponse(
            data: bilibili.app.interfaces.v1.CursorV2Reply,
            business: HistoryBusiness = HistoryBusiness.Video
        ) = HistoryData(
            cursor = data.cursor.max,
            data = data.itemsList
                .filter { business.accepts(it.business) }
                .filter {
                    it.cardItemCase == CursorItem.CardItemCase.CARD_UGC ||
                            it.cardItemCase == CursorItem.CardItemCase.CARD_OGV ||
                            it.cardItemCase == CursorItem.CardItemCase.CARD_LIVE
                }
                .map { HistoryItem.fromHistoryItem(it) }
        )
    }
}

data class HistoryItem(
    val oid: Long,
    val bvid: String,
    val cid: Long,
    val kid: Long,
    val epid: Int?,
    val seasonId: Int?,
    val title: String,
    val cover: String,
    val author: String,
    val mid: Long?,
    val duration: Int,
    val progress: Int,
    val type: HistoryItemType,
    val tagName: String = "",
    val liveStatus: Int = 0,
    val uri: String = ""
) {
    companion object {
        fun fromHistoryItem(item: dev.aaa1115910.biliapi.http.entity.history.HistoryItem) =
            HistoryItem(
                oid = item.history.oid,
                bvid = item.history.bvid,
                cid = item.history.cid,
                kid = 0,
                epid = item.history.epid,
                seasonId = null,
                title = item.title,
                cover = item.cover,
                author = item.authorName,
                mid = item.authorMid,
                duration = item.duration,
                progress = item.progress,
                type = when (item.history.business) {
                    "archive" -> HistoryItemType.Archive
                    "pgc" -> HistoryItemType.Pgc
                    "live" -> HistoryItemType.Live
                    else -> HistoryItemType.Unknown
                },
                tagName = item.tagName,
                liveStatus = item.liveStatus,
                uri = item.uri
            )

        @Suppress("RemoveRedundantQualifierName")
        fun fromHistoryItem(item: bilibili.app.interfaces.v1.CursorItem) = HistoryItem(
            oid = item.oid,
            bvid = when (item.cardItemCase) {
                CursorItem.CardItemCase.CARD_UGC -> item.cardUgc.bvid
                CursorItem.CardItemCase.CARD_OGV -> ""
                else -> ""
            },
            cid = when (item.cardItemCase) {
                CursorItem.CardItemCase.CARD_UGC -> item.cardUgc.cid
                CursorItem.CardItemCase.CARD_OGV -> 0
                else -> 0
            },
            kid = item.kid,
            epid = null,
            seasonId = when (item.cardItemCase) {
                CursorItem.CardItemCase.CARD_OGV -> item.kid.toInt()
                else -> null
            },
            title = item.title,
            cover = when (item.cardItemCase) {
                CursorItem.CardItemCase.CARD_UGC -> item.cardUgc.cover
                CursorItem.CardItemCase.CARD_OGV -> item.cardOgv.cover
                CursorItem.CardItemCase.CARD_LIVE -> item.cardLive.cover
                else -> ""
            },
            author = when (item.cardItemCase) {
                CursorItem.CardItemCase.CARD_UGC -> item.cardUgc.name
                CursorItem.CardItemCase.CARD_OGV -> ""
                CursorItem.CardItemCase.CARD_LIVE -> item.cardLive.name
                else -> ""
            },
            mid = when (item.cardItemCase) {
                CursorItem.CardItemCase.CARD_UGC -> item.cardUgc.mid
                CursorItem.CardItemCase.CARD_OGV -> null
                CursorItem.CardItemCase.CARD_LIVE -> item.cardLive.mid
                else -> null
            },
            duration = when (item.cardItemCase) {
                CursorItem.CardItemCase.CARD_UGC -> item.cardUgc.duration.toInt()
                CursorItem.CardItemCase.CARD_OGV -> item.cardOgv.duration.toInt()
                else -> 0
            },
            progress = when (item.cardItemCase) {
                CursorItem.CardItemCase.CARD_UGC -> item.cardUgc.progress.toInt()
                CursorItem.CardItemCase.CARD_OGV -> item.cardOgv.progress.toInt()
                else -> 0
            },
            type = when (item.cardItemCase) {
                CursorItem.CardItemCase.CARD_UGC -> HistoryItemType.Archive
                CursorItem.CardItemCase.CARD_OGV -> HistoryItemType.Pgc
                CursorItem.CardItemCase.CARD_LIVE -> HistoryItemType.Live
                else -> HistoryItemType.Unknown
            },
            tagName = when (item.cardItemCase) {
                CursorItem.CardItemCase.CARD_LIVE -> item.cardLive.tag
                else -> ""
            },
            liveStatus = when (item.cardItemCase) {
                CursorItem.CardItemCase.CARD_LIVE -> item.cardLive.ststus
                else -> 0
            },
            uri = item.uri
        )
    }
}

enum class HistoryItemType {
    Unknown, Archive, Pgc, Live
}

enum class HistoryBusiness(val apiName: String) {
    Video(""),
    Live("live");

    fun accepts(itemBusiness: String): Boolean {
        return when (this) {
            Video -> itemBusiness == "archive" || itemBusiness == "pgc"
            Live -> itemBusiness == "live"
        }
    }
}
