package com.abbas57.stockframe.ui.product

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.abbas57.stockframe.domain.model.Product
import com.abbas57.stockframe.domain.model.ProductListItem
import com.abbas57.stockframe.domain.model.ProductListUiData
import com.abbas57.stockframe.domain.model.StockStatus
import com.abbas57.stockframe.ui.common.UiState
import com.abbas57.stockframe.ui.theme.*

@Composable
fun ProductListScreen(
    onProductClick: (productId: String) -> Unit,
    onAddProductClick: () -> Unit,
    viewModel: ProductListViewModel = hiltViewModel(),
    contentPadding: PaddingValues
) {
    val state by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
        when (val current = state) {
            is UiState.Loading, is UiState.Idle -> {
                // Listener hasn't emitted yet (first launch, cold start).
                // No skeleton shimmer in V1 — a centered spinner is the
                // same discipline as Login/Register: don't build polish
                // the feature list never asked for.
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text(
                        text = current.message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            is UiState.Success -> {
                ProductListContent(
                    data = current.data,
                    onSearchQueryChanged = viewModel::onSearchQueryChanged,
                    onCategorySelected = viewModel::onCategorySelected,
                    onLoadMore = viewModel::onLoadMore,
                    onProductClick = onProductClick
                )
            }
        }

        FloatingActionButton(
            onClick = onAddProductClick,
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            containerColor = Blue400,
            contentColor = Neutral0
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add product")
        }
    }
}

@Composable
private fun ProductListContent(
    data: ProductListUiData,
    onSearchQueryChanged: (String) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onLoadMore: () -> Unit,
    onProductClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            OutlinedTextField(
                value = data.searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = { Text("Search products or SKUs...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    CategoryChip(
                        label = "All Categories",
                        selected = data.selectedCategoryId == null,
                        onClick = { onCategorySelected(null) }
                    )
                }
                items(data.categories) { category ->
                    CategoryChip(
                        label = category.name,
                        selected = data.selectedCategoryId == category.id,
                        onClick = { onCategorySelected(category.id) }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        if (data.items.isEmpty()) {
            item {
                // Distinct from the Loading case in the parent — this is a
                // legitimate zero-result state (search/filter matched
                // nothing, or the catalog is genuinely empty), not "still
                // fetching." Worth a real empty-state message rather than
                // an indistinguishable blank screen.
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text(
                        "No products match your search.",
                        color = Neutral500,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        items(data.items, key = { it.product.id }) { item ->
            ProductCard(item = item, onClick = { onProductClick(item.product.id) })
            Spacer(Modifier.height(12.dp))
        }

        if (data.canLoadMore) {
            item {
                OutlinedButton(
                    onClick = onLoadMore,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Load More Products")
                }
                Spacer(Modifier.height(80.dp)) // clears the FAB
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Blue400,
            selectedLabelColor = Neutral0
        )
    )
}

@Composable
private fun ProductCard(item: ProductListItem, onClick: () -> Unit) {
    val product = item.product
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Neutral0),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            // Image thumbnail — three states:
            // 1. imageUrl is null (product has no image) → grey placeholder
            // 2. imageUrl exists but not loaded yet → same grey placeholder
            //    (Coil handles the in-between state automatically via
            //    placeholder/error params — no spinner needed at this size)
            // 3. imageUrl loaded → the actual product photo
            if (product.imageUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(product.imageUrl)
                        .crossfade(true)
                        // Shows the grey box while loading instead of
                        // a blank gap — same visual treatment as
                        // the no-image state, so the list doesn't
                        // jump/reflow when images arrive.
                        .placeholder(ColorDrawable(Color.parseColor("#F1EFE8")))
                        .error(ColorDrawable(android.graphics.Color.parseColor("#F1EFE8")))
                        .build(),
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                // No image set — show a neutral placeholder that
                // matches the card surface, not a broken-image icon.
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Neutral100, RoundedCornerShape(8.dp))
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, style = MaterialTheme.typography.titleLarge)
                if (item.categoryName != null) {
                    Text(
                        item.categoryName,
                        style = MaterialTheme.typography.bodySmall,
                        color = Neutral500
                    )
                }
                Spacer(Modifier.height(8.dp))
                StockBadge(product)
            }
        }
    }
}

@Composable
private fun StockBadge(product: Product) {
    // Three distinct visual states, matching the Stitch mock exactly:
    // Low Stock -> red alert chip with a warning icon (this product needs
    // attention soon). Out of Stock -> neutral grey chip (a fact, not an
    // active alert — there's nothing actionable about a quantity of zero
    // beyond what Stock Adjustment, Sprint 3, will handle). In Stock ->
    // plain text, no chip container at all, since "fine" doesn't need a
    // visual treatment competing for attention with the two states that do.
    when (product.stockStatus) {
        StockStatus.LOW_STOCK -> {
            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                modifier = Modifier
                    .background(Red50, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = null,
                    tint = Red500,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "${product.quantity} left (Low)",
                    color = Red800,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        StockStatus.OUT_OF_STOCK -> {
            Box(
                modifier = Modifier
                    .background(Neutral100, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("Out of Stock", color = Neutral700, style = MaterialTheme.typography.bodySmall)
            }
        }
        StockStatus.IN_STOCK -> {
            Text(
                "${product.quantity} in stock",
                color = Neutral500,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}