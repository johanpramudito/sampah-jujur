package com.melodi.sampahjujur.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.melodi.sampahjujur.data.local.converter.TransactionConverters
import com.melodi.sampahjujur.model.PickupRequest
import com.melodi.sampahjujur.model.Transaction
import com.melodi.sampahjujur.model.TransactionItem
import com.melodi.sampahjujur.model.WasteItem

/**
 * Room entity for caching completed transactions locally.
 * Enables offline viewing of transaction history and faster earnings calculations.
 *
 * @property id Unique transaction identifier
 * @property requestId Associated pickup request ID
 * @property householdId Household user ID
 * @property collectorId Collector user ID
 * @property estimatedWasteItemsJson JSON string of estimated waste items
 * @property actualWasteItemsJson JSON string of actual waste items
 * @property estimatedValue Total estimated value
 * @property finalAmount Final paid amount
 * @property paymentMethod Payment method used
 * @property paymentStatus Payment status
 * @property locationLatitude Pickup location latitude
 * @property locationLongitude Pickup location longitude
 * @property locationAddress Pickup location address
 * @property completedAt Transaction completion timestamp
 * @property notes Additional notes
 * @property cachedAt When this record was cached locally
 */
@Entity(tableName = "transactions")
@TypeConverters(TransactionConverters::class)
data class TransactionEntity(
    @PrimaryKey
    val id: String,
    val requestId: String,
    val householdId: String,
    val collectorId: String,
    val estimatedWasteItems: List<WasteItem>,
    val actualWasteItems: List<TransactionItem>,
    val estimatedValue: Double,
    val finalAmount: Double,
    val paymentMethod: String,
    val paymentStatus: String,
    val locationLatitude: Double,
    val locationLongitude: Double,
    val locationAddress: String,
    val completedAt: Long,
    val notes: String,
    val cachedAt: Long = System.currentTimeMillis()
) {
    /**
     * Converts Room entity to domain model
     */
    fun toTransaction(): Transaction {
        return Transaction(
            id = id,
            requestId = requestId,
            householdId = householdId,
            collectorId = collectorId,
            estimatedWasteItems = estimatedWasteItems,
            actualWasteItems = actualWasteItems,
            estimatedValue = estimatedValue,
            finalAmount = finalAmount,
            paymentMethod = paymentMethod,
            paymentStatus = paymentStatus,
            location = PickupRequest.Location(
                latitude = locationLatitude,
                longitude = locationLongitude,
                address = locationAddress
            ),
            completedAt = completedAt,
            notes = notes
        )
    }

    companion object {
        /**
         * Creates Room entity from domain model
         */
        fun fromTransaction(transaction: Transaction): TransactionEntity {
            return TransactionEntity(
                id = transaction.id,
                requestId = transaction.requestId,
                householdId = transaction.householdId,
                collectorId = transaction.collectorId,
                estimatedWasteItems = transaction.estimatedWasteItems,
                actualWasteItems = transaction.actualWasteItems,
                estimatedValue = transaction.estimatedValue,
                finalAmount = transaction.finalAmount,
                paymentMethod = transaction.paymentMethod,
                paymentStatus = transaction.paymentStatus,
                locationLatitude = transaction.location.latitude,
                locationLongitude = transaction.location.longitude,
                locationAddress = transaction.location.address,
                completedAt = transaction.completedAt,
                notes = transaction.notes,
                cachedAt = System.currentTimeMillis()
            )
        }
    }
}
