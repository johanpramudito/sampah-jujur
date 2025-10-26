package com.melodi.sampahjujur.model

/**
 * Aggregated earnings snapshot for a collector based on completed transactions.
 */
data class Earnings(
    val collectorId: String = "",
    val totalEarnings: Double = 0.0,
    val totalTransactions: Int = 0,
    val totalWasteCollected: Double = 0.0,
    val earningsToday: Double = 0.0,
    val earningsThisWeek: Double = 0.0,
    val earningsThisMonth: Double = 0.0,
    val transactionHistory: List<Transaction> = emptyList()
) {
    fun getAveragePerTransaction(): Double =
        if (totalTransactions > 0) totalEarnings / totalTransactions else 0.0

    fun getAveragePerKg(): Double =
        if (totalWasteCollected > 0) totalEarnings / totalWasteCollected else 0.0
}
