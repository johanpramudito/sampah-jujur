package com.melodi.sampahjujur.data.local.dao

import androidx.room.*
import com.melodi.sampahjujur.data.local.entity.WasteItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for WasteItem operations.
 * Provides methods for CRUD operations on draft waste items stored locally.
 */
@Dao
interface WasteItemDao {

    /**
     * Insert a new waste item into the database
     * @param wasteItem The waste item to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(wasteItem: WasteItemEntity)

    /**
     * Insert multiple waste items at once
     * @param wasteItems List of waste items to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(wasteItems: List<WasteItemEntity>)

    /**
     * Update an existing waste item
     * @param wasteItem The waste item to update
     */
    @Update
    suspend fun update(wasteItem: WasteItemEntity)

    /**
     * Delete a waste item from the database
     * @param wasteItem The waste item to delete
     */
    @Delete
    suspend fun delete(wasteItem: WasteItemEntity)

    /**
     * Delete a waste item by its ID
     * @param id The ID of the waste item to delete
     * @return Number of rows deleted
     */
    @Query("DELETE FROM waste_items WHERE id = :id")
    suspend fun deleteById(id: String): Int

    /**
     * Get all waste items for a specific household as a Flow (reactive)
     * @param householdId The household ID
     * @return Flow of list of waste items
     */
    @Query("SELECT * FROM waste_items WHERE householdId = :householdId ORDER BY createdAt DESC")
    fun getWasteItemsByHousehold(householdId: String): Flow<List<WasteItemEntity>>

    /**
     * Get a single waste item by ID
     * @param id The waste item ID
     * @return The waste item or null if not found
     */
    @Query("SELECT * FROM waste_items WHERE id = :id")
    suspend fun getWasteItemById(id: String): WasteItemEntity?

    /**
     * Get all unsynced waste items for a household
     * @param householdId The household ID
     * @return List of unsynced waste items
     */
    @Query("SELECT * FROM waste_items WHERE householdId = :householdId AND isSynced = 0")
    suspend fun getUnsyncedItems(householdId: String): List<WasteItemEntity>

    /**
     * Mark a waste item as synced with Firebase
     * @param id The waste item ID
     * @return Number of rows updated
     */
    @Query("UPDATE waste_items SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String): Int

    /**
     * Mark multiple items as synced
     * @param ids List of waste item IDs
     * @return Number of rows updated
     */
    @Query("UPDATE waste_items SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markMultipleAsSynced(ids: List<String>): Int

    /**
     * Delete all waste items for a household
     * @param householdId The household ID
     * @return Number of rows deleted
     */
    @Query("DELETE FROM waste_items WHERE householdId = :householdId")
    suspend fun deleteAllForHousehold(householdId: String): Int

    /**
     * Get count of unsynced items for a household
     * @param householdId The household ID
     * @return Number of unsynced items
     */
    @Query("SELECT COUNT(*) FROM waste_items WHERE householdId = :householdId AND isSynced = 0")
    suspend fun getUnsyncedCount(householdId: String): Int
}
