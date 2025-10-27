package com.melodi.sampahjujur.ui.screens.collector

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EuroSymbol
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.melodi.sampahjujur.model.Transaction
import com.melodi.sampahjujur.ui.screens.household.formatDateTime
import com.melodi.sampahjujur.ui.theme.PrimaryGreen
import com.melodi.sampahjujur.ui.theme.StatusInProgress
import com.melodi.sampahjujur.viewmodel.CollectorPerformanceMetrics
import com.melodi.sampahjujur.viewmodel.CollectorViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectorPerformanceRoute(
    onBackClick: () -> Unit,
    viewModel: CollectorViewModel = hiltViewModel()
) {
    val metrics by viewModel.performanceMetrics.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val isLoading = metrics.totalTransactions == 0 &&
        metrics.totalCompleted == 0 &&
        metrics.totalAccepted == 0 &&
        metrics.totalInProgress == 0

    CollectorPerformanceScreen(
        metrics = metrics,
        isLoading = isLoading,
        snackbarHostState = snackbarHostState,
        onBackClick = onBackClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectorPerformanceScreen(
    metrics: CollectorPerformanceMetrics,
    isLoading: Boolean,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Performance",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = PrimaryGreen
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                item {
                    PerformanceHighlights(metrics = metrics)
                }

                item {
                    StatusBreakdownSection(metrics = metrics)
                }

                item {
                    EarningsBreakdownSection(metrics = metrics)
                }

                item {
                    EfficiencySection(metrics = metrics)
                }

                if (metrics.recentTransactions.isNotEmpty()) {
                    item {
                        RecentTransactionsSection(transactions = metrics.recentTransactions)
                    }
                }
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }

            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }
        }
    }
}

@Composable
private fun PerformanceHighlights(metrics: CollectorPerformanceMetrics) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = PrimaryGreen),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Total Earnings",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = formatCurrency(metrics.totalEarnings),
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatChip(
                        label = "Completed",
                        value = metrics.totalCompleted.toString(),
                        background = Color.White.copy(alpha = 0.12f)
                    )
                    StatChip(
                        label = "Active",
                        value = metrics.activePickups.toString(),
                        background = Color.White.copy(alpha = 0.12f)
                    )
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completion",
                        tint = PrimaryGreen
                    )
                    Column {
                        Text(
                            text = "Completion Rate",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${formatPercentage(metrics.completionRate)} of handled pickups",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = metrics.completionRate.toFloat().coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxWidth(),
                    color = PrimaryGreen
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Cancellation rate ${formatPercentage(metrics.cancellationRate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun StatChip(
    label: String,
    value: String,
    background: Color
) {
    Column(
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.labelSmall
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StatusBreakdownSection(metrics: CollectorPerformanceMetrics) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = "Pickup status",
                    tint = PrimaryGreen
                )
                Text(
                    text = "Pickup Status Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            StatusRow(label = "Accepted", value = metrics.totalAccepted)
            StatusRow(label = "In Progress", value = metrics.totalInProgress)
            StatusRow(label = "Completed", value = metrics.totalCompleted, highlight = true)
            StatusRow(label = "Cancelled", value = metrics.totalCancelled, color = Color.Red)
        }
    }
}

@Composable
private fun StatusRow(
    label: String,
    value: Int,
    color: Color = Color.Gray,
    highlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (highlight) PrimaryGreen else color,
            fontWeight = if (highlight) FontWeight.SemiBold else FontWeight.Normal
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (highlight) PrimaryGreen else Color.DarkGray
        )
    }
}

@Composable
private fun EarningsBreakdownSection(metrics: CollectorPerformanceMetrics) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AttachMoney,
                    contentDescription = "Earnings breakdown",
                    tint = PrimaryGreen
                )
                Text(
                    text = "Earnings Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            EarningsRow("Today", metrics.earningsToday)
            Divider(color = Color.LightGray.copy(alpha = 0.3f))
            EarningsRow("This Week", metrics.earningsThisWeek)
            Divider(color = Color.LightGray.copy(alpha = 0.3f))
            EarningsRow("This Month", metrics.earningsThisMonth)
        }
    }
}

@Composable
private fun EarningsRow(label: String, amount: Double) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = formatCurrency(amount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = PrimaryGreen
        )
    }
}

@Composable
private fun EfficiencySection(metrics: CollectorPerformanceMetrics) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Assessment,
                    contentDescription = "Efficiency",
                    tint = StatusInProgress
                )
                Text(
                    text = "Efficiency Metrics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Average per pickup",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Text(
                text = formatCurrency(metrics.averagePerPickup),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Average per kg",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        text = formatCurrency(metrics.averagePerKg),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Total waste handled",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f kg", metrics.totalWasteKg),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentTransactionsSection(transactions: List<Transaction>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = CardDefaults.elevatedShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Recent Transactions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            transactions.forEachIndexed { index, transaction ->
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Request #${transaction.requestId.take(8).uppercase()}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatDateTime(transaction.completedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatCurrency(transaction.finalAmount),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = PrimaryGreen
                    )
                }

                if (index < transactions.lastIndex) {
                    Divider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        color = Color.LightGray.copy(alpha = 0.2f)
                    )
                }
            }
        }
    }
}

private fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
    format.maximumFractionDigits = 0
    format.currency = java.util.Currency.getInstance("IDR")
    return format.format(amount)
}

private fun formatPercentage(value: Double): String {
    return String.format(Locale.getDefault(), "%.0f%%", value * 100)
}

@Preview(showBackground = true)
@Composable
private fun CollectorPerformancePreview() {
    val sampleTransactions = List(3) { index ->
        Transaction(
            id = "txn$index",
            requestId = "req$index",
            finalAmount = 45000.0 + index * 10000,
            completedAt = System.currentTimeMillis() - index * 86_400_000L
        )
    }
    CollectorPerformanceScreen(
        metrics = CollectorPerformanceMetrics(
            totalCompleted = 24,
            totalInProgress = 3,
            totalAccepted = 5,
            totalCancelled = 2,
            activePickups = 8,
            completionRate = 0.78,
            cancellationRate = 0.06,
            totalTransactions = 24,
            totalEarnings = 2450000.0,
            totalWasteKg = 512.5,
            averagePerPickup = 102000.0,
            averagePerKg = 4800.0,
            earningsToday = 120000.0,
            earningsThisWeek = 540000.0,
            earningsThisMonth = 2100000.0,
            recentTransactions = sampleTransactions
        ),
        isLoading = false,
        snackbarHostState = remember { SnackbarHostState() },
        onBackClick = {}
    )
}
