package com.melodi.sampahjujur.model

/**
 * Represents a completed recyclable waste transaction between a household and a collector.
 * Stores both the original estimate from the pickup request and the actual settlement details.
 */
data class Transaction(
    val id: String = "",
    val requestId: String = "",
    val householdId: String = "",
    val collectorId: String = "",
    val estimatedWasteItems: List<WasteItem> = emptyList(),
    val actualWasteItems: List<TransactionItem> = emptyList(),
    val estimatedValue: Double = 0.0,
    val finalAmount: Double = 0.0,
    val paymentMethod: String = PAYMENT_CASH,
    val paymentStatus: String = STATUS_PENDING,
    val location: PickupRequest.Location = PickupRequest.Location(),
    val completedAt: Long = System.currentTimeMillis(),
    val notes: String = ""
) {

    companion object {
        const val PAYMENT_CASH = "cash"
        const val PAYMENT_TRANSFER = "transfer"
        const val PAYMENT_EWALLET = "e_wallet"

        const val STATUS_PENDING = "pending"
        const val STATUS_COMPLETED = "completed"
    }

    /**
     * Returns the total actual waste weight if provided, otherwise falls back to estimated weight.
     */
    fun getTotalWeight(): Double {
        return when {
            actualWasteItems.isNotEmpty() -> actualWasteItems.sumOf { it.actualWeight }
            estimatedWasteItems.isNotEmpty() -> estimatedWasteItems.sumOf { it.weight }
            else -> 0.0
        }
    }

    fun hasActualBreakdown(): Boolean = actualWasteItems.isNotEmpty()
}

/**
 * Actual waste breakdown provided by the collector during transaction completion.
 */
data class TransactionItem(
    val type: String = "",
    val estimatedWeight: Double = 0.0,
    val estimatedValue: Double = 0.0,
    val actualWeight: Double = 0.0,
    val actualValue: Double = 0.0
)
