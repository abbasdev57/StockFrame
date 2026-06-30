package com.abbas57.stockframe.ui.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abbas57.stockframe.domain.model.InventoryTransaction
import com.abbas57.stockframe.domain.model.Product
import com.abbas57.stockframe.domain.repository.InventoryTransactionRepository
import com.abbas57.stockframe.domain.repository.ProductRepository
import com.abbas57.stockframe.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Holds ONLY the successfully-loaded payload — loading/error states are
 * modeled separately by the UiState<T> wrapper around this, not by a
 * boolean field on this class. This is what lets ProductDetailScreen's
 * `when (state) { is UiState.Loading -> ...; is UiState.Success -> ... }`
 * pattern actually work — `state` must be of type UiState<ProductDetailUiState>
 * for that sealed-class matching to type-check at all.
 */
data class ProductDetailUiState(
    val product: Product,
    val transactions: List<InventoryTransaction> = emptyList()
)

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val transactionRepository: InventoryTransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<ProductDetailUiState>>(UiState.Loading)
    val uiState: StateFlow<UiState<ProductDetailUiState>> = _uiState.asStateFlow()

    // Transactions are tracked separately here so the Flow collector
    // below can update them independently of whether the product fetch
    // has completed yet — see loadProduct() for how the two combine.
    private var latestTransactions: List<InventoryTransaction> = emptyList()
    private var latestProduct: Product? = null

    fun loadProduct(productId: String) {
        viewModelScope.launch {
            productRepository.getProductById(productId)
                .onSuccess { product ->
                    latestProduct = product
                    emitCurrentState()
                }
                .onFailure { e ->
                    _uiState.value = UiState.Error(e.message ?: "Could not load product")
                }
        }

        viewModelScope.launch {
            transactionRepository.observeTransactionsForProduct(productId)
                .catch { e ->
                    _uiState.value = UiState.Error(e.message ?: "Could not load transactions")
                }
                .collect { transactions ->
                    latestTransactions = transactions
                    emitCurrentState()
                }
        }
    }

    /**
     * Only emits UiState.Success once latestProduct is non-null — the
     * product fetch and transaction Flow run concurrently and may
     * resolve in either order; emitting a Success with a null product
     * before the fetch completes would crash the screen's `current.data.product`
     * access (or silently produce a bad screen). Transactions defaulting
     * to an empty list is safe either way — an empty movement history
     * is a legitimate, correctly-rendered state, not an error.
     */
    private fun emitCurrentState() {
        val product = latestProduct ?: return
        _uiState.value = UiState.Success(
            ProductDetailUiState(product = product, transactions = latestTransactions)
        )
    }
}