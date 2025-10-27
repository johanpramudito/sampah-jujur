package com.melodi.sampahjujur.ui.screens.collector

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.viewinterop.AndroidView
import com.melodi.sampahjujur.model.PickupRequest
import com.melodi.sampahjujur.model.TransactionItem
import com.melodi.sampahjujur.model.User
import com.melodi.sampahjujur.model.WasteItem
import com.melodi.sampahjujur.ui.screens.household.StatusBadge
import com.melodi.sampahjujur.ui.screens.household.WasteItemDetailCard
import com.melodi.sampahjujur.ui.screens.household.formatDateTime
import com.melodi.sampahjujur.ui.theme.PrimaryGreen
import com.melodi.sampahjujur.viewmodel.CollectorViewModel
import kotlinx.coroutines.flow.flowOf
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectorRequestDetailRoute(
    requestId: String,
    onBackClick: () -> Unit,
    viewModel: CollectorViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val request by viewModel.observeRequest(requestId).collectAsState(initial = null)
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCompleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSuccessMessage()
        }
    }

    LaunchedEffect(uiState.showTransactionSuccess) {
        if (uiState.showTransactionSuccess) {
            snackbarHostState.showSnackbar("Transaction completed")
            viewModel.clearTransactionSuccess()
            onBackClick()
        }
    }

    val currentRequest = request
    val householdFlow = remember(currentRequest?.householdId) {
        val id = currentRequest?.householdId
        if (id.isNullOrBlank()) {
            flowOf<User?>(null)
        } else {
            viewModel.observeUser(id)
        }
    }
    val household by householdFlow.collectAsState(initial = null)

    if (currentRequest == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        fun openExternalMaps(location: PickupRequest.Location) {
            val lat = location.latitude
            val lng = location.longitude
            if (lat == 0.0 && lng == 0.0) {
                Toast.makeText(context, "Location coordinates unavailable", Toast.LENGTH_SHORT).show()
                return
            }

            val encodedLabel = Uri.encode(location.address.ifBlank { "$lat,$lng" })
            val intents = listOf(
                Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$lat,$lng")),
                Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?q=$encodedLabel")),
                Intent(Intent.ACTION_VIEW, Uri.parse("waze://?ll=$lat,$lng&navigate=yes")),
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng"))
            )

            val launchIntent = intents.firstOrNull { intent ->
                intent.resolveActivity(context.packageManager) != null
            }

            if (launchIntent != null) {
                context.startActivity(launchIntent)
            } else {
                Toast.makeText(context, "No maps application found", Toast.LENGTH_LONG).show()
            }
        }

        fun contactHousehold(user: User?) {
            val phone = user?.phone?.takeIf { it.isNotBlank() }
            if (phone == null) {
                Toast.makeText(context, "Household phone number unavailable", Toast.LENGTH_SHORT).show()
                return
            }

            val phoneUri = Uri.fromParts("tel", phone, null)
            val intents = listOf(
                Intent(Intent.ACTION_DIAL, phoneUri),
                Intent(Intent.ACTION_VIEW, phoneUri)
            )

            var launched = false
            intents.forEach { intent ->
                if (launched) return@forEach
                try {
                    context.startActivity(intent)
                    launched = true
                } catch (_: ActivityNotFoundException) {
                    // try next intent
                } catch (_: Exception) {
                    // ignore and try next
                }
            }

            if (!launched) {
                val chooser = Intent.createChooser(
                    Intent(Intent.ACTION_DIAL, phoneUri),
                    "Call household"
                )
                try {
                    context.startActivity(chooser)
                    launched = true
                } catch (_: Exception) {
                    // Fall through to toast
                }
            }

            if (!launched) {
                Toast.makeText(context, "No dialer application found", Toast.LENGTH_SHORT).show()
            }
        }

        CollectorRequestDetailScreen(
            request = currentRequest,
            household = household,
            isLoading = uiState.isLoading,
            snackbarHostState = snackbarHostState,
            onBackClick = onBackClick,
            onAcceptRequest = { viewModel.acceptPickupRequest(currentRequest) },
            onNavigateToLocation = { openExternalMaps(it) },
            onStartPickup = { viewModel.markRequestInProgress(currentRequest.id) },
            onCompletePickup = { showCompleteDialog = true },
            onContactHousehold = { contactHousehold(it) },
            onCancelRequest = { viewModel.cancelCollectorRequest(currentRequest.id) }
        )
    }

    if (showCompleteDialog && currentRequest != null) {
        CompleteTransactionDialog(
            wasteItems = currentRequest.wasteItems,
            onDismiss = { showCompleteDialog = false },
            onComplete = { actualItems, paymentMethod ->
                val transactionItems = actualItems.map { item ->
                    TransactionItem(
                        type = item.type,
                        estimatedWeight = item.estimatedWeight,
                        estimatedValue = item.estimatedValue,
                        actualWeight = item.actualWeight.toDoubleOrNull() ?: item.estimatedWeight,
                        actualValue = item.actualValue.toDoubleOrNull() ?: item.estimatedValue
                    )
                }
                val finalAmount = transactionItems.sumOf { it.actualValue }
                viewModel.completePickupRequest(
                    requestId = currentRequest.id,
                    finalAmount = finalAmount,
                    actualWasteItems = transactionItems,
                    paymentMethod = paymentMethod,
                    notes = ""
                )
                showCompleteDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectorRequestDetailScreen(
    request: PickupRequest,
    household: User? = null,
    isLoading: Boolean = false,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit = {},
    onAcceptRequest: () -> Unit = {},
    onNavigateToLocation: (PickupRequest.Location) -> Unit = {},
    onStartPickup: () -> Unit = {},
    onCompletePickup: () -> Unit = {},
    onContactHousehold: (User?) -> Unit = {},
    onCancelRequest: () -> Unit = {}
) {
    var showCancelDialog by remember { mutableStateOf(false) }
    val displayName = household?.fullName?.takeIf { it.isNotBlank() } ?: "Unknown Household"
    val phoneNumber = household?.phone?.takeIf { it.isNotBlank() }
    val emailAddress = household?.email?.takeIf { it.isNotBlank() }
    val hasValidLocation = request.pickupLocation.latitude != 0.0 ||
        request.pickupLocation.longitude != 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Request Details",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
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
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                )
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                // Status Card
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
                                Column {
                                    Text(
                                        text = "Request #${request.id.take(8).uppercase()}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Created on ${formatDateTime(request.createdAt)}",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                                StatusBadge(status = request.status)
                            }
                        }
                    }
                }

                // Household Info Card
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
                                text = "Household Information",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            PrimaryGreen.copy(alpha = 0.1f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Household",
                                        tint = PrimaryGreen
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = displayName,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (phoneNumber != null) {
                                        Text(
                                            text = phoneNumber,
                                            fontSize = 14.sp,
                                            color = Color.Gray
                                        )
                                    } else {
                                        Text(
                                            text = "Phone not provided",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                    if (emailAddress != null) {
                                        Text(
                                            text = emailAddress,
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { onContactHousehold(household) },
                                    enabled = phoneNumber != null
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = "Call",
                                        tint = if (phoneNumber != null) PrimaryGreen else Color.Gray
                                    )
                                }
                            }
                        }
                    }
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

                            if (hasValidLocation) {
                                PickupLocationMap(
                                    location = request.pickupLocation,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                        .background(
                                            Color.LightGray.copy(alpha = 0.3f),
                                            RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Location map unavailable",
                                        color = Color.Gray
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
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
                                    text = request.pickupLocation.address.ifBlank { "Address not provided" },
                                    fontSize = 14.sp,
                                    color = Color.DarkGray,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (hasValidLocation) {
                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = { onNavigateToLocation(request.pickupLocation) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PrimaryGreen
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Navigation,
                                        contentDescription = "Open in Maps",
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Open In Maps",
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                // Waste Items Section
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
                                text = "Waste Items",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            request.wasteItems.forEachIndexed { index, item ->
                                WasteItemDetailCard(item = item)
                                if (index < request.wasteItems.size - 1) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                            Spacer(modifier = Modifier.height(16.dp))

                            // Totals
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Total Weight",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = "${request.wasteItems.sumOf { it.weight }} kg",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Estimated Value",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = "Rp ${String.format("%,.0f", request.wasteItems.sumOf { it.estimatedValue })}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryGreen
                                    )
                                }
                            }
                        }
                    }
                }

                // Notes Card
                if (request.notes.isNotBlank()) {
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
                                    text = "Household Notes",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = request.notes,
                                    fontSize = 14.sp,
                                    color = Color.DarkGray
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Cancel Confirmation Dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = Color.Red,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "Cancel Request?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Are you sure you want to cancel this pickup? The household will be notified.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelDialog = false
                        onCancelRequest()
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red
                    )
                ) {
                    Text("Yes, Cancel", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Keep Request", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
private fun PickupLocationMap(
    location: PickupRequest.Location,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mapView = remember(location.latitude, location.longitude) {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(false)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            setBuiltInZoomControls(false)
            isClickable = false
            isFocusable = false
            isFocusableInTouchMode = false
            setOnTouchListener { _, _ -> true }
        }
    }

    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose { mapView.onPause() }
    }

    val geoPoint = remember(location.latitude, location.longitude) {
        GeoPoint(location.latitude, location.longitude)
    }

    LaunchedEffect(geoPoint) {
        mapView.overlays.clear()
        val marker = Marker(mapView).apply {
            position = geoPoint
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = location.address
        }
        mapView.overlays.add(marker)
        mapView.controller.setZoom(16.0)
        mapView.controller.setCenter(geoPoint)
        mapView.invalidate()
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
    ) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CollectorRequestDetailScreenPreview() {
    MaterialTheme {
        val snackbarHostState = remember { SnackbarHostState() }
        CollectorRequestDetailScreen(
            request = PickupRequest(
                id = "req123456789",
                householdId = "user1",
                wasteItems = listOf(
                    WasteItem("plastic", 5.0, 10.0, "Clean plastic bottles"),
                    WasteItem("paper", 3.0, 6.0, "Newspapers and magazines"),
                    WasteItem("metal", 2.0, 8.0, "Aluminum cans")
                ),
                pickupLocation = PickupRequest.Location(
                    latitude = -6.200000,
                    longitude = 106.816666,
                    address = "123 Main Street, Springfield, IL 62701"
                ),
                status = PickupRequest.STATUS_ACCEPTED,
                notes = "Please ring the doorbell twice. Items are in the garage.",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            ),
            household = User(
                id = "user1",
                fullName = "Jane Smith",
                email = "jane.smith@example.com",
                phone = "+1 (555) 987-6543",
                userType = User.ROLE_HOUSEHOLD
            ),
            snackbarHostState = snackbarHostState,
            onNavigateToLocation = {},
            onContactHousehold = {}
        )
    }
}
