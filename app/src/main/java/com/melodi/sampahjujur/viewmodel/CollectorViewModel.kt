package com.melodi.sampahjujur.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melodi.sampahjujur.model.Earnings
import com.melodi.sampahjujur.model.PickupRequest
import com.melodi.sampahjujur.model.Transaction
import com.melodi.sampahjujur.model.TransactionItem
import com.melodi.sampahjujur.repository.AuthRepository
import com.melodi.sampahjujur.repository.WasteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the collector user interface.
 * Handles pending request monitoring and request acceptance logic.
 */
@HiltViewModel
class CollectorViewModel @Inject constructor(
    private val wasteRepository: WasteRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CollectorUiState())
    val uiState: StateFlow<CollectorUiState> = _uiState.asStateFlow()

    private val _earningsState = MutableStateFlow(Earnings())
    val earnings: StateFlow<Earnings> = _earningsState.asStateFlow()

    private val _pendingRequests = MutableLiveData<List<PickupRequest>>()
    val pendingRequests: LiveData<List<PickupRequest>> = _pendingRequests

    private val _myRequests = MutableLiveData<List<PickupRequest>>()
    val myRequests: LiveData<List<PickupRequest>> = _myRequests

    private val _acceptRequestResult = MutableLiveData<Result<Unit>?>()
    val acceptRequestResult: LiveData<Result<Unit>?> = _acceptRequestResult

    init {
        startListeningToPendingRequests()
        loadMyRequests()
        loadCollectorEarnings()
    }

    /**
     * Starts listening to pending pickup requests for real-time updates
     */
    private fun startListeningToPendingRequests() {
        viewModelScope.launch {
            wasteRepository.getPendingRequests().collect { requests ->
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
                    filteredRequests = filtered
                )
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
                wasteRepository.getCollectorRequests(currentUser.id).collect { requests ->
                    _myRequests.value = requests
                }
            } else {
                _myRequests.value = emptyList()
            }
        }
    }

    private fun loadCollectorEarnings() {
        viewModelScope.launch {
            val currentUser = authRepository.getCurrentUser()
            if (currentUser?.isCollector() == true) {
                wasteRepository.getCollectorEarnings(currentUser.id).collect { earnings ->
                    _earningsState.value = earnings
                }
            } else {
                _earningsState.value = Earnings()
            }
        }
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
                currentState.copy(
                    isLoading = false,
                    successMessage = "Pickup started"
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
     * Observes a pickup request by ID for detail screens.
     */
    fun observeRequest(requestId: String): Flow<PickupRequest?> {
        return wasteRepository.watchPickupRequest(requestId)
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
            "weight" -> requests.sortedByDescending { it.getTotalWeight() }
            "distance" -> requests // TODO: Implement distance-based sorting
            else -> requests.sortedByDescending { it.createdAt }
        }
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
