package com.abbas57.stockframe.ui.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abbas57.stockframe.domain.model.Product
import com.abbas57.stockframe.domain.model.TransactionType
import com.abbas57.stockframe.domain.repository.InventoryTransactionRepository
import com.abbas57.stockframe.domain.repository.ProductRepository
import com.abbas57.stockframe.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Form state for the Stock Adjustment screen — same two-state pattern
 * as Add/Edit Product: formState holds what the user is actively typing,
 * saveState models the submit action's Loading/Success/Error lifecycle.
 */
data class StockAdjustmentFormState(
    val product: Product? = null,         // loaded once on screen open
    val isLoadingProduct: Boolean = true,
    val type: TransactionType = TransactionType.IN,  // defaults to Stock In
    val quantityText: String = "",
    val reason: String = "",
    val note: String = "",
    val availableReasons: List<String> = emptyList()
)

@HiltViewModel
class StockAdjustmentViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val transactionRepository: InventoryTransactionRepository
) : ViewModel() {

    private val _formState = MutableStateFlow(StockAdjustmentFormState())
    val formState: StateFlow<StockAdjustmentFormState> = _formState.asStateFlow()

    private val _saveState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val saveState: StateFlow<UiState<Unit>> = _saveState.asStateFlow()

    /**
     * Loads the product once when the screen opens, then updates the
     * available reasons based on the default type (IN). Reasons are
     * type-dependent — "Restock" makes no sense for a Stock Out, and
     * "Damaged" makes no sense for a Stock In — so they update whenever
     * the type toggle changes too.
     */
    fun loadProduct(productId: String) {
        viewModelScope.launch {
            productRepository.getProductById(productId)
                .onSuccess { product ->
                    _formState.value = _formState.value.copy(
                        product = product,
                        isLoadingProduct = false,
                        availableReasons = reasonsFor(TransactionType.IN)
                    )
                }
                .onFailure {
                    _formState.value = _formState.value.copy(isLoadingProduct = false)
                }
        }
    }

    fun onTypeChanged(type: TransactionType) {
        _formState.value = _formState.value.copy(
            type = type,
            reason = "",  // clear reason on type switch — avoids "Damaged" staying selected when switching to IN
            availableReasons = reasonsFor(type)
        )
    }

    fun onQuantityChanged(value: String) {
        _formState.value = _formState.value.copy(quantityText = value)
    }

    fun onReasonChanged(reason: String) {
        _formState.value = _formState.value.copy(reason = reason)
    }

    fun onNoteChanged(note: String) {
        _formState.value = _formState.value.copy(note = note)
    }

    fun confirmAdjustment() {
        val form = _formState.value
        val product = form.product ?: return

        val quantity = form.quantityText.toIntOrNull()
        if (quantity == null || quantity <= 0) {
            _saveState.value = UiState.Error("Enter a valid quantity greater than zero")
            return
        }
        if (form.reason.isBlank()) {
            _saveState.value = UiState.Error("Please select a reason")
            return
        }

        // Client-side stock-out guard — the repository's runTransaction
        // enforces this server-side too, but catching it here gives an
        // immediate, friendly error message rather than a Firebase
        // exception that has to be unwrapped and re-displayed.
        if (form.type == TransactionType.OUT && product.quantity - quantity < 0) {
            _saveState.value = UiState.Error(
                "Cannot remove $quantity units — only ${product.quantity} in stock"
            )
            return
        }

        viewModelScope.launch {
            _saveState.value = UiState.Loading
            transactionRepository.recordAdjustment(
                productId = product.id,
                productName = product.name,
                type = form.type,
                quantity = quantity,
                reason = form.reason,
                note = form.note.ifBlank { null }
            )
                .onSuccess { _saveState.value = UiState.Success(Unit) }
                .onFailure { e -> _saveState.value = UiState.Error(e.message ?: "Adjustment failed") }
        }
    }

    /**
     * Reason lists are type-specific so the dropdown always shows
     * contextually appropriate options. These are V1 starter lists —
     * a Phase 2 "custom reasons" feature would let owners add their own.
     */
    private fun reasonsFor(type: TransactionType): List<String> = when (type) {
        TransactionType.IN -> listOf("Restock", "Initial stock", "Return from customer", "Found in warehouse", "Other")
        TransactionType.OUT -> listOf("Sale", "Damaged", "Expired", "Lost", "Returned to supplier", "Other")
        TransactionType.ADJUSTMENT -> listOf("Manual correction", "Stock count correction", "Other")
    }
}