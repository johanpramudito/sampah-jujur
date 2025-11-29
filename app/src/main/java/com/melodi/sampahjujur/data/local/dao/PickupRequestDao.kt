package com.melodi.sampahjujur.data.local.dao

import androidx.room.*
import com.melodi.sampahjujur.data.local.entity.PickupRequestEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for pickup request offline storage and sync
 */
@Dao
interface PickupRequestDao {

    /**
     * Insert a new pickup request
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(request: PickupRequestEntity)

    /**
     * Get all unsynced pickup requests for background sync
     */
    @Query("SELECT * FROM pickup_requests WHERE isSynced = 0 ORDER BY createdAt DESC")
    suspend fun getUnsyncedRequests(): List<PickupRequestEntity>

    /**
     * Get all pickup requests for a household (for UI display)
     */
    @Query("SELECT * FROM pickup_requests WHERE householdId = :householdId ORDER BY createdAt DESC")
    fun getRequestsByHousehold(householdId: String): Flow<List<PickupRequestEntity>>

    /**
     * Mark a request as synced after successful Firebase upload
     */
    @Query("UPDATE pickup_requests SET isSynced = 1 WHERE id = :requestId")
    suspend fun markAsSynced(requestId: String)

    /**
     * Delete old synced requests (cleanup)
     */
    @Query("DELETE FROM pickup_requests WHERE isSynced = 1 AND createdAt < :cutoffTime")
    suspend fun deleteOldSynced(cutoffTime: Long)

    /**
     * Get a specific request by ID
     */
    @Query("SELECT * FROM pickup_requests WHERE id = :requestId")
    suspend fun getRequestById(requestId: String): PickupRequestEntity?
}
