package io.github.smallora.sharereborn.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.smallora.sharereborn.data.WeiboApi
import io.github.smallora.sharereborn.data.model.WeiboStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val items: List<WeiboStatus> = emptyList(),
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val hasMore: Boolean = true,
    val loadedOnce: Boolean = false,
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val api = WeiboApi(app)
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private var maxId: String? = null

    fun loadInitial() {
        if (_state.value.loading) return
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { api.hotTimeline() }
                .onSuccess { page ->
                    maxId = page.maxId
                    _state.value = HomeUiState(
                        items = page.items,
                        loading = false,
                        hasMore = page.maxId != null,
                        loadedOnce = true,
                    )
                }
                .onFailure { t ->
                    _state.value = _state.value.copy(
                        loading = false,
                        error = t.message ?: "加载失败",
                        loadedOnce = true,
                    )
                }
        }
    }

    fun refresh() {
        if (_state.value.refreshing) return
        _state.value = _state.value.copy(refreshing = true, error = null)
        viewModelScope.launch {
            runCatching { api.hotTimeline() }
                .onSuccess { page ->
                    maxId = page.maxId
                    _state.value = _state.value.copy(
                        items = page.items,
                        refreshing = false,
                        hasMore = page.maxId != null,
                    )
                }
                .onFailure { t ->
                    _state.value = _state.value.copy(
                        refreshing = false,
                        error = t.message ?: "刷新失败",
                    )
                }
        }
    }

    fun loadMore() {
        val s = _state.value
        if (s.loadingMore || s.loading || !s.hasMore) return
        val mid = maxId ?: return
        _state.value = s.copy(loadingMore = true)
        viewModelScope.launch {
            runCatching { api.hotTimeline(maxId = mid) }
                .onSuccess { page ->
                    maxId = page.maxId
                    _state.value = _state.value.copy(
                        items = _state.value.items + page.items,
                        loadingMore = false,
                        hasMore = page.maxId != null,
                    )
                }
                .onFailure { t ->
                    _state.value = _state.value.copy(
                        loadingMore = false,
                        error = t.message ?: "加载更多失败",
                    )
                }
        }
    }

    fun dismissError() {
        _state.value = _state.value.copy(error = null)
    }
}
