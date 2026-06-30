package com.abbas57.stockframe.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abbas57.stockframe.domain.model.InventoryTransaction
import com.abbas57.stockframe.domain.model.TransactionType
import com.abbas57.stockframe.domain.repository.InventoryTransactionRepository
import com.abbas57.stockframe.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * null = "All" filter chip selected — no type filter applied,
 * all transaction types come through the Firestore query.
 * Non-null = specific type, pushed server-side as a whereEqualTo clause.
 */
data class HistoryUiState(
    val transactions: List<InventoryTransaction> = emptyList(),
    val selectedFilter: TransactionType? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val transactionRepository: InventoryTransactionRepository
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow<TransactionType?>(null)
    val selectedFilter: StateFlow<TransactionType?> = _selectedFilter

    /**
     * flatMapLatest is the right operator here — when the filter changes,
     * it cancels the previous Firestore listener and opens a new one with
     * the updated query rather than filtering client-side from a fixed
     * "all transactions" Flow. This matters at real usage scale: if a
     * client has 10,000 transactions and selects "Stock Out", you want
     * Firestore to apply that filter before sending data, not download
     * all 10,000 and discard 7,000 client-side.
     *
     * stateIn with WhileSubscribed(5000) keeps the listener alive for
     * 5 seconds after the last collector goes away (e.g. brief config
     * change) so rotation doesn't tear down and rebuild the Firestore
     * connection unnecessarily.
     */
    val uiState: StateFlow<UiState<HistoryUiState>> = _selectedFilter
        .flatMapLatest { filter ->
            transactionRepository.observeTransactions(typeFilter = filter)
                .map<List<InventoryTransaction>, UiState<HistoryUiState>> { transactions ->
                    UiState.Success(
                        HistoryUiState(
                            transactions = transactions,
                            selectedFilter = filter
                        )
                    )
                }
        }
        .catch { e ->
            emit(UiState.Error(e.message ?: "An unknown error occurred"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UiState.Loading
        )

    fun onFilterSelected(type: TransactionType?) {
        _selectedFilter.value = type
    }
}