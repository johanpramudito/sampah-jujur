package com.melodi.sampahjujur.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.melodi.sampahjujur.model.WasteItem

/**
 * Room entity for storing draft waste items locally.
 * Enables offline creation and editing of waste items before syncing to Firebase.
 *
 * @property id Unique identifier for the waste item
 * @property householdId ID of the household that owns this draft item
 * @property type Type of waste (e.g., "plastic", "paper", "metal")
 * @property weight Weight in kilograms
 * @property estimatedValue Estimated monetary value
 * @property description Optional notes about the item
 * @property imageUrl URL of uploaded image (from Cloudinary)
 * @property createdAt Timestamp when item was created
 * @property isSynced Flag indicating if this item has been synced to Firebase
 */
@Entity(tableName = "waste_items")
data class WasteItemEntity(
    @PrimaryKey
    val id: String,
    val householdId: String,
    val type: String,
    val weight: Double,
    val estimatedValue: Double,
    val description: String,
    val imageUrl: String,
    val createdAt: Long,
    val isSynced: Boolean = false
) {
    /**
     * Converts Room entity to domain model
     */
    fun toWasteItem(): WasteItem {
        return WasteItem(
            id = id,
            type = type,
            weight = weight,
            estimatedValue = estimatedValue,
            description = description,
            imageUrl = imageUrl,
            createdAt = createdAt
        )
    }

    companion object {
        /**
         * Creates Room entity from domain model
         */
        fun fromWasteItem(wasteItem: WasteItem, householdId: String, isSynced: Boolean = false): WasteItemEntity {
            return WasteItemEntity(
                id = wasteItem.id.ifEmpty { "${householdId}_${System.currentTimeMillis()}" },
                householdId = householdId,
                type = wasteItem.type,
                weight = wasteItem.weight,
                estimatedValue = wasteItem.estimatedValue,
                description = wasteItem.description,
                imageUrl = wasteItem.imageUrl,
                createdAt = wasteItem.createdAt,
                isSynced = isSynced
            )
        }
    }
}
