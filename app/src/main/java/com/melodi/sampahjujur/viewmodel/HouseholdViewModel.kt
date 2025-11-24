package com.melodi.sampahjujur.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melodi.sampahjujur.model.PickupRequest
import com.melodi.sampahjujur.model.WasteItem
import com.melodi.sampahjujur.repository.AuthRepository
import com.melodi.sampahjujur.repository.LocationRepository
import com.melodi.sampahjujur.repository.WasteRepository
import com.melodi.sampahjujur.utils.CloudinaryUploadService
import com.melodi.sampahjujur.utils.HouseholdNotificationHelper
import com.google.firebase.firestore.GeoPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
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
    private val authRepository: AuthRepository,
    private val locationRepository: LocationRepository,
    private val preferencesRepository: com.melodi.sampahjujur.repository.PreferencesRepository,
    private val notificationHelper: HouseholdNotificationHelper
) : ViewModel() {

    companion object {
        private const val TAG = "HouseholdViewModel"
    }

    private var householdId: String? = null
    private var householdRequestsJob: Job? = null
    private var wasteItemsJob: Job? = null

    private val _uiState = MutableStateFlow(HouseholdUiState())
    val uiState: StateFlow<HouseholdUiState> = _uiState.asStateFlow()

    private val _userRequests = MutableLiveData<List<PickupRequest>>()
    val userRequests: LiveData<List<PickupRequest>> = _userRequests

    private val _createRequestResult = MutableLiveData<Result<PickupRequest>?>()
    val createRequestResult: LiveData<Result<PickupRequest>?> = _createRequestResult

    private val requestStatusMap = mutableMapOf<String, String>()
    private var statusNotificationInitialized = false

    init {
        initializeHouseholdData()
        observeLocationSettings()
    }

    /**
     * Observes location settings and clears location data when disabled
     */
    private fun observeLocationSettings() {
        viewModelScope.launch {
            preferencesRepository.isLocationEnabled.collect { isEnabled ->
                // Update location enabled state
                _uiState.value = _uiState.value.copy(isLocationEnabled = isEnabled)

                if (!isEnabled) {
                    // Clear location from UI state
                    _uiState.value = _uiState.value.copy(
                        selectedLocation = null,
                        selectedAddress = ""
                    )

                    // Clear draft location from database
                    householdId?.let { id ->
                        wasteRepository.clearDraftPickupLocation(id)
                    }

                    Log.d(TAG, "Location services disabled - cleared location data")
                }
            }
        }
    }

    private fun initializeHouseholdData() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            if (user?.isHousehold() == true) {
                if (householdId != user.id) {
                    householdId = user.id
                }
                observeHouseholdRequests(user.id)
                observeWasteItems(user.id)
                loadDraftPickupLocation(user.id)
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
                    val statusMap = requests.associate { it.id to it.status }

                    // Only send notifications if current user is a household user AND the request belongs to them
                    val currentUser = authRepository.getCurrentUser()
                    if (statusNotificationInitialized && currentUser?.isHousehold() == true) {
                        requests
                            .filter { it.householdId == currentUser.id } // Ensure request belongs to current user
                            .forEach { request ->
                                val previousStatus = requestStatusMap[request.id]
                                val currentStatus = request.status
                                if (previousStatus != null && previousStatus != currentStatus) {
                                    when (currentStatus) {
                                        PickupRequest.STATUS_ACCEPTED,
                                        PickupRequest.STATUS_IN_PROGRESS,
                                        PickupRequest.STATUS_COMPLETED,
                                        PickupRequest.STATUS_CANCELLED -> {
                                            viewModelScope.launch {
                                                notificationHelper.notifyRequestStatusChanged(request, currentStatus)
                                            }
                                        }
                                    }
                                }
                            }
                    } else {
                        statusNotificationInitialized = true
                    }

                    requestStatusMap.clear()
                    requestStatusMap.putAll(statusMap)

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
            if (householdId != currentUser.id) {
                householdId = currentUser.id
                observeHouseholdRequests(currentUser.id)
                observeWasteItems(currentUser.id)
            }
            currentUser.id
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

            householdId = currentUser.id

            val totalValue = wasteItems.sumOf { it.estimatedValue }

            val pickupRequest = PickupRequest(
                householdId = currentUser.id,
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
                // Clear waste items from database
                val clearResult = wasteRepository.clearWasteItems(currentUser.id)
                if (clearResult.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = clearResult.exceptionOrNull()?.message ?: "Failed to reset waste items"
                    )
                }

                // Clear draft location from database
                wasteRepository.clearDraftPickupLocation(currentUser.id)

                // Clear location from UI state
                _uiState.value = _uiState.value.copy(
                    selectedLocation = null,
                    selectedAddress = ""
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to create request"
                )
            }
        }
    }

    /**
     * Observes a single pickup request by ID
     *
     * @param requestId ID of the request to observe
     * @return Flow of PickupRequest or null if not found
     */
    fun observeRequest(requestId: String): Flow<PickupRequest?> {
        return wasteRepository.observeRequest(requestId)
    }

    /**
     * Gets collector information by their user ID
     *
     * @param collectorId The collector's user ID
     * @return User object of the collector or null if not found
     */
    suspend fun getCollectorInfo(collectorId: String): com.melodi.sampahjujur.model.User? {
        return authRepository.getUserById(collectorId)
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

            val result = wasteRepository.cancelPickupRequest(requestId, currentUser.id)

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
     * Also deletes the associated image from Cloudinary
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

            // Find the waste item to get its image URL before deleting
            val wasteItem = _uiState.value.currentWasteItems.find { it.id == wasteItemId }

            // Delete from database
            val result = wasteRepository.deleteWasteItem(householdUid, wasteItemId)
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to remove waste item"
                )
                return@launch
            }

            // Delete image from Cloudinary if it exists
            if (wasteItem != null && wasteItem.imageUrl.isNotBlank()) {
                try {
                    val deleted = CloudinaryUploadService.deleteImage(wasteItem.imageUrl)
                    if (deleted) {
                        Log.d(TAG, "Successfully deleted image for waste item: $wasteItemId")
                    } else {
                        Log.w(TAG, "Failed to delete image for waste item: $wasteItemId")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting image for waste item: $wasteItemId", e)
                    // Don't show error to user - item is already deleted from database
                }
            }
        }
    }

    /**
     * Sets the pickup location for the current request and saves to database
     *
     * @param location Geographic location
     * @param address Human-readable address
     */
    fun setPickupLocation(location: GeoPoint, address: String) {
        _uiState.value = _uiState.value.copy(
            selectedLocation = location,
            selectedAddress = address
        )

        // Save to database for persistence
        viewModelScope.launch {
            val householdUid = ensureHouseholdId()
            if (householdUid != null) {
                val locationData = mapOf(
                    "latitude" to location.latitude,
                    "longitude" to location.longitude,
                    "address" to address
                )
                wasteRepository.saveDraftPickupLocation(householdUid, locationData)
            }
        }
    }

    /**
     * Gets the current device location and updates the UI state
     */
    fun getCurrentLocation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingLocation = true, errorMessage = null)

            if (!locationRepository.hasLocationPermission()) {
                _uiState.value = _uiState.value.copy(
                    isLoadingLocation = false,
                    errorMessage = "Location permission not granted. Please enable location permission in settings."
                )
                return@launch
            }

            val locationResult = locationRepository.getCurrentLocation()

            if (locationResult.isSuccess) {
                val geoPoint = locationResult.getOrNull()!!

                // Get address from coordinates
                val addressResult = locationRepository.getAddressFromLocation(geoPoint)

                if (addressResult.isSuccess) {
                    val address = addressResult.getOrNull()!!
                    // Use setPickupLocation to save to database
                    setPickupLocation(geoPoint, address)
                    _uiState.value = _uiState.value.copy(isLoadingLocation = false)
                } else {
                    // Use coordinates as fallback if address lookup fails
                    val fallbackAddress = "Lat: ${String.format("%.6f", geoPoint.latitude)}, " +
                            "Lng: ${String.format("%.6f", geoPoint.longitude)}"
                    // Use setPickupLocation to save to database
                    setPickupLocation(geoPoint, fallbackAddress)
                    _uiState.value = _uiState.value.copy(
                        isLoadingLocation = false,
                        errorMessage = "Could not get address, using coordinates"
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoadingLocation = false,
                    errorMessage = locationResult.exceptionOrNull()?.message
                        ?: "Failed to get current location"
                )
            }
        }
    }

    /**
     * Checks if location permissions are granted
     */
    fun hasLocationPermission(): Boolean {
        return locationRepository.hasLocationPermission()
    }

    /**
     * Gets address from geographic coordinates
     * Used by LocationPickerScreen for reverse geocoding
     */
    suspend fun getAddressFromLocation(geoPoint: GeoPoint): Result<String> {
        return locationRepository.getAddressFromLocation(geoPoint)
    }

    /**
     * Loads the saved draft pickup location from database
     */
    private fun loadDraftPickupLocation(householdUid: String) {
        viewModelScope.launch {
            val result = wasteRepository.getDraftPickupLocation(householdUid)
            if (result.isSuccess) {
                val locationData = result.getOrNull()
                if (locationData != null) {
                    val latitude = locationData["latitude"] as? Double
                    val longitude = locationData["longitude"] as? Double
                    val address = locationData["address"] as? String

                    if (latitude != null && longitude != null && address != null) {
                        _uiState.value = _uiState.value.copy(
                            selectedLocation = GeoPoint(latitude, longitude),
                            selectedAddress = address
                        )
                    }
                }
            }
        }
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

            // Clear waste items
            val result = wasteRepository.clearWasteItems(householdUid)
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to reset waste items"
                )
            }

            // Clear draft location
            wasteRepository.clearDraftPickupLocation(householdUid)

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
    val isLoadingLocation: Boolean = false,
    val errorMessage: String? = null,
    val currentWasteItems: List<WasteItem> = emptyList(),
    val selectedLocation: GeoPoint? = null,
    val selectedAddress: String = "",
    val isLocationEnabled: Boolean = true
)




