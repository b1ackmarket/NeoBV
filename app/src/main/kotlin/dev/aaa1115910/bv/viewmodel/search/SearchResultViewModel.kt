package dev.aaa1115910.bv.viewmodel.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dev.aaa1115910.biliapi.repositories.SearchFilterDuration
import dev.aaa1115910.biliapi.repositories.SearchFilterOrderType
import dev.aaa1115910.biliapi.repositories.SearchRepository
import dev.aaa1115910.biliapi.repositories.SearchType
import dev.aaa1115910.biliapi.repositories.SearchTypePage
import dev.aaa1115910.biliapi.repositories.SearchTypeResult
import dev.aaa1115910.bv.util.Partition
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.fException
import dev.aaa1115910.bv.util.fInfo
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class SearchResultViewModel(
    private val searchRepository: SearchRepository
) : ViewModel() {
    companion object {
        private val logger = KotlinLogging.logger { }
    }

    var keyword by mutableStateOf("")
    var searchType by mutableStateOf(SearchType.Video)

    var videoSearchResult by mutableStateOf(SearchResult(SearchType.Video))
    var mediaBangumiSearchResult by mutableStateOf(SearchResult(SearchType.MediaBangumi))
    var mediaFtSearchResult by mutableStateOf(SearchResult(SearchType.MediaFt))
    var biliUserSearchResult by mutableStateOf(SearchResult(SearchType.BiliUser))

    var selectedOrder by mutableStateOf(SearchFilterOrderType.ComprehensiveSort)
    var selectedDuration by mutableStateOf(SearchFilterDuration.All)
    var selectedPartition: Partition? by mutableStateOf(null)
    var selectedChildPartition: Partition? by mutableStateOf(null)

    private val loadingStates = SearchType.entries.associateWith { mutableStateOf(false) }
    private val hasMoreStates = SearchType.entries.associateWith { mutableStateOf(true) }
    private val loadGenerations = SearchType.entries.associateWith { 0 }.toMutableMap()
    private val accumulators = SearchType.entries.associateWith { searchType ->
        when (searchType) {
            SearchType.Video -> SearchResultAccumulator<SearchTypeResult.Video, SearchTypePage>(
                itemKey = { it.aid },
                initialCursor = SearchTypePage()
            )

            SearchType.MediaBangumi,
            SearchType.MediaFt -> SearchResultAccumulator<SearchTypeResult.Pgc, SearchTypePage>(
                itemKey = { it.seasonId },
                initialCursor = SearchTypePage()
            )

            SearchType.BiliUser -> SearchResultAccumulator<SearchTypeResult.User, SearchTypePage>(
                itemKey = { it.mid },
                initialCursor = SearchTypePage()
            )
        }
    }

    var enableProxySearchResult = false

    suspend fun update(searchType: SearchType = this.searchType): Boolean {
        resetPage(searchType)
        clearResult(searchType)
        return loadMore(searchType, true)
    }

    private fun resetPage(searchType: SearchType) {
        loadGenerations[searchType] = loadGenerations.getValue(searchType) + 1
        loadingStates.getValue(searchType).value = false
        hasMoreStates.getValue(searchType).value = true
        when (searchType) {
            SearchType.Video -> {
                accumulators.getValue(searchType).video().clear(SearchTypePage())
                videoSearchResult = videoSearchResult.resetPage()
            }

            SearchType.MediaBangumi -> {
                accumulators.getValue(searchType).pgc().clear(SearchTypePage())
                mediaBangumiSearchResult = mediaBangumiSearchResult.resetPage()
            }

            SearchType.MediaFt -> {
                accumulators.getValue(searchType).pgc().clear(SearchTypePage())
                mediaFtSearchResult = mediaFtSearchResult.resetPage()
            }

            SearchType.BiliUser -> {
                accumulators.getValue(searchType).user().clear(SearchTypePage())
                biliUserSearchResult = biliUserSearchResult.resetPage()
            }
        }
    }

    private fun clearResult(searchType: SearchType) {
        when (searchType) {
            SearchType.Video -> videoSearchResult = videoSearchResult.clear()
            SearchType.MediaBangumi -> mediaBangumiSearchResult = mediaBangumiSearchResult.clear()
            SearchType.MediaFt -> mediaFtSearchResult = mediaFtSearchResult.clear()
            SearchType.BiliUser -> biliUserSearchResult = biliUserSearchResult.clear()
        }
    }

    suspend fun loadMore(
        searchType: SearchType,
        ignoreUpdating: Boolean = false
    ): Boolean {
        val isLoadingState = loadingStates.getValue(searchType)
        val hasMoreState = hasMoreStates.getValue(searchType)
        if (!hasMoreState.value) return true
        if (isLoadingState.value && !ignoreUpdating) return true

        isLoadingState.value = true
        val generation = loadGenerations.getValue(searchType)
        val requestKeyword = keyword
        val requestTid = selectedChildPartition?.tid ?: selectedPartition?.tid
        val requestOrder = selectedOrder
        val requestDuration = selectedDuration
        val requestApiType = Prefs.recommendationApiType.toRequestApiType()
        val requestEnableProxy = enableProxySearchResult
        val page = when (searchType) {
            SearchType.Video -> videoSearchResult.page
            SearchType.MediaBangumi -> mediaBangumiSearchResult.page
            SearchType.MediaFt -> mediaFtSearchResult.page
            SearchType.BiliUser -> biliUserSearchResult.page
        }

        logger.fInfo { "Load search result: [keyword=$requestKeyword, type=$searchType, page=${page}]" }
        return try {
            val searchResultResponse = withContext(Dispatchers.IO) {
                searchRepository.searchType(
                        keyword = requestKeyword,
                        type = searchType,
                        page = page,
                        tid = requestTid,
                        order = requestOrder,
                        duration = requestDuration,
                        preferApiType = requestApiType,
                        enableProxy = requestEnableProxy
                )
            }

            withContext(Dispatchers.Main) {
                if (loadGenerations.getValue(searchType) != generation) return@withContext false
                hasMoreState.value = searchResultResponse.page != page
                when (searchType) {
                    SearchType.Video -> {
                        val accumulator = accumulators.getValue(searchType).video()
                        accumulator.append(searchResultResponse.page, searchResultResponse.videos)
                        videoSearchResult = videoSearchResult.copy(
                            videos = accumulator.items,
                            page = accumulator.cursor
                        )
                    }

                    SearchType.MediaBangumi -> {
                        val accumulator = accumulators.getValue(searchType).pgc()
                        accumulator.append(searchResultResponse.page, searchResultResponse.pgcs)
                        mediaBangumiSearchResult = mediaBangumiSearchResult.copy(
                            mediaBangumis = accumulator.items,
                            page = accumulator.cursor
                        )
                    }

                    SearchType.MediaFt -> {
                        val accumulator = accumulators.getValue(searchType).pgc()
                        accumulator.append(searchResultResponse.page, searchResultResponse.pgcs)
                        mediaFtSearchResult = mediaFtSearchResult.copy(
                            mediaFts = accumulator.items,
                            page = accumulator.cursor
                        )
                    }

                    SearchType.BiliUser -> {
                        val accumulator = accumulators.getValue(searchType).user()
                        accumulator.append(searchResultResponse.page, searchResultResponse.users)
                        biliUserSearchResult = biliUserSearchResult.copy(
                            biliUsers = accumulator.items,
                            page = accumulator.cursor
                        )
                    }
                }
                true
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            logger.fException(e) { "Failed to load search result" }
            false
        } finally {
            if (loadGenerations.getValue(searchType) == generation) {
                withContext(NonCancellable + Dispatchers.Main) {
                    isLoadingState.value = false
                }
            }
        }
    }

    data class SearchResult(
        val type: SearchType,
        val videos: List<SearchTypeResult.Video> = emptyList(),
        val mediaBangumis: List<SearchTypeResult.Pgc> = emptyList(),
        val mediaFts: List<SearchTypeResult.Pgc> = emptyList(),
        val biliUsers: List<SearchTypeResult.User> = emptyList(),
        val page: SearchTypePage = SearchTypePage()
    ) {
        val count get() = videos.size + mediaBangumis.size + mediaFts.size + biliUsers.size

        fun resetPage() = copy(page = SearchTypePage())

        fun clear() :SearchResult = copy(
            videos = emptyList(),
            mediaBangumis = emptyList(),
            mediaFts = emptyList(),
            biliUsers = emptyList(),
            page = SearchTypePage()
        )

    }
}

private fun SearchResultAccumulator<*, *>.video() =
    this as SearchResultAccumulator<SearchTypeResult.Video, SearchTypePage>

private fun SearchResultAccumulator<*, *>.pgc() =
    this as SearchResultAccumulator<SearchTypeResult.Pgc, SearchTypePage>

private fun SearchResultAccumulator<*, *>.user() =
    this as SearchResultAccumulator<SearchTypeResult.User, SearchTypePage>
