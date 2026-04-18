package io.shahinhasanov.broker.ui.declarations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.shahinhasanov.broker.data.model.Declaration
import io.shahinhasanov.broker.data.model.DeclarationStatus
import io.shahinhasanov.broker.data.repository.DeclarationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeclarationsListUiState(
    val filter: DeclarationStatus? = null,
    val declarations: List<Declaration> = emptyList(),
    val isRefreshing: Boolean = false,
    val error: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DeclarationsListViewModel @Inject constructor(
    private val repository: DeclarationRepository
) : ViewModel() {

    private val filter = MutableStateFlow<DeclarationStatus?>(null)
    private val refreshing = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    private val declarations = filter
        .flatMapLatest { repository.observeDeclarations(it) }
        .catch { emit(emptyList()) }

    val uiState: StateFlow<DeclarationsListUiState> =
        combine(filter, declarations, refreshing, error) { f, list, isRefreshing, err ->
            DeclarationsListUiState(
                filter = f,
                declarations = list,
                isRefreshing = isRefreshing,
                error = err
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DeclarationsListUiState()
        )

    private val _events = MutableStateFlow<ListEvent?>(null)
    val events: StateFlow<ListEvent?> = _events.asStateFlow()

    init {
        refresh()
    }

    fun setFilter(status: DeclarationStatus?) {
        filter.value = status
    }

    fun refresh() {
        if (refreshing.value) return
        viewModelScope.launch {
            refreshing.value = true
            error.value = null
            val outcome = repository.refresh()
            refreshing.value = false
            outcome.onFailure { throwable ->
                error.value = throwable.message ?: "refresh failed"
            }
        }
    }

    fun clearError() {
        error.value = null
    }

    fun consumeEvent() {
        _events.value = null
    }

    sealed interface ListEvent {
        data class Navigate(val id: String) : ListEvent
    }
}
