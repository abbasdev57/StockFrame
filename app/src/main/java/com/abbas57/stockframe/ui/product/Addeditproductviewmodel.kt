package com.abbas57.stockframe.ui.products

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abbas57.stockframe.domain.model.Category
import com.abbas57.stockframe.domain.model.Product
import com.abbas57.stockframe.domain.repository.CategoryRepository
import com.abbas57.stockframe.domain.repository.ProductRepository
import com.abbas57.stockframe.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
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
    val localImageFile: File? = null,        // replaces localImageBase64 / localImageUri
    val localImagePreviewUri: Uri? = null,    // kept separately, purely for the form's own image preview — Coil can load a Uri directly without us decoding anything by hand
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

    /**
     * Called when the image picker returns a Uri. Two things happen:
     * 1. The Uri itself is kept for the form's own live preview (Coil
     *    can render a content:// Uri directly — no need to decode it
     *    ourselves just to show it on screen).
     * 2. Separately, a resized/compressed COPY is written to a real File
     *    in app cache — this is what actually gets uploaded. We compress
     *    before upload (not just before display) to keep Cloudinary
     *    bandwidth/storage credits low and the upload fast, same
     *    motivation as the earlier Base64 compression step, just feeding
     *    a file instead of a string this time.
     */
    fun onImagePicked(uri: Uri, context: Context) {
        _formState.value = _formState.value.copy(localImagePreviewUri = uri)
        viewModelScope.launch {
            val file = compressToFile(uri, context)
            _formState.value = _formState.value.copy(localImageFile = file)
        }
    }

    private suspend fun compressToFile(uri: Uri, context: Context): File? =
        withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
                val original = BitmapFactory.decodeStream(inputStream)
                val scale = 1000f / maxOf(original.width, original.height)
                val resized = if (scale < 1f) {
                    Bitmap.createScaledBitmap(
                        original,
                        (original.width * scale).toInt(),
                        (original.height * scale).toInt(),
                        true
                    )
                } else original

                // cacheDir, not filesDir — this file only needs to exist
                // long enough to be uploaded this session; it's disposable,
                // and the OS is free to clear app cache under storage
                // pressure without anything breaking, unlike filesDir
                // which implies "keep this around."
                val outFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
                FileOutputStream(outFile).use { out ->
                    resized.compress(Bitmap.CompressFormat.JPEG, 80, out)
                }
                outFile
            } catch (e: Exception) {
                null
            }
        }

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
            _saveState.value = UiState.Error("Enter a valid unit price"); return
        }
        if (quantity == null || quantity < 0) {
            _saveState.value = UiState.Error("Enter a valid quantity"); return
        }
        if (minimumStock == null || minimumStock < 0) {
            _saveState.value = UiState.Error("Enter a valid minimum stock threshold"); return
        }

        viewModelScope.launch {
            _saveState.value = UiState.Loading

            val current = existingProduct
            if (form.isEditMode && current != null) {
                val updated = current.copy(
                    name = form.name, sku = form.sku,
                    description = form.description.ifBlank { null },
                    categoryId = form.categoryId, unitPrice = unitPrice,
                    quantity = quantity, minimumStock = minimumStock
                )
                productRepository.updateProduct(updated, previousQuantity = current.quantity, form.localImageFile)
                    .onSuccess { _saveState.value = UiState.Success(updated) }
                    .onFailure { e -> _saveState.value = UiState.Error(e.message ?: "Could not update product") }
            } else {
                val newProduct = Product(
                    id = "", ownerId = "", name = form.name, sku = form.sku,
                    description = form.description.ifBlank { null },
                    categoryId = form.categoryId, unitPrice = unitPrice,
                    quantity = quantity, minimumStock = minimumStock,
                    createdAt = 0L, updatedAt = 0L
                )
                productRepository.addProduct(newProduct, form.localImageFile)
                    .onSuccess { saved -> _saveState.value = UiState.Success(saved) }
                    .onFailure { e -> _saveState.value = UiState.Error(e.message ?: "Could not save product") }
            }
        }
    }
}