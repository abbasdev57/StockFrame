package com.abbas57.stockframe.ui.stock

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.abbas57.stockframe.domain.model.TransactionType
import com.abbas57.stockframe.ui.common.UiState
import com.abbas57.stockframe.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockAdjustmentScreen(
    productId: String,
    onSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: StockAdjustmentViewModel = hiltViewModel()
) {
    val form by viewModel.formState.collectAsState()
    val saveState by viewModel.saveState.collectAsState()

    LaunchedEffect(productId) { viewModel.loadProduct(productId) }

    // Navigate back exactly once on success — same LaunchedEffect-keyed
    // pattern as Login/Register. A successful adjustment pops back to
    // Product Detail, where the updated quantity and new history entry
    // will already be live via their Flow observers.
    LaunchedEffect(saveState) {
        if (saveState is UiState.Success) onSuccess()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stock adjustment") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->

        if (form.isLoadingProduct) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        val product = form.product
        if (product == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { Text("Product not found", color = MaterialTheme.colorScheme.error) }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Product context card — shows what's being adjusted
            // before the user touches any controls, so it's clear
            // which product this action will affect.
            Card(
                colors = CardDefaults.cardColors(containerColor = Neutral50),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(product.name, style = MaterialTheme.typography.titleLarge)
                    Text("SKU: ${product.sku}", color = Neutral500, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Text("Current stock", style = MaterialTheme.typography.bodySmall, color = Neutral500)
                    Text(
                        "${product.quantity} units",
                        style = MaterialTheme.typography.headlineSmall,
                        color = when {
                            product.quantity <= 0 -> Neutral700
                            product.quantity <= product.minimumStock -> Red500
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
            Spacer(Modifier.height(20.dp))

            // Type toggle — Stock In / Stock Out as a segmented button
            // so the visual distinction (green vs red intent) is clear
            // before the user commits to any numbers.
            Text("Adjustment type", style = MaterialTheme.typography.bodyMedium, color = Neutral700)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(TransactionType.IN to "Stock In", TransactionType.OUT to "Stock Out").forEach { (type, label) ->
                    val selected = form.type == type
                    Button(
                        onClick = { viewModel.onTypeChanged(type) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when {
                                selected && type == TransactionType.IN -> Green500
                                selected && type == TransactionType.OUT -> Red500
                                else -> Neutral100
                            },
                            contentColor = if (selected) Neutral0 else Neutral700
                        )
                    ) { Text(label) }
                }
            }
            Spacer(Modifier.height(20.dp))

            Text("Quantity", style = MaterialTheme.typography.bodyMedium, color = Neutral700)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = form.quantityText,
                onValueChange = viewModel::onQuantityChanged,
                placeholder = { Text("0") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            // Reason dropdown
            Text("Reason", style = MaterialTheme.typography.bodyMedium, color = Neutral700)
            Spacer(Modifier.height(4.dp))
            ReasonDropdown(
                reasons = form.availableReasons,
                selectedReason = form.reason,
                onReasonSelected = viewModel::onReasonChanged
            )
            Spacer(Modifier.height(16.dp))

            Text("Additional note (optional)", style = MaterialTheme.typography.bodyMedium, color = Neutral700)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = form.note,
                onValueChange = viewModel::onNoteChanged,
                placeholder = { Text("Explain the context of this adjustment") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))

            if (saveState is UiState.Error) {
                Text(
                    (saveState as UiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(8.dp))
            }

            Button(
                onClick = viewModel::confirmAdjustment,
                enabled = saveState !is UiState.Loading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (form.type == TransactionType.IN) Green500 else Red500
                )
            ) {
                if (saveState is UiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Neutral0)
                } else {
                    Text("Confirm adjustment")
                }
            }
            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Discard") }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReasonDropdown(
    reasons: List<String>,
    selectedReason: String,
    onReasonSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedReason.ifBlank { "Select a reason" },
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            reasons.forEach { reason ->
                DropdownMenuItem(
                    text = { Text(reason) },
                    onClick = { onReasonSelected(reason); expanded = false }
                )
            }
        }
    }
}