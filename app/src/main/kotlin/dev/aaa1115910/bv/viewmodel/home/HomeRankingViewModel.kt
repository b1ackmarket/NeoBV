package dev.aaa1115910.bv.viewmodel.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dev.aaa1115910.biliapi.entity.ugc.UgcItem
import dev.aaa1115910.biliapi.http.entity.RiskControlException
import dev.aaa1115910.biliapi.repositories.HomeRankingPeriod
import dev.aaa1115910.biliapi.repositories.HomeRankingResult
import dev.aaa1115910.biliapi.repositories.RecommendVideoRepository
import dev.aaa1115910.bv.telemetry.FirebaseTelemetry
import dev.aaa1115910.bv.util.addAllWithMainContext
import dev.aaa1115910.bv.util.fWarn
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.android.annotation.KoinViewModel

enum class HomeRankingType(
    val displayName: String,
    val hasPeriods: Boolean,
    val listId: Int? = null,
    val listType: Int? = null
) {
    Weekly("每周必刷", true),
    MusicHot("全站音乐热歌榜", true, listId = 401, listType = 1),
    MusicOriginal("全站音乐二创榜", true, listId = 400, listType = 3),
    Precious("入站必刷", false)
}

@KoinViewModel
class HomeRankingViewModel(
    private val recommendVideoRepository: RecommendVideoRepository
) : ViewModel() {
    private val logger = KotlinLogging.logger {}

    val items = mutableStateListOf<UgcItem>()
    var selectedType by mutableStateOf(HomeRankingType.Weekly)
        private set
    var periods by mutableStateOf<List<HomeRankingPeriod>>(emptyList())
        private set
    var selectedPeriodId by mutableStateOf<Int?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var loaded by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    suspend fun refresh() {
        load(type = selectedType, periodId = selectedPeriodId)
    }

    suspend fun selectType(type: HomeRankingType) {
        load(type = type, periodId = null)
    }

    suspend fun selectPeriod(period: HomeRankingPeriod) {
        load(type = selectedType, periodId = period.id)
    }

    private suspend fun load(type: HomeRankingType, periodId: Int?) {
        if (loading) return
        loading = true
        errorMessage = null
        selectedType = type

        runCatching {
            when (type) {
                HomeRankingType.Weekly -> recommendVideoRepository.getWeeklyRanking(periodId)
                HomeRankingType.MusicHot -> recommendVideoRepository.getMusicRanking(
                    listId = type.listId!!,
                    listType = type.listType!!,
                    periodId = periodId
                )

                HomeRankingType.MusicOriginal -> recommendVideoRepository.getMusicRanking(
                    listId = type.listId!!,
                    listType = type.listType!!,
                    periodId = periodId
                )

                HomeRankingType.Precious -> recommendVideoRepository.getPreciousRanking()
            }
        }.onSuccess { result ->
            applyResult(result)
        }.onFailure { error ->
            logger.fWarn { "Load home ranking failed: ${error.stackTraceToString()}" }
            FirebaseTelemetry.reportApiError(
                throwable = error,
                endpoint = "home_ranking/${type.name}"
            )
            errorMessage = when (error) {
                is RiskControlException -> "加载失败：触发风控，请稍后再试"
                else -> "加载失败：${error.localizedMessage ?: error.message ?: "未知错误"}"
            }
        }

        loaded = true
        loading = false
    }

    private suspend fun applyResult(result: HomeRankingResult) {
        items.clear()
        items.addAllWithMainContext(result.items)
        periods = result.periods
        selectedPeriodId = result.selectedPeriodId
    }
}
