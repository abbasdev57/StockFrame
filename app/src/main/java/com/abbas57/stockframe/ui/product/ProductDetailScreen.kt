package com.abbas57.stockframe.ui.products

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.abbas57.stockframe.domain.model.StockStatus
import com.abbas57.stockframe.ui.common.UiState
import com.abbas57.stockframe.ui.theme.*

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

    androidx.compose.runtime.LaunchedEffect(productId) {
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
                val product = current.data
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
                    Text("SKU: ${product.sku}", color = Neutral500, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(20.dp))

                    // The single largest, most prominent number on this
                    // screen — matches the Stitch mock's emphasis, and is
                    // the actual reason a business owner opens this screen.
                    Text("Current stock", style = MaterialTheme.typography.bodyMedium, color = Neutral500)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "${product.quantity}",
                            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 40.sp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("units", color = Neutral500, modifier = Modifier.padding(bottom = 6.dp))
                    }
                    Spacer(Modifier.height(12.dp))

                    if (product.stockStatus != StockStatus.IN_STOCK) {
                        val (bg, fg, label) = when (product.stockStatus) {
                            StockStatus.LOW_STOCK -> Triple(Red50, Red800, "Low stock — at or below threshold of ${product.minimumStock}")
                            StockStatus.OUT_OF_STOCK -> Triple(Neutral100, Neutral700, "Out of stock")
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
                        Icon(Icons.Filled.SwapVert, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Stock adjustment")
                    }
                    Spacer(Modifier.height(20.dp))

                    if (!product.description.isNullOrBlank()) {
                        Text("Description", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(6.dp))
                        Text(product.description, color = Neutral700, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(20.dp))
                    }

                    Text("Inventory movement history", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    // Real movement data is a Sprint 3 dependency — that
                    // screen is what actually WRITES inventory_transactions
                    // via Stock Adjustment. Showing this as an honest empty
                    // state now, rather than fabricating a populated-looking
                    // placeholder list, since fake data here would look like
                    // a real feature that silently does nothing.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Movement history will appear here once Stock Adjustment is available.",
                            color = Neutral500,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}