package com.abbas57.stockframe.domain.model

import com.abbas57.stockframe.domain.model.Category
import com.abbas57.stockframe.domain.model.Product

/**
 * Pulled out of ProductListViewModel.kt into its own file — same reason
 * UiState.kt lives apart from LoginViewModel.kt: these are shapes other
 * files (ProductListScreen.kt) need to import directly, not private
 * implementation detail of the ViewModel that happens to be public.
 */

/** Display-only join of Product + its category name. Lives in the ui
 * layer, not domain/model — see Product.kt's class doc: the domain model
 * deliberately does NOT denormalize a category name onto itself. This is
 * where that join actually happens, recomputed on every emission from
 * either source flow, so renaming a category updates every product row
 * immediately with no extra write anywhere. */
data class ProductListItem(
    val product: Product,
    val categoryName: String?
)

data class ProductListUiData(
    val items: List<ProductListItem>,
    val categories: List<Category>,
    val selectedCategoryId: String?, // null = "All Categories" chip
    val searchQuery: String,
    val canLoadMore: Boolean
)