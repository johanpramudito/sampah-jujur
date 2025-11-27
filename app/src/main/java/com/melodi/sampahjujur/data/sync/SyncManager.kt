package com.melodi.sampahjujur.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.melodi.sampahjujur.data.local.dao.WasteItemDao
import com.melodi.sampahjujur.data.local.entity.WasteItemEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages synchronization between Room database and Firebase Firestore.
 * Handles:
 * - Network connectivity monitoring
 * - Automatic sync when connection is restored
 * - Syncing draft waste items to Firebase
 *
 * This is a simplified sync manager for the 1-week quick implementation.
 * For production, consider using WorkManager for more robust background sync.
 */
@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wasteItemDao: WasteItemDao,
    private val firestore: FirebaseFirestore
) {
    private val tag = "SyncManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.d(tag, "Network available - triggering sync")
            scope.launch {
                syncAllPendingData()
            }
        }

        override fun onLost(network: Network) {
            Log.d(tag, "Network lost")
        }
    }

    init {
        registerNetworkCallback()
    }

    /**
     * Registers network callback to monitor connectivity changes
     */
    private fun registerNetworkCallback() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
            Log.d(tag, "Network callback registered")
        } catch (e: Exception) {
            Log.e(tag, "Failed to register network callback", e)
        }
    }

    /**
     * Checks if device is currently online
     */
    fun isOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Manually trigger sync of all pending data
     */
    suspend fun syncAllPendingData() {
        if (!isOnline()) {
            Log.d(tag, "Device offline - skipping sync")
            return
        }

        Log.d(tag, "Starting sync of all pending data")
        // Note: We'll add household ID parameter when integrating with repositories
        // For now, this is a placeholder for the sync framework
    }

    /**
     * Sync waste items for a specific household to Firebase
     * @param householdId The household ID
     * @return Result indicating success or failure
     */
    suspend fun syncWasteItems(householdId: String): Result<Unit> {
        if (!isOnline()) {
            return Result.failure(Exception("Device is offline"))
        }

        return try {
            // Get all unsynced waste items for this household
            val unsyncedItems = wasteItemDao.getUnsyncedItems(householdId)

            if (unsyncedItems.isEmpty()) {
                Log.d(tag, "No items to sync for household: $householdId")
                return Result.success(Unit)
            }

            Log.d(tag, "Syncing ${unsyncedItems.size} waste items for household: $householdId")

            // Get current waste items from Firebase
            val userRef = firestore.collection("users").document(householdId)
            val snapshot = userRef.get().await()

            @Suppress("UNCHECKED_CAST")
            val currentItems = snapshot.get("draftWasteItems") as? List<Map<String, Any>> ?: emptyList()

            // Convert Room entities to Firebase map format
            val newItems = unsyncedItems.map { entity ->
                mapOf(
                    "id" to entity.id,
                    "type" to entity.type,
                    "weight" to entity.weight,
                    "estimatedValue" to entity.estimatedValue,
                    "description" to entity.description,
                    "imageUrl" to entity.imageUrl,
                    "createdAt" to entity.createdAt
                )
            }

            // Merge with existing items (avoiding duplicates)
            val existingIds = currentItems.mapNotNull { it["id"] as? String }.toSet()
            val itemsToAdd = newItems.filter { (it["id"] as String) !in existingIds }
            val mergedItems = currentItems + itemsToAdd

            // Update Firebase with merged items
            userRef.update("draftWasteItems", mergedItems).await()

            // Mark items as synced in Room
            val syncedIds = unsyncedItems.map { it.id }
            wasteItemDao.markMultipleAsSynced(syncedIds)

            Log.d(tag, "Successfully synced ${unsyncedItems.size} items")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to sync waste items", e)
            Result.failure(e)
        }
    }

    /**
     * Force sync for a specific waste item
     * @param wasteItem The waste item entity to sync
     * @param householdId The household ID
     */
    suspend fun syncSingleItem(wasteItem: WasteItemEntity, householdId: String): Result<Unit> {
        if (!isOnline()) {
            return Result.failure(Exception("Device is offline"))
        }

        return try {
            val userRef = firestore.collection("users").document(householdId)

            // Get current items
            val snapshot = userRef.get().await()
            @Suppress("UNCHECKED_CAST")
            val currentItems = snapshot.get("draftWasteItems") as? MutableList<Map<String, Any>>
                ?: mutableListOf()

            // Add the new item
            val newItem = mapOf(
                "id" to wasteItem.id,
                "type" to wasteItem.type,
                "weight" to wasteItem.weight,
                "estimatedValue" to wasteItem.estimatedValue,
                "description" to wasteItem.description,
                "imageUrl" to wasteItem.imageUrl,
                "createdAt" to wasteItem.createdAt
            )

            // Check if item already exists (update) or is new (add)
            val existingIndex = currentItems.indexOfFirst { (it["id"] as? String) == wasteItem.id }
            if (existingIndex >= 0) {
                currentItems[existingIndex] = newItem
            } else {
                currentItems.add(newItem)
            }

            // Update Firebase
            userRef.update("draftWasteItems", currentItems).await()

            // Mark as synced
            wasteItemDao.markAsSynced(wasteItem.id)

            Log.d(tag, "Successfully synced single item: ${wasteItem.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to sync single item", e)
            Result.failure(e)
        }
    }

    /**
     * Cleanup - unregister network callback
     * Call this when the app is being destroyed (if needed)
     */
    fun cleanup() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            Log.d(tag, "Network callback unregistered")
        } catch (e: Exception) {
            Log.e(tag, "Failed to unregister network callback", e)
        }
    }
}
