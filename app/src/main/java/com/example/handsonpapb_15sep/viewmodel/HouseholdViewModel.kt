package com.example.handsonpapb_15sep.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.handsonpapb_15sep.model.PickupRequest
import com.example.handsonpapb_15sep.model.WasteItem
import com.example.handsonpapb_15sep.repository.AuthRepository
import com.example.handsonpapb_15sep.repository.WasteRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the household user interface.
 * Handles pickup request creation and manages household-specific data.
 */
@HiltViewModel
class HouseholdViewModel @Inject constructor(
    private val wasteRepository: WasteRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HouseholdUiState())
    val uiState: StateFlow<HouseholdUiState> = _uiState.asStateFlow()

    private val _userRequests = MutableLiveData<List<PickupRequest>>()
    val userRequests: LiveData<List<PickupRequest>> = _userRequests

    private val _createRequestResult = MutableLiveData<Result<PickupRequest>?>()
    val createRequestResult: LiveData<Result<PickupRequest>?> = _createRequestResult

    init {
        loadUserRequests()
    }

    /**
     * Creates a new pickup request with the provided details
     *
     * @param wasteItems List of waste items to be collected
     * @param location Geographic location for pickup
     * @param address Human-readable address
     * @param notes Additional notes or instructions
     */
    fun createPickupRequest(
        wasteItems: List<WasteItem>,
        location: GeoPoint,
        address: String,
        notes: String = ""
    ) {
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

            if (!currentUser.isHousehold()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Invalid user role"
                )
                return@launch
            }

            val totalValue = wasteItems.sumOf { it.estimatedValue }

            val pickupRequest = PickupRequest(
                householdId = currentUser.uid,
                location = location,
                timestamp = Timestamp.now(),
                status = PickupRequest.STATUS_PENDING,
                wasteItems = wasteItems,
                totalValue = totalValue,
                address = address,
                notes = notes
            )

            val result = wasteRepository.postPickupRequest(pickupRequest)

            _uiState.value = _uiState.value.copy(isLoading = false)
            _createRequestResult.value = result

            if (result.isSuccess) {
                // Refresh the user's requests list
                loadUserRequests()
            } else {
                _uiState.value = _uiState.value.copy(
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to create request"
                )
            }
        }
    }

    /**
     * Cancels a pickup request if it's still pending
     *
     * @param requestId ID of the request to cancel
     */
    fun cancelPickupRequest(requestId: String) {
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

            val result = wasteRepository.cancelPickupRequest(requestId, currentUser.uid)

            _uiState.value = if (result.isSuccess) {
                _uiState.value.copy(isLoading = false)
            } else {
                _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to cancel request"
                )
            }
        }
    }

    /**
     * Loads the current user's pickup requests
     */
    private fun loadUserRequests() {
        viewModelScope.launch {
            val currentUser = authRepository.getCurrentUser()
            if (currentUser?.isHousehold() == true) {
                wasteRepository.getHouseholdRequests(currentUser.uid).collect { requests ->
                    _userRequests.value = requests
                }
            }
        }
    }

    /**
     * Adds a waste item to the current request being created
     *
     * @param wasteItem The waste item to add
     */
    fun addWasteItem(wasteItem: WasteItem) {
        val currentItems = _uiState.value.currentWasteItems.toMutableList()
        currentItems.add(wasteItem)
        _uiState.value = _uiState.value.copy(currentWasteItems = currentItems)
    }

    /**
     * Removes a waste item from the current request being created
     *
     * @param index Index of the waste item to remove
     */
    fun removeWasteItem(index: Int) {
        val currentItems = _uiState.value.currentWasteItems.toMutableList()
        if (index >= 0 && index < currentItems.size) {
            currentItems.removeAt(index)
            _uiState.value = _uiState.value.copy(currentWasteItems = currentItems)
        }
    }

    /**
     * Sets the pickup location for the current request
     *
     * @param location Geographic location
     * @param address Human-readable address
     */
    fun setPickupLocation(location: GeoPoint, address: String) {
        _uiState.value = _uiState.value.copy(
            selectedLocation = location,
            selectedAddress = address
        )
    }

    /**
     * Clears any error messages
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * Clears the create request result
     */
    fun clearCreateRequestResult() {
        _createRequestResult.value = null
    }

    /**
     * Resets the current request form
     */
    fun resetCurrentRequest() {
        _uiState.value = _uiState.value.copy(
            currentWasteItems = emptyList(),
            selectedLocation = null,
            selectedAddress = ""
        )
    }
}

/**
 * UI state for the household interface
 */
data class HouseholdUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentWasteItems: List<WasteItem> = emptyList(),
    val selectedLocation: GeoPoint? = null,
    val selectedAddress: String = ""
)
