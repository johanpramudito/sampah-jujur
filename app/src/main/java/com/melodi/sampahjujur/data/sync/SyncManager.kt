package com.melodi.sampahjujur.data.sync

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.melodi.sampahjujur.data.local.dao.PickupRequestDao
import com.melodi.sampahjujur.data.local.dao.WasteItemDao
import com.melodi.sampahjujur.data.local.entity.WasteItemEntity
import com.melodi.sampahjujur.repository.AuthRepository
import com.melodi.sampahjujur.utils.CloudinaryUploadService
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
    private val pickupRequestDao: PickupRequestDao,
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository
) {
    private val tag = "SyncManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val sharedPrefs: SharedPreferences = context.getSharedPreferences("image_cache", Context.MODE_PRIVATE)

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
     * This is called when network becomes available
     */
    suspend fun syncAllPendingData() {
        if (!isOnline()) {
            Log.d(tag, "Device offline - skipping sync")
            return
        }

        Log.d(tag, "Starting sync of all pending data")

        try {
            // Get current user
            val currentUser = authRepository.getCurrentUser()
            if (currentUser == null) {
                Log.d(tag, "No current user - skipping sync")
                return
            }

            // 1. Sync waste items with image retry logic
            syncWasteItemsWithImageRetry(currentUser.id)

            // 2. Sync pickup requests
            syncPickupRequests(currentUser.id)

            Log.d(tag, "Completed sync of all pending data")
        } catch (e: Exception) {
            Log.e(tag, "Error during syncAllPendingData", e)
        }
    }

    /**
     * Sync waste items with retry logic for failed image uploads
     * Detects items with "pending_upload:*" format and retries the upload
     */
    suspend fun syncWasteItemsWithImageRetry(householdId: String): Result<Unit> {
        if (!isOnline()) {
            return Result.failure(Exception("Device is offline"))
        }

        return try {
            // Get all unsynced waste items
            val unsyncedItems = wasteItemDao.getUnsyncedItems(householdId)

            if (unsyncedItems.isEmpty()) {
                Log.d(tag, "No items to sync for household: $householdId")
                return Result.success(Unit)
            }

            Log.d(tag, "Syncing ${unsyncedItems.size} waste items with image retry")

            // Process each item
            for (entity in unsyncedItems) {
                try {
                    var updatedEntity = entity

                    // Check if image URL indicates pending upload
                    if (entity.imageUrl.startsWith("pending_upload:")) {
                        val tempId = entity.imageUrl.removePrefix("pending_upload:")
                        val cachedUriString = sharedPrefs.getString("image_$tempId", null)

                        if (cachedUriString != null) {
                            Log.d(tag, "Retrying image upload for item ${entity.id}")
                            val imageUri = Uri.parse(cachedUriString)

                            // Ensure Cloudinary is initialized
                            CloudinaryUploadService.initialize(context)

                            // Retry upload
                            val uploadResult = CloudinaryUploadService.uploadImage(
                                context = context,
                                imageUri = imageUri,
                                folder = "sampah-jujur/waste-items"
                            )

                            if (uploadResult != null) {
                                val imageUrl = uploadResult
                                // Update entity with real URL
                                updatedEntity = entity.copy(imageUrl = imageUrl)
                                wasteItemDao.update(updatedEntity)

                                // Clear cached URI
                                sharedPrefs.edit().remove("image_$tempId").apply()
                                Log.d(tag, "Successfully uploaded image for item ${entity.id}")
                            } else {
                                Log.e(tag, "Failed to upload image for item ${entity.id} - uploadResult was null")
                                // Continue with other items even if this one fails
                            }
                        } else {
                            Log.w(tag, "No cached URI found for item ${entity.id}, keeping pending status")
                        }
                    }

                    // Sync the item to Firebase (with real or pending URL)
                    syncSingleItem(updatedEntity, householdId)
                } catch (e: Exception) {
                    Log.e(tag, "Failed to process item ${entity.id}", e)
                    // Continue with other items
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to sync waste items with retry", e)
            Result.failure(e)
        }
    }

    /**
     * Sync unsynced pickup requests to Firebase
     */
    private suspend fun syncPickupRequests(householdId: String) {
        try {
            val unsyncedRequests = pickupRequestDao.getUnsyncedRequests()

            if (unsyncedRequests.isEmpty()) {
                Log.d(tag, "No pickup requests to sync")
                return
            }

            Log.d(tag, "Syncing ${unsyncedRequests.size} pickup requests")

            for (entity in unsyncedRequests) {
                try {
                    val request = entity.toPickupRequest()
                    val docRef = firestore.collection("pickup_requests").document(entity.id)
                    docRef.set(request).await()

                    // Mark as synced
                    pickupRequestDao.markAsSynced(entity.id)
                    Log.d(tag, "Successfully synced pickup request ${entity.id}")
                } catch (e: Exception) {
                    Log.e(tag, "Failed to sync pickup request ${entity.id}", e)
                    // Continue with other requests
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error syncing pickup requests", e)
        }
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
