package com.abbas57.stockframe.ui.products

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.abbas57.stockframe.domain.model.Category
import com.abbas57.stockframe.ui.common.UiState
import com.abbas57.stockframe.ui.theme.*

/**
 * Single screen handles both Add and Edit — same shape as everywhere else
 * in this codebase preferring one form over near-duplicate screens
 * (StockframeTopBar's two-state pattern is the same instinct). productId
 * is null for Add; passing a real ID switches the ViewModel into edit
 * mode via loadProductForEdit, called exactly once via LaunchedEffect.
 */
@Composable
fun AddEditProductScreen(
    productId: String?,
    onSaveComplete: () -> Unit,
    onDiscard: () -> Unit,
    viewModel: AddEditProductViewModel = hiltViewModel()
) {
    val form by viewModel.formState.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    LaunchedEffect(productId) {
        if (productId != null) viewModel.loadProductForEdit(productId)
    }

    // Fires onSaveComplete exactly once per successful save, same
    // LaunchedEffect-keyed-on-state pattern as Login/Register reacting to
    // UiState.Success rather than checking state inside a click handler.
    LaunchedEffect(saveState) {
        if (saveState is UiState.Success) onSaveComplete()
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        // PickVisualMedia returns null if the user backs out of the picker
        // without choosing anything — that's a normal cancel, not an error,
        // so it's silently ignored rather than surfaced as a failure.
        if (uri != null) viewModel.onImagePicked(uri.toString())
    }

    if (form.isLoadingExisting) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            if (form.isEditMode) "Edit product" else "Add new product",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(20.dp))

        FormSectionCard {
            LabeledField("Product name") {
                OutlinedTextField(
                    value = form.name,
                    onValueChange = viewModel::onNameChanged,
                    placeholder = { Text("e.g. Ergonomic Office Chair") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(12.dp))

            LabeledField("SKU") {
                OutlinedTextField(
                    value = form.sku,
                    onValueChange = viewModel::onSkuChanged,
                    placeholder = { Text("INV-10293") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(12.dp))

            LabeledField("Description (optional)") {
                OutlinedTextField(
                    value = form.description,
                    onValueChange = viewModel::onDescriptionChanged,
                    placeholder = { Text("Brief details about this product") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Category", style = MaterialTheme.typography.bodyMedium, color = Neutral700)
                TextButton(onClick = { showAddCategoryDialog = true }) {
                    Text("Add new category")
                }
            }
            CategoryDropdown(
                categories = form.categories,
                selectedCategoryId = form.categoryId,
                onCategorySelected = viewModel::onCategorySelected
            )
        }

        Spacer(Modifier.height(16.dp))

        FormSectionCard {
            Text("Inventory details", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))

            LabeledField("Unit price (\$)") {
                OutlinedTextField(
                    value = form.unitPriceText,
                    onValueChange = viewModel::onUnitPriceChanged,
                    placeholder = { Text("0.00") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(12.dp))

            LabeledField("Quantity") {
                OutlinedTextField(
                    value = form.quantityText,
                    onValueChange = viewModel::onQuantityChanged,
                    placeholder = { Text("0") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(12.dp))

            LabeledField("Minimum stock threshold") {
                OutlinedTextField(
                    value = form.minimumStockText,
                    onValueChange = viewModel::onMinimumStockChanged,
                    placeholder = { Text("10") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        FormSectionCard {
            Text("Product image", style = MaterialTheme.typography.bodyMedium, color = Neutral700)
            Spacer(Modifier.height(8.dp))

            val displayImage = form.localImageUri ?: form.existingImageUrl
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .border(1.dp, Neutral300, RoundedCornerShape(8.dp))
                    .background(Neutral50, RoundedCornerShape(8.dp))
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) {
                        imagePickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (displayImage != null) {
                    AsyncImage(
                        model = displayImage,
                        contentDescription = "Product image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.AddAPhoto, contentDescription = null, tint = Neutral500)
                        Spacer(Modifier.height(8.dp))
                        Text("Click to upload", color = Neutral700)
                        Text("PNG or JPG up to 10MB", style = MaterialTheme.typography.bodySmall, color = Neutral500)
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        if (saveState is UiState.Error) {
            Text(
                text = (saveState as UiState.Error).message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
        }

        Button(
            onClick = viewModel::save,
            enabled = saveState !is UiState.Loading,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Blue400)
        ) {
            if (saveState is UiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Neutral0)
            } else {
                Text("Save product")
            }
        }
        Spacer(Modifier.height(12.dp))

        OutlinedButton(onClick = onDiscard, modifier = Modifier.fillMaxWidth()) {
            Text("Discard changes")
        }
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Blue50, RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Icon(Icons.Filled.Info, contentDescription = null, tint = Blue800, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "The minimum stock threshold helps us notify you when items are running low.",
                style = MaterialTheme.typography.bodySmall,
                color = Blue800
            )
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onConfirm = { name ->
                viewModel.addCategory(name)
                showAddCategoryDialog = false
            }
        )
    }
}

@Composable
private fun FormSectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Neutral50)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun LabeledField(label: String, field: @Composable () -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Neutral700)
        Spacer(Modifier.height(4.dp))
        field()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    categories: List<Category>,
    selectedCategoryId: String?,
    onCategorySelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = categories.firstOrNull { it.id == selectedCategoryId }?.name ?: "Select category"

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (categories.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No categories yet — add one above") },
                    onClick = { expanded = false },
                    enabled = false
                )
            }
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        onCategorySelected(category.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun AddCategoryDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add new category") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("e.g. Electronics") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}