package com.melodi.sampahjujur.repository

import com.melodi.sampahjujur.model.PickupRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository class for handling all interactions with the Firestore pickup_requests collection.
 * Manages pickup request lifecycle from creation to completion.
 */
@Singleton
class WasteRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    companion object {
        private const val PICKUP_REQUESTS_COLLECTION = "pickup_requests"
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
            .orderBy("timestamp", Query.Direction.DESCENDING)
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
            .orderBy("timestamp", Query.Direction.DESCENDING)
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
                PickupRequest.STATUS_COMPLETED
            ))
            .orderBy("timestamp", Query.Direction.DESCENDING)
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
     * Accepts a pickup request by assigning it to a collector
     *
     * @param requestId ID of the pickup request to accept
     * @param collectorId ID of the collector accepting the request
     * @return Result indicating success or failure
     */
    suspend fun acceptPickupRequest(requestId: String, collectorId: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "collectorId" to collectorId,
                "status" to PickupRequest.STATUS_ACCEPTED
            )

            firestore.collection(PICKUP_REQUESTS_COLLECTION)
                .document(requestId)
                .update(updates)
                .await()

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
                .update("status", newStatus)
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
    suspend fun completeTransaction(requestId: String, finalAmount: Double): Result<Unit> {
        return try {
            // TODO: In production, this should call a Firebase Cloud Function
            // The Cloud Function would:
            // 1. Validate the request and amount
            // 2. Process the payment through a secure payment gateway
            // 3. Update the request status to completed
            // 4. Create a transaction record
            // 5. Send notifications to both parties

            // For now, we'll just update the status and amount
            val updates = mapOf(
                "status" to PickupRequest.STATUS_COMPLETED,
                "totalValue" to finalAmount
            )

            firestore.collection(PICKUP_REQUESTS_COLLECTION)
                .document(requestId)
                .update(updates)
                .await()

            // TODO: Create transaction record in separate collection
            // TODO: Send push notifications to household and collector
            // TODO: Update user statistics/ratings

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
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
}
