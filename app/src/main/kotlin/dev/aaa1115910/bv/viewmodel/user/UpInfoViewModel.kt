package dev.aaa1115910.bv.viewmodel.user

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aaa1115910.biliapi.entity.user.SpaceVideoPage
import dev.aaa1115910.biliapi.http.BiliHttpApi
import dev.aaa1115910.biliapi.repositories.UserRepository
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.addWithMainContext
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.formatHourMinSec
import dev.aaa1115910.bv.util.toWanString
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class UpInfoViewModel(
    private val userRepository: UserRepository
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
    var seriesSummaryText by mutableStateOf("")
    var seasonsSeriesItems by mutableStateOf<List<String>>(emptyList())
    var isFollowing by mutableStateOf(false)
    var profileLoaded by mutableStateOf(false)
        private set
    var profileLoading by mutableStateOf(false)
        private set
    var videosLoading by mutableStateOf(false)
        private set
    var spaceVideos = mutableStateListOf<VideoCardData>()

    private var page = SpaceVideoPage()
    private var updating = false
    val noMore get() = !page.hasNext

    fun update() {
        viewModelScope.launch(Dispatchers.Default) {
            updateProfile()
            updateSpaceVideos()
        }
    }

    private suspend fun updateProfile() {
        if (profileLoading || upMid <= 0L || profileLoaded) return
        profileLoading = true
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
            upName = cardData.card.name.ifBlank { userInfo?.name.orEmpty().ifBlank { upName } }
            upFace = cardData.card.face.ifBlank { userInfo?.face.orEmpty() }
            upSign = userInfo?.sign?.takeIf { it.isNotBlank() } ?: cardData.card.sign
            followerText = cardData.follower.toWanString()
            likeText = cardData.likeNum.toWanString()
            archiveText = cardData.archiveCount.toWanString()
            isFollowing = cardData.following || userInfo?.isFollowed == true
            seriesSummaryText = runCatching {
                val seriesData = BiliHttpApi.getUserSeasonsSeries(
                    mid = upMid,
                    pageSize = 6
                ).getResponseData().itemsLists
                val seasons = seriesData.seasonsList.mapNotNull { item ->
                    item.meta.title
                        .ifBlank { item.meta.name }
                        .takeIf { it.isNotBlank() }
                        ?.let { title ->
                            if (item.meta.total > 0) "$title(${item.meta.total})" else title
                        }
                }
                val series = seriesData.seriesList.mapNotNull { item ->
                    item.meta.title
                        .ifBlank { item.meta.name }
                        .takeIf { it.isNotBlank() }
                        ?.let { title ->
                            if (item.meta.total > 0) "$title(${item.meta.total})" else title
                        }
                }
                buildList {
                    if (seasons.isNotEmpty()) add("合集 ${seasons.take(3).joinToString(" / ")}")
                    if (series.isNotEmpty()) add("系列 ${series.take(3).joinToString(" / ")}")
                }.joinToString("  ·  ")
                    .also {
                        seasonsSeriesItems = seasons + series
                    }
            }.getOrDefault("")
            profileLoaded = true
        }.onFailure {
            logger.fInfo { "Update up profile failed: ${it.stackTraceToString()}" }
        }
        profileLoading = false
    }

    private suspend fun updateSpaceVideos() {
        if (updating || noMore) return
        logger.fInfo { "Updating up [mid=$upMid] space videos from page $page" }
        updating = true
        videosLoading = true
        runCatching {
            val spaceVideoData = userRepository.getSpaceVideos(
                mid = upMid,
                page = page,
                preferApiType = Prefs.playbackApiType
            )
            spaceVideoData.videos.forEach { spaceVideoItem ->
                spaceVideos.addWithMainContext(
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
                )
            }
            page = spaceVideoData.page
            logger.fInfo { "Update up space videos success" }
        }.onFailure {
            logger.fInfo { "Update up space videos failed: ${it.stackTraceToString()}" }
        }
        videosLoading = false
        updating = false
    }
}
