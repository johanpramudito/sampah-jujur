package com.melodi.sampahjujur.data.local.converter

import androidx.room.TypeConverter
import com.melodi.sampahjujur.model.TransactionItem
import com.melodi.sampahjujur.model.WasteItem
import org.json.JSONArray
import org.json.JSONObject

/**
 * Type converters for Room database to handle complex types in TransactionEntity.
 * Converts List<WasteItem> and List<TransactionItem> to/from JSON strings.
 */
class TransactionConverters {

    /**
     * Converts List<WasteItem> to JSON string for storage
     */
    @TypeConverter
    fun fromWasteItemList(items: List<WasteItem>): String {
        val jsonArray = JSONArray()
        items.forEach { item ->
            val jsonObject = JSONObject().apply {
                put("id", item.id)
                put("type", item.type)
                put("weight", item.weight)
                put("estimatedValue", item.estimatedValue)
                put("description", item.description)
                put("imageUrl", item.imageUrl)
                put("createdAt", item.createdAt)
            }
            jsonArray.put(jsonObject)
        }
        return jsonArray.toString()
    }

    /**
     * Converts JSON string back to List<WasteItem>
     */
    @TypeConverter
    fun toWasteItemList(json: String): List<WasteItem> {
        val items = mutableListOf<WasteItem>()
        val jsonArray = JSONArray(json)
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            items.add(
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
        return items
    }

    /**
     * Converts List<TransactionItem> to JSON string for storage
     */
    @TypeConverter
    fun fromTransactionItemList(items: List<TransactionItem>): String {
        val jsonArray = JSONArray()
        items.forEach { item ->
            val jsonObject = JSONObject().apply {
                put("type", item.type)
                put("estimatedWeight", item.estimatedWeight)
                put("estimatedValue", item.estimatedValue)
                put("actualWeight", item.actualWeight)
                put("actualValue", item.actualValue)
            }
            jsonArray.put(jsonObject)
        }
        return jsonArray.toString()
    }

    /**
     * Converts JSON string back to List<TransactionItem>
     */
    @TypeConverter
    fun toTransactionItemList(json: String): List<TransactionItem> {
        val items = mutableListOf<TransactionItem>()
        val jsonArray = JSONArray(json)
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            items.add(
                TransactionItem(
                    type = jsonObject.optString("type", ""),
                    estimatedWeight = jsonObject.optDouble("estimatedWeight", 0.0),
                    estimatedValue = jsonObject.optDouble("estimatedValue", 0.0),
                    actualWeight = jsonObject.optDouble("actualWeight", 0.0),
                    actualValue = jsonObject.optDouble("actualValue", 0.0)
                )
            )
        }
        return items
    }
}
