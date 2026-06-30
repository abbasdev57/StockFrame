package com.abbas57.stockframe.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.abbas57.stockframe.domain.model.InventoryTransaction
import com.abbas57.stockframe.domain.model.TransactionType
import com.abbas57.stockframe.ui.common.UiState
import com.abbas57.stockframe.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    contentPadding: PaddingValues,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        // Screen header
        Text(
            "Inventory history",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        // Filter chips — "All" + one per TransactionType.
        // null selection = All, matching the ViewModel's filter contract.
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { viewModel.onFilterSelected(null) },
                    label = { Text("All") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Blue400,
                        selectedLabelColor = Neutral0
                    )
                )
            }
            items(TransactionType.entries) { type ->
                FilterChip(
                    selected = selectedFilter == type,
                    onClick = { viewModel.onFilterSelected(type) },
                    label = { Text(type.displayLabel()) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = when (type) {
                            TransactionType.IN -> Green500
                            TransactionType.OUT -> Red500
                            TransactionType.ADJUSTMENT -> Blue400
                        },
                        selectedLabelColor = Neutral0
                    )
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = Neutral100)

        when (val current = uiState) {
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
                val transactions = current.data.transactions
                if (transactions.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (selectedFilter == null) "No inventory movements yet."
                            else "No ${selectedFilter?.displayLabel()} movements yet.",
                            color = Neutral500,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(transactions, key = { it.id }) { transaction ->
                            TransactionRow(transaction = transaction)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(transaction: InventoryTransaction) {
    val (typeBg, typeFg) = when (transaction.type) {
        TransactionType.IN -> Green50 to Green800
        TransactionType.OUT -> Red50 to Red800
        TransactionType.ADJUSTMENT -> Blue50 to Blue800
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Neutral0),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type badge
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

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    transaction.productName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    transaction.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = Neutral500
                )
                Text(
                    formatDate(transaction.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = Neutral300
                )
            }

            Spacer(Modifier.width(8.dp))

            // Signed quantity — positive green for IN/positive ADJUSTMENT,
            // negative red for OUT/negative ADJUSTMENT, so the direction
            // of change is clear at a glance without reading the type badge.
            Text(
                text = if (transaction.quantity >= 0) "+${transaction.quantity}"
                else "${transaction.quantity}",
                style = MaterialTheme.typography.titleLarge,
                color = if (transaction.quantity >= 0) Green500 else Red500
            )
        }

        // Note row — only rendered when a note exists, so the card
        // doesn't waste vertical space on an empty row for the
        // majority of transactions that have no note.
        if (!transaction.note.isNullOrBlank()) {
            HorizontalDivider(color = Neutral100, modifier = Modifier.padding(horizontal = 12.dp))
            Text(
                transaction.note,
                style = MaterialTheme.typography.bodySmall,
                color = Neutral500,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

private fun formatDate(timestamp: Long): String =
    SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.getDefault()).format(Date(timestamp))