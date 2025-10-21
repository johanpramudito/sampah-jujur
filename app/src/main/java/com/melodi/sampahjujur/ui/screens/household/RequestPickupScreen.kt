package com.melodi.sampahjujur.ui.screens.household

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.rememberAsyncImagePainter
import com.melodi.sampahjujur.model.WasteItem
import com.melodi.sampahjujur.ui.components.HouseholdBottomNavBar
import com.melodi.sampahjujur.ui.theme.PrimaryGreen
import com.melodi.sampahjujur.ui.theme.SampahJujurTheme
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/**
 * Composable screen that lets household users select a pickup location, add waste items (including optional images),
 * enter notes, and submit a pickup request while handling location permissions, loading states, and result feedback.
 *
 * The screen displays:
 * - A location card with a small map preview and a button to get or update current location (requests permissions if needed).
 * - A waste items card where users can add, view, and remove items; shows totals for weight and estimated value.
 * - A multiline notes field and a submit button that creates a pickup request via the provided view model.
 * - Dialogs and snackbars for permission denial, add-item input, success confirmation, and error reporting.
 *
 * @param viewModel View model that exposes UI state and actions for managing selected location, waste items, and request creation.
 * @param onNavigate Callback invoked with a navigation route string (examples: "location_picker", "my_requests") when the UI requests navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestPickupScreen(
    viewModel: com.melodi.sampahjujur.viewmodel.HouseholdViewModel,
    onNavigate: (String) -> Unit = {}
) {
    var notes by remember { mutableStateOf("") }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var showPermissionDeniedDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()
    val wasteItems = uiState.currentWasteItems
    val selectedAddress = uiState.selectedAddress
    val isLoadingLocation = uiState.isLoadingLocation
    val snackbarHostState = remember { SnackbarHostState() }
    val createRequestResult by viewModel.createRequestResult.observeAsState()

    // Permission launcher for location
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted && coarseLocationGranted) {
            // Permissions granted, get current location
            viewModel.getCurrentLocation()
        } else {
            showPermissionDeniedDialog = true
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Request Pickup",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = {
            HouseholdBottomNavBar(
                selectedRoute = "request",
                onNavigate = onNavigate
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Complete the form to schedule your pickup",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Location Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Pickup Location",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Selected Address Display with Map Preview
                        if (selectedAddress.isNotEmpty() && uiState.selectedLocation != null) {
                            // Small Map Preview
                            MapPreview(
                                latitude = uiState.selectedLocation!!.latitude,
                                longitude = uiState.selectedLocation!!.longitude,
                                onClick = { onNavigate("location_picker") }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Location",
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = selectedAddress,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        } else {
                            Text(
                                text = "No location selected. Tap 'Select Location' to set your pickup address.",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Get Current Location Button
                        Button(
                            onClick = {
                                if (viewModel.hasLocationPermission()) {
                                    viewModel.getCurrentLocation()
                                } else {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryGreen
                            ),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !isLoadingLocation
                        ) {
                            if (isLoadingLocation) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Getting location...")
                            } else {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = "Get Location"
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (selectedAddress.isEmpty()) "Get Current Location" else "Update Location")
                            }
                        }
                    }
                }
            }

            // Waste Items Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Waste Items",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            IconButton(onClick = { showAddItemDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add",
                                    tint = PrimaryGreen
                                )
                            }
                        }

                        if (wasteItems.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Empty",
                                    tint = Color.Gray.copy(alpha = 0.5f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No waste items added yet.",
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Tap the '+' icon to add items.",
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                            wasteItems.forEachIndexed { index, item ->
                                WasteItemCard(
                                    item = item,
                                    onRemove = {
                                        if (item.id.isBlank()) {
                                            return@WasteItemCard
                                        }
                                        viewModel.removeWasteItem(item.id)
                                    }
                                )
                                if (index < wasteItems.size - 1) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Totals
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Total: ${wasteItems.sumOf { it.weight }} kg",
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Est. Value: $${wasteItems.sumOf { it.estimatedValue }}",
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreen
                                )
                            }
                        }
                    }
                }
            }

            // Notes
            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Additional notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    placeholder = { Text("e.g. Leave by the side gate") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedIndicatorColor = PrimaryGreen,
                        unfocusedIndicatorColor = Color.LightGray
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Submit Button
            item {
                Button(
                    onClick = {
                        if (uiState.selectedLocation != null) {
                            viewModel.createPickupRequest(
                                wasteItems = wasteItems,
                                location = uiState.selectedLocation!!,
                                address = selectedAddress,
                                notes = notes
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryGreen
                    ),
                    shape = RoundedCornerShape(28.dp),
                    enabled = wasteItems.isNotEmpty() && selectedAddress.isNotEmpty() && !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = "Submit Pickup Request",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }

            // Bottom spacer for better scrollability
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Show Add Item Dialog
    if (showAddItemDialog) {
        AddWasteItemDialog(
            onDismiss = { showAddItemDialog = false },
            onAddItem = { type, weight, value, description, imageUrl ->
                viewModel.addWasteItem(
                    WasteItem(
                        type = type.lowercase(),
                        weight = weight,
                        estimatedValue = value,
                        description = description,
                        imageUrl = imageUrl
                    )
                )
                showAddItemDialog = false
            }
        )
    }

    // Show success dialog when request is created
    LaunchedEffect(createRequestResult) {
        if (createRequestResult?.isSuccess == true) {
            showSuccessDialog = true
            notes = ""
            viewModel.clearCreateRequestResult()
        } else if (createRequestResult?.isFailure == true) {
            val error = createRequestResult?.exceptionOrNull()?.message ?: "Failed to create request"
            snackbarHostState.showSnackbar(error)
            viewModel.clearCreateRequestResult()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        val errorMessage = uiState.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(errorMessage)
        viewModel.clearError()
    }

    // Permission Denied Dialog
    if (showPermissionDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDeniedDialog = false },
            title = { Text("Location Permission Required") },
            text = {
                Text(
                    "This app needs location permission to detect your current location. " +
                            "You can grant permission in Settings, or manually select a location by tapping the map.",
                    textAlign = TextAlign.Start
                )
            },
            confirmButton = {
                TextButton(onClick = { showPermissionDeniedDialog = false }) {
                    Text("OK", color = PrimaryGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPermissionDeniedDialog = false
                    onNavigate("location_picker")
                }) {
                    Text("Select Manually", color = PrimaryGreen)
                }
            }
        )
    }

    // Success Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = PrimaryGreen,
                    modifier = Modifier.size(64.dp)
                )
            },
            title = {
                Text(
                    text = "Request Submitted!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Your pickup request has been submitted successfully.",
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "A collector will accept your request soon. You can track your request in the 'My Requests' tab.",
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onNavigate("my_requests")
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryGreen
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("View My Requests", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSuccessDialog = false }) {
                    Text("Close", color = Color.Gray)
                }
            }
        )
    }
}

/**
 * Displays a card summarizing a waste item with an optional thumbnail, item details, and a remove control.
 *
 * Shows the item's capitalized type, weight in kilograms, estimated value, and an optional description
 * (limited to two lines). Renders a cropped image thumbnail when `item.imageUrl` is not blank. Pressing the
 * delete icon invokes the removal callback.
 *
 * @param item The WasteItem to render (uses `type`, `weight`, `estimatedValue`, `description`, and `imageUrl`).
 * @param onRemove Callback invoked when the user taps the delete button to remove this item.
 */
@Composable
fun WasteItemCard(
    item: WasteItem,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Image thumbnail
            if (item.imageUrl.isNotBlank()) {
                Image(
                    painter = rememberAsyncImagePainter(item.imageUrl),
                    contentDescription = "Waste item image",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.type.replaceFirstChar { it.uppercase() },
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Weight: ${item.weight} kg",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = "Value: $${item.estimatedValue}",
                    fontSize = 12.sp,
                    color = PrimaryGreen
                )
                if (item.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.description,
                        fontSize = 11.sp,
                        color = Color.Gray.copy(alpha = 0.7f),
                        maxLines = 2
                    )
                }
            }
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = Color.Red
                )
            }
        }
    }
}

@Composable
fun MapPreview(
    latitude: Double,
    longitude: Double,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current

    // Initialize OSMDroid configuration
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(
            context,
            context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = context.packageName
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)

                    // Make completely non-interactive
                    setMultiTouchControls(false)
                    isClickable = false
                    isFocusable = false
                    isFocusableInTouchMode = false
                    setOnTouchListener { _, _ -> true } // Consume all touch events

                    val location = GeoPoint(latitude, longitude)
                    controller.setZoom(16.0)
                    controller.setCenter(location)

                    // Add marker at the location
                    val marker = Marker(this).apply {
                        position = location
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "Pickup Location"
                    }
                    overlays.add(marker)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Full overlay to block all interactions and indicate clickability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.05f))
                .clickable { onClick() } // Only the overlay is clickable
        )

        // "Tap to change" hint
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp),
            color = Color.White.copy(alpha = 0.9f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit location",
                    tint = PrimaryGreen,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Tap to change",
                    fontSize = 11.sp,
                    color = Color.DarkGray
                )
            }
        }
    }
}

// Preview disabled - requires ViewModel injection
// @Preview(showBackground = true)
// @Composable
// fun RequestPickupScreenPreview() {
//     SampahJujurTheme {
//         RequestPickupScreen()
//     }
// }