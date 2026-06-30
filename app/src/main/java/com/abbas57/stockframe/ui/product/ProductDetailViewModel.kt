package com.abbas57.stockframe.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abbas57.stockframe.domain.model.Product
import com.abbas57.stockframe.domain.repository.ProductRepository
import com.abbas57.stockframe.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<Product>>(UiState.Idle)
    val uiState: StateFlow<UiState<Product>> = _uiState.asStateFlow()

    /**
     * A one-shot getProductById call, not observeProducts() filtered down
     * to one item. Watching the entire live products collection just to
     * render a single detail screen would mean re-subscribing every other
     * user's-worth of product changes (filtered client-side) for a screen
     * that only ever needs one document. If live updates here become a
     * real need later, the right fix is a single-document Firestore
     * listener, not reusing the list-level Flow.
     */
    fun loadProduct(productId: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            productRepository.getProductById(productId)
                .onSuccess { product -> _uiState.value = UiState.Success(product) }
                .onFailure { e -> _uiState.value = UiState.Error(e.message ?: "Could not load product") }
        }
    }
}