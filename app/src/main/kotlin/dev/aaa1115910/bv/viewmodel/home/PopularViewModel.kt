package dev.aaa1115910.bv.viewmodel.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dev.aaa1115910.biliapi.entity.rank.PopularVideoPage
import dev.aaa1115910.biliapi.entity.ugc.UgcItem
import dev.aaa1115910.biliapi.repositories.RecommendVideoRepository
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.telemetry.FirebaseTelemetry
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.addAllWithMainContext
import dev.aaa1115910.bv.util.fError
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.toast
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class PopularViewModel(
    private val recommendVideoRepository: RecommendVideoRepository
) : ViewModel() {
    private val logger = KotlinLogging.logger {}
    val popularVideoList = mutableStateListOf<UgcItem>()

    private var nextPage = PopularVideoPage()
    var selectedCategory by mutableStateOf(PopularRankCategory.All)
        private set
    var refreshing by mutableStateOf(false)
    var loading by mutableStateOf(false)

    suspend fun loadMore(
        beforeAppendData: () -> Unit = {}
    ) {
        if (!loading) loadData(
            beforeAppendData = beforeAppendData
        )
    }

    private suspend fun loadData(
        beforeAppendData: () -> Unit
    ) {
        loading = true
        logger.fInfo { "Load more popular videos" }
        runCatching {
            when (selectedCategory.feedSource) {
                PopularFeedSource.Popular -> {
                    val popularVideoData = recommendVideoRepository.getPopularVideos(
                        page = nextPage,
                        preferApiType = Prefs.recommendationApiType.toRequestApiType(),
                        useAuth = Prefs.recommendationApiType.useAuth
                    )
                    beforeAppendData()
                    nextPage = popularVideoData.nextPage
                    popularVideoList.addAllWithMainContext(popularVideoData.list)
                }

                PopularFeedSource.Rank -> {
                    if (popularVideoList.isNotEmpty()) return@runCatching
                    val rankItems = recommendVideoRepository.getRankingVideos(selectedCategory.rid ?: 0)
                    beforeAppendData()
                    popularVideoList.addAllWithMainContext(rankItems)
                }
            }
        }.onFailure {
            logger.fError { "Load popular video list failed: ${it.stackTraceToString()}" }
            FirebaseTelemetry.reportApiError(
                throwable = it,
                endpoint = selectedCategory.slug?.let { slug -> "/v/popular/rank/$slug" }
                    ?: "/x/web-interface/popular"
            )
            withContext(Dispatchers.Main) {
                "加载热门视频失败: ${it.localizedMessage}".toast(BVApp.context)
            }
        }
        loading = false
    }

    fun clearData() {
        popularVideoList.clear()
        resetPage()
        loading = false
    }

    fun resetPage() {
        nextPage = PopularVideoPage()
        refreshing = true
    }

    fun selectCategory(category: PopularRankCategory) {
        if (selectedCategory == category && popularVideoList.isNotEmpty()) return
        selectedCategory = category
        clearData()
    }
}
