package com.melodi.sampahjujur.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melodi.sampahjujur.model.PickupRequest
import com.melodi.sampahjujur.repository.AuthRepository
import com.melodi.sampahjujur.repository.WasteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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

    private val _pendingRequests = MutableLiveData<List<PickupRequest>>()
    val pendingRequests: LiveData<List<PickupRequest>> = _pendingRequests

    private val _myRequests = MutableLiveData<List<PickupRequest>>()
    val myRequests: LiveData<List<PickupRequest>> = _myRequests

    private val _acceptRequestResult = MutableLiveData<Result<Unit>?>()
    val acceptRequestResult: LiveData<Result<Unit>?> = _acceptRequestResult

    init {
        startListeningToPendingRequests()
        loadMyRequests()
    }

    /**
     * Starts listening to pending pickup requests for real-time updates
     */
    private fun startListeningToPendingRequests() {
        viewModelScope.launch {
            wasteRepository.getPendingRequests().collect { requests ->
                _pendingRequests.value = requests
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasPendingRequests = requests.isNotEmpty()
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

            _uiState.value = _uiState.value.copy(isLoading = false)
            _acceptRequestResult.value = result

            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
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

            val result = wasteRepository.updateRequestStatus(
                requestId,
                PickupRequest.STATUS_IN_PROGRESS
            )

            _uiState.value = if (result.isSuccess) {
                _uiState.value.copy(isLoading = false)
            } else {
                _uiState.value.copy(
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
    fun completePickupRequest(requestId: String, finalAmount: Double) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // In a real app, this would show a payment confirmation dialog
            // and integrate with a secure payment system
            val result = wasteRepository.completeTransaction(requestId, finalAmount)

            _uiState.value = if (result.isSuccess) {
                _uiState.value.copy(
                    isLoading = false,
                    showTransactionSuccess = true
                )
            } else {
                _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to complete transaction"
                )
            }
        }
    }

    /**
     * Filters pending requests based on search criteria
     *
     * @param query Search query (can be address, waste type, etc.)
     */
    fun filterPendingRequests(query: String) {
        val allRequests = _pendingRequests.value ?: emptyList()

        val filteredRequests = if (query.isBlank()) {
            allRequests
        } else {
            allRequests.filter { request ->
                request.pickupLocation.address.contains(query, ignoreCase = true) ||
                request.wasteItems.any { it.type.contains(query, ignoreCase = true) } ||
                request.notes.contains(query, ignoreCase = true)
            }
        }

        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredRequests = filteredRequests
        )
    }

    /**
     * Sorts pending requests by different criteria
     *
     * @param sortBy Sorting criteria ("distance", "value", "time")
     */
    fun sortPendingRequests(sortBy: String) {
        val requests = _uiState.value.filteredRequests.ifEmpty {
            _pendingRequests.value ?: emptyList()
        }

        val sortedRequests = when (sortBy) {
            "value" -> requests.sortedByDescending { it.totalValue }
            "time" -> requests.sortedBy { it.createdAt }
            "weight" -> requests.sortedByDescending { it.getTotalWeight() }
            // "distance" would require current location - placeholder for now
            "distance" -> requests // TODO: Implement distance-based sorting
            else -> requests
        }

        _uiState.value = _uiState.value.copy(
            sortBy = sortBy,
            filteredRequests = sortedRequests
        )
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
        _uiState.value = _uiState.value.copy(showTransactionSuccess = false)
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
    val hasPendingRequests: Boolean = false,
    val searchQuery: String = "",
    val sortBy: String = "time",
    val filteredRequests: List<PickupRequest> = emptyList(),
    val selectedRequestForNavigation: PickupRequest? = null,
    val showTransactionSuccess: Boolean = false
)
