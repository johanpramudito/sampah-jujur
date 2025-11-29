package com.melodi.sampahjujur.data.local.dao

import androidx.room.*
import com.melodi.sampahjujur.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Transaction operations.
 * Provides methods for caching and querying completed transactions locally.
 */
@Dao
interface TransactionDao {

    /**
     * Insert a new transaction into the cache
     * @param transaction The transaction to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity)

    /**
     * Insert multiple transactions at once
     * @param transactions List of transactions to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    /**
     * Get all transactions for a collector (for earnings calculation)
     * @param collectorId The collector ID
     * @return Flow of list of transactions
     */
    @Query("SELECT * FROM transactions WHERE collectorId = :collectorId ORDER BY completedAt DESC")
    fun getTransactionsByCollector(collectorId: String): Flow<List<TransactionEntity>>

    /**
     * Get all transactions for a household
     * @param householdId The household ID
     * @return Flow of list of transactions
     */
    @Query("SELECT * FROM transactions WHERE householdId = :householdId ORDER BY completedAt DESC")
    fun getTransactionsByHousehold(householdId: String): Flow<List<TransactionEntity>>

    /**
     * Get a single transaction by ID
     * @param transactionId The transaction ID
     * @return The transaction or null if not found
     */
    @Query("SELECT * FROM transactions WHERE id = :transactionId")
    suspend fun getTransactionById(transactionId: String): TransactionEntity?

    /**
     * Get transactions for a collector within a date range
     * @param collectorId The collector ID
     * @param startTime Start timestamp
     * @param endTime End timestamp
     * @return List of transactions in the range
     */
    @Query("SELECT * FROM transactions WHERE collectorId = :collectorId AND completedAt BETWEEN :startTime AND :endTime ORDER BY completedAt DESC")
    suspend fun getTransactionsByCollectorInRange(
        collectorId: String,
        startTime: Long,
        endTime: Long
    ): List<TransactionEntity>

    /**
     * Calculate total earnings for a collector
     * @param collectorId The collector ID
     * @return Total earnings (sum of finalAmount)
     */
    @Query("SELECT SUM(finalAmount) FROM transactions WHERE collectorId = :collectorId")
    suspend fun getTotalEarnings(collectorId: String): Double?

    /**
     * Calculate earnings for a specific time period
     * @param collectorId The collector ID
     * @param startTime Start timestamp
     * @param endTime End timestamp
     * @return Earnings in the period
     */
    @Query("SELECT SUM(finalAmount) FROM transactions WHERE collectorId = :collectorId AND completedAt BETWEEN :startTime AND :endTime")
    suspend fun getEarningsInRange(
        collectorId: String,
        startTime: Long,
        endTime: Long
    ): Double?

    /**
     * Get transaction count for a collector
     * @param collectorId The collector ID
     * @return Number of completed transactions
     */
    @Query("SELECT COUNT(*) FROM transactions WHERE collectorId = :collectorId")
    suspend fun getTransactionCount(collectorId: String): Int

    /**
     * Get total weight collected by a collector
     * @param collectorId The collector ID
     * @return Total weight (calculated from transaction items)
     */
    @Query("SELECT * FROM transactions WHERE collectorId = :collectorId")
    suspend fun getAllTransactionsForCollector(collectorId: String): List<TransactionEntity>

    /**
     * Delete a transaction
     * @param transaction The transaction to delete
     */
    @Delete
    suspend fun delete(transaction: TransactionEntity)

    /**
     * Delete a transaction by ID
     * @param transactionId The transaction ID
     * @return Number of rows deleted
     */
    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun deleteById(transactionId: String): Int

    /**
     * Clear all cached transactions
     * @return Number of rows deleted
     */
    @Query("DELETE FROM transactions")
    suspend fun clearAll(): Int

    /**
     * Delete old transactions (older than specified timestamp)
     * Useful for cache cleanup
     * @param olderThan Timestamp - transactions older than this will be deleted
     * @return Number of rows deleted
     */
    @Query("DELETE FROM transactions WHERE completedAt < :olderThan")
    suspend fun deleteOlderThan(olderThan: Long): Int
}
