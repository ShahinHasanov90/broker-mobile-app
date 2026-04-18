package io.shahinhasanov.broker.ui.declarations

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.shahinhasanov.broker.data.model.Declaration
import io.shahinhasanov.broker.data.repository.DeclarationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeclarationDetailUiState(
    val declaration: Declaration? = null,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DeclarationDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: DeclarationRepository
) : ViewModel() {

    private val declarationId: String = requireNotNull(savedStateHandle["declarationId"]) {
        "declarationId route argument missing"
    }

    private val refreshing = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    private val declaration = MutableStateFlow(declarationId)
        .flatMapLatest { repository.observeDeclaration(it) }

    val uiState: StateFlow<DeclarationDetailUiState> =
        combine(declaration, refreshing, error) { model, isRefreshing, err ->
            DeclarationDetailUiState(
                declaration = model,
                isRefreshing = isRefreshing,
                error = err
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DeclarationDetailUiState()
        )

    init {
        refresh()
    }

    fun refresh() {
        if (refreshing.value) return
        viewModelScope.launch {
            refreshing.value = true
            val outcome = repository.refreshOne(declarationId)
            refreshing.value = false
            outcome.onFailure { error.value = it.message }
        }
    }
}
