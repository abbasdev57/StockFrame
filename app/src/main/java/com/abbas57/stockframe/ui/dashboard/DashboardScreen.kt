package com.abbas57.stockframe.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.abbas57.stockframe.domain.model.InventoryTransaction
import com.abbas57.stockframe.domain.model.TransactionType
import com.abbas57.stockframe.ui.common.UiState
import com.abbas57.stockframe.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.platform.LocalLocale

@Composable
fun DashboardScreen(
    contentPadding: PaddingValues,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    when (val current = uiState) {
        is UiState.Loading, is UiState.Idle -> {
            Box(
                modifier = Modifier.fillMaxSize().padding(contentPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        }
        is UiState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize().padding(contentPadding),
                contentAlignment = Alignment.Center
            ) { Text(current.message, color = MaterialTheme.colorScheme.error) }
        }
        is UiState.Success -> {
            DashboardContent(data = current.data, contentPadding = contentPadding)
        }
    }
}

@Composable
private fun DashboardContent(data: DashboardUiState, contentPadding: PaddingValues) {
    val currencyFormat = NumberFormat.getCurrencyInstance(LocalLocale.current.platformLocale)

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Operations overview",
                style = MaterialTheme.typography.headlineSmall
            )
        }

        // Stat cards — 2x2 grid layout
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    label = "Total products",
                    value = data.totalProducts.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Categories",
                    value = data.categoriesCount.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    label = "Inventory value",
                    value = currencyFormat.format(data.inventoryValue),
                    modifier = Modifier.weight(1f)
                )
                // Low stock card gets a red tint when count > 0 —
                // it's the only stat card that warrants an alert color
                // because it's the only one representing a problem,
                // not just a measurement.
                StatCard(
                    label = "Needs attention",
                    value = data.lowStockCount.toString(),
                    modifier = Modifier.weight(1f),
                    isAlert = data.lowStockCount > 0
                )
            }
        }

        // Stock Trends chart
        if (data.stockTrends.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Neutral0),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Stock trends", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Last 30 days",
                            style = MaterialTheme.typography.bodySmall,
                            color = Neutral500
                        )
                        Spacer(Modifier.height(16.dp))

                        // Legend
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            LegendItem(color = Blue400, label = "Stock In")
                            LegendItem(color = Red500, label = "Stock Out")
                        }
                        Spacer(Modifier.height(12.dp))

                        StockTrendsChart(
                            trends = data.stockTrends,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                        )
                    }
                }
            }
        }

        // Recent activity
        item {
            Text("Recent activity", style = MaterialTheme.typography.titleLarge)
        }

        if (data.recentActivity.isEmpty()) {
            item {
                Text(
                    "No activity yet.",
                    color = Neutral500,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            items(data.recentActivity, key = { it.id }) { transaction ->
                ActivityRow(transaction = transaction)
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isAlert: Boolean = false
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isAlert) Red50 else Neutral50
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = if (isAlert) Red500 else Neutral500
            )
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                color = if (isAlert) Red800 else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = Neutral500)
    }
}

/**
 * Custom Canvas bar chart — no third-party charting library needed for
 * this simple grouped bar shape. Two bars per day (Stock In in blue,
 * Stock Out in red), heights proportional to the maximum value across
 * all days so the tallest bar always fills the chart height and shorter
 * bars scale correctly relative to it.
 *
 * Only the last 10 days are rendered to keep bars readable at a
 * comfortable width on a phone screen — 30 bars at phone width would
 * be ~10dp each, too narrow to distinguish. Full 30-day data is still
 * available via the History screen's date range.
 */
@Composable
private fun StockTrendsChart(
    trends: List<DayTrend>,
    modifier: Modifier = Modifier
) {
    val visibleTrends = trends.takeLast(10)
    val maxValue = visibleTrends.maxOfOrNull { maxOf(it.stockIn, it.stockOut) }
        ?.takeIf { it > 0 } ?: 1

    val barColor = Blue400
    val outColor = Red500

    Canvas(modifier = modifier) {
        val totalBars = visibleTrends.size
        val groupWidth = size.width / totalBars
        val barWidth = groupWidth * 0.3f
        val gap = groupWidth * 0.05f

        visibleTrends.forEachIndexed { index, trend ->
            val groupLeft = index * groupWidth

            // Stock In bar
            val inHeight = (trend.stockIn.toFloat() / maxValue) * size.height
            drawRect(
                color = barColor,
                topLeft = Offset(groupLeft + gap, size.height - inHeight),
                size = Size(barWidth, inHeight)
            )

            // Stock Out bar
            val outHeight = (trend.stockOut.toFloat() / maxValue) * size.height
            drawRect(
                color = outColor,
                topLeft = Offset(groupLeft + gap + barWidth + gap, size.height - outHeight),
                size = Size(barWidth, outHeight)
            )
        }
    }
}

@Composable
private fun ActivityRow(transaction: InventoryTransaction) {
    val (typeBg, typeFg) = when (transaction.type) {
        TransactionType.IN -> Green50 to Green800
        TransactionType.OUT -> Red50 to Red800
        TransactionType.ADJUSTMENT -> Blue50 to Blue800
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
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
            Text(
                transaction.productName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                SimpleDateFormat("MMM dd · hh:mm a", LocalLocale.current.platformLocale).format(Date(transaction.createdAt)),
                style = MaterialTheme.typography.bodySmall,
                color = Neutral500
            )
        }
        Text(
            if (transaction.quantity >= 0) "+${transaction.quantity}" else "${transaction.quantity}",
            style = MaterialTheme.typography.titleLarge,
            color = if (transaction.quantity >= 0) Green500 else Red500
        )
    }
    HorizontalDivider(color = Neutral100)
}