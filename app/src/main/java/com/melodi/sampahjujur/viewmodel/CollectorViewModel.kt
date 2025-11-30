package com.melodi.sampahjujur.viewmodel

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melodi.sampahjujur.model.Earnings
import com.melodi.sampahjujur.model.PickupRequest
import com.melodi.sampahjujur.model.Transaction
import com.melodi.sampahjujur.model.TransactionItem
import com.melodi.sampahjujur.model.User
import com.melodi.sampahjujur.repository.AuthRepository
import com.melodi.sampahjujur.repository.LocationRepository
import com.melodi.sampahjujur.repository.LocationTrackingRepository
import com.melodi.sampahjujur.repository.TransactionCacheRepository
import com.melodi.sampahjujur.repository.WasteRepository
import com.melodi.sampahjujur.service.LocationTrackingService
// Removed CollectorNotificationHelper - using FCM instead
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import javax.inject.Inject

data class CollectorPerformanceMetrics(
    val totalCompleted: Int = 0,
    val totalInProgress: Int = 0,
    val totalAccepted: Int = 0,
    val totalCancelled: Int = 0,
    val activePickups: Int = 0,
    val completionRate: Double = 0.0,
    val cancellationRate: Double = 0.0,
    val totalTransactions: Int = 0,
    val totalSpent: Double = 0.0,
    val totalWasteKg: Double = 0.0,
    val averagePerPickup: Double = 0.0,
    val averagePerKg: Double = 0.0,
    val spentToday: Double = 0.0,
    val spentThisWeek: Double = 0.0,
    val spentThisMonth: Double = 0.0,
    val recentTransactions: List<Transaction> = emptyList()
)

/**
 * ViewModel for the collector user interface.
 * Handles pending request monitoring and request acceptance logic.
 */
@HiltViewModel
class CollectorViewModel @Inject constructor(
    private val wasteRepository: WasteRepository,
    private val authRepository: AuthRepository,
    private val locationRepository: LocationRepository,
    private val transactionCacheRepository: TransactionCacheRepository,
    private val locationTrackingRepository: LocationTrackingRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(CollectorUiState())
    val uiState: StateFlow<CollectorUiState> = _uiState.asStateFlow()

    private val _earningsState = MutableStateFlow(Earnings())
    val earnings: StateFlow<Earnings> = _earningsState.asStateFlow()

    private val _mapState = MutableStateFlow(CollectorMapState())
    val mapState: StateFlow<CollectorMapState> = _mapState.asStateFlow()

    private val _performanceMetrics = MutableStateFlow(CollectorPerformanceMetrics())
    val performanceMetrics: StateFlow<CollectorPerformanceMetrics> = _performanceMetrics.asStateFlow()

    private val _pendingRequests = MutableLiveData<List<PickupRequest>>()
    val pendingRequests: LiveData<List<PickupRequest>> = _pendingRequests

    private val _myRequests = MutableLiveData<List<PickupRequest>>()
    val myRequests: LiveData<List<PickupRequest>> = _myRequests

    private val _acceptRequestResult = MutableLiveData<Result<Unit>?>()
    val acceptRequestResult: LiveData<Result<Unit>?> = _acceptRequestResult

    private val knownPendingRequestIds = mutableSetOf<String>()
    private var pendingNotificationInitialized = false

    init {
        startListeningToPendingRequests()
        loadMyRequests()
        loadCollectorEarnings()
        refreshCollectorLocation()
    }

    /**
     * Starts listening to pending pickup requests for real-time updates
     */
    private fun startListeningToPendingRequests() {
        viewModelScope.launch {
            wasteRepository.getPendingRequests()
                .catch { throwable ->
                    _pendingRequests.value = emptyList()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        hasPendingRequests = false,
                        filteredRequests = emptyList(),
                        errorMessage = throwable.message
                    )
                    _mapState.value = _mapState.value.copy(pendingMarkers = emptyList())
                }
                .collect { requests ->
                    val incomingIds = requests.map { it.id }.toSet()

                    // FCM notifications are now sent from the server when requests are created
                    // No need for local notification logic here
                    knownPendingRequestIds
                        .apply { clear() }
                        .addAll(incomingIds)

                    val currentState = _uiState.value
                    val filtered = applyPendingRequestFilters(
                        requests = requests,
                        query = currentState.searchQuery,
                        sortBy = currentState.sortBy
                    )
                    _pendingRequests.value = requests
                    _uiState.value = currentState.copy(
                        isLoading = false,
                        hasPendingRequests = requests.isNotEmpty(),
                        filteredRequests = filtered,
                        errorMessage = null
                    )
                    updateMapMarkers(requests)
                }
        }
    }

    /**
     * Loads the collector's accepted requests
     */
    private fun loadMyRequests() {
        viewModelScope.launch {
            val currentUser = authRepository.getCurrentUser()
            if (currentUser?.isCollector() == true) {
                wasteRepository.getCollectorRequests(currentUser.id)
                    .catch {
                        _myRequests.value = emptyList()
                        updatePerformanceMetrics(emptyList(), _earningsState.value)
                    }
                    .collect { requests ->
                        _myRequests.value = requests
                        updatePerformanceMetrics(requests, _earningsState.value)
                    }
            } else {
                _myRequests.value = emptyList()
                updatePerformanceMetrics(emptyList(), _earningsState.value)
            }
        }
    }

    private fun loadCollectorEarnings() {
        viewModelScope.launch {
            val currentUser = authRepository.getCurrentUser()
            if (currentUser?.isCollector() == true) {
                // Use cached transactions from Room for offline support
                transactionCacheRepository.getCachedTransactionsByCollector(currentUser.id)
                    .catch {
                        _earningsState.value = Earnings()
                        updatePerformanceMetrics(_myRequests.value ?: emptyList(), Earnings())
                    }
                    .collect { cachedTransactions ->
                        // Convert cached transactions to Earnings model
                        val earnings = calculateEarningsFromTransactions(cachedTransactions)
                        _earningsState.value = earnings
                        updatePerformanceMetrics(_myRequests.value ?: emptyList(), earnings)
                    }
            } else {
                _earningsState.value = Earnings()
                updatePerformanceMetrics(_myRequests.value ?: emptyList(), Earnings())
            }
        }
    }

    private fun calculateEarningsFromTransactions(
        transactions: List<Transaction>
    ): Earnings {
        if (transactions.isEmpty()) return Earnings()

        val now = System.currentTimeMillis()
        val totalSpent = transactions.sumOf { it.finalAmount }
        val totalWasteKg = transactions.sumOf { it.getTotalWeight() }
        val spentToday = transactions.filter { it.completedAt >= now - 24 * 60 * 60 * 1000 }.sumOf { it.finalAmount }
        val spentThisWeek = transactions.filter { it.completedAt >= now - 7 * 24 * 60 * 60 * 1000 }.sumOf { it.finalAmount }
        val spentThisMonth = transactions.filter { it.completedAt >= now - 30 * 24 * 60 * 60 * 1000 }.sumOf { it.finalAmount }

        return Earnings(
            totalTransactions = transactions.size,
            totalSpent = totalSpent,
            totalWasteCollected = totalWasteKg,
            spentToday = spentToday,
            spentThisWeek = spentThisWeek,
            spentThisMonth = spentThisMonth,
            transactionHistory = transactions
        )
    }

    /**
     * Accepts a pickup request and assigns it to the current collector
     *
     * @param request The pickup request to accept
     */
    fun acceptPickupRequest(request: PickupRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val currentUser = authRepository.getCurrentUser()
            if (currentUser == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "User not authenticated"
                )
                return@launch
            }

            if (!currentUser.isCollector()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Invalid user role"
                )
                return@launch
            }

            if (!request.isPending()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "This request is no longer available"
                )
                return@launch
            }

            val result = wasteRepository.acceptPickupRequest(request.id, currentUser.id)

            _acceptRequestResult.value = result

            val currentState = _uiState.value

            _uiState.value = if (result.isSuccess) {
                val updatedPending = (_pendingRequests.value ?: emptyList())
                    .filterNot { it.id == request.id }
                _pendingRequests.value = updatedPending
                updateMapMarkers(updatedPending)

                val filtered = applyPendingRequestFilters(
                    requests = updatedPending,
                    query = currentState.searchQuery,
                    sortBy = currentState.sortBy
                )

                currentState.copy(
                    isLoading = false,
                    successMessage = "Request accepted successfully",
                    errorMessage = null,
                    filteredRequests = filtered
                )
            } else {
                currentState.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to accept request"
                )
            }
        }
    }

    /**
     * Marks a request as in progress (collector is on the way)
     *
     * @param requestId ID of the request to update
     */
    fun markRequestInProgress(requestId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val currentUser = authRepository.getCurrentUser()
            if (currentUser == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "User not authenticated"
                )
                return@launch
            }

            if (!currentUser.isCollector()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Invalid user role"
                )
                return@launch
            }

            val result = wasteRepository.markRequestInProgress(requestId, currentUser.id)

            val currentState = _uiState.value

            _uiState.value = if (result.isSuccess) {
                // Start location tracking service
                startLocationTrackingService(requestId, currentUser.id)

                currentState.copy(
                    isLoading = false,
                    successMessage = "Pickup started - Location tracking enabled"
                )
            } else {
                currentState.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to update status"
                )
            }
        }
    }

    /**
     * Completes a pickup request and processes the transaction
     *
     * @param requestId ID of the request to complete
     * @param finalAmount Final amount to pay to the household
     */
    fun completePickupRequest(
        requestId: String,
        finalAmount: Double,
        actualWasteItems: List<TransactionItem> = emptyList(),
        paymentMethod: String = Transaction.PAYMENT_CASH,
        notes: String = ""
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val currentUser = authRepository.getCurrentUser()
            if (currentUser == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "User not authenticated"
                )
                return@launch
            }

            if (!currentUser.isCollector()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Invalid user role"
                )
                return@launch
            }

            val result = wasteRepository.completeTransaction(
                requestId = requestId,
                collectorId = currentUser.id,
                finalAmount = finalAmount,
                actualWasteItems = actualWasteItems,
                paymentMethod = paymentMethod,
                notes = notes
            )

            val currentState = _uiState.value

            _uiState.value = if (result.isSuccess) {
                // Stop location tracking (service will also auto-stop via Firestore listener)
                stopLocationTrackingService()

                // Cache the completed transaction to Room database
                result.getOrNull()?.let { transaction ->
                    viewModelScope.launch {
                        transactionCacheRepository.cacheTransaction(transaction)
                    }
                }

                currentState.copy(
                    isLoading = false,
                    showTransactionSuccess = true,
                    completedTransaction = result.getOrNull(),
                    successMessage = "Transaction completed"
                )
            } else {
                currentState.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to complete transaction"
                )
            }
        }
    }

    /**
     * Cancels a collector's accepted request
     */
    fun cancelCollectorRequest(requestId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val currentUser = authRepository.getCurrentUser()
            if (currentUser == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "User not authenticated"
                )
                return@launch
            }

            if (!currentUser.isCollector()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Invalid user role"
                )
                return@launch
            }

            val result = wasteRepository.cancelCollectorRequest(requestId, currentUser.id)
            val currentState = _uiState.value

            _uiState.value = if (result.isSuccess) {
                // Stop location tracking (service will also auto-stop via Firestore listener)
                stopLocationTrackingService()

                currentState.copy(
                    isLoading = false,
                    successMessage = "Request cancelled"
                )
            } else {
                currentState.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to cancel request"
                )
            }
        }
    }

    /**
     * Observes a pickup request by ID for detail screens.
     */
    fun observeRequest(requestId: String): Flow<PickupRequest?> {
        return wasteRepository.watchPickupRequest(requestId)
    }

    fun observeUser(userId: String): Flow<User?> {
        if (userId.isBlank()) return flowOf(null)
        return wasteRepository.observeUser(userId)
    }

    /**
     * Filters pending requests based on search criteria
     *
     * @param query Search query (can be address, waste type, etc.)
     */
    fun filterPendingRequests(query: String) {
        val allRequests = _pendingRequests.value ?: emptyList()

        val sanitizedQuery = query.trim()
        val filteredRequests = applyPendingRequestFilters(
            requests = allRequests,
            query = sanitizedQuery,
            sortBy = _uiState.value.sortBy
        )

        _uiState.value = _uiState.value.copy(
            searchQuery = sanitizedQuery,
            filteredRequests = filteredRequests
        )
    }

    /**
     * Sorts pending requests by different criteria
     *
     * @param sortBy Sorting criteria ("distance", "value", "time")
     */
    fun sortPendingRequests(sortBy: String) {
        val normalizedSortBy = when (sortBy) {
            "value", "weight", "distance", "time" -> sortBy
            else -> "time"
        }

        val allRequests = _pendingRequests.value ?: emptyList()
        val filteredRequests = applyPendingRequestFilters(
            requests = allRequests,
            query = _uiState.value.searchQuery,
            sortBy = normalizedSortBy
        )

        _uiState.value = _uiState.value.copy(
            sortBy = normalizedSortBy,
            filteredRequests = filteredRequests
        )
    }

    private fun applyPendingRequestFilters(
        requests: List<PickupRequest>,
        query: String,
        sortBy: String
    ): List<PickupRequest> {
        if (requests.isEmpty()) return emptyList()
        val filtered = filterRequestsByQuery(requests, query)
        return sortRequests(filtered, sortBy)
    }

    private fun filterRequestsByQuery(
        requests: List<PickupRequest>,
        query: String
    ): List<PickupRequest> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) {
            return requests
        }

        return requests.filter { request ->
            request.id.contains(normalizedQuery, ignoreCase = true) ||
            request.householdId.contains(normalizedQuery, ignoreCase = true) ||
            request.pickupLocation.address.contains(normalizedQuery, ignoreCase = true) ||
            request.notes.contains(normalizedQuery, ignoreCase = true) ||
            request.wasteItems.any { item ->
                item.type.contains(normalizedQuery, ignoreCase = true) ||
                item.description.contains(normalizedQuery, ignoreCase = true)
            }
        }
    }

    private fun sortRequests(
        requests: List<PickupRequest>,
        sortBy: String
    ): List<PickupRequest> {
        return when (sortBy) {
            "value" -> requests.sortedByDescending { it.totalValue }
            "weight" -> requests.sortedByDescending { it.wasteItems.size }
            "distance" -> sortRequestsByDistance(requests)
            else -> requests.sortedByDescending { it.createdAt }
        }
    }

    private fun sortRequestsByDistance(requests: List<PickupRequest>): List<PickupRequest> {
        val collectorLocation = _mapState.value.collectorLocation
        if (collectorLocation == null) {
            return requests.sortedByDescending { it.createdAt }
        }

        return requests.sortedBy { request ->
            haversineDistanceKm(
                collectorLocation.latitude,
                collectorLocation.longitude,
                request.pickupLocation.latitude,
                request.pickupLocation.longitude
            )
        }
    }

    private fun haversineDistanceKm(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val earthRadiusKm = 6371.0

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val originLat = Math.toRadians(lat1)
        val destinationLat = Math.toRadians(lat2)

        val a = sin(dLat / 2).pow(2.0) +
            sin(dLon / 2).pow(2.0) * cos(originLat) * cos(destinationLat)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadiusKm * c
    }

    fun distanceToRequest(request: PickupRequest): Double? {
        val collectorLocation = _mapState.value.collectorLocation ?: return null
        if (request.pickupLocation.latitude == 0.0 && request.pickupLocation.longitude == 0.0) {
            return null
        }

        return haversineDistanceKm(
            collectorLocation.latitude,
            collectorLocation.longitude,
            request.pickupLocation.latitude,
            request.pickupLocation.longitude
        )
    }

    private fun updatePerformanceMetrics(
        requests: List<PickupRequest>,
        earnings: Earnings
    ) {
        val accepted = requests.count { it.status == PickupRequest.STATUS_ACCEPTED }
        val inProgress = requests.count { it.status == PickupRequest.STATUS_IN_PROGRESS }
        val completed = requests.count { it.status == PickupRequest.STATUS_COMPLETED }
        val cancelled = requests.count { it.status == PickupRequest.STATUS_CANCELLED }

        val totalTracked = accepted + inProgress + completed + cancelled
        val active = accepted + inProgress

        val completionRate = if (totalTracked > 0) completed.toDouble() / totalTracked else 0.0
        val cancellationRate = if (totalTracked > 0) cancelled.toDouble() / totalTracked else 0.0

        val sortedTransactions = earnings.transactionHistory
            .sortedByDescending { it.completedAt }
            .take(5)

        _performanceMetrics.value = CollectorPerformanceMetrics(
            totalCompleted = completed,
            totalInProgress = inProgress,
            totalAccepted = accepted,
            totalCancelled = cancelled,
            activePickups = active,
            completionRate = completionRate,
            cancellationRate = cancellationRate,
            totalTransactions = earnings.totalTransactions,
            totalSpent = earnings.totalSpent,
            totalWasteKg = earnings.totalWasteCollected,
            averagePerPickup = earnings.getAveragePerTransaction(),
            averagePerKg = earnings.getAveragePerKg(),
            spentToday = earnings.spentToday,
            spentThisWeek = earnings.spentThisWeek,
            spentThisMonth = earnings.spentThisMonth,
            recentTransactions = sortedTransactions
        )
    }

    /**
     * Refreshes the collector's current location for map display.
     */
    fun refreshCollectorLocation() {
        viewModelScope.launch {
            _mapState.value = _mapState.value.copy(isLoadingLocation = true, errorMessage = null)

            val result = locationRepository.getLastKnownLocation()
            _mapState.value = if (result.isSuccess) {
                val location = result.getOrNull()
                _mapState.value.copy(
                    isLoadingLocation = false,
                    collectorLocation = location?.let {
                        PickupRequest.Location(
                            latitude = it.latitude,
                            longitude = it.longitude,
                            address = ""
                        )
                    },
                    errorMessage = null
                )
            } else {
                _mapState.value.copy(
                    isLoadingLocation = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Unable to fetch current location"
                )
            }

            if (_uiState.value.sortBy == "distance") {
                val currentRequests = _pendingRequests.value ?: emptyList()
                val resorted = sortRequests(currentRequests, "distance")
                _uiState.value = _uiState.value.copy(filteredRequests = resorted)
            }

            // Refresh marker distances
            _pendingRequests.value?.let { updateMapMarkers(it) }
        }
    }

    private fun updateMapMarkers(requests: List<PickupRequest>) {
        val collectorLocation = _mapState.value.collectorLocation
        val markers = requests
            .filter { request ->
                request.pickupLocation.latitude != 0.0 || request.pickupLocation.longitude != 0.0
            }
            .map { request ->
                val distanceKm = collectorLocation?.let {
                    haversineDistanceKm(
                        it.latitude,
                        it.longitude,
                        request.pickupLocation.latitude,
                        request.pickupLocation.longitude
                    )
                }

                MapMarker(
                    requestId = request.id,
                    latitude = request.pickupLocation.latitude,
                    longitude = request.pickupLocation.longitude,
                    status = request.status,
                    address = request.pickupLocation.address,
                    distanceKm = distanceKm
                )
            }

        _mapState.value = _mapState.value.copy(pendingMarkers = markers)
    }

    fun clearMapError() {
        _mapState.value = _mapState.value.copy(errorMessage = null)
    }

    /**
     * Gets route to a pickup location
     * This is a placeholder function for map integration
     *
     * @param request The pickup request to navigate to
     */
    fun getRouteToPickup(request: PickupRequest) {
        // TODO: Integrate with mapping SDK (OsmAnd or similar)
        // This would:
        // 1. Get current location
        // 2. Calculate optimized route to pickup location
        // 3. Launch navigation or show route on map
        // 4. Provide turn-by-turn directions

        _uiState.value = _uiState.value.copy(
            selectedRequestForNavigation = request
        )
    }

    /**
     * Clears any error messages
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    /**
     * Clears the accept request result
     */
    fun clearAcceptRequestResult() {
        _acceptRequestResult.value = null
    }

    /**
     * Clears the transaction success flag
     */
    fun clearTransactionSuccess() {
        _uiState.value = _uiState.value.copy(
            showTransactionSuccess = false,
            completedTransaction = null
        )
    }

    /**
     * Clears the selected request for navigation
     */
    fun clearNavigation() {
        _uiState.value = _uiState.value.copy(selectedRequestForNavigation = null)
    }

    /**
     * Starts the location tracking foreground service.
     * Called when marking request as "in_progress".
     *
     * @param requestId The pickup request ID being tracked
     * @param collectorId The collector's user ID
     */
    private fun startLocationTrackingService(requestId: String, collectorId: String) {
        android.util.Log.d("CollectorViewModel", "🚀 STARTING LocationTrackingService for request: $requestId, collector: $collectorId")
        val intent = Intent(context, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_START_TRACKING
            putExtra(LocationTrackingService.EXTRA_REQUEST_ID, requestId)
            putExtra(LocationTrackingService.EXTRA_COLLECTOR_ID, collectorId)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.util.Log.d("CollectorViewModel", "📱 Starting foreground service (API ${Build.VERSION.SDK_INT})")
            context.startForegroundService(intent)
        } else {
            android.util.Log.d("CollectorViewModel", "📱 Starting regular service (API ${Build.VERSION.SDK_INT})")
            context.startService(intent)
        }
        android.util.Log.d("CollectorViewModel", "✅ Service start command sent")
    }

    /**
     * Stops the location tracking service.
     * Called when request is completed or cancelled.
     * Service will also auto-stop via Firestore listener.
     */
    private fun stopLocationTrackingService() {
        val intent = Intent(context, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_STOP_TRACKING
        }
        context.startService(intent)
    }
}

/**
 * UI state for the collector interface
 */
data class CollectorUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val hasPendingRequests: Boolean = false,
    val searchQuery: String = "",
    val sortBy: String = "time",
    val filteredRequests: List<PickupRequest> = emptyList(),
    val selectedRequestForNavigation: PickupRequest? = null,
    val showTransactionSuccess: Boolean = false,
    val completedTransaction: Transaction? = null
)

data class CollectorMapState(
    val isLoadingLocation: Boolean = false,
    val collectorLocation: PickupRequest.Location? = null,
    val pendingMarkers: List<MapMarker> = emptyList(),
    val errorMessage: String? = null
)

data class MapMarker(
    val requestId: String,
    val latitude: Double,
    val longitude: Double,
    val status: String,
    val address: String,
    val distanceKm: Double? = null
)
