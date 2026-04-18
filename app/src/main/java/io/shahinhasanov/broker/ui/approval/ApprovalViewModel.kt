package io.shahinhasanov.broker.ui.approval

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.shahinhasanov.broker.data.model.Declaration
import io.shahinhasanov.broker.data.repository.DeclarationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ApprovalUiState(
    val declaration: Declaration? = null,
    val reason: String = "",
    val note: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null
)

sealed interface ApprovalEvent {
    data object Approved : ApprovalEvent
    data object Rejected : ApprovalEvent
    data class Failed(val message: String) : ApprovalEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ApprovalViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: DeclarationRepository
) : ViewModel() {

    private val declarationId: String = requireNotNull(savedStateHandle["declarationId"])

    private val reason = MutableStateFlow("")
    private val note = MutableStateFlow("")
    private val submitting = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    private val declaration = MutableStateFlow(declarationId)
        .flatMapLatest { repository.observeDeclaration(it) }

    private val _events = Channel<ApprovalEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val uiState: StateFlow<ApprovalUiState> =
        combine(declaration, reason, note, submitting, error) { decl, r, n, isSubmitting, err ->
            ApprovalUiState(
                declaration = decl,
                reason = r,
                note = n,
                isSubmitting = isSubmitting,
                error = err
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ApprovalUiState()
        )

    fun onReasonChanged(value: String) { reason.value = value }
    fun onNoteChanged(value: String) { note.value = value }

    fun approve() {
        if (submitting.value) return
        viewModelScope.launch {
            submitting.value = true
            val outcome = repository.approve(declarationId, note.value.takeIf { it.isNotBlank() })
            submitting.value = false
            outcome.fold(
                onSuccess = { _events.trySend(ApprovalEvent.Approved) },
                onFailure = {
                    val msg = it.message ?: "approval failed"
                    error.value = msg
                    _events.trySend(ApprovalEvent.Failed(msg))
                }
            )
        }
    }

    fun reject() {
        if (submitting.value) return
        val trimmed = reason.value.trim()
        if (trimmed.isEmpty()) {
            error.value = "rejection reason is required"
            return
        }
        viewModelScope.launch {
            submitting.value = true
            val outcome = repository.reject(declarationId, trimmed)
            submitting.value = false
            outcome.fold(
                onSuccess = { _events.trySend(ApprovalEvent.Rejected) },
                onFailure = {
                    val msg = it.message ?: "rejection failed"
                    error.value = msg
                    _events.trySend(ApprovalEvent.Failed(msg))
                }
            )
        }
    }
}
