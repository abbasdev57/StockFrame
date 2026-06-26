package com.abbas57.stockframe.ui.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abbas57.stockframe.domain.model.ProductListItem
import com.abbas57.stockframe.domain.model.ProductListUiData
import com.abbas57.stockframe.domain.repository.CategoryRepository
import com.abbas57.stockframe.domain.repository.ProductRepository
import com.abbas57.stockframe.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

// ProductListItem and ProductListUiData live in ProductListModels.kt —
// see that file for why they were moved out of here.

private const val PAGE_SIZE = 5

@HiltViewModel
class ProductListViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    private val _visibleCount = MutableStateFlow(PAGE_SIZE)

    /**
     * Defaults to the live listener (productRepository.observeProducts()).
     * This is the half of the Settings toggle decision that's actually
     * buildable right now — there's no Settings screen yet to read a
     * preference from, so manual-fetch mode isn't wired in this sprint.
     * The repository already supports both; swapping this single line
     * for a preference-driven branch is the entire cost of finishing that
     * feature later, which is the point of having decided the contract
     * shape back in Task 1.
     */
    private val productsFlow = productRepository.observeProducts()
    private val categoriesFlow = categoryRepository.observeCategories()

    val uiState: StateFlow<UiState<ProductListUiData>> = combine(
        productsFlow,
        categoriesFlow,
        _searchQuery,
        _selectedCategoryId,
        _visibleCount
    ) { products, categories, query, selectedCategoryId, visibleCount ->
        val categoryNameById = categories.associateBy({ it.id }, { it.name })

        val filtered = products
            .filter { selectedCategoryId == null || it.categoryId == selectedCategoryId }
            .filter {
                query.isBlank() ||
                        it.name.contains(query, ignoreCase = true) ||
                        it.sku.contains(query, ignoreCase = true)
            }

        val page = filtered.take(visibleCount)

        // Explicit UiState<ProductListUiData> type here (rather than letting
        // it infer as UiState.Success<ProductListUiData>) is what lets the
        // stateIn below accept UiState.Loading as a valid initialValue — a
        // plain upcast, not a runtime safety concern.
        val result: UiState<ProductListUiData> = UiState.Success(
            ProductListUiData(
                items = page.map { ProductListItem(it, categoryNameById[it.categoryId]) },
                categories = categories,
                selectedCategoryId = selectedCategoryId,
                searchQuery = query,
                canLoadMore = filtered.size > page.size
            )
        )
        result
    }.stateIn(
        scope = viewModelScope,
        // Lazily started, kept alive 5s after the last collector goes away
        // (e.g. brief configuration change) so the Firestore listener isn't
        // torn down and immediately recreated on every rotation.
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UiState.Loading
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        _visibleCount.value = PAGE_SIZE // reset pagination on a new search, same as changing the filter
    }

    fun onCategorySelected(categoryId: String?) {
        _selectedCategoryId.value = categoryId
        _visibleCount.value = PAGE_SIZE
    }

    fun onLoadMore() {
        _visibleCount.value += PAGE_SIZE
    }
}