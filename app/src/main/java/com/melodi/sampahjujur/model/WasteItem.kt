package com.melodi.sampahjujur.model

/**
 * Data class representing an individual waste item within a pickup request.
 * Contains information about the type, weight, estimated value, and persistence metadata of recyclable waste.
 *
 * @property type Type of waste (e.g., "plastic", "paper", "metal", "glass")
 * @property weight Weight of the waste item in kilograms
 * @property estimatedValue Estimated monetary value in local currency
 * @property description Optional description or notes about the waste item
 * @property imageUrl URL of the uploaded image (proof of waste item) stored in Cloudinary
 * @property id Document identifier when the item is stored in Firestore (empty for transient items)
 * @property createdAt Timestamp (milliseconds) recording when the item was created
 */
data class WasteItem(
    val type: String = "",
    val weight: Double = 0.0,
    val estimatedValue: Double = 0.0,
    val description: String = "",
    val imageUrl: String = "",
    val id: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val TYPE_PLASTIC = "plastic"
        const val TYPE_PAPER = "paper"
        const val TYPE_METAL = "metal"
        const val TYPE_GLASS = "glass"
        const val TYPE_ELECTRONICS = "electronics"
        const val TYPE_CARDBOARD = "cardboard"

        /**
         * Returns a list of available waste types
         */
        fun getAvailableTypes(): List<String> = listOf(
            TYPE_PLASTIC,
            TYPE_PAPER,
            TYPE_METAL,
            TYPE_GLASS,
            TYPE_ELECTRONICS,
            TYPE_CARDBOARD
        )
    }

    /**
     * Validates if the waste item has all required fields
     */
    fun isValid(): Boolean = type.isNotBlank() && weight > 0.0
}
