package com.melodi.sampahjujur.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.melodi.sampahjujur.model.PickupRequest
import com.melodi.sampahjujur.model.WasteItem
import org.json.JSONArray
import org.json.JSONObject

/**
 * Room entity for offline storage of pickup requests
 */
@Entity(tableName = "pickup_requests")
data class PickupRequestEntity(
    @PrimaryKey val id: String,
    val householdId: String,
    val collectorId: String,
    val wasteItemsJson: String,  // JSON-serialized List<WasteItem>
    val locationJson: String,    // JSON-serialized Location
    val notes: String,
    val status: String,
    val totalValue: Double,
    val createdAt: Long,
    val updatedAt: Long,
    val isSynced: Boolean
) {
    companion object {
        /**
         * Convert domain model to entity for Room storage
         */
        fun fromPickupRequest(request: PickupRequest): PickupRequestEntity {
            // Serialize waste items
            val wasteItemsArray = JSONArray()
            request.wasteItems.forEach { item ->
                val jsonObject = JSONObject().apply {
                    put("id", item.id)
                    put("type", item.type)
                    put("weight", item.weight)
                    put("estimatedValue", item.estimatedValue)
                    put("description", item.description)
                    put("imageUrl", item.imageUrl)
                    put("createdAt", item.createdAt)
                }
                wasteItemsArray.put(jsonObject)
            }

            // Serialize location
            val locationObject = JSONObject().apply {
                put("latitude", request.pickupLocation.latitude)
                put("longitude", request.pickupLocation.longitude)
                put("address", request.pickupLocation.address)
            }

            return PickupRequestEntity(
                id = request.id.ifEmpty { "local_${System.currentTimeMillis()}" },
                householdId = request.householdId,
                collectorId = request.collectorId ?: "",
                wasteItemsJson = wasteItemsArray.toString(),
                locationJson = locationObject.toString(),
                notes = request.notes,
                status = request.status,
                totalValue = request.totalValue,
                createdAt = request.createdAt,
                updatedAt = request.updatedAt,
                isSynced = false
            )
        }
    }

    /**
     * Convert entity back to domain model
     */
    fun toPickupRequest(): PickupRequest {
        // Deserialize waste items
        val wasteItems = mutableListOf<WasteItem>()
        val jsonArray = JSONArray(wasteItemsJson)
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            wasteItems.add(
                WasteItem(
                    id = jsonObject.optString("id", ""),
                    type = jsonObject.optString("type", ""),
                    weight = jsonObject.optDouble("weight", 0.0),
                    estimatedValue = jsonObject.optDouble("estimatedValue", 0.0),
                    description = jsonObject.optString("description", ""),
                    imageUrl = jsonObject.optString("imageUrl", ""),
                    createdAt = jsonObject.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        // Deserialize location
        val locationObject = JSONObject(locationJson)
        val location = PickupRequest.Location(
            latitude = locationObject.optDouble("latitude", 0.0),
            longitude = locationObject.optDouble("longitude", 0.0),
            address = locationObject.optString("address", "")
        )

        return PickupRequest(
            id = id,
            householdId = householdId,
            collectorId = collectorId.ifEmpty { null },
            wasteItems = wasteItems,
            pickupLocation = location,
            notes = notes,
            status = status,
            totalValue = totalValue,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
