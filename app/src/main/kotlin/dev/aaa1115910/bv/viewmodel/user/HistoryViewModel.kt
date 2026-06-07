package dev.aaa1115910.bv.viewmodel.user

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aaa1115910.biliapi.http.entity.AuthFailureException
import dev.aaa1115910.biliapi.entity.user.HistoryBusiness
import dev.aaa1115910.biliapi.entity.user.HistoryItem
import dev.aaa1115910.biliapi.repositories.HistoryRepository
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.BuildConfig
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.entity.live.LiveRoomCard
import dev.aaa1115910.bv.repository.UserRepository
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.fWarn
import dev.aaa1115910.bv.util.formatHourMinSec
import dev.aaa1115910.bv.util.toast
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class HistoryViewModel(
    private val userRepository: UserRepository,
    private val historyRepository: HistoryRepository
) : ViewModel() {
    companion object {
        private val logger = KotlinLogging.logger { }
        private const val MaxEmptyPageSkips = 12
    }

    var selectedType by mutableStateOf(HistoryContentType.Video)
        private set
    var histories = mutableStateListOf<VideoCardData>()
    var liveHistories = mutableStateListOf<LiveRoomCard>()
    var noMore by mutableStateOf(false)
    var refreshingType by mutableStateOf<HistoryContentType?>(null)
        private set

    private var videoCursor = 0L
    private var liveCursor = 0L
    private var videoNoMore = false
    private var liveNoMore = false
    private var updatingType: HistoryContentType? = null

    private var updateJob: Job? = null

    fun update() {
        update(reset = false)
    }

    private fun update(reset: Boolean) {
        if(updateJob?.isActive == true) return
        val type = selectedType
        updateJob = viewModelScope.launch(Dispatchers.IO) {
            updateHistories(type, reset = reset)
        }
    }

    fun selectType(type: HistoryContentType) {
        if (selectedType == type) return
        selectedType = type
        syncNoMore()
        if (currentItemCount() == 0 && !noMore) update()
    }

    fun refreshType(type: HistoryContentType) {
        updateJob?.cancel()
        updateJob = null
        updatingType = null
        selectedType = type
        resetTypePagination(type)
        clearTypeItems(type)
        syncNoMore()
        update(reset = true)
    }

    fun currentItemCount(): Int = when (selectedType) {
        HistoryContentType.Video -> histories.size
        HistoryContentType.Live -> liveHistories.size
    }

    fun clearData() {
        updateJob?.cancel()
        histories.clear()
        liveHistories.clear()
        videoCursor = 0
        liveCursor = 0
        videoNoMore = false
        liveNoMore = false
        noMore = false
        updatingType = null
    }

    private suspend fun updateHistories(
        type: HistoryContentType,
        reset: Boolean = false,
        context: Context = BVApp.context
    ) {
        if (updatingType != null || (!reset && noMore(type))) return
        val cursor = if (reset) 0L else cursor(type)
        logger.fInfo { "Updating histories with params [type=$type, cursor=$cursor, apiType=${Prefs.apiType}]" }
        updatingType = type
        withContext(Dispatchers.Main) {
            refreshingType = type
        }
        runCatching {
            val nextVideos = mutableListOf<VideoCardData>()
            val nextLives = mutableListOf<LiveRoomCard>()
            var nextCursor = cursor
            var skippedEmptyPages = 0
            do {
                val data = historyRepository.getHistories(
                    cursor = nextCursor,
                    business = type.business,
                    preferApiType = Prefs.apiType
                )
                nextCursor = data.cursor

                data.data.forEach { historyItem ->
                    when (type) {
                        HistoryContentType.Video -> {
                            nextVideos.add(
                                VideoCardData(
                                    avid = historyItem.oid,
                                    title = historyItem.title,
                                    cover = historyItem.cover,
                                    upName = historyItem.author,
                                    upMid = historyItem.mid,
                                    timeString = if (historyItem.progress == -1) context.getString(R.string.play_time_finish)
                                    else context.getString(
                                        R.string.play_time_history,
                                        (historyItem.progress * 1000L).formatHourMinSec(),
                                        (historyItem.duration * 1000L).formatHourMinSec()
                                    )
                                )
                            )
                        }

                        HistoryContentType.Live -> {
                            val roomId = historyItem.liveRoomId() ?: return@forEach
                            nextLives.add(
                                LiveRoomCard(
                                    roomId = roomId,
                                    title = historyItem.title,
                                    cover = historyItem.cover,
                                    upName = historyItem.author,
                                    online = 0,
                                    areaName = historyItem.tagName,
                                    badges = listOf(if (historyItem.liveStatus == 1) "直播中" else "未开播")
                                )
                            )
                        }
                    }
                }

                if (type.hasLoadedItems(nextVideos, nextLives) || nextCursor == 0L) break
                skippedEmptyPages++
            } while (skippedEmptyPages < MaxEmptyPageSkips)

            withContext(Dispatchers.Main) {
                when (type) {
                    HistoryContentType.Video -> {
                        histories.addAll(nextVideos)
                    }

                    HistoryContentType.Live -> {
                        liveHistories.addAll(nextLives)
                    }
                }
            }
            //update cursor
            setCursor(type, nextCursor)
            logger.fInfo { "Update history cursor: [type=$type, cursor=$nextCursor]" }
            logger.fInfo { "Update histories success" }
            if (nextCursor == 0L) {
                withContext(Dispatchers.Main) {
                    setNoMore(type, true)
                    syncNoMore()
                }
                logger.fInfo { "No more history" }
            }
        }.onFailure {
            logger.fWarn { "Update histories failed: ${it.stackTraceToString()}" }
            when (it) {
                is AuthFailureException -> {
                    withContext(Dispatchers.Main) {
                        BVApp.context.getString(R.string.exception_auth_failure)
                            .toast(BVApp.context)
                    }
                    logger.fInfo { "User auth failure" }
                    if (!BuildConfig.DEBUG) userRepository.logout()
                }

                else -> {}
            }
        }
        updatingType = null
        withContext(Dispatchers.Main) {
            if (refreshingType == type) refreshingType = null
        }
    }

    private fun cursor(type: HistoryContentType): Long = when (type) {
        HistoryContentType.Video -> videoCursor
        HistoryContentType.Live -> liveCursor
    }

    private fun setCursor(type: HistoryContentType, cursor: Long) {
        when (type) {
            HistoryContentType.Video -> videoCursor = cursor
            HistoryContentType.Live -> liveCursor = cursor
        }
    }

    private fun noMore(type: HistoryContentType): Boolean = when (type) {
        HistoryContentType.Video -> videoNoMore
        HistoryContentType.Live -> liveNoMore
    }

    private fun setNoMore(type: HistoryContentType, value: Boolean) {
        when (type) {
            HistoryContentType.Video -> videoNoMore = value
            HistoryContentType.Live -> liveNoMore = value
        }
    }

    private fun resetTypePagination(type: HistoryContentType) {
        when (type) {
            HistoryContentType.Video -> {
                videoCursor = 0L
                videoNoMore = false
            }

            HistoryContentType.Live -> {
                liveCursor = 0L
                liveNoMore = false
            }
        }
        noMore = false
    }

    private fun clearTypeItems(type: HistoryContentType) {
        when (type) {
            HistoryContentType.Video -> histories.clear()
            HistoryContentType.Live -> liveHistories.clear()
        }
    }

    private fun syncNoMore() {
        noMore = noMore(selectedType)
    }
}

enum class HistoryContentType(
    val displayName: String,
    val business: HistoryBusiness
) {
    Video("视频", HistoryBusiness.Video),
    Live("直播", HistoryBusiness.Live)
}

private fun HistoryItem.liveRoomId(): Int? {
    oid.takeIf { it > 0L && it <= Int.MAX_VALUE }?.let { return it.toInt() }
    return uri.substringAfter("live.bilibili.com/", "")
        .takeWhile { it.isDigit() }
        .toIntOrNull()
}

private fun HistoryContentType.hasLoadedItems(
    videos: List<VideoCardData>,
    lives: List<LiveRoomCard>
): Boolean = when (this) {
    HistoryContentType.Video -> videos.isNotEmpty()
    HistoryContentType.Live -> lives.isNotEmpty()
}
