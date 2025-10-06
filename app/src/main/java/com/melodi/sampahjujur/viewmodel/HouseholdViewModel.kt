package com.melodi.sampahjujur.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melodi.sampahjujur.model.PickupRequest
import com.melodi.sampahjujur.model.WasteItem
import com.melodi.sampahjujur.repository.AuthRepository
import com.melodi.sampahjujur.repository.WasteRepository
import com.google.firebase.firestore.GeoPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
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

    private var householdId: String? = null
    private var householdRequestsJob: Job? = null
    private var wasteItemsJob: Job? = null

    private val _uiState = MutableStateFlow(HouseholdUiState())
    val uiState: StateFlow<HouseholdUiState> = _uiState.asStateFlow()

    private val _userRequests = MutableLiveData<List<PickupRequest>>()
    val userRequests: LiveData<List<PickupRequest>> = _userRequests

    private val _createRequestResult = MutableLiveData<Result<PickupRequest>?>()
    val createRequestResult: LiveData<Result<PickupRequest>?> = _createRequestResult

    init {
        initializeHouseholdData()
    }

    private fun initializeHouseholdData() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            if (user?.isHousehold() == true) {
                if (householdId != user.uid) {
                    householdId = user.uid
                }
                observeHouseholdRequests(user.uid)
                observeWasteItems(user.uid)
            } else {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "User not authenticated"
                )
            }
        }
    }

    private fun observeHouseholdRequests(householdUid: String) {
        householdRequestsJob?.cancel()
        householdRequestsJob = viewModelScope.launch {
            wasteRepository.getHouseholdRequests(householdUid)
                .catch { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "Failed to load requests"
                    )
                }
                .collect { requests ->
                    _userRequests.value = requests
                }
        }
    }

    private fun observeWasteItems(householdUid: String) {
        wasteItemsJob?.cancel()
        wasteItemsJob = viewModelScope.launch {
            wasteRepository.listenToHouseholdWasteItems(householdUid)
                .catch { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "Failed to load waste items"
                    )
                }
                .collect { items ->
                    _uiState.value = _uiState.value.copy(
                        currentWasteItems = items
                    )
                }
        }
    }

    private suspend fun ensureHouseholdId(): String? {
        householdId?.let { return it }

        val currentUser = authRepository.getCurrentUser()
        return if (currentUser?.isHousehold() == true) {
            if (householdId != currentUser.uid) {
                householdId = currentUser.uid
                observeHouseholdRequests(currentUser.uid)
                observeWasteItems(currentUser.uid)
            }
            currentUser.uid
        } else {
            null
        }
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

            householdId = currentUser.uid

            val totalValue = wasteItems.sumOf { it.estimatedValue }

            val pickupRequest = PickupRequest(
                householdId = currentUser.uid,
                pickupLocation = PickupRequest.Location(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    address = address
                ),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                status = PickupRequest.STATUS_PENDING,
                wasteItems = wasteItems,
                totalValue = totalValue,
                notes = notes
            )

            val result = wasteRepository.postPickupRequest(pickupRequest)

            _uiState.value = _uiState.value.copy(isLoading = false)
            _createRequestResult.value = result

            if (result.isSuccess) {
                val clearResult = wasteRepository.clearWasteItems(currentUser.uid)
                if (clearResult.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = clearResult.exceptionOrNull()?.message ?: "Failed to reset waste items"
                    )
                }
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
     * Adds a waste item to the current request being created
     *
     * @param wasteItem The waste item to add
     */
    fun addWasteItem(wasteItem: WasteItem) {
        viewModelScope.launch {
            val householdUid = ensureHouseholdId()
            if (householdUid == null) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "User not authenticated"
                )
                return@launch
            }

            val result = wasteRepository.addWasteItem(householdUid, wasteItem)
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to add waste item"
                )
            }
        }
    }

    /**
     * Removes a waste item from the current request being created
     *
     * @param wasteItemId Firestore document ID of the waste item to remove
     */
    fun removeWasteItem(wasteItemId: String) {
        viewModelScope.launch {
            val householdUid = ensureHouseholdId()
            if (householdUid == null) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "User not authenticated"
                )
                return@launch
            }

            val result = wasteRepository.deleteWasteItem(householdUid, wasteItemId)
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to remove waste item"
                )
            }
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
        viewModelScope.launch {
            val householdUid = ensureHouseholdId()
            if (householdUid == null) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "User not authenticated"
                )
                return@launch
            }

            val result = wasteRepository.clearWasteItems(householdUid)
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to reset waste items"
                )
            }

            _uiState.value = _uiState.value.copy(
                currentWasteItems = emptyList(),
                selectedLocation = null,
                selectedAddress = ""
            )
        }
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

