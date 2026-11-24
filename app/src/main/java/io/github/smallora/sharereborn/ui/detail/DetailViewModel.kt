package io.github.smallora.sharereborn.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.smallora.sharereborn.data.WeiboApi
import io.github.smallora.sharereborn.data.model.WeiboComment
import io.github.smallora.sharereborn.data.model.WeiboStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DetailUiState(
    val status: WeiboStatus? = null,
    val comments: List<WeiboComment> = emptyList(),
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val hasMore: Boolean = true,
)

class DetailViewModel(app: Application) : AndroidViewModel(app) {

    private val api = WeiboApi(app)
    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    private var mblogid: String = ""
    private var commentMaxId: String? = null

    fun load(mblogid: String, seed: WeiboStatus? = null) {
        if (this.mblogid == mblogid && _state.value.status != null) return
        this.mblogid = mblogid
        commentMaxId = null
        _state.value = DetailUiState(status = seed, loading = true)
        viewModelScope.launch {
            val detail = runCatching { api.statusDetail(mblogid) }
            val comments = runCatching { api.comments(seed?.id ?: detail.getOrNull()?.id.orEmpty()) }
            _state.value = _state.value.copy(
                status = detail.getOrNull() ?: seed,
                comments = comments.getOrNull()?.items ?: emptyList(),
                loading = false,
                hasMore = comments.getOrNull()?.maxId != null,
                error = when {
                    detail.isFailure -> detail.exceptionOrNull()?.message ?: "详情加载失败"
                    comments.isFailure -> comments.exceptionOrNull()?.message ?: "评论加载失败"
                    else -> null
                },
            )
            commentMaxId = comments.getOrNull()?.maxId
        }
    }

    fun loadMoreComments() {
        val s = _state.value
        val mid = commentMaxId
        if (s.loadingMore || mid.isNullOrEmpty() || !s.hasMore) return
        val id = s.status?.id.orEmpty()
        if (id.isEmpty()) return
        _state.value = s.copy(loadingMore = true)
        viewModelScope.launch {
            runCatching { api.comments(id, maxId = mid) }
                .onSuccess { page ->
                    commentMaxId = page.maxId
                    _state.value = _state.value.copy(
                        comments = _state.value.comments + page.items,
                        loadingMore = false,
                        hasMore = page.maxId != null,
                    )
                }
                .onFailure {
                    _state.value = _state.value.copy(loadingMore = false)
                }
        }
    }

    fun retry() {
        val id = mblogid
        if (id.isNotEmpty()) {
            mblogid = ""
            load(id, _state.value.status)
        }
    }
}
