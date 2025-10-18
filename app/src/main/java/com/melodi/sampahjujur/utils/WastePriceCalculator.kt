package com.melodi.sampahjujur.utils

/**
 * Utility object for calculating estimated market values of waste items.
 * Prices are based on typical market rates for recyclable materials.
 */
object WastePriceCalculator {

    // Price per kg in USD for different waste types
    private val pricePerKg = mapOf(
        "plastic" to 0.50,      // $0.50 per kg
        "paper" to 0.10,        // $0.10 per kg
        "metal" to 2.00,        // $2.00 per kg (aluminum, steel)
        "glass" to 0.05,        // $0.05 per kg
        "electronics" to 3.00,  // $3.00 per kg (contains valuable metals)
        "cardboard" to 0.15,    // $0.15 per kg
        "other" to 0.08         // $0.08 per kg (default)
    )

    /**
     * Calculates the estimated market value for a waste item
     *
     * @param type The type of waste (case-insensitive)
     * @param weight The weight in kilograms
     * @return The estimated value in USD, rounded to 2 decimal places
     */
    fun calculateValue(type: String, weight: Double): Double {
        if (weight <= 0) return 0.0

        val normalizedType = type.lowercase().trim()
        val priceRate = pricePerKg[normalizedType] ?: pricePerKg["other"]!!

        val value = weight * priceRate
        return String.format("%.2f", value).toDouble()
    }

    /**
     * Gets the price per kg for a specific waste type
     *
     * @param type The type of waste (case-insensitive)
     * @return The price per kg in USD
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
     * @return Formatted string with type and price (e.g., "Plastic ($0.50/kg)")
     */
    fun getWasteTypeWithPrice(type: String): String {
        val normalizedType = type.lowercase().trim()
        val price = pricePerKg[normalizedType] ?: pricePerKg["other"]!!
        return "$type ($${"%.2f".format(price)}/kg)"
    }
}
