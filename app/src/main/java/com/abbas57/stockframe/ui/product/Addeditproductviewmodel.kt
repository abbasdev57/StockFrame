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

data class AddEditProductFormState(
    val isEditMode: Boolean = false,
    val isLoadingExisting: Boolean = false,
    val name: String = "",
    val sku: String = "",
    val description: String = "",
    val categoryId: String? = null,
    val categories: List<Category> = emptyList(),
    val unitPriceText: String = "",
    val quantityText: String = "",
    val minimumStockText: String = "",
    val localImageUri: String? = null,
    val existingImageUrl: String? = null,
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

    private var existingProduct: Product? = null

    init {
        viewModelScope.launch {
            categoryRepository.observeCategories().collect { categories ->
                _formState.value = _formState.value.copy(categories = categories)
            }
        }
    }

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

    fun addCategory(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            categoryRepository.addCategory(name)
                .onSuccess { category -> _formState.value = _formState.value.copy(categoryId = category.id) }
                .onFailure { e -> _formState.value = _formState.value.copy(errorMessage = e.message ?: "Could not add category") }
        }
    }

    fun save() {
        val form = _formState.value

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
                val updated = current.copy(
                    name = form.name,
                    sku = form.sku,
                    description = form.description.ifBlank { null },
                    categoryId = form.categoryId,
                    unitPrice = unitPrice,
                    quantity = quantity,
                    minimumStock = minimumStock
                )
                // current.quantity is the BEFORE value, captured at the
                // moment Edit was opened — this is what lets the repository
                // compute the delta and decide whether an inventory_transactions
                // entry is needed at all.
                productRepository.updateProduct(updated, previousQuantity = current.quantity, form.localImageUri)
                    .onSuccess { _saveState.value = UiState.Success(updated) }
                    .onFailure { e -> _saveState.value = UiState.Error(e.message ?: "Could not update product") }
            } else {
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