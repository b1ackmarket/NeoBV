package dev.aaa1115910.biliapi.repositories

import bilibili.app.show.v1.PopularGrpcKt
import bilibili.app.show.v1.popularResultReq
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.entity.home.RecommendData
import dev.aaa1115910.biliapi.entity.home.RecommendPage
import dev.aaa1115910.biliapi.entity.rank.PopularVideoData
import dev.aaa1115910.biliapi.entity.rank.PopularVideoPage
import dev.aaa1115910.biliapi.entity.ugc.UgcItem
import dev.aaa1115910.biliapi.http.entity.video.MusicTopListArchive
import dev.aaa1115910.biliapi.http.entity.video.MusicTopListItem
import dev.aaa1115910.biliapi.http.entity.video.MusicTopListPeriodItem
import dev.aaa1115910.biliapi.http.entity.video.WeeklySeriesItem
import dev.aaa1115910.biliapi.http.BiliHttpApi
import org.koin.core.annotation.Single

data class HomeRankingPeriod(
    val id: Int,
    val label: String
)

data class HomeRankingResult(
    val items: List<UgcItem>,
    val periods: List<HomeRankingPeriod> = emptyList(),
    val selectedPeriodId: Int? = null
)

@Single
class RecommendVideoRepository(
    private val authRepository: AuthRepository,
    private val channelRepository: ChannelRepository
) {
    private val popularStub
        get() = runCatching {
            PopularGrpcKt.PopularCoroutineStub(channelRepository.defaultChannel!!)
        }.getOrNull()

    suspend fun getPopularVideos(
        page: PopularVideoPage,
        preferApiType: ApiType = ApiType.Web,
        useAuth: Boolean = true
    ): PopularVideoData {
        return when (preferApiType) {
            ApiType.Web -> {
                val response = BiliHttpApi.getPopularVideoData(
                    pageSize = page.nextWebPageSize,
                    pageNumber = page.nextWebPageNumber,
                    sessData = if (useAuth) authRepository.sessionData ?: "" else ""
                ).getResponseData()
                val list = response.list.map { UgcItem.fromVideoInfo(it) }
                val nextPage = PopularVideoPage(
                    nextWebPageSize = page.nextWebPageSize,
                    nextWebPageNumber = page.nextWebPageNumber + 1
                )
                PopularVideoData(
                    list = list,
                    nextPage = nextPage,
                    noMore = response.noMore
                )
            }

            ApiType.App -> {
                val reply = popularStub?.index(popularResultReq {
                    idx = page.nextAppIndex.toLong()
                })
                val list = reply?.itemsList
                    ?.filter { it.itemCase == bilibili.app.card.v1.Card.ItemCase.SMALL_COVER_V5 }
                    ?.map { UgcItem.fromSmallCoverV5(it.smallCoverV5) }
                    ?: emptyList()
                val nextPage = PopularVideoPage(
                    nextAppIndex = list.lastOrNull()?.idx ?: -1
                )
                PopularVideoData(
                    list = list,
                    nextPage = nextPage,
                    noMore = nextPage.nextAppIndex == -1
                )
            }
        }
    }

    suspend fun getRecommendVideos(
        page: RecommendPage = RecommendPage(),
        preferApiType: ApiType = ApiType.Web,
        useAuth: Boolean = true
    ): RecommendData {
        val items = when (preferApiType) {
            ApiType.Web -> BiliHttpApi.getFeedRcmd(
                idx = page.nextWebIdx,
                sessData = if (useAuth) authRepository.sessionData else null
            )
                .getResponseData().item
                .map { UgcItem.fromRcmdItem(it) }

            ApiType.App -> BiliHttpApi.getFeedIndex(
                idx = page.nextAppIdx,
                accessKey = if (useAuth) authRepository.accessToken else null
            )
                .getResponseData().items
                .filter { it.cardGoto == "av" }
                .map { UgcItem.fromRcmdItem(it) }
        }
        val nextPage = when (preferApiType) {
            ApiType.Web -> RecommendPage(
                nextWebIdx = page.nextWebIdx + 1
            )

            ApiType.App -> RecommendPage(
                nextAppIdx = items.first().idx + 1
            )
        }
        return RecommendData(
            items = items,
            nextPage = nextPage
        )
    }

    suspend fun getRankingVideos(rid: Int): List<UgcItem> {
        return BiliHttpApi.getRankingV2(rid)
            .getResponseData()
            .list
            .map { UgcItem.fromVideoInfo(it) }
    }

    suspend fun getWeeklyRanking(periodId: Int? = null): HomeRankingResult {
        val periods = BiliHttpApi.getWeeklySeriesList()
            .getResponseData()
            .list
            .map { it.toHomeRankingPeriod() }
        val selectedPeriod = periodId ?: periods.firstOrNull()?.id
        val data = selectedPeriod?.let {
            BiliHttpApi.getWeeklySeriesOne(it).getResponseData()
        }
        return HomeRankingResult(
            items = data?.list.orEmpty().map { UgcItem.fromVideoInfo(it) },
            periods = periods,
            selectedPeriodId = selectedPeriod
        )
    }

    suspend fun getPreciousRanking(): HomeRankingResult {
        return HomeRankingResult(
            items = BiliHttpApi.getPopularPreciousAllData(
                sessData = authRepository.sessionData ?: ""
            ).getResponseData().list.map { UgcItem.fromVideoInfo(it) }
        )
    }

    suspend fun getMusicRanking(
        listId: Int,
        listType: Int,
        periodId: Int? = null
    ): HomeRankingResult {
        val periods = BiliHttpApi.getMusicTopListPeriods(listId, listType)
            .getResponseData()
            .list
            .values
            .flatten()
            .sortedByDescending { it.period }
            .map { it.toHomeRankingPeriod() }
        val selectedPeriod = periodId ?: periods.firstOrNull()?.id
        val items = BiliHttpApi.getMusicTopList(
            listId = listId,
            listType = listType,
            periodId = selectedPeriod
        ).getResponseData().list.flatMap { it.toUgcItems() }
        return HomeRankingResult(
            items = items,
            periods = periods,
            selectedPeriodId = selectedPeriod
        )
    }
}

private fun WeeklySeriesItem.toHomeRankingPeriod(): HomeRankingPeriod {
    return HomeRankingPeriod(id = number, label = name.ifBlank { "第${number}期" })
}

private fun MusicTopListPeriodItem.toHomeRankingPeriod(): HomeRankingPeriod {
    return HomeRankingPeriod(id = id, label = "第${period}期")
}

private fun MusicTopListItem.toUgcItems(): List<UgcItem> {
    val archives = arcList.orEmpty().mapNotNull { it.toUgcItem() }
    if (archives.isNotEmpty()) return archives
    return listOfNotNull(toUgcItem())
}

private fun MusicTopListItem.toUgcItem(): UgcItem? {
    if (creationAid <= 0L) return null
    return UgcItem(
        aid = creationAid,
        bvid = creationBvid,
        title = creationTitle,
        cover = creationCover,
        author = creationNickname,
        authorMid = creationUp.takeIf { it > 0L },
        play = creationPlay.takeIf { it > 0 } ?: heat,
        danmaku = -1,
        duration = creationDuration,
        pubTime = creationReasonText()
    )
}

private fun MusicTopListArchive.toUgcItem(): UgcItem? {
    if (aid <= 0L) return null
    return UgcItem(
        aid = aid,
        bvid = bvid,
        title = title,
        cover = cover,
        author = upName,
        authorMid = mid.takeIf { it > 0L },
        play = play,
        danmaku = -1,
        duration = 0,
        pubTime = null
    )
}

private fun MusicTopListItem.creationReasonText(): String? {
    return listOfNotNull(
        "音乐榜 #$rank".takeIf { rank > 0 },
        "热度 $heat".takeIf { heat > 0 }
    ).joinToString(" · ").takeIf { it.isNotBlank() }
}
