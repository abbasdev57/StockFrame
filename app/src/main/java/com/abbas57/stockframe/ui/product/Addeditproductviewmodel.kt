package com.abbas57.stockframe.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abbas57.stockframe.domain.model.Category
import com.abbas57.stockframe.domain.model.Product
import com.abbas57.stockframe.domain.repository.CategoryRepository
import com.abbas57.stockframe.domain.repository.ProductRepository
import com.abbas57.stockframe.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Local, unsubmitted form state — deliberately NOT wrapped in UiState<T>.
 * UiState's Loading/Success/Error/Idle shape models a request/response
 * action (login, save); the form itself is just mutable input the user is
 * actively typing into, with no "loading" or "error" concept of its own.
 * saveState below is the UiState<T> that models the actual save action.
 */
data class AddEditProductFormState(
    val isEditMode: Boolean = false,
    val isLoadingExisting: Boolean = false, // true only while fetching the existing product in edit mode
    val name: String = "",
    val sku: String = "",
    val description: String = "",
    val categoryId: String? = null,
    val categories: List<Category> = emptyList(),
    val unitPriceText: String = "",
    val quantityText: String = "",
    val minimumStockText: String = "",
    val localImageUri: String? = null, // newly picked image, not yet uploaded
    val existingImageUrl: String? = null, // already-uploaded image, edit mode only
    val errorMessage: String? = null
)

@HiltViewModel
class AddEditProductViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _formState = MutableStateFlow(AddEditProductFormState())
    val formState: StateFlow<AddEditProductFormState> = _formState.asStateFlow()

    private val _saveState = MutableStateFlow<UiState<Product>>(UiState.Idle)
    val saveState: StateFlow<UiState<Product>> = _saveState.asStateFlow()

    private var existingProduct: Product? = null // null in add mode, the original record in edit mode

    init {
        // Category dropdown stays live for the lifetime of this screen —
        // adding a category inline and immediately seeing it selectable
        // depends on this, rather than a one-shot fetch taken at screen open.
        viewModelScope.launch {
            categoryRepository.observeCategories().collect { categories ->
                _formState.value = _formState.value.copy(categories = categories)
            }
        }
    }

    /** Called once, from the screen's entry point, only when opening in edit mode. Add mode never calls this. */
    fun loadProductForEdit(productId: String) {
        _formState.value = _formState.value.copy(isEditMode = true, isLoadingExisting = true)
        viewModelScope.launch {
            productRepository.getProductById(productId)
                .onSuccess { product ->
                    existingProduct = product
                    _formState.value = _formState.value.copy(
                        isLoadingExisting = false,
                        name = product.name,
                        sku = product.sku,
                        description = product.description ?: "",
                        categoryId = product.categoryId,
                        unitPriceText = product.unitPrice.toString(),
                        quantityText = product.quantity.toString(),
                        minimumStockText = product.minimumStock.toString(),
                        existingImageUrl = product.imageUrl
                    )
                }
                .onFailure { e ->
                    _formState.value = _formState.value.copy(
                        isLoadingExisting = false,
                        errorMessage = e.message ?: "Could not load product"
                    )
                }
        }
    }

    fun onNameChanged(value: String) { _formState.value = _formState.value.copy(name = value) }
    fun onSkuChanged(value: String) { _formState.value = _formState.value.copy(sku = value) }
    fun onDescriptionChanged(value: String) { _formState.value = _formState.value.copy(description = value) }
    fun onCategorySelected(categoryId: String) { _formState.value = _formState.value.copy(categoryId = categoryId) }
    fun onUnitPriceChanged(value: String) { _formState.value = _formState.value.copy(unitPriceText = value) }
    fun onQuantityChanged(value: String) { _formState.value = _formState.value.copy(quantityText = value) }
    fun onMinimumStockChanged(value: String) { _formState.value = _formState.value.copy(minimumStockText = value) }
    fun onImagePicked(localUri: String) { _formState.value = _formState.value.copy(localImageUri = localUri) }

    /**
     * Inline category creation. On success, immediately selects the new
     * category — the whole point of "inline" is the user never leaves
     * this form to do it. The new category also arrives via the
     * observeCategories() collector above and lands in the dropdown list
     * at the same time, so selecting it by ID here is safe even though
     * the list update technically comes from a separate emission.
     */
    fun addCategory(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            categoryRepository.addCategory(name)
                .onSuccess { category ->
                    _formState.value = _formState.value.copy(categoryId = category.id)
                }
                .onFailure { e ->
                    _formState.value = _formState.value.copy(errorMessage = e.message ?: "Could not add category")
                }
        }
    }

    fun save() {
        val form = _formState.value

        // Same validation-order principle as RegisterViewModel: cheapest /
        // most obvious checks first (blank required fields) before parsing
        // numeric fields, so the user sees "name is required" rather than
        // a confusing numeric parse error when they haven't even gotten
        // that far yet.
        if (form.name.isBlank() || form.sku.isBlank()) {
            _saveState.value = UiState.Error("Product name and SKU are required")
            return
        }
        if (form.categoryId == null) {
            _saveState.value = UiState.Error("Please select or add a category")
            return
        }

        val unitPrice = form.unitPriceText.toDoubleOrNull()
        val quantity = form.quantityText.toIntOrNull()
        val minimumStock = form.minimumStockText.toIntOrNull()

        if (unitPrice == null || unitPrice < 0) {
            _saveState.value = UiState.Error("Enter a valid unit price")
            return
        }
        if (quantity == null || quantity < 0) {
            _saveState.value = UiState.Error("Enter a valid quantity")
            return
        }
        if (minimumStock == null || minimumStock < 0) {
            _saveState.value = UiState.Error("Enter a valid minimum stock threshold")
            return
        }

        viewModelScope.launch {
            _saveState.value = UiState.Loading

            val current = existingProduct
            if (form.isEditMode && current != null) {
                // copy() over the original record — preserves id/ownerId/
                // createdAt/isActive exactly as they were; only the fields
                // this form actually edits change. quantity IS editable
                // here despite the Product.kt class doc's warning, because
                // that warning is about Stock Adjustment never being
                // bypassed for ROUTINE stock changes — initial product
                // setup (including correcting a typo'd starting quantity
                // right after creation) is a deliberately different case.
                val updated = current.copy(
                    name = form.name,
                    sku = form.sku,
                    description = form.description.ifBlank { null },
                    categoryId = form.categoryId,
                    unitPrice = unitPrice,
                    quantity = quantity,
                    minimumStock = minimumStock
                )
                productRepository.updateProduct(updated, form.localImageUri)
                    .onSuccess { _saveState.value = UiState.Success(updated) }
                    .onFailure { e -> _saveState.value = UiState.Error(e.message ?: "Could not update product") }
            } else {
                // id/ownerId/createdAt/updatedAt are placeholder values here
                // — ProductRepositoryImpl.addProduct overwrites all four
                // with the real generated ID and current owner/timestamps
                // before writing. See Task 1's doc comment on why that
                // origin lives in the data layer, not here.
                val newProduct = Product(
                    id = "",
                    ownerId = "",
                    name = form.name,
                    sku = form.sku,
                    description = form.description.ifBlank { null },
                    categoryId = form.categoryId,
                    unitPrice = unitPrice,
                    quantity = quantity,
                    minimumStock = minimumStock,
                    createdAt = 0L,
                    updatedAt = 0L
                )
                productRepository.addProduct(newProduct, form.localImageUri)
                    .onSuccess { saved -> _saveState.value = UiState.Success(saved) }
                    .onFailure { e -> _saveState.value = UiState.Error(e.message ?: "Could not save product") }
            }
        }
    }
}