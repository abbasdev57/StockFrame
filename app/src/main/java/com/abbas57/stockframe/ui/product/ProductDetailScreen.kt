package com.abbas57.stockframe.ui.product

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.abbas57.stockframe.domain.model.InventoryTransaction
import com.abbas57.stockframe.domain.model.StockStatus
import com.abbas57.stockframe.domain.model.TransactionType
import com.abbas57.stockframe.ui.common.UiState
import com.abbas57.stockframe.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import androidx.compose.ui.platform.LocalLocale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: String,
    onEditClick: (productId: String) -> Unit,
    onStockAdjustmentClick: (productId: String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ProductDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(productId) {
        viewModel.loadProduct(productId)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { onEditClick(productId) }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit product")
                }
            }
        )

        when (val current = state) {
            is UiState.Loading, is UiState.Idle -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(current.message, color = MaterialTheme.colorScheme.error)
                }
            }

            is UiState.Success -> {
                // Single smart-cast of `current` here. Both `product` and
                // `transactions` are derived from this one cast — no second
                // re-examination of `state` anywhere else in this branch.
                // That was the root cause of every cascading error before:
                // a duplicate `(state as? UiState.Success)` further down
                // confused the compiler about which type `state` had been
                // narrowed to at that point in the function.
                val product = current.data.product
                val transactions = current.data.transactions

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Neutral50)
                    )
                    Spacer(Modifier.height(16.dp))

                    Text(product.name, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "SKU: ${product.sku}",
                        color = Neutral500,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(20.dp))

                    // The single largest, most prominent number on this
                    // screen — matches the Stitch mock emphasis, and is
                    // the actual reason a business owner opens this screen.
                    Text(
                        "Current stock",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Neutral500
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "${product.quantity}",
                            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 40.sp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "units",
                            color = Neutral500,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    Spacer(Modifier.height(12.dp))

                    if (product.stockStatus != StockStatus.IN_STOCK) {
                        val (bg, fg, label) = when (product.stockStatus) {
                            StockStatus.LOW_STOCK -> Triple(
                                Red50,
                                Red800,
                                "Low stock — at or below threshold of ${product.minimumStock}"
                            )

                            StockStatus.OUT_OF_STOCK -> Triple(
                                Neutral100,
                                Neutral700,
                                "Out of stock"
                            )

                            else -> Triple(Neutral0, Neutral900, "")
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(bg, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(label, color = fg, style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    Button(
                        onClick = { onStockAdjustmentClick(productId) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Blue400)
                    ) {
                        Icon(
                            Icons.Filled.SwapVert,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Stock adjustment")
                    }
                    Spacer(Modifier.height(20.dp))

                    if (!product.description.isNullOrBlank()) {
                        Text("Description", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            product.description,
                            color = Neutral700,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(20.dp))
                    }

                    Text("Inventory movement history", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))

                    if (transactions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No movements recorded yet.",
                                color = Neutral500,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    } else {
                        // Show last 5 movements inline — full history is always one
                        // tap away on the History tab, so no "load more" needed here.
                        transactions.take(5).forEach { transaction ->
                            ProductDetailTransactionRow(transaction)
                            Spacer(Modifier.height(8.dp))
                        }
                        if (transactions.size > 5) {
                            TextButton(onClick = { /* navigate to History tab — optional */ }) {
                                Text("View all ${transactions.size} movements")
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun ProductDetailTransactionRow(transaction: InventoryTransaction) {
    val (typeBg, typeFg) = when (transaction.type) {
        TransactionType.IN -> Green50 to Green800
        TransactionType.OUT -> Red50 to Red800
        TransactionType.ADJUSTMENT -> Blue50 to Blue800
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .background(typeBg, RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                transaction.type.displayLabel(),
                color = typeFg,
                style = MaterialTheme.typography.labelLarge
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(transaction.reason, style = MaterialTheme.typography.bodySmall, color = Neutral700)
            Text(
                SimpleDateFormat("MMM dd, yyyy", LocalLocale.current.platformLocale).format(Date(transaction.createdAt)),
                style = MaterialTheme.typography.bodySmall,
                color = Neutral300
            )
        }
        Text(
            if (transaction.quantity >= 0) "+${transaction.quantity}" else "${transaction.quantity}",
            style = MaterialTheme.typography.titleLarge,
            color = if (transaction.quantity >= 0) Green500 else Red500
        )
    }
    HorizontalDivider(color = Neutral100, modifier = Modifier.padding(top = 8.dp))
}