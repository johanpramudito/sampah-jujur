package com.melodi.sampahjujur.model

/**
 * Aggregated spending/purchases snapshot for a collector based on completed transactions.
 * Tracks money spent by collectors purchasing waste from households.
 */
data class Earnings(
    val collectorId: String = "",
    val totalSpent: Double = 0.0,
    val totalTransactions: Int = 0,
    val totalWasteCollected: Double = 0.0,
    val spentToday: Double = 0.0,
    val spentThisWeek: Double = 0.0,
    val spentThisMonth: Double = 0.0,
    val transactionHistory: List<Transaction> = emptyList()
) {
    fun getAveragePerTransaction(): Double =
        if (totalTransactions > 0) totalSpent / totalTransactions else 0.0

    fun getAveragePerKg(): Double =
        if (totalWasteCollected > 0) totalSpent / totalWasteCollected else 0.0
}
