package com.melodi.sampahjujur.repository

import com.melodi.sampahjujur.data.local.dao.TransactionDao
import com.melodi.sampahjujur.data.local.entity.TransactionEntity
import com.melodi.sampahjujur.model.Earnings
import com.melodi.sampahjujur.model.Transaction
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing transaction caching with Room Database.
 * Provides offline access to transaction history and faster earnings calculations.
 *
 * Benefits over pure Firebase:
 * - Works offline (view past transactions without internet)
 * - Faster queries (no network latency)
 * - Complex aggregations (calculate earnings locally)
 * - Reduced Firebase reads (lower costs)
 */
@Singleton
class TransactionCacheRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val firestore: FirebaseFirestore
) {

    companion object {
        private const val TRANSACTIONS_COLLECTION = "transactions"
    }

    /**
     * Get transactions for a collector from cache (offline-capable).
     * Returns Flow for reactive updates.
     *
     * @param collectorId The collector ID
     * @return Flow of list of transactions
     */
    fun getCachedTransactionsByCollector(collectorId: String): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByCollector(collectorId)
            .map { entities ->
                entities.map { it.toTransaction() }
            }
    }

    /**
     * Get transactions for a household from cache (offline-capable).
     *
     * @param householdId The household ID
     * @return Flow of list of transactions
     */
    fun getCachedTransactionsByHousehold(householdId: String): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByHousehold(householdId)
            .map { entities ->
                entities.map { it.toTransaction() }
            }
    }

    /**
     * Fetch transactions from Firebase and cache them in Room.
     * Call this periodically or on app start to refresh cache.
     *
     * @param collectorId The collector ID
     * @return Result indicating success or failure
     */
    suspend fun syncCollectorTransactions(collectorId: String): Result<Unit> {
        return try {
            val transactions = firestore.collection(TRANSACTIONS_COLLECTION)
                .whereEqualTo("collectorId", collectorId)
                .orderBy("completedAt", Query.Direction.DESCENDING)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(Transaction::class.java) }

            // Cache all transactions in Room
            val entities = transactions.map { TransactionEntity.fromTransaction(it) }
            transactionDao.insertAll(entities)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch household transactions from Firebase and cache them.
     *
     * @param householdId The household ID
     * @return Result indicating success or failure
     */
    suspend fun syncHouseholdTransactions(householdId: String): Result<Unit> {
        return try {
            val transactions = firestore.collection(TRANSACTIONS_COLLECTION)
                .whereEqualTo("householdId", householdId)
                .orderBy("completedAt", Query.Direction.DESCENDING)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(Transaction::class.java) }

            val entities = transactions.map { TransactionEntity.fromTransaction(it) }
            transactionDao.insertAll(entities)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Calculate collector earnings from cached transactions (offline-capable).
     * Much faster than Firebase aggregation queries.
     *
     * @param collectorId The collector ID
     * @return Earnings object with all statistics
     */
    suspend fun calculateEarnings(collectorId: String): Earnings {
        val transactions = transactionDao.getAllTransactionsForCollector(collectorId)
        val transactionModels = transactions.map { it.toTransaction() }

        val totalEarnings = transactionModels.sumOf { it.finalAmount }
        val totalTransactions = transactionModels.size
        val totalWeight = transactionModels.sumOf { it.getTotalWeight() }

        // Calculate time-based earnings
        val calendar = Calendar.getInstance()

        // Today
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayStart = calendar.timeInMillis

        val todayTransactions = transactionModels.filter { it.completedAt >= todayStart }
        val todayEarnings = todayTransactions.sumOf { it.finalAmount }

        // This week
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        val weekStart = calendar.timeInMillis

        val weekTransactions = transactionModels.filter { it.completedAt >= weekStart }
        val weekEarnings = weekTransactions.sumOf { it.finalAmount }

        // This month
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val monthStart = calendar.timeInMillis

        val monthTransactions = transactionModels.filter { it.completedAt >= monthStart }
        val monthEarnings = monthTransactions.sumOf { it.finalAmount }

        return Earnings(
            collectorId = collectorId,
            totalEarnings = totalEarnings,
            totalTransactions = totalTransactions,
            totalWasteCollected = totalWeight,
            earningsToday = todayEarnings,
            earningsThisWeek = weekEarnings,
            earningsThisMonth = monthEarnings,
            transactionHistory = transactionModels
        )
    }

    /**
     * Cache a single transaction (called when transaction is completed).
     *
     * @param transaction The transaction to cache
     */
    suspend fun cacheTransaction(transaction: Transaction) {
        val entity = TransactionEntity.fromTransaction(transaction)
        transactionDao.insert(entity)
    }

    /**
     * Get earnings for a specific time range.
     *
     * @param collectorId The collector ID
     * @param startTime Start timestamp
     * @param endTime End timestamp
     * @return Total earnings in the range
     */
    suspend fun getEarningsInRange(
        collectorId: String,
        startTime: Long,
        endTime: Long
    ): Double {
        return transactionDao.getEarningsInRange(collectorId, startTime, endTime) ?: 0.0
    }

    /**
     * Clear old transactions from cache (cleanup).
     * Call this periodically to prevent cache bloat.
     *
     * @param olderThanDays Delete transactions older than this many days
     */
    suspend fun clearOldTransactions(olderThanDays: Int = 90) {
        val cutoffTime = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L)
        transactionDao.deleteOlderThan(cutoffTime)
    }
}
