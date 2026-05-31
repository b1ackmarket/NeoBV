package dev.aaa1115910.bv.viewmodel.pgc

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aaa1115910.biliapi.entity.CarouselData
import dev.aaa1115910.biliapi.entity.pgc.PgcCinemaTabData
import dev.aaa1115910.biliapi.repositories.PgcRepository
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.util.addAllWithMainContext
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.toast
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class PgcCinemaViewModel(
    private val pgcRepository: PgcRepository
) : ViewModel() {
    private val logger = KotlinLogging.logger("PgcCinemaViewModel")

    val carouselItems = mutableStateListOf<CarouselData.CarouselItem>()
    val rows = mutableStateListOf<PgcCinemaTabData.Row>()
    var updating by mutableStateOf(false)
        private set
    var hasNext by mutableStateOf(true)
        private set

    private var nextCursor: String? = null

    init {
        loadMore()
    }

    fun loadMore() {
        if (!hasNext || updating) return
        viewModelScope.launch(Dispatchers.IO) {
            updateCinemaTab()
        }
    }

    fun reloadAll() {
        logger.fInfo { "Reload all PGC cinema data" }
        clearAll()
        loadMore()
    }

    private fun clearAll() {
        carouselItems.clear()
        rows.clear()
        nextCursor = null
        hasNext = true
    }

    private suspend fun updateCinemaTab() {
        withContext(Dispatchers.Main) { updating = true }
        runCatching {
            pgcRepository.getCinemaTab(cursor = nextCursor)
        }.onSuccess { data ->
            nextCursor = data.nextCursor.takeIf { it.isNotBlank() }
            withContext(Dispatchers.Main) { hasNext = data.hasNext && nextCursor != null }
            if (carouselItems.isEmpty()) {
                carouselItems.addAllWithMainContext(data.carouselItems)
            }
            rows.addAllWithMainContext(data.rows)
        }.onFailure { error ->
            logger.fInfo { "Update PGC cinema failed: ${error.stackTraceToString()}" }
            withContext(Dispatchers.Main) {
                "加载影视数据失败: ${error.message}".toast(BVApp.context)
            }
        }
        withContext(Dispatchers.Main) { updating = false }
    }
}
