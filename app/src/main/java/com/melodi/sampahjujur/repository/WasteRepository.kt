package com.melodi.sampahjujur.repository

import com.melodi.sampahjujur.model.Earnings
import com.melodi.sampahjujur.model.PickupRequest
import com.melodi.sampahjujur.model.Transaction
import com.melodi.sampahjujur.model.TransactionItem
import com.melodi.sampahjujur.model.User
import com.melodi.sampahjujur.model.WasteItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository class for handling all interactions with the Firestore pickup_requests collection.
 * Manages pickup request lifecycle from creation to completion.
 */
@Singleton
class WasteRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
    private val notificationRepository: NotificationRepository
) {

    companion object {
        private const val PICKUP_REQUESTS_COLLECTION = "pickup_requests"
        private const val USERS_COLLECTION = "users"
        private const val TRANSACTIONS_COLLECTION = "transactions"
        private const val FIELD_DRAFT_WASTE_ITEMS = "draftWasteItems"
        private const val FIELD_DRAFT_LOCATION = "draftPickupLocation"
    }

    /**
     * Posts a new pickup request to Firestore
     *
     * @param pickupRequest The pickup request to be created
     * @return Result containing the created PickupRequest with generated ID or error
     */
    suspend fun postPickupRequest(pickupRequest: PickupRequest): Result<PickupRequest> {
        return try {
            val docRef = firestore.collection(PICKUP_REQUESTS_COLLECTION)
                .document()

            val requestWithId = pickupRequest.copy(id = docRef.id)

            docRef.set(requestWithId).await()

            // Notify all collectors of new request
            notificationRepository.notifyNewRequest(requestWithId.id)

            Result.success(requestWithId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets all pending pickup requests as a Flow for real-time updates
     *
     * @return Flow of list of pending pickup requests
     */
    fun getPendingRequests(): Flow<List<PickupRequest>> = callbackFlow {
        val listener = firestore.collection(PICKUP_REQUESTS_COLLECTION)
            .whereEqualTo("status", PickupRequest.STATUS_PENDING)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val requests = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(PickupRequest::class.java)
                } ?: emptyList()

                trySend(requests)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Gets pickup requests for a specific household as a Flow
     *
     * @param householdId The household user ID
     * @return Flow of list of pickup requests for the household
     */
    fun getHouseholdRequests(householdId: String): Flow<List<PickupRequest>> = callbackFlow {
        val listener = firestore.collection(PICKUP_REQUESTS_COLLECTION)
            .whereEqualTo("householdId", householdId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val requests = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(PickupRequest::class.java)
                } ?: emptyList()

                trySend(requests)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Observes a single pickup request by ID as a Flow for real-time updates
     *
     * @param requestId The pickup request ID
     * @return Flow of PickupRequest or null if not found
     */
    fun observeRequest(requestId: String): Flow<PickupRequest?> = callbackFlow {
        val listener = firestore.collection(PICKUP_REQUESTS_COLLECTION)
            .document(requestId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val request = snapshot?.toObject(PickupRequest::class.java)
                trySend(request)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Gets accepted pickup requests for a specific collector as a Flow
     *
     * @param collectorId The collector user ID
     * @return Flow of list of pickup requests accepted by the collector
     */
    fun getCollectorRequests(collectorId: String): Flow<List<PickupRequest>> = callbackFlow {
        val listener = firestore.collection(PICKUP_REQUESTS_COLLECTION)
            .whereEqualTo("collectorId", collectorId)
            .whereIn("status", listOf(
                PickupRequest.STATUS_ACCEPTED,
                PickupRequest.STATUS_IN_PROGRESS,
                PickupRequest.STATUS_COMPLETED,
                PickupRequest.STATUS_CANCELLED
            ))
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val requests = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(PickupRequest::class.java)
                } ?: emptyList()

                trySend(requests)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Observes a single pickup request document for real-time changes.
     */
    fun watchPickupRequest(requestId: String): Flow<PickupRequest?> = callbackFlow {
        val listener = firestore.collection(PICKUP_REQUESTS_COLLECTION)
            .document(requestId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val request = snapshot?.toObject(PickupRequest::class.java)
                trySend(request)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Observes a user document for real-time updates.
     */
    fun observeUser(userId: String): Flow<User?> = callbackFlow {
        if (userId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val user = snapshot?.toObject(User::class.java)
                trySend(user)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Accepts a pickup request by assigning it to a collector
     *
     * @param requestId ID of the pickup request to accept
     * @param collectorId ID of the collector accepting the request
     * @return Result indicating success or failure
     */
    suspend fun acceptPickupRequest(requestId: String, collectorId: String): Result<Unit> {
        return try {
            // First, update the request status
            firestore.runTransaction { transaction ->
                val requestRef = firestore.collection(PICKUP_REQUESTS_COLLECTION).document(requestId)
                val snapshot = transaction.get(requestRef)

                if (!snapshot.exists()) {
                    throw IllegalStateException("Request no longer exists")
                }

                val status = snapshot.getString("status")
                val assignedCollector = snapshot.getString("collectorId")

                if (status != PickupRequest.STATUS_PENDING || !assignedCollector.isNullOrBlank()) {
                    throw IllegalStateException("Request is no longer available")
                }

                transaction.update(
                    requestRef,
                    mapOf(
                        "collectorId" to collectorId,
                        "status" to PickupRequest.STATUS_ACCEPTED,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
            }.await()

            // After successful acceptance, create a chat
            try {
                // Get request details to extract household info
                val requestDoc = firestore.collection(PICKUP_REQUESTS_COLLECTION)
                    .document(requestId)
                    .get()
                    .await()

                val request = requestDoc.toObject(PickupRequest::class.java)

                if (request != null) {
                    // Get household and collector user details
                    val household = authRepository.getUserById(request.householdId)
                    val collector = authRepository.getUserById(collectorId)

                    if (household != null && collector != null) {
                        // Create chat for this request
                        val chatResult = chatRepository.createChat(
                            requestId = requestId,
                            householdId = request.householdId,
                            householdName = household.fullName,
                            collectorId = collectorId,
                            collectorName = collector.fullName
                        )

                        chatResult.onSuccess { chatId ->
                            android.util.Log.d("WasteRepository", "Successfully created chat $chatId for request $requestId")
                        }.onFailure { error ->
                            android.util.Log.e("WasteRepository", "Failed to create chat for request $requestId: ${error.message}", error)
                        }
                    } else {
                        android.util.Log.e("WasteRepository", "Could not create chat: household=$household, collector=$collector")
                    }
                } else {
                    android.util.Log.e("WasteRepository", "Could not create chat: request is null")
                }
            } catch (e: Exception) {
                // Log error but don't fail the acceptance
                android.util.Log.e("WasteRepository", "Exception while creating chat for request $requestId", e)
            }

            // Notify household that request was accepted
            notificationRepository.notifyStatusChange(requestId, PickupRequest.STATUS_ACCEPTED)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Marks an accepted pickup request as in progress, ensuring only the assigned collector can start it.
     *
     * @param requestId ID of the pickup request to update
     * @param collectorId ID of the collector attempting to start the pickup
     * @return Result indicating success or failure
     */
    suspend fun markRequestInProgress(requestId: String, collectorId: String): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val requestRef = firestore.collection(PICKUP_REQUESTS_COLLECTION).document(requestId)
                val snapshot = transaction.get(requestRef)

                if (!snapshot.exists()) {
                    throw IllegalStateException("Request no longer exists")
                }

                val request = snapshot.toObject(PickupRequest::class.java)
                    ?: throw IllegalStateException("Unable to read request data")

                if (request.collectorId.isNullOrBlank() || request.collectorId != collectorId) {
                    throw IllegalStateException("Request is assigned to a different collector")
                }

                when (request.status) {
                    PickupRequest.STATUS_ACCEPTED -> {
                        transaction.update(
                            requestRef,
                            mapOf(
                                "status" to PickupRequest.STATUS_IN_PROGRESS,
                                "updatedAt" to System.currentTimeMillis()
                            )
                        )
                    }
                    PickupRequest.STATUS_IN_PROGRESS -> {
                        // Already in progress - nothing to change
                    }
                    else -> {
                        throw IllegalStateException("Request cannot be started from its current status")
                    }
                }

                Unit
            }.await()

            // Notify household that request is in progress
            notificationRepository.notifyStatusChange(requestId, PickupRequest.STATUS_IN_PROGRESS)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cancels a pickup request that has been accepted by the current collector.
     *
     * @param requestId ID of the pickup request to cancel
     * @param collectorId ID of the collector performing the cancellation
     */
    suspend fun cancelCollectorRequest(requestId: String, collectorId: String): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val requestRef = firestore.collection(PICKUP_REQUESTS_COLLECTION).document(requestId)
                val snapshot = transaction.get(requestRef)

                if (!snapshot.exists()) {
                    throw IllegalStateException("Request no longer exists")
                }

                val request = snapshot.toObject(PickupRequest::class.java)
                    ?: throw IllegalStateException("Unable to read request data")

                if (request.collectorId.isNullOrBlank() || request.collectorId != collectorId) {
                    throw IllegalStateException("Request is assigned to a different collector")
                }

                if (request.status !in listOf(
                        PickupRequest.STATUS_ACCEPTED,
                        PickupRequest.STATUS_IN_PROGRESS
                    )
                ) {
                    throw IllegalStateException("Request cannot be cancelled from its current status")
                }

                transaction.update(
                    requestRef,
                    mapOf(
                        "status" to PickupRequest.STATUS_CANCELLED,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
            }.await()

            // Notify household that request was cancelled
            notificationRepository.notifyStatusChange(requestId, PickupRequest.STATUS_CANCELLED)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates the status of a pickup request
     *
     * @param requestId ID of the pickup request
     * @param newStatus New status to set
     * @return Result indicating success or failure
     */
    suspend fun updateRequestStatus(requestId: String, newStatus: String): Result<Unit> {
        return try {
            firestore.collection(PICKUP_REQUESTS_COLLECTION)
                .document(requestId)
                .update(
                    mapOf(
                        "status" to newStatus,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Completes a transaction for a pickup request
     * This is a placeholder function that would integrate with a payment system
     * In a real implementation, this would call a Firebase Cloud Function for security
     *
     * @param requestId ID of the pickup request
     * @param finalAmount Final amount paid to the household
     * @return Result indicating success or failure
     */
    suspend fun completeTransaction(
        requestId: String,
        collectorId: String,
        finalAmount: Double,
        actualWasteItems: List<TransactionItem> = emptyList(),
        paymentMethod: String = Transaction.PAYMENT_CASH,
        notes: String = ""
    ): Result<Transaction> {
        return try {
            val completedAt = System.currentTimeMillis()
            val transactionId = firestore.collection(TRANSACTIONS_COLLECTION).document().id

            val transactionRecord = firestore.runTransaction { transaction ->
                val requestRef = firestore.collection(PICKUP_REQUESTS_COLLECTION).document(requestId)
                val snapshot = transaction.get(requestRef)

                if (!snapshot.exists()) {
                    throw IllegalStateException("Pickup request not found")
                }

                val request = snapshot.toObject(PickupRequest::class.java)
                    ?: throw IllegalStateException("Unable to read request data")

                if (request.collectorId.isNullOrBlank() || request.collectorId != collectorId) {
                    throw IllegalStateException("Request is assigned to a different collector")
                }

                if (request.status != PickupRequest.STATUS_IN_PROGRESS &&
                    request.status != PickupRequest.STATUS_ACCEPTED
                ) {
                    throw IllegalStateException("Request cannot be completed from its current status")
                }

                val transactionData = Transaction(
                    id = transactionId,
                    requestId = requestId,
                    householdId = request.householdId,
                    collectorId = collectorId,
                    estimatedWasteItems = request.wasteItems,
                    actualWasteItems = actualWasteItems,
                    estimatedValue = request.totalValue,
                    finalAmount = finalAmount,
                    paymentMethod = paymentMethod,
                    paymentStatus = Transaction.STATUS_COMPLETED,
                    location = request.pickupLocation,
                    completedAt = completedAt,
                    notes = notes
                )

                transaction.update(
                    requestRef,
                    mapOf(
                        "status" to PickupRequest.STATUS_COMPLETED,
                        "totalValue" to finalAmount,
                        "updatedAt" to completedAt
                    )
                )

                val transactionRef = firestore.collection(TRANSACTIONS_COLLECTION).document(transactionId)
                transaction.set(transactionRef, transactionData)

                transactionData
            }.await()

            // Notify household that request was completed
            notificationRepository.notifyStatusChange(requestId, PickupRequest.STATUS_COMPLETED)

            Result.success(transactionRecord)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observes collector earnings in real-time based on completed transactions.
     */
    fun getCollectorEarnings(collectorId: String): Flow<Earnings> = callbackFlow {
        if (collectorId.isBlank()) {
            trySend(Earnings(collectorId = collectorId))
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(TRANSACTIONS_COLLECTION)
            .whereEqualTo("collectorId", collectorId)
            .whereEqualTo("paymentStatus", Transaction.STATUS_COMPLETED)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val transactions = snapshot?.documents
                    ?.mapNotNull { it.toObject(Transaction::class.java) }
                    ?.sortedByDescending { it.completedAt }
                    ?: emptyList()

                val currentTime = System.currentTimeMillis()
                val earnings = Earnings(
                    collectorId = collectorId,
                    totalEarnings = transactions.sumOf { it.finalAmount },
                    totalTransactions = transactions.size,
                    totalWasteCollected = transactions.sumOf { it.getTotalWeight() },
                    earningsToday = transactions
                        .filter { it.completedAt >= startOfDayMillis(currentTime) }
                        .sumOf { it.finalAmount },
                    earningsThisWeek = transactions
                        .filter { it.completedAt >= startOfWeekMillis(currentTime) }
                        .sumOf { it.finalAmount },
                    earningsThisMonth = transactions
                        .filter { it.completedAt >= startOfMonthMillis(currentTime) }
                        .sumOf { it.finalAmount },
                    transactionHistory = transactions
                )

                trySend(earnings)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Cancels a pickup request (only if it's still pending)
     *
     * @param requestId ID of the pickup request to cancel
     * @param userId ID of the user requesting cancellation (must be the household owner)
     * @return Result indicating success or failure
     */
    suspend fun cancelPickupRequest(requestId: String, userId: String): Result<Unit> {
        return try {
            val doc = firestore.collection(PICKUP_REQUESTS_COLLECTION)
                .document(requestId)
                .get()
                .await()

            val request = doc.toObject(PickupRequest::class.java)
                ?: throw Exception("Pickup request not found")

            if (request.householdId != userId) {
                throw Exception("Only the request owner can cancel")
            }

            if (!request.isPending()) {
                throw Exception("Can only cancel pending requests")
            }

            firestore.collection(PICKUP_REQUESTS_COLLECTION)
                .document(requestId)
                .update("status", PickupRequest.STATUS_CANCELLED)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun listenToHouseholdWasteItems(householdId: String): Flow<List<WasteItem>> = callbackFlow {
        val listener = firestore.collection(USERS_COLLECTION)
            .document(householdId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val items = snapshot?.toObject(User::class.java)?.draftWasteItems.orEmpty()
                val normalizedItems = mutableListOf<WasteItem>()

                items.forEach { item ->
                    if (item.id.isNotBlank()) {
                        if (normalizedItems.none { it.id == item.id }) {
                            normalizedItems.add(item)
                        }
                    } else {
                        normalizedItems.add(item)
                    }
                }

                val sortedItems = normalizedItems.sortedByDescending { it.createdAt }
                trySend(sortedItems)
            }

        awaitClose { listener.remove() }
    }

    suspend fun addWasteItem(householdId: String, wasteItem: WasteItem): Result<WasteItem> {
        return try {
            val resultItem = firestore.runTransaction { transaction ->
                val userRef = firestore.collection(USERS_COLLECTION).document(householdId)
                val snapshot = transaction.get(userRef)
                val existing = snapshot.toObject(User::class.java)?.draftWasteItems ?: emptyList()

                val createdAt = if (wasteItem.createdAt == 0L) {
                    System.currentTimeMillis()
                } else {
                    wasteItem.createdAt
                }

                val itemWithId = wasteItem.copy(
                    id = wasteItem.id.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString(),
                    createdAt = createdAt
                )

                val updatedItems = existing.filterNot { it.id == itemWithId.id } + itemWithId

                if (snapshot.exists()) {
                    transaction.update(userRef, FIELD_DRAFT_WASTE_ITEMS, updatedItems)
                } else {
                    transaction.set(
                        userRef,
                        mapOf(FIELD_DRAFT_WASTE_ITEMS to updatedItems),
                        SetOptions.merge()
                    )
                }

                itemWithId
            }.await()

            Result.success(resultItem)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteWasteItem(householdId: String, wasteItemId: String): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val userRef = firestore.collection(USERS_COLLECTION).document(householdId)
                val snapshot = transaction.get(userRef)
                if (!snapshot.exists()) {
                    return@runTransaction Unit
                }

                val existing = snapshot.toObject(User::class.java)?.draftWasteItems ?: emptyList()
                val updatedItems = existing.filterNot { it.id == wasteItemId }

                if (updatedItems.size == existing.size) {
                    return@runTransaction Unit
                }

                transaction.update(userRef, FIELD_DRAFT_WASTE_ITEMS, updatedItems)
                Unit
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearWasteItems(householdId: String): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val userRef = firestore.collection(USERS_COLLECTION).document(householdId)
                val snapshot = transaction.get(userRef)

                if (!snapshot.exists()) {
                    transaction.set(
                        userRef,
                        mapOf(FIELD_DRAFT_WASTE_ITEMS to emptyList<WasteItem>()),
                        SetOptions.merge()
                    )
                } else {
                    transaction.update(userRef, FIELD_DRAFT_WASTE_ITEMS, emptyList<WasteItem>())
                }

                Unit
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Saves draft pickup location to the user's document
     *
     * @param householdId The household user ID
     * @param location The location data to save (latitude, longitude, address)
     * @return Result indicating success or failure
     */
    suspend fun saveDraftPickupLocation(
        householdId: String,
        location: Map<String, Any>
    ): Result<Unit> {
        return try {
            val userRef = firestore.collection(USERS_COLLECTION).document(householdId)

            userRef.set(
                mapOf(FIELD_DRAFT_LOCATION to location),
                SetOptions.merge()
            ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets the saved draft pickup location from the user's document
     *
     * @param householdId The household user ID
     * @return Result containing the location map or null if not saved
     */
    suspend fun getDraftPickupLocation(householdId: String): Result<Map<String, Any>?> {
        return try {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .document(householdId)
                .get()
                .await()

            @Suppress("UNCHECKED_CAST")
            val location = snapshot.get(FIELD_DRAFT_LOCATION) as? Map<String, Any>

            Result.success(location)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Clears the draft pickup location from the user's document
     *
     * @param householdId The household user ID
     * @return Result indicating success or failure
     */
    suspend fun clearDraftPickupLocation(householdId: String): Result<Unit> {
        return try {
            val userRef = firestore.collection(USERS_COLLECTION).document(householdId)
            val snapshot = userRef.get().await()

            if (snapshot.exists()) {
                userRef.update(FIELD_DRAFT_LOCATION, null).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun startOfDayMillis(reference: Long): Long = Calendar.getInstance().apply {
        timeInMillis = reference
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun startOfWeekMillis(reference: Long): Long = Calendar.getInstance().apply {
        timeInMillis = reference
        set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun startOfMonthMillis(reference: Long): Long = Calendar.getInstance().apply {
        timeInMillis = reference
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

}
