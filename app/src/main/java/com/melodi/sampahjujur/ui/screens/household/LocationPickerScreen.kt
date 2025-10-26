package com.melodi.sampahjujur.ui.screens.household

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.firebase.firestore.GeoPoint
import com.melodi.sampahjujur.ui.theme.PrimaryGreen
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint as OsmGeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerScreen(
    viewModel: com.melodi.sampahjujur.viewmodel.HouseholdViewModel,
    onLocationSelected: (GeoPoint, String) -> Unit = { _, _ -> },
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Get the current UI state from ViewModel
    val uiState by viewModel.uiState.collectAsState()

    // Initialize with previously selected location or default to Yogyakarta, Indonesia
    val initialLocation = remember {
        uiState.selectedLocation?.let {
            OsmGeoPoint(it.latitude, it.longitude)
        } ?: OsmGeoPoint(-7.7956, 110.3695)
    }

    var currentLocation by remember { mutableStateOf(initialLocation) }
    var selectedAddress by remember { mutableStateOf(uiState.selectedAddress) }
    var isLoadingAddress by remember { mutableStateOf(false) }
    var isLoadingLocation by remember { mutableStateOf(false) }
    var showPermissionDeniedDialog by remember { mutableStateOf(false) }

    var mapView by remember { mutableStateOf<MapView?>(null) }
    var centerMarker by remember { mutableStateOf<Marker?>(null) }

    // Track map movement for auto-address update
    var scrollDebounceJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted && coarseLocationGranted) {
            // Permissions granted, get current location via ViewModel
            viewModel.getCurrentLocation()
            // The location will be updated in uiState, so we'll observe it
            scope.launch {
                isLoadingLocation = true
                // Wait a bit for the ViewModel to update
                kotlinx.coroutines.delay(500)
                uiState.selectedLocation?.let { geoPoint ->
                    val newLocation = OsmGeoPoint(geoPoint.latitude, geoPoint.longitude)
                    currentLocation = newLocation
                    mapView?.controller?.apply {
                        setZoom(17.0)
                        animateTo(newLocation)
                    }
                }
                isLoadingLocation = false
            }
        } else {
            showPermissionDeniedDialog = true
        }
    }

    // Function to get address from map center
    fun updateAddressFromMapCenter() {
        scope.launch {
            isLoadingAddress = true
            val center = mapView?.mapCenter
            if (center != null) {
                val geoPoint = GeoPoint(center.latitude, center.longitude)
                val addressResult = viewModel.getAddressFromLocation(geoPoint)
                if (addressResult.isSuccess) {
                    selectedAddress = addressResult.getOrNull() ?: ""
                } else {
                    selectedAddress = "Lat: ${String.format("%.6f", center.latitude)}, " +
                            "Lng: ${String.format("%.6f", center.longitude)}"
                }
            }
            isLoadingAddress = false
        }
    }

    // Initialize OSMDroid configuration
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(
            context,
            context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = context.packageName
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Select Location",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // OpenStreetMap View
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(15.0)
                        controller.setCenter(currentLocation)

                        // Add center marker
                        val marker = Marker(this).apply {
                            position = currentLocation
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = "Selected Location"
                        }
                        overlays.add(marker)
                        centerMarker = marker

                        // Update marker position when map is scrolled
                        addMapListener(object : org.osmdroid.events.MapListener {
                            override fun onScroll(event: org.osmdroid.events.ScrollEvent?): Boolean {
                                val center = mapCenter
                                marker.position = OsmGeoPoint(center.latitude, center.longitude)
                                invalidate()

                                // Auto-update address after user stops scrolling (debounce)
                                scrollDebounceJob?.cancel()
                                scrollDebounceJob = scope.launch {
                                    kotlinx.coroutines.delay(500) // Wait 500ms after scroll stops
                                    updateAddressFromMapCenter()
                                }

                                return true
                            }

                            override fun onZoom(event: org.osmdroid.events.ZoomEvent?): Boolean {
                                return false
                            }
                        })

                        mapView = this
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    // Update when needed
                }
            )

            // Address Card at the top
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Selected Location",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (isLoadingAddress) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = PrimaryGreen
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Getting address...",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                    } else {
                        Text(
                            text = selectedAddress.ifEmpty { "Move the map to select a location" },
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // Current Location Button
            FloatingActionButton(
                onClick = {
                    if (viewModel.hasLocationPermission()) {
                        viewModel.getCurrentLocation()
                        // The location will be updated in uiState
                        scope.launch {
                            isLoadingLocation = true
                            // Wait for ViewModel to update
                            kotlinx.coroutines.delay(500)
                            uiState.selectedLocation?.let { geoPoint ->
                                val newLocation = OsmGeoPoint(geoPoint.latitude, geoPoint.longitude)
                                currentLocation = newLocation
                                mapView?.controller?.apply {
                                    setZoom(17.0)
                                    animateTo(newLocation)
                                }
                                // Update address after moving to new location
                                kotlinx.coroutines.delay(500) // Wait for animation
                                updateAddressFromMapCenter()
                            }
                            isLoadingLocation = false
                        }
                    } else {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 100.dp),
                containerColor = Color.White,
                shape = CircleShape
            ) {
                if (isLoadingLocation) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = PrimaryGreen
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Current Location",
                        tint = PrimaryGreen
                    )
                }
            }

            // Confirm Button
            Button(
                onClick = {
                    val center = mapView?.mapCenter
                    if (center != null) {
                        val geoPoint = GeoPoint(center.latitude, center.longitude)
                        viewModel.setPickupLocation(geoPoint, selectedAddress)
                        onLocationSelected(geoPoint, selectedAddress)
                        onBack()
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen
                ),
                shape = RoundedCornerShape(28.dp),
                enabled = selectedAddress.isNotEmpty() && !isLoadingAddress
            ) {
                Text(
                    text = "Confirm Location",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }

    // Permission Denied Dialog
    if (showPermissionDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDeniedDialog = false },
            title = { Text("Location Permission Required") },
            text = {
                Text(
                    "This app needs location permission to detect your current location. " +
                            "You can still select a location manually by moving the map.",
                    textAlign = TextAlign.Start
                )
            },
            confirmButton = {
                TextButton(onClick = { showPermissionDeniedDialog = false }) {
                    Text("OK", color = PrimaryGreen)
                }
            }
        )
    }

    // Clean up map when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            mapView?.onDetach()
        }
    }

    // Initial address load (only if no address is already selected)
    LaunchedEffect(Unit) {
        if (selectedAddress.isEmpty()) {
            kotlinx.coroutines.delay(1000) // Wait for map to load
            updateAddressFromMapCenter()
        }
    }
}
