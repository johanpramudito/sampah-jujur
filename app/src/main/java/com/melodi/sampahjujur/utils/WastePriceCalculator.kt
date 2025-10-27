package com.melodi.sampahjujur.utils

/**
 * Utility object for calculating estimated market values of waste items.
 * Prices are based on typical market rates for recyclable materials.
 * Currency: Indonesian Rupiah (Rp)
 */
object WastePriceCalculator {

    // Price per kg in Rupiah for different waste types
    // Based on actual bank sampah (waste bank) prices in Indonesia 2024-2025
    private val pricePerKg = mapOf(
        "plastic" to 4000.0,      // Rp 4,000 per kg (avg. PET bottles, plastic cups)
        "paper" to 2000.0,        // Rp 2,000 per kg (avg. mixed paper)
        "metal" to 9000.0,        // Rp 9,000 per kg (aluminum - common recyclable metal)
        "glass" to 1000.0,        // Rp 1,000 per kg (glass bottles)
        "electronics" to 2000.0,  // Rp 2,000 per kg (avg. electronic waste)
        "cardboard" to 1200.0,    // Rp 1,200 per kg (cardboard boxes)
        "other" to 1000.0         // Rp 1,000 per kg (default mixed waste)
    )

    /**
     * Calculates the estimated market value for a waste item
     *
     * @param type The type of waste (case-insensitive)
     * @param weight The weight in kilograms
     * @return The estimated value in Rupiah (Rp), rounded to nearest whole number
     */
    fun calculateValue(type: String, weight: Double): Double {
        if (weight <= 0) return 0.0

        val normalizedType = type.lowercase().trim()
        val priceRate = pricePerKg[normalizedType] ?: pricePerKg["other"]!!

        val value = weight * priceRate
        return kotlin.math.round(value)
    }

    /**
     * Gets the price per kg for a specific waste type
     *
     * @param type The type of waste (case-insensitive)
     * @return The price per kg in Rupiah (Rp)
     */
    fun getPricePerKg(type: String): Double {
        val normalizedType = type.lowercase().trim()
        return pricePerKg[normalizedType] ?: pricePerKg["other"]!!
    }

    /**
     * Gets all available waste types
     *
     * @return List of waste type names
     */
    fun getWasteTypes(): List<String> {
        return listOf(
            "Plastic",
            "Paper",
            "Metal",
            "Glass",
            "Electronics",
            "Cardboard",
            "Other"
        )
    }

    /**
     * Gets waste type with price information
     *
     * @param type The waste type
     * @return Formatted string with type and price (e.g., "Plastic (Rp 8,310/kg)")
     */
    fun getWasteTypeWithPrice(type: String): String {
        val normalizedType = type.lowercase().trim()
        val price = pricePerKg[normalizedType] ?: pricePerKg["other"]!!
        return "$type (Rp ${String.format("%,.0f", price)}/kg)"
    }
}
