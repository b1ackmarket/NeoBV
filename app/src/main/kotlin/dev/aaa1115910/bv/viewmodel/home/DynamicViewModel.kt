package dev.aaa1115910.bv.viewmodel.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aaa1115910.biliapi.entity.user.DynamicVideo
import dev.aaa1115910.biliapi.http.entity.AuthFailureException
import dev.aaa1115910.biliapi.repositories.UserRepository
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.BuildConfig
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.addAllWithMainContext
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.fWarn
import dev.aaa1115910.bv.util.toast
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import dev.aaa1115910.bv.repository.UserRepository as BvUserRepository
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class DynamicViewModel(
    private val bvUserRepository: BvUserRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    companion object {
        const val ALL_UP_AUTHORS_FILTER = "全部UP主"
    }

    private val logger = KotlinLogging.logger {}
    val dynamicList = mutableStateListOf<DynamicVideo>()

    private var currentPage = 0
    var loading by mutableStateOf(false)
        private set

    var hasMore by mutableStateOf(true)
        private set

    private var historyOffset: String? = null
    private var updateBaseline: String? = null
    val isLogin get() = bvUserRepository.isLogin
    var selectedAuthor by mutableStateOf<String?>(null)
        private set

    val authorFilters: List<String>
        get() = buildList {
            add(ALL_UP_AUTHORS_FILTER)
            val authors = LinkedHashSet<String>()
            dynamicList.forEach { video ->
                if (video.author.isNotBlank()) {
                    authors += video.author
                }
            }
            addAll(authors)
        }

    val filteredDynamicList: List<DynamicVideo>
        get() = selectedAuthor?.let { author ->
            dynamicList.filter { it.author == author }
        } ?: dynamicList

    init {
        if (isLogin) {
            ensureLoaded()
        }
    }

    suspend fun loadMore() {
        if (!loading) loadData()
    }

    fun ensureLoaded() {
        if (!isLogin || dynamicList.isNotEmpty() || loading) return
        viewModelScope.launch(Dispatchers.IO) {
            loadData()
        }
    }

    fun refresh() {
        if (!isLogin) return
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                clearData()
            }
            loadData()
        }
    }

    fun onLoginStateChanged(isLogin: Boolean) {
        if (!isLogin) {
            clearData()
            return
        }
        ensureLoaded()
    }

    fun selectAuthor(author: String?) {
        selectedAuthor = author
    }

    private suspend fun loadData() {
        if (!hasMore || !bvUserRepository.isLogin) return
        if (loading) return

        withContext(Dispatchers.Main) {
            loading = true
        }
        val nextPage = currentPage + 1

        try {
            logger.fInfo { "Load dynamic page: $nextPage, offset=$historyOffset" }

            val data = userRepository.getDynamicVideos(
                page = nextPage,
                offset = historyOffset.orEmpty(),
                updateBaseline = updateBaseline.orEmpty(),
                preferApiType = Prefs.apiType
            )

            currentPage = nextPage
            dynamicList.addAllWithMainContext(data.videos)

            historyOffset = data.historyOffset
            updateBaseline = data.updateBaseline
            hasMore = data.hasMore
            if (selectedAuthor != null && dynamicList.none { it.author == selectedAuthor }) {
                selectedAuthor = null
            }

            logger.fInfo { "Loaded page=$currentPage size=${data.videos.size}" }

        } catch (e: Exception) {
            logger.fWarn { "Load dynamic failed: ${e.stackTraceToString()}" }

            when (e) {
                is AuthFailureException -> {
                    withContext(Dispatchers.Main) {
                        BVApp.context.getString(R.string.exception_auth_failure)
                            .toast(BVApp.context)
                    }
                    if (!BuildConfig.DEBUG) bvUserRepository.logout()
                }

                else -> {
                    withContext(Dispatchers.Main) {
                        "加载动态失败: ${e.localizedMessage}".toast(BVApp.context)
                    }
                }
            }

        } finally {
            withContext(Dispatchers.Main) {
                loading = false
            }
        }
    }

    fun clearData() {
        dynamicList.clear()
        currentPage = 0
        loading = false
        hasMore = true
        historyOffset = null
        updateBaseline = null
        selectedAuthor = null
    }
}
