package dev.aaa1115910.bv.viewmodel.user

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aaa1115910.biliapi.entity.FavoriteItemType
import dev.aaa1115910.biliapi.entity.user.SpaceVideoPage
import dev.aaa1115910.biliapi.http.BiliHttpApi
import dev.aaa1115910.biliapi.http.entity.user.UserSeasonArchivesData
import dev.aaa1115910.biliapi.http.entity.user.UserSeasonsSeriesData
import dev.aaa1115910.biliapi.repositories.FavoriteRepository
import dev.aaa1115910.biliapi.repositories.UserRepository
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.formatHourMinSec
import dev.aaa1115910.bv.util.toWanString
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class UpInfoViewModel(
    private val userRepository: UserRepository,
    private val favoriteRepository: FavoriteRepository
) : ViewModel() {
    companion object {
        private val logger = KotlinLogging.logger { }
    }

    var upName by mutableStateOf("")
    var upMid by mutableLongStateOf(0L)
    var upFace by mutableStateOf("")
    var upSign by mutableStateOf("")
    var followerText by mutableStateOf("")
    var likeText by mutableStateOf("")
    var archiveText by mutableStateOf("")
    var isFollowing by mutableStateOf(false)
    var selectedTab by mutableStateOf(UpSpaceTab.Videos)
    var profileLoaded by mutableStateOf(false)
        private set
    var profileLoading by mutableStateOf(false)
        private set
    var videosLoading by mutableStateOf(false)
        private set
    var videosLoaded by mutableStateOf(false)
        private set
    var spaceVideos = mutableStateListOf<VideoCardData>()
    var seasonsSeriesLoading by mutableStateOf(false)
        private set
    var seasonsSeriesLoaded by mutableStateOf(false)
        private set
    var seasonsSeries = mutableStateListOf<UpSeasonSeriesGroup>()
    var favoritesLoading by mutableStateOf(false)
        private set
    var favoritesLoaded by mutableStateOf(false)
        private set
    var favorites = mutableStateListOf<UpFavoriteGroup>()

    private var page = SpaceVideoPage()
    private var updating = false
    val noMore get() = !page.hasNext
    val visibleTabs: List<UpSpaceTab>
        get() = buildList {
            add(UpSpaceTab.Videos)
            if (seasonsSeries.isNotEmpty()) add(UpSpaceTab.SeasonsSeries)
            if (favorites.isNotEmpty()) add(UpSpaceTab.Favorites)
        }

    fun update() {
        viewModelScope.launch(Dispatchers.IO) {
            launch { updateProfile() }
            launch { updateSeasonsSeries() }
            launch { updateFavorites() }
            updateSpaceVideos()
        }
    }

    private suspend fun updateProfile() {
        if (profileLoading || upMid <= 0L || profileLoaded) return
        withContext(Dispatchers.Main) {
            profileLoading = true
        }
        runCatching {
            val cardData = BiliHttpApi.getUserCardInfo(
                uid = upMid,
                photo = true,
                sessData = Prefs.sessData
            ).getResponseData()
            val userInfo = runCatching {
                BiliHttpApi.getUserInfo(
                    uid = upMid,
                    sessData = Prefs.sessData
                ).getResponseData()
            }.getOrNull()
            withContext(Dispatchers.Main) {
                upName = cardData.card.name.ifBlank { userInfo?.name.orEmpty().ifBlank { upName } }
                upFace = cardData.card.face.ifBlank { userInfo?.face.orEmpty() }
                upSign = userInfo?.sign?.takeIf { it.isNotBlank() } ?: cardData.card.sign
                followerText = cardData.follower.toWanString()
                likeText = cardData.likeNum.toWanString()
                archiveText = cardData.archiveCount.toWanString()
                isFollowing = cardData.following || userInfo?.isFollowed == true
                profileLoaded = true
            }
        }.onFailure {
            logger.fInfo { "Update up profile failed: ${it.stackTraceToString()}" }
        }
        withContext(Dispatchers.Main) {
            profileLoading = false
        }
    }

    private suspend fun updateSpaceVideos() {
        if (updating || noMore) return
        logger.fInfo { "Updating up [mid=$upMid] space videos from page $page" }
        withContext(Dispatchers.Main) {
            updating = true
            videosLoading = true
        }
        runCatching {
            val spaceVideoData = userRepository.getSpaceVideos(
                mid = upMid,
                page = page,
                preferApiType = Prefs.playbackApiType
            )
            val newVideos = spaceVideoData.videos.map { spaceVideoItem ->
                VideoCardData(
                    avid = spaceVideoItem.aid,
                    title = spaceVideoItem.title,
                    //TODO 这里在改造 app 端接口时，没找到在空间内显示为合集样式封面的UP,没法进一步测试接口
                    cover = spaceVideoItem.cover,
                    upName = spaceVideoItem.author,
                    playString = spaceVideoItem.play.takeIf { it != -1 }.toWanString(),
                    danmakuString = spaceVideoItem.danmaku.takeIf { it != -1 }.toWanString(),
                    timeString = (spaceVideoItem.duration * 1000L).formatHourMinSec(),
                    pubTime = spaceVideoItem.pubTime
                )
            }
            withContext(Dispatchers.Main) {
                spaceVideos.addAll(newVideos)
                page = spaceVideoData.page
            }
            logger.fInfo { "Update up space videos success" }
        }.onFailure {
            logger.fInfo { "Update up space videos failed: ${it.stackTraceToString()}" }
        }
        withContext(Dispatchers.Main) {
            videosLoaded = true
            videosLoading = false
            updating = false
        }
    }

    private suspend fun updateSeasonsSeries() {
        if (seasonsSeriesLoading || seasonsSeriesLoaded || upMid <= 0L) return
        withContext(Dispatchers.Main) {
            seasonsSeriesLoading = true
        }
        runCatching {
            val seriesData = BiliHttpApi.getUserSeasonsSeries(
                mid = upMid,
                pageSize = 20
            ).getResponseData().itemsLists
            val groups = (seriesData.seasonsList.mapNotNull { item ->
                item.meta.toGroup(UpSeasonSeriesType.Season)
            } + seriesData.seriesList.mapNotNull { item ->
                item.meta.toGroup(UpSeasonSeriesType.Series)
            }).map { group ->
                val videos = runCatching {
                    when (group.type) {
                        UpSeasonSeriesType.Season -> BiliHttpApi.getUserSeasonArchives(
                            mid = upMid,
                            seasonId = group.id,
                            pageSize = 30
                        ).getResponseData().archives

                        UpSeasonSeriesType.Series -> BiliHttpApi.getUserSeriesArchives(
                            mid = upMid,
                            seriesId = group.id,
                            pageSize = 30
                        ).getResponseData().archives
                    }.map { it.toVideoCardData(upName) }
                }.onFailure {
                    logger.fInfo { "Update up ${group.type} archives failed: ${it.stackTraceToString()}" }
                }.getOrDefault(emptyList())
                group.copy(videos = videos)
            }.filter { it.videos.isNotEmpty() || it.total > 0 }
            withContext(Dispatchers.Main) {
                seasonsSeries.clear()
                seasonsSeries.addAll(groups)
            }
        }.onFailure {
            logger.fInfo { "Update up seasons series failed: ${it.stackTraceToString()}" }
        }
        withContext(Dispatchers.Main) {
            seasonsSeriesLoaded = true
            seasonsSeriesLoading = false
        }
    }

    private suspend fun updateFavorites() {
        if (favoritesLoading || favoritesLoaded || upMid <= 0L) return
        withContext(Dispatchers.Main) {
            favoritesLoading = true
        }
        runCatching {
            val folders = favoriteRepository.getAllFavoriteFolderMetadataList(
                mid = upMid,
                type = FavoriteItemType.Video,
                preferApiType = Prefs.playbackApiType
            ).filter { it.mediaCount > 0 }
            val groups = folders.take(8).mapNotNull { folder ->
                runCatching {
                    val data = favoriteRepository.getFavoriteFolderData(
                        mediaId = folder.id,
                        pageSize = 10,
                        pageNumber = 1,
                        preferApiType = Prefs.playbackApiType
                    )
                    UpFavoriteGroup(
                        id = folder.id,
                        title = folder.title,
                        cover = folder.cover,
                        total = folder.mediaCount,
                        videos = data.medias
                            .filter { it.type == FavoriteItemType.Video }
                            .map {
                                VideoCardData(
                                    avid = it.id,
                                    title = it.title,
                                    cover = it.cover,
                                    upName = it.upper.name,
                                    upMid = it.upper.mid,
                                    timeString = (it.duration * 1000L).formatHourMinSec(),
                                    pubTime = it.pubtime.takeIf { time -> time > 0L }?.toSmartPubTime()
                                )
                            }
                    )
                }.onFailure {
                    logger.fInfo { "Update up favorite folder ${folder.id} failed: ${it.stackTraceToString()}" }
                }.getOrNull()
            }.filter { it.videos.isNotEmpty() }
            withContext(Dispatchers.Main) {
                favorites.clear()
                favorites.addAll(groups)
            }
        }.onFailure {
            logger.fInfo { "Update up favorite failed: ${it.stackTraceToString()}" }
        }
        withContext(Dispatchers.Main) {
            favoritesLoaded = true
            favoritesLoading = false
        }
    }

    fun selectTab(tab: UpSpaceTab) {
        selectedTab = tab
    }

    fun setFollow(follow: Boolean) {
        if (upMid <= 0L || !Prefs.isLogin) return

        viewModelScope.launch(Dispatchers.IO) {
            logger.fInfo { "${if (follow) "Add" else "Del"} follow to up $upMid" }
            runCatching {
                if (follow) {
                    userRepository.followUser(
                        mid = upMid,
                        preferApiType = Prefs.playbackApiType
                    )
                } else {
                    userRepository.unfollowUser(
                        mid = upMid,
                        preferApiType = Prefs.playbackApiType
                    )
                }
            }.onSuccess { result ->
                logger.fInfo { "${if (follow) "Add" else "Del"} follow up result: $result" }
                if (result) {
                    withContext(Dispatchers.Main) {
                        isFollowing = follow
                    }
                }
            }.onFailure {
                logger.fInfo { "Update up follow failed: ${it.stackTraceToString()}" }
            }
        }
    }

    private fun UserSeasonsSeriesData.Meta.toGroup(type: UpSeasonSeriesType): UpSeasonSeriesGroup? {
        val groupId = when (type) {
            UpSeasonSeriesType.Season -> seasonId
            UpSeasonSeriesType.Series -> seriesId
        }.takeIf { it > 0L } ?: return null
        val groupTitle = title.ifBlank { name }.takeIf { it.isNotBlank() } ?: return null
        return UpSeasonSeriesGroup(
            id = groupId,
            type = type,
            title = groupTitle,
            cover = cover,
            total = total,
            description = description
        )
    }

    private fun UserSeasonArchivesData.Archive.toVideoCardData(defaultUpName: String): VideoCardData {
        return VideoCardData(
            avid = aid,
            title = title,
            cover = pic,
            upName = defaultUpName,
            upMid = upMid.takeIf { it > 0L } ?: upMid,
            playString = stat.view.takeIf { it != -1 }.toWanString(),
            danmakuString = stat.danmaku.takeIf { it != -1 }.toWanString(),
            timeString = (duration * 1000L).formatHourMinSec(),
            pubTime = pubdate.takeIf { it > 0L }?.toSmartPubTime()
        )
    }

    private fun Long.toSmartPubTime(): String {
        val nowSeconds = System.currentTimeMillis() / 1000
        val diff = nowSeconds - this
        return when {
            diff < 60 -> "刚刚"
            diff < 3600 -> "${diff / 60}分钟前"
            diff < 86400 -> "${diff / 3600}小时前"
            diff < 86400 * 30 -> "${diff / 86400}天前"
            else -> java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(java.util.Date(this * 1000))
        }
    }
}

enum class UpSpaceTab(val displayName: String) {
    Videos("投稿"),
    SeasonsSeries("合集和系列"),
    Favorites("收藏")
}

enum class UpSeasonSeriesType(val displayName: String) {
    Season("合集"),
    Series("系列")
}

data class UpSeasonSeriesGroup(
    val id: Long,
    val type: UpSeasonSeriesType,
    val title: String,
    val cover: String,
    val total: Int,
    val description: String,
    val videos: List<VideoCardData> = emptyList()
)

data class UpFavoriteGroup(
    val id: Long,
    val title: String,
    val cover: String?,
    val total: Int,
    val videos: List<VideoCardData>
)
