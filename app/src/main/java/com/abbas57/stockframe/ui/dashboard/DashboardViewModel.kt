package com.abbas57.stockframe.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abbas57.stockframe.domain.model.InventoryTransaction
import com.abbas57.stockframe.domain.model.Product
import com.abbas57.stockframe.domain.model.TransactionType
import com.abbas57.stockframe.domain.repository.CategoryRepository
import com.abbas57.stockframe.domain.repository.InventoryTransactionRepository
import com.abbas57.stockframe.domain.repository.ProductRepository
import com.abbas57.stockframe.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

data class DashboardUiState(
    val totalProducts: Int = 0,
    val categoriesCount: Int = 0,
    val inventoryValue: Double = 0.0,
    val lowStockCount: Int = 0,
    val recentActivity: List<InventoryTransaction> = emptyList(),
    val stockTrends: List<DayTrend> = emptyList()
)

/**
 * One bar in the Stock Trends chart — one calendar day's worth of
 * stock movement summarised into a single data point. stockIn and
 * stockOut are always positive here (absolute values) since the
 * chart renders them as separate coloured bars, not a net delta.
 */
data class DayTrend(
    val label: String,     // "Jun 28" — short display label for the x-axis
    val stockIn: Int,
    val stockOut: Int
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: InventoryTransactionRepository
) : ViewModel() {

    val uiState: StateFlow<UiState<DashboardUiState>> = combine(
        productRepository.observeProducts(),
        categoryRepository.observeCategories(),
        transactionRepository.observeTransactions()
    ) { products, categories, transactions ->

        val lowStockItems = products.filter {
            it.quantity <= it.minimumStock && it.quantity > 0
        }
        val outOfStockItems = products.filter { it.quantity <= 0 }

        UiState.Success(
            DashboardUiState(
                totalProducts = products.size,
                categoriesCount = categories.size,
                // Inventory value = sum of (quantity × unitPrice) across
                // all active products. This is a real-time snapshot —
                // it updates the moment any stock adjustment lands.
                inventoryValue = products.sumOf { it.quantity * it.unitPrice },
                // Low Stock Items count includes both "low" AND "out of
                // stock" products — both need the owner's attention,
                // and combining them into one "needs attention" number
                // matches the Stitch dashboard's design intent.
                lowStockCount = lowStockItems.size + outOfStockItems.size,
                // Most recent 10 transactions — enough for a useful
                // activity feed without overwhelming the screen.
                recentActivity = transactions.take(10),
                stockTrends = buildStockTrends(transactions)
            )
        ) as UiState<DashboardUiState>
    }.catch { e ->
        emit(UiState.Error(e.message ?: "An unknown error occurred"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UiState.Loading
    )

    /**
     * Groups the last 30 days' transactions into per-day DayTrend
     * objects for the bar chart. Days with no activity still appear
     * as zero-height bars so the x-axis is always a continuous 30-day
     * window, not a sparse list of only active days (which would make
     * the chart look different every day even without meaningful change).
     */
    private fun buildStockTrends(transactions: List<InventoryTransaction>): List<DayTrend> {
        val calendar = Calendar.getInstance()
        val dateFormat = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
        val dayFormat = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault())

        // Build a map of dayKey -> DayTrend for the last 30 days,
        // seeded with zero values so every day has an entry even if
        // no transactions occurred that day.
        val trendMap = LinkedHashMap<String, DayTrend>()
        repeat(30) { daysAgo ->
            calendar.time = java.util.Date()
            calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
            val dayKey = dayFormat.format(calendar.time)
            val label = dateFormat.format(calendar.time)
            trendMap[dayKey] = DayTrend(label = label, stockIn = 0, stockOut = 0)
        }

        // Accumulate real transaction data into the seeded map
        val cutoff = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        transactions
            .filter { it.createdAt >= cutoff }
            .forEach { txn ->
                val dayKey = dayFormat.format(java.util.Date(txn.createdAt))
                val existing = trendMap[dayKey] ?: return@forEach
                trendMap[dayKey] = when (txn.type) {
                    TransactionType.IN ->
                        existing.copy(stockIn = existing.stockIn + txn.quantity)
                    TransactionType.OUT ->
                        // quantity is stored as negative for OUT — abs() for chart display
                        existing.copy(stockOut = existing.stockOut + kotlin.math.abs(txn.quantity))
                    TransactionType.ADJUSTMENT -> existing // adjustments not plotted on trends chart
                }
            }

        // Reverse so the chart reads oldest-left → newest-right
        return trendMap.values.toList().reversed()
    }
}