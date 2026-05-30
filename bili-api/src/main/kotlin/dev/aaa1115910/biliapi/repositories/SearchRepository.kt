package dev.aaa1115910.biliapi.repositories

import bilibili.app.interfaces.v1.suggestionResult3Req
import bilibili.pagination.pagination
import bilibili.polymer.app.search.v1.SearchByTypeRequest
import bilibili.polymer.app.search.v1.searchByTypeRequest
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.entity.search.Hotword
import dev.aaa1115910.biliapi.grpc.utils.handleGrpcException
import dev.aaa1115910.biliapi.http.BiliHttpApi
import dev.aaa1115910.biliapi.http.BiliHttpProxyApi
import dev.aaa1115910.biliapi.http.entity.search.SearchLiveResult
import dev.aaa1115910.biliapi.http.entity.search.SearchMediaResult
import dev.aaa1115910.biliapi.http.entity.search.SearchVideoResult
import dev.aaa1115910.biliapi.http.entity.search.SearchBiliUserResult
import org.koin.core.annotation.Single
import dev.aaa1115910.biliapi.http.util.smartDate

@Single
class SearchRepository(
    private val authRepository: AuthRepository,
    private val channelRepository: ChannelRepository
) {
    private val searchSuggestStub
        get() = runCatching {
            bilibili.app.interfaces.v1.SearchGrpcKt.SearchCoroutineStub(channelRepository.defaultChannel!!)
        }.getOrNull()

    private val searchResultStub
        get() = runCatching {
            bilibili.polymer.app.search.v1.SearchGrpcKt.SearchCoroutineStub(channelRepository.defaultChannel!!)
        }.getOrNull()

    private val proxySearchResultStub
        get() = runCatching {
            bilibili.polymer.app.search.v1.SearchGrpcKt.SearchCoroutineStub(channelRepository.proxyChannel!!)
        }.getOrNull()

    /*private val searchStub
        get() = runCatching {
            SearchGrpcKt.SearchCoroutineStub(channelRepository.defaultChannel!!)
        }.getOrNull()

    suspend fun search(
        keyword: String,
        page: Int = 1,
        pageSize: Int = 20,
        preferApiType: ApiType = ApiType.Web
    ): SearchData {
        return when (preferApiType) {
            ApiType.Web -> {
                val data = BiliHttpApi.search(
                    keyword = keyword,
                    page = page,
                    pageSize = pageSize,
                    sessData = authRepository.sessionData!!,
                ).getResponseData()
                SearchData.fromSearchResponse(data)
            }

            ApiType.App -> {
                val reply = searchStub?.searchV2(searchV2Req {
                    this.keyword = keyword
                    this.page = page
                    this.pageSize = pageSize
                })
                SearchData.fromSearchResponse(reply!!)
            }
        }
    }*/

    suspend fun getSearchHotwords(
        limit: Int = 30,
        preferApiType: ApiType = ApiType.Web
    ): List<Hotword> {
        return when (preferApiType) {
            ApiType.Web -> BiliHttpApi.getWebSearchSquare(limit = limit)
                .getResponseData().trending.list
                .map { Hotword.fromHttpWebHotword(it) }

            /*ApiType.App -> BiliHttpApi.getAppSearchSquare(limit = limit)
                .getResponseData()
                .firstOrNull { it.type == "trending" }
                ?.data?.list
                ?.map { Hotword.fromHttpAppSquareDataItem(it) }
                ?: emptyList()*/

            ApiType.App -> BiliHttpApi.getSearchTrendRank(limit = 50)
                .getResponseData().list
                .map { Hotword.fromHttpAppSearchTrendingHotword(it) }
        }
    }

    suspend fun getSearchSuggest(
        keyword: String,
        preferApiType: ApiType = ApiType.App
    ): List<String> {
        return when (preferApiType) {
            ApiType.Web -> BiliHttpApi.getKeywordSuggest(
                term = keyword,
                buvid = authRepository.buvid ?: "",
            ).suggests.map { it.value }

            //TODO 返回的关键词提示中可能包含通过avid/bvid/专栏id等的直达跳转结果项，需要过滤掉或进行单独处理
            ApiType.App -> searchSuggestStub?.suggest3(suggestionResult3Req {
                this.keyword = keyword
            })?.listList?.map { it.keyword } ?: emptyList()
        }
    }

    /**
     * 按分类进行搜索
     *
     * app 端的接口无法对视频投稿结果进行筛选搜索
     */
    suspend fun searchType(
        keyword: String,
        type: SearchType,
        tid: Int?,
        order: SearchFilterOrderType,
        duration: SearchFilterDuration,
        page: SearchTypePage,
        preferApiType: ApiType = ApiType.App,
        enableProxy: Boolean = false
    ): SearchTypeResult {
        suspend fun searchByApp(): SearchTypeResult {
            val searchTypeReply = runCatching {
                val searchTypeRequest = searchByTypeRequest {
                    this.keyword = keyword
                    this.type = type.grpcTypeParam
                    categorySort = order.grpcOrderParam
                    userType = SearchByTypeRequest.UserType.ALL
                    userSort = SearchByTypeRequest.UserSort.USER_SORT_DEFAULT
                    pagination = pagination {
                        next = page.nextPageForApp
                    }
                }
                if (enableProxy) {
                    proxySearchResultStub?.searchByType(searchTypeRequest)
                        ?: throw IllegalStateException("Proxy search result stub is not initialized")
                } else {
                    searchResultStub?.searchByType(searchTypeRequest)
                        ?: throw IllegalStateException("Search result stub is not initialized")
                }
            }.onFailure { handleGrpcException(it) }.getOrThrow()
            return SearchTypeResult.fromSearchTypeResult(searchTypeReply, preferredType = type)
        }

        suspend fun searchByWeb(): SearchTypeResult {
            return runCatching {
                val webSearchType = when (type) {
                    SearchType.Live -> "live_room"
                    else -> type.httpTypeParam
                }
                val response = if (enableProxy) {
                    BiliHttpProxyApi.searchType(
                        keyword = keyword,
                        type = webSearchType,
                        page = page.nextPageForWeb,
                        tid = tid,
                        order = order.httpOrderParam,
                        duration = duration.httpDurationParam,
                        buvid3 = authRepository.buvid3 ?: "",
                        sessData = authRepository.sessionData ?: "",
                    )
                } else {
                    BiliHttpApi.searchType(
                        keyword = keyword,
                        type = webSearchType,
                        page = page.nextPageForWeb,
                        tid = tid,
                        order = order.httpOrderParam,
                        duration = duration.httpDurationParam,
                        buvid3 = authRepository.buvid3 ?: "",
                        sessData = authRepository.sessionData ?: "",
                    )
                }.getResponseData()
                SearchTypeResult.fromSearchTypeResult(response, preferredType = type)
            }.getOrElse { webError ->
                if (type == SearchType.Video || preferApiType == ApiType.Web) {
                    runCatching { searchByApp() }.getOrElse { throw webError }
                } else {
                    throw webError
                }
            }
        }

        return when (preferApiType) {
            ApiType.Web -> searchByWeb()
            ApiType.App -> runCatching { searchByApp() }
                .let { appResult ->
                    val result = appResult.getOrNull()
                    if (type == SearchType.Live && result != null && result.lives.isEmpty()) {
                        searchByWeb()
                    } else {
                        appResult.getOrElse { appError ->
                            if (type == SearchType.Video || type == SearchType.Live) {
                                searchByWeb()
                            } else {
                                throw appError
                            }
                        }
                    }
                }
        }
    }
}

data class SearchTypePage(
    val nextPageForWeb: Int = 1,
    val nextPageForApp: String = ""
)

enum class SearchType(
    val httpTypeParam: String,
    val grpcTypeParam: Int
) {
    Video(httpTypeParam = "video", grpcTypeParam = 10),
    MediaBangumi(httpTypeParam = "media_bangumi", grpcTypeParam = 7),
    MediaFt(httpTypeParam = "media_ft", grpcTypeParam = 8),
    Live(httpTypeParam = "live", grpcTypeParam = 4),
    BiliUser(httpTypeParam = "bili_user", grpcTypeParam = 2),
    //Article grpcTypeParam = 6
}

enum class SearchFilterOrderType(
    val httpOrderParam: String?,
    val grpcOrderParam: SearchByTypeRequest.CategorySort
) {
    ComprehensiveSort(
        httpOrderParam = "totalrank",
        grpcOrderParam = SearchByTypeRequest.CategorySort.CATEGORY_SORT_DEFAULT
    ),
    MostClicks(
        httpOrderParam = "click",
        grpcOrderParam = SearchByTypeRequest.CategorySort.CATEGORY_SORT_CLICK_COUNT
    ),
    LatestPublish(
        httpOrderParam = "pubdate",
        grpcOrderParam = SearchByTypeRequest.CategorySort.CATEGORY_SORT_PUBLISH_TIME
    ),
    MostDanmaku(
        httpOrderParam = "dm",
        grpcOrderParam = SearchByTypeRequest.CategorySort.UNRECOGNIZED
    ),
    MostFavorites(
        httpOrderParam = "stow",
        grpcOrderParam = SearchByTypeRequest.CategorySort.UNRECOGNIZED
    ),
    MostComment(
        httpOrderParam = null,
        grpcOrderParam = SearchByTypeRequest.CategorySort.CATEGORY_SORT_COMMENT_COUNT
    ),
    MostLikes(
        httpOrderParam = null,
        grpcOrderParam = SearchByTypeRequest.CategorySort.CATEGORY_SORT_LIKE_COUNT
    );

    companion object {
        val webFilters =
            listOf(ComprehensiveSort, MostClicks, LatestPublish, MostDanmaku, MostFavorites)
        val allFilters =
            listOf(ComprehensiveSort, MostClicks, LatestPublish, MostComment, MostLikes)
    }
}

enum class SearchFilterDuration(
    val httpDurationParam: Int?,
    //val grpcOrderParam: SearchByTypeRequest.
) {
    All(null),
    LessThan10Minutes(1),
    Between10And30Minutes(2),
    Between30And60Minutes(3),
    MoreThan60Minutes(4);
}

data class SearchTypeResult(
    val videos: List<Video> = emptyList(),
    val pgcs: List<Pgc> = emptyList(),
    val lives: List<Live> = emptyList(),
    val users: List<User> = emptyList(),
    val page: SearchTypePage
) {
    companion object {
        fun fromSearchTypeResult(
            result: dev.aaa1115910.biliapi.http.entity.search.SearchResultData,
            preferredType: SearchType? = null
        ): SearchTypeResult {
            val page = SearchTypePage(nextPageForWeb = result.page + 1)
            val videos = result.searchTypeResults.filterIsInstance<SearchVideoResult>()
                .map { Video.fromSearchVideoResult(it) }
            val pgcs = result.searchTypeResults.filterIsInstance<SearchMediaResult>()
                .map { Pgc.fromSearchPgcResult(it) }
            val lives = result.searchTypeResults.filterIsInstance<SearchLiveResult>()
                .mapNotNull { Live.fromSearchLiveResult(it) }
            val users = result.searchTypeResults.filterIsInstance<SearchBiliUserResult>()
                .map { User.fromSearchUserResult(it) }

            preferredType?.let { type ->
                return when (type) {
                    SearchType.Video -> SearchTypeResult(videos = videos, page = page)
                    SearchType.MediaBangumi,
                    SearchType.MediaFt -> SearchTypeResult(pgcs = pgcs, page = page)
                    SearchType.Live -> SearchTypeResult(lives = lives, page = page)
                    SearchType.BiliUser -> SearchTypeResult(users = users, page = page)
                }
            }

            return when (result.searchTypeResults.firstOrNull { it is SearchVideoResult || it is SearchMediaResult || it is SearchLiveResult || it is SearchBiliUserResult }) {
                is SearchVideoResult -> {
                    SearchTypeResult(
                        videos = videos,
                        page = page
                    )
                }

                is SearchMediaResult -> {
                    SearchTypeResult(
                        pgcs = pgcs,
                        page = page
                    )
                }

                is SearchLiveResult -> {
                    SearchTypeResult(
                        lives = lives,
                        page = page
                    )
                }

                is SearchBiliUserResult -> {
                    SearchTypeResult(
                        users = users,
                        page = page
                    )
                }

                else -> {
                    SearchTypeResult(page = page)
                }
            }
        }

        fun fromSearchTypeResult(
            result: bilibili.polymer.app.search.v1.SearchByTypeResponse,
            preferredType: SearchType? = null
        ): SearchTypeResult {
            val page = SearchTypePage(nextPageForApp = result.pagination.next)
            val videos = result.itemsList
                .filter { it.cardItemCase == bilibili.polymer.app.search.v1.Item.CardItemCase.AV }
                .map { Video.fromSearchVideoCard(it) }
            val pgcs = result.itemsList
                .filter { it.cardItemCase == bilibili.polymer.app.search.v1.Item.CardItemCase.BANGUMI }
                .map { Pgc.fromSearchPgcCard(it) }
            val users = result.itemsList
                .filter { it.cardItemCase == bilibili.polymer.app.search.v1.Item.CardItemCase.AUTHOR }
                .map { User.fromSearchUserCard(it) }
            val lives = result.itemsList.mapNotNull { item ->
                when (item.cardItemCase) {
                    bilibili.polymer.app.search.v1.Item.CardItemCase.LIVE -> Live.fromSearchLiveCard(item)
                    bilibili.polymer.app.search.v1.Item.CardItemCase.LIVE_INLINE -> Live.fromSearchLiveInlineCard(item)
                    else -> null
                }
            }

            preferredType?.let { type ->
                return when (type) {
                    SearchType.Video -> SearchTypeResult(videos = videos, page = page)
                    SearchType.MediaBangumi,
                    SearchType.MediaFt -> SearchTypeResult(pgcs = pgcs, page = page)
                    SearchType.Live -> SearchTypeResult(lives = lives, page = page)
                    SearchType.BiliUser -> SearchTypeResult(users = users, page = page)
                }
            }

            return when (result.itemsList.firstOrNull {
                it.cardItemCase in resultCardTypes
            }?.cardItemCase) {
                bilibili.polymer.app.search.v1.Item.CardItemCase.AV -> {
                    SearchTypeResult(
                        videos = videos,
                        page = page
                    )
                }

                bilibili.polymer.app.search.v1.Item.CardItemCase.BANGUMI -> {
                    SearchTypeResult(
                        pgcs = pgcs,
                        page = page
                    )
                }

                bilibili.polymer.app.search.v1.Item.CardItemCase.AUTHOR -> {
                    SearchTypeResult(
                        users = users,
                        page = page
                    )
                }

                bilibili.polymer.app.search.v1.Item.CardItemCase.LIVE -> {
                    SearchTypeResult(
                        lives = lives,
                        page = page
                    )
                }

                bilibili.polymer.app.search.v1.Item.CardItemCase.LIVE_INLINE -> {
                    SearchTypeResult(
                        lives = lives,
                        page = page
                    )
                }

                else -> {
                    SearchTypeResult(page = page)
                }
            }
        }

        private val resultCardTypes = listOf(
            bilibili.polymer.app.search.v1.Item.CardItemCase.AV,
            bilibili.polymer.app.search.v1.Item.CardItemCase.BANGUMI,
            bilibili.polymer.app.search.v1.Item.CardItemCase.AUTHOR,
            bilibili.polymer.app.search.v1.Item.CardItemCase.LIVE,
            bilibili.polymer.app.search.v1.Item.CardItemCase.LIVE_INLINE
        )
    }

    interface SearchTypeResultItem

    data class Video(
        val aid: Long,
        val bvid: String,
        val title: String,
        val cover: String,
        val author: String,
        val mid: Long,
        val duration: Int,
        val play: Int,
        val danmaku: Int,
        val pubTime: String? = null
    ) : SearchTypeResultItem {
        companion object {
            fun fromSearchVideoResult(video: dev.aaa1115910.biliapi.http.entity.search.SearchVideoResult) =
                Video(
                    aid = video.aid,
                    bvid = video.bvid,
                    title = video.title,
                    cover = "https:${video.pic}",
                    author = video.author,
                    mid = video.mid,
                    duration = convertStringTimeToSeconds(video.duration),
                    play = video.play,
                    danmaku = video.danmaku,
                    pubTime = video.pubDate.smartDate
                )

            fun fromSearchVideoCard(video: bilibili.polymer.app.search.v1.Item) =
                Video(
                    aid = video.param.toLong(),
                    bvid = video.av.share.video.bvid,
                    title = video.av.title,
                    cover = video.av.cover,
                    author = video.av.author,
                    mid = video.av.mid,
                    duration = convertStringTimeToSeconds(video.av.duration),
                    play = video.av.play,
                    danmaku = video.av.danmaku
                )
        }
    }

    data class Pgc(
        val title: String,
        val cover: String,
        val star: Float,
        val seasonId: Int
    ) : SearchTypeResultItem {
        companion object {
            fun fromSearchPgcResult(pgc: dev.aaa1115910.biliapi.http.entity.search.SearchMediaResult) =
                Pgc(
                    title = pgc.title,
                    cover = pgc.cover,
                    star = pgc.mediaScore.score,
                    seasonId = pgc.seasonId
                )

            fun fromSearchPgcCard(pgc: bilibili.polymer.app.search.v1.Item) =
                Pgc(
                    title = pgc.bangumi.title,
                    cover = pgc.bangumi.cover,
                    star = pgc.bangumi.rating.toFloat(),
                    seasonId = pgc.bangumi.seasonId.toInt()
                )
        }
    }

    data class Live(
        val roomId: Int,
        val title: String,
        val cover: String,
        val upName: String,
        val online: Int,
        val areaName: String = ""
    ) : SearchTypeResultItem {
        companion object {
            fun fromSearchLiveResult(live: dev.aaa1115910.biliapi.http.entity.search.SearchLiveResult): Live? {
                val roomId = live.roomId.takeIf { it > 0 } ?: return null
                return Live(
                    roomId = roomId,
                    title = live.title,
                    cover = normalizeProtocolUrl(live.cover.ifBlank { live.userCover }),
                    upName = live.uname,
                    online = live.online,
                    areaName = live.cateName
                )
            }

            fun fromSearchLiveCard(item: bilibili.polymer.app.search.v1.Item): Live? {
                val roomId = item.param.toIntOrNull()
                    ?: item.uri.substringAfter("live.bilibili.com/", "")
                        .takeWhile { it.isDigit() }
                        .toIntOrNull()
                    ?: return null
                return Live(
                    roomId = roomId,
                    title = item.live.title,
                    cover = item.live.cover,
                    upName = item.live.name,
                    online = item.live.online,
                    areaName = item.live.badge
                )
            }

            fun fromSearchLiveInlineCard(item: bilibili.polymer.app.search.v1.Item): Live? {
                val roomId = item.liveInline.roomid.takeIf { it > 0 }?.toInt()
                    ?: item.param.toIntOrNull()
                    ?: return null
                return Live(
                    roomId = roomId,
                    title = item.liveInline.title,
                    cover = item.liveInline.cover,
                    upName = "",
                    online = 0
                )
            }
        }
    }

    data class User(
        val mid: Long,
        val name: String,
        val avatar: String,
        val sign: String
    ) : SearchTypeResultItem {
        companion object {
            fun fromSearchUserResult(user: dev.aaa1115910.biliapi.http.entity.search.SearchBiliUserResult) =
                User(
                    mid = user.mid,
                    name = user.uname,
                    avatar = "https:${user.upic}",
                    sign = user.usign
                )

            fun fromSearchUserCard(user: bilibili.polymer.app.search.v1.Item) =
                User(
                    mid = user.param.toLong(),
                    name = user.author.title,
                    avatar = user.author.cover,
                    sign = user.author.sign
                )
        }
    }
}

private fun normalizeProtocolUrl(url: String): String {
    return when {
        url.startsWith("//") -> "https:$url"
        else -> url
    }
}

private fun convertStringTimeToSeconds(time: String): Int {
    val parts = time.split(":")
    val hours = if (parts.size == 3) parts[0].toInt() else 0
    val minutes = parts[parts.size - 2].toInt()
    val seconds = parts[parts.size - 1].toInt()
    return (hours * 3600) + (minutes * 60) + seconds
}
