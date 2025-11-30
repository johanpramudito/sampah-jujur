package com.melodi.sampahjujur.ui.screens.household

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.BottomSheetValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.material.rememberBottomSheetState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.CachePolicy
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.melodi.sampahjujur.model.LocationUpdate
import com.melodi.sampahjujur.model.PickupRequest
import com.melodi.sampahjujur.model.WasteItem
import com.melodi.sampahjujur.ui.theme.*
import com.melodi.sampahjujur.viewmodel.HouseholdViewModel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.util.BoundingBox
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

/**
 * Route composable for Household Request Detail Screen
 * Handles data fetching and state management
 */
@Composable
fun HouseholdRequestDetailRoute(
    requestId: String,
    onBackClick: () -> Unit,
    onNavigateToChat: (String) -> Unit = {},
    viewModel: HouseholdViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val request by viewModel.observeRequest(requestId).collectAsState(initial = null)
    var collectorInfo by remember { mutableStateOf<com.melodi.sampahjujur.model.User?>(null) }
    val scope = rememberCoroutineScope()

    // Observe real-time collector location ONLY when request is in_progress
    val collectorLocation by remember(request?.status) {
        if (request?.status == PickupRequest.STATUS_IN_PROGRESS) {
            viewModel.observeCollectorLocation(requestId)
        } else {
            flowOf(null)
        }
    }.collectAsState(initial = null)

    // Log location updates for debugging
    LaunchedEffect(collectorLocation) {
        collectorLocation?.let {
            android.util.Log.d("HouseholdLocation", "Collector location updated: lat=${it.latitude}, lng=${it.longitude}, accuracy=${it.accuracy}m, timestamp=${it.timestamp}")
        }
    }

    fun launchDialer(phone: String?) {
        val target = phone?.takeIf { it.isNotBlank() }
        if (target == null) {
            Toast.makeText(context, "Collector phone number unavailable", Toast.LENGTH_SHORT).show()
            return
        }

        val phoneUri = Uri.fromParts("tel", target, null)
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
                // try next
            } catch (_: Exception) {
                // ignore and try next intent
            }
        }

        if (!launched) {
            val chooser = Intent.createChooser(
                Intent(Intent.ACTION_DIAL, phoneUri),
                "Call collector with"
            )
            try {
                context.startActivity(chooser)
                launched = true
            } catch (_: Exception) {
                // unable to launch chooser
            }
        }

        if (!launched) {
            Toast.makeText(context, "No dialer application found", Toast.LENGTH_SHORT).show()
        }
    }

    // Fetch collector information when request has a collector assigned
    LaunchedEffect(request) {
        val currentRequest = request
        if (currentRequest != null) {
            val collectorId = currentRequest.collectorId
            if (!collectorId.isNullOrBlank()) {
                scope.launch {
                    collectorInfo = viewModel.getCollectorInfo(collectorId)
                }
            } else {
                collectorInfo = null
            }
        }
    }

    if (request == null) {
        // Show loading state
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = PrimaryGreen)
        }
    } else {
        RequestDetailScreen(
            request = request!!,
            collectorName = collectorInfo?.fullName,
            collectorPhone = collectorInfo?.phone,
            collectorVehicle = if (collectorInfo?.vehicleType?.isNotBlank() == true) {
                "${collectorInfo?.vehicleType} - ${collectorInfo?.vehiclePlateNumber}"
            } else null,
            collectorProfileImageUrl = collectorInfo?.profileImageUrl,
            collectorLocation = collectorLocation,
            onBackClick = onBackClick,
            onCancelRequest = {
                viewModel.cancelPickupRequest(requestId)
                onBackClick()
            },
            onContactCollector = { launchDialer(collectorInfo?.phone) },
            onOpenChat = { onNavigateToChat(requestId) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestDetailScreen(
    request: PickupRequest,
    collectorName: String? = null,
    collectorPhone: String? = null,
    collectorVehicle: String? = null,
    collectorProfileImageUrl: String? = null,
    collectorLocation: LocationUpdate? = null,
    onBackClick: () -> Unit = {},
    onCancelRequest: () -> Unit = {},
    onContactCollector: () -> Unit = {},
    onOpenChat: () -> Unit = {}
) {
    var showCancelDialog by remember { mutableStateOf(false) }
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }
    var showExpandedMap by remember { mutableStateOf(false) }

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
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
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

            // Location Card with Map
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
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Location",
                                tint = PrimaryGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pickup Location",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Live Tracking Info Banner with Collector Details (only for in_progress status)
                        if (request.status == "in_progress") {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White
                                ),
                                elevation = CardDefaults.cardElevation(2.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    // Live tracking indicator
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MyLocation,
                                            contentDescription = "Live Tracking",
                                            tint = Color(0xFF1976D2),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (collectorLocation != null)
                                                "Collector is on the way"
                                            else
                                                "Waiting for collector location...",
                                            fontSize = 13.sp,
                                            color = Color(0xFF424242),
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(
                                                    Color(0xFF2196F3),
                                                    CircleShape
                                                )
                                        )
                                    }

                                    // Collector info (if available)
                                    if (collectorName != null) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        HorizontalDivider(
                                            color = Color.LightGray.copy(alpha = 0.3f)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Collector Avatar
                                            ProfileImage(
                                                imageUrl = collectorProfileImageUrl,
                                                size = 44.dp,
                                                iconSize = 24.dp
                                            )

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = collectorName,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.Black
                                                )
                                                if (collectorVehicle != null) {
                                                    Spacer(modifier = Modifier.height(3.dp))
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = Icons.Default.DirectionsCar,
                                                            contentDescription = "Vehicle",
                                                            tint = Color.Gray,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = collectorVehicle,
                                                            fontSize = 13.sp,
                                                            color = Color.Gray
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // OpenStreetMap Preview with Collector Location
                        Box {
                            LiveTrackingMapPreview(
                                pickupLatitude = request.pickupLocation.latitude,
                                pickupLongitude = request.pickupLocation.longitude,
                                collectorLocation = collectorLocation
                            )
                            // Track Location button overlay (only for in_progress)
                            if (request.status == "in_progress") {
                                Button(
                                    onClick = { showExpandedMap = true },
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PrimaryGreen
                                    ),
                                    shape = RoundedCornerShape(20.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    elevation = ButtonDefaults.buttonElevation(
                                        defaultElevation = 4.dp,
                                        pressedElevation = 8.dp
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MyLocation,
                                        contentDescription = "Track Location",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Track Location",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Address",
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = request.pickupLocation.address,
                                fontSize = 14.sp,
                                color = Color.DarkGray,
                                modifier = Modifier.weight(1f)
                            )
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
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Waste",
                                tint = PrimaryGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Waste Items (${request.wasteItems.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        request.wasteItems.forEachIndexed { index, item ->
                            WasteItemDetailCard(
                                item = item,
                                onImageClick = { imageUrl ->
                                    selectedImageUrl = imageUrl
                                }
                            )
                            if (index < request.wasteItems.size - 1) {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                        Spacer(modifier = Modifier.height(16.dp))

                        // Totals Summary
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Total Weight
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Scale,
                                        contentDescription = "Weight",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Total Weight",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${String.format("%.1f", request.wasteItems.sumOf { it.weight })} kg",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }

                            // Total Value
                            Column(horizontalAlignment = Alignment.End) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AttachMoney,
                                        contentDescription = "Value",
                                        tint = PrimaryGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Estimated Value",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Rp ${String.format("%,.0f", request.wasteItems.sumOf { it.estimatedValue })}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreen
                                )
                            }
                        }
                    }
                }
            }

            // Notes Card (if notes exist)
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notes,
                                    contentDescription = "Notes",
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Additional Notes",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFF9F9F9)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = request.notes,
                                    fontSize = 14.sp,
                                    color = Color.DarkGray,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Collector Info Card (if collector assigned)
            if (collectorName != null) {
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
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Collector",
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Collector Information",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ProfileImage(
                                    imageUrl = collectorProfileImageUrl,
                                    size = 56.dp,
                                    iconSize = 32.dp
                                )

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = collectorName,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (collectorPhone != null) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable(onClick = onContactCollector)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Phone,
                                                contentDescription = "Call collector",
                                                tint = PrimaryGreen,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = collectorPhone,
                                                fontSize = 14.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                    if (collectorVehicle != null) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.DirectionsCar,
                                                contentDescription = "Vehicle",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = collectorVehicle,
                                                fontSize = 14.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Action Buttons
            item {
                when (request.status) {
                    "pending" -> {
                        Button(
                            onClick = { showCancelDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Red.copy(alpha = 0.1f),
                                contentColor = Color.Red
                            ),
                            shape = RoundedCornerShape(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = "Cancel"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Cancel Request",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    "accepted", "in_progress" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Open Chat Button
                            Button(
                                onClick = onOpenChat,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryGreen
                                ),
                                shape = RoundedCornerShape(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Message,
                                    contentDescription = "Chat"
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Open Chat",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }

                            // Call Collector Button
                            OutlinedButton(
                                onClick = onContactCollector,
                                modifier = Modifier
                                    .height(56.dp),
                                shape = RoundedCornerShape(28.dp),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    width = 2.dp
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "Call",
                                    tint = PrimaryGreen
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
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
                Text("Are you sure you want to cancel this pickup request? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelDialog = false
                        onCancelRequest()
                    },
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

    // Expanded Map Dialog
    if (showExpandedMap) {
        ExpandedMapDialog(
            request = request,
            collectorLocation = collectorLocation,
            collectorName = collectorName,
            collectorVehicle = collectorVehicle,
            collectorProfileImageUrl = collectorProfileImageUrl,
            onDismiss = { showExpandedMap = false },
            onContactCollector = onContactCollector,
            onOpenChat = onOpenChat
        )
    }

    // Full-Screen Image Viewer
    if (selectedImageUrl != null) {
        Dialog(
            onDismissRequest = { selectedImageUrl = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { selectedImageUrl = null }
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(selectedImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Waste Item Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                IconButton(
                    onClick = { selectedImageUrl = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun WasteItemDetailCard(
    item: WasteItem,
    onImageClick: (String) -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon based on waste type
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            PrimaryGreen.copy(alpha = 0.1f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getWasteTypeIcon(item.type),
                        contentDescription = item.type,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.type.replaceFirstChar { it.uppercase() },
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    if (item.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.description,
                            fontSize = 13.sp,
                            color = Color.Gray,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${String.format("%.1f", item.weight)} kg",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Rp ${String.format("%,.0f", item.estimatedValue)}",
                        fontSize = 13.sp,
                        color = PrimaryGreen,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Show image if available
            if (item.imageUrl.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .clickable { onImageClick(item.imageUrl) },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(item.imageUrl)
                            .crossfade(true)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .networkCachePolicy(CachePolicy.ENABLED)
                            .build(),
                        contentDescription = "Waste item image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = android.R.drawable.ic_menu_gallery),
                        error = painterResource(id = android.R.drawable.ic_menu_report_image)
                    )
                }
            }
        }
    }
}

fun getWasteTypeIcon(type: String) = when (type.lowercase()) {
    "plastic" -> Icons.Default.Recycling
    "paper" -> Icons.Default.Description
    "metal" -> Icons.Default.Hardware
    "glass" -> Icons.Default.LocalDrink
    "electronics" -> Icons.Default.PhoneAndroid
    "cardboard" -> Icons.Default.Inventory
    else -> Icons.Default.Delete
}

fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun LiveTrackingMapPreview(
    pickupLatitude: Double,
    pickupLongitude: Double,
    collectorLocation: LocationUpdate? = null
) {
    val context = LocalContext.current
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var pickupMarker by remember { mutableStateOf<Marker?>(null) }
    var collectorMarker by remember { mutableStateOf<Marker?>(null) }
    var isInitialized by remember { mutableStateOf(false) }

    // Initialize OSMDroid configuration
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(
            context,
            context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = context.packageName
    }

    // Update ONLY collector marker position when location changes
    LaunchedEffect(collectorLocation) {
        if (!isInitialized) return@LaunchedEffect // Skip until map is initialized

        mapView?.let { map ->
            android.util.Log.d("MapUpdate", "Updating collector marker position: ${collectorLocation?.latitude}, ${collectorLocation?.longitude}")

            if (collectorLocation != null) {
                // Update or create collector marker
                if (collectorMarker == null) {
                    // First time - create and add collector marker
                    collectorMarker = Marker(map).apply {
                        position = collectorLocation.toGeoPoint()
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "Collector"
                        icon = context.getDrawable(com.melodi.sampahjujur.R.drawable.ic_collector_marker)
                    }
                    map.overlays.add(collectorMarker)
                } else {
                    // Update existing marker position (smooth movement)
                    collectorMarker?.position = collectorLocation.toGeoPoint()
                }
            } else {
                // Remove collector marker if no location
                collectorMarker?.let { map.overlays.remove(it) }
                collectorMarker = null
            }

            map.invalidate()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
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

                    val pickupPoint = GeoPoint(pickupLatitude, pickupLongitude)
                    controller.setZoom(16.0)
                    controller.setCenter(pickupPoint)

                    // Add pickup location marker (default OSMDroid marker)
                    pickupMarker = Marker(this).apply {
                        position = pickupPoint
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "Pickup Location"
                    }
                    overlays.add(pickupMarker)

                    // Store reference
                    mapView = this
                    isInitialized = true
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

data class RouteInfo(
    val points: List<GeoPoint>,
    val distance: Double, // in meters
    val duration: Double  // in seconds
)

fun formatDuration(seconds: Double): String {
    val minutes = (seconds / 60).toInt()
    return when {
        minutes < 1 -> "< 1 min"
        minutes < 60 -> "$minutes min"
        else -> {
            val hours = minutes / 60
            val remainingMinutes = minutes % 60
            if (remainingMinutes == 0) "$hours hr" else "$hours hr $remainingMinutes min"
        }
    }
}

fun formatDistance(meters: Double): String {
    return when {
        meters < 1000 -> "${meters.toInt()} m"
        else -> String.format(Locale.getDefault(), "%.1f km", meters / 1000)
    }
}

suspend fun fetchRoute(startLat: Double, startLon: Double, endLat: Double, endLon: Double): RouteInfo? {
    return withContext(Dispatchers.IO) {
        try {
            val url = "https://router.project-osrm.org/route/v1/driving/$startLon,$startLat;$endLon,$endLat?overview=full&geometries=geojson"
            val response = URL(url).readText()
            val json = JSONObject(response)

            if (json.getString("code") == "Ok") {
                val routes = json.getJSONArray("routes")
                if (routes.length() > 0) {
                    val route = routes.getJSONObject(0)
                    val geometry = route.getJSONObject("geometry")
                    val coordinates = geometry.getJSONArray("coordinates")

                    val points = mutableListOf<GeoPoint>()
                    for (i in 0 until coordinates.length()) {
                        val coord = coordinates.getJSONArray(i)
                        val lon = coord.getDouble(0)
                        val lat = coord.getDouble(1)
                        points.add(GeoPoint(lat, lon))
                    }

                    val distance = route.getDouble("distance")
                    val duration = route.getDouble("duration")

                    RouteInfo(points, distance, duration)
                } else null
            } else null
        } catch (e: Exception) {
            android.util.Log.e("RouteError", "Failed to fetch route", e)
            null
        }
    }
}

@Composable
fun ProfileImage(
    imageUrl: String?,
    size: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp = size * 0.6f,
    fallbackIcon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.Person
) {
    if (!imageUrl.isNullOrBlank()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = "Profile Picture",
            modifier = Modifier
                .size(size)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .background(
                    PrimaryGreen.copy(alpha = 0.1f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = fallbackIcon,
                contentDescription = "Profile",
                tint = PrimaryGreen,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ExpandedMapDialog(
    request: PickupRequest,
    collectorLocation: LocationUpdate?,
    collectorName: String? = null,
    collectorVehicle: String? = null,
    collectorProfileImageUrl: String? = null,
    onDismiss: () -> Unit,
    onContactCollector: () -> Unit = {},
    onOpenChat: () -> Unit = {}
) {
    val pickupLatitude = request.pickupLocation.latitude
    val pickupLongitude = request.pickupLocation.longitude
    val pickupAddress = request.pickupLocation.address
    val context = LocalContext.current
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var pickupMarker by remember { mutableStateOf<Marker?>(null) }
    var collectorMarker by remember { mutableStateOf<Marker?>(null) }
    var routePolyline by remember { mutableStateOf<Polyline?>(null) }
    var routeInfo by remember { mutableStateOf<RouteInfo?>(null) }
    var hasInitialCentering by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Bottom sheet state for collector info
    val bottomSheetState = rememberBottomSheetState(
        initialValue = if (collectorName != null) BottomSheetValue.Collapsed else BottomSheetValue.Collapsed
    )
    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = bottomSheetState
    )
    val isSheetCollapsed by remember {
        derivedStateOf { bottomSheetState.currentValue == BottomSheetValue.Collapsed }
    }

    // Initialize OSMDroid configuration
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(
            context,
            context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = context.packageName
    }

    // Update ONLY collector marker position when location changes (without resetting zoom/center)
    LaunchedEffect(collectorLocation) {
        mapView?.let { map ->
            if (collectorLocation != null) {
                if (collectorMarker == null) {
                    // First time - create and add collector marker
                    collectorMarker = Marker(map).apply {
                        position = collectorLocation.toGeoPoint()
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "Collector"
                        snippet = "Accuracy: ${collectorLocation.getAccuracyDescription()}"
                        icon = context.getDrawable(com.melodi.sampahjujur.R.drawable.ic_collector_marker)
                    }
                    map.overlays.add(collectorMarker)

                    // Only center on first collector location
                    if (!hasInitialCentering) {
                        map.controller.setCenter(collectorLocation.toGeoPoint())
                        hasInitialCentering = true
                    }
                } else {
                    // Just update position (smooth movement, no zoom/center change)
                    collectorMarker?.position = collectorLocation.toGeoPoint()
                    collectorMarker?.snippet = "Accuracy: ${collectorLocation.getAccuracyDescription()}"
                }

                // Fetch and draw route
                coroutineScope.launch {
                    val route = fetchRoute(
                        collectorLocation.latitude,
                        collectorLocation.longitude,
                        pickupLatitude,
                        pickupLongitude
                    )

                    if (route != null) {
                        routeInfo = route

                        // Remove old route polyline
                        routePolyline?.let { map.overlays.remove(it) }

                        // Create new route polyline
                        routePolyline = Polyline().apply {
                            setPoints(route.points)
                            outlinePaint.color = android.graphics.Color.parseColor("#2196F3")
                            outlinePaint.strokeWidth = 12f
                            outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                        }
                        map.overlays.add(0, routePolyline) // Add at bottom so markers are on top

                        map.invalidate()
                    }
                }
            } else {
                // Remove collector marker and route if no location
                collectorMarker?.let { map.overlays.remove(it) }
                collectorMarker = null
                routePolyline?.let { map.overlays.remove(it) }
                routePolyline = null
                routeInfo = null
            }

            map.invalidate()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BottomSheetScaffold(
            scaffoldState = bottomSheetScaffoldState,
            sheetPeekHeight = if (collectorName != null) 250.dp else 0.dp,
            sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            sheetElevation = 8.dp,
            sheetBackgroundColor = Color.White,
            backgroundColor = Color.White,
            sheetContent = {
                // Collector Info Bottom Sheet
                if (collectorName != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.85f) // Use 85% of screen height
                            .padding(16.dp)
                    ) {
                        // Drag handle
                        Box(
                            modifier = Modifier
                                .width(48.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.LightGray.copy(alpha = 0.6f))
                                .align(Alignment.CenterHorizontally)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Row 1: Vehicle info and action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Vehicle info (Icon + License plate)
                            if (collectorVehicle != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsCar,
                                        contentDescription = "Vehicle",
                                        tint = Color.Black,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = collectorVehicle,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Action Buttons (Message and Call)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Call Button
                                FloatingActionButton(
                                    onClick = onContactCollector,
                                    modifier = Modifier.size(44.dp),
                                    containerColor = PrimaryGreen,
                                    shape = CircleShape
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = "Call Collector",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                // Message Button
                                FloatingActionButton(
                                    onClick = onOpenChat,
                                    modifier = Modifier.size(44.dp),
                                    containerColor = Color.White,
                                    shape = CircleShape,
                                    elevation = FloatingActionButtonDefaults.elevation(
                                        defaultElevation = 2.dp
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Message,
                                        contentDescription = "Message Collector",
                                        tint = PrimaryGreen,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Row 2: Collector Avatar and Name
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            border = BorderStroke(2.dp, PrimaryGreen.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Collector Avatar
                                ProfileImage(
                                    imageUrl = collectorProfileImageUrl,
                                    size = 56.dp,
                                    iconSize = 32.dp
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                // Collector Name
                                Text(
                                    text = collectorName,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.Black
                                )
                            }
                        }

                        // Expanded content - show waste items
                        if (!isSheetCollapsed) {
                            Spacer(modifier = Modifier.height(20.dp))

                            // Waste Items Section (scrollable)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Section Header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Waste Items (${request.wasteItems.size})",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.Black
                                    )
                                }

                                // Waste Items List
                                request.wasteItems.forEach { item ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFFF9F9F9)
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Icon based on waste type
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .background(
                                                        PrimaryGreen.copy(alpha = 0.1f),
                                                        CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = getWasteTypeIcon(item.type),
                                                    contentDescription = item.type,
                                                    tint = PrimaryGreen,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            // Item details
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.type.replaceFirstChar { it.uppercase() },
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = "${item.weight} kg",
                                                    fontSize = 12.sp,
                                                    color = Color.Gray
                                                )
                                            }

                                            // Estimated value
                                            Text(
                                                text = "Rp ${String.format("%,d", item.estimatedValue.toInt())}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = PrimaryGreen
                                            )
                                        }
                                    }
                                }

                                // Total Summary
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = PrimaryGreen.copy(alpha = 0.1f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Total Weight",
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )
                                            Text(
                                                text = "${request.getTotalWeight()} kg",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "Estimated Value",
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )
                                            Text(
                                                text = "Rp ${String.format("%,d", request.totalValue.toInt())}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = PrimaryGreen
                                            )
                                        }
                                    }
                                }

                                // Notes Section (if notes exist)
                                if (request.notes.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFFFFF9E6)
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Info,
                                                    contentDescription = "Note",
                                                    tint = Color(0xFFFFA000),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text(
                                                    text = "Note",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFFFFA000)
                                                )
                                            }
                                            Text(
                                                text = request.notes,
                                                fontSize = 13.sp,
                                                color = Color(0xFF5D4037),
                                                lineHeight = 18.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.height(1.dp))
                }
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            ) {
                // Full-screen interactive map
                AndroidView(
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)

                            // Make interactive
                            setMultiTouchControls(true)
                            isClickable = true

                            val pickupPoint = GeoPoint(pickupLatitude, pickupLongitude)
                            controller.setZoom(16.0)
                            controller.setCenter(pickupPoint)

                            // Add pickup location marker (uses default OSMDroid marker)
                            pickupMarker = Marker(this).apply {
                                position = pickupPoint
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                title = "Pickup Location"
                                snippet = pickupAddress
                            }
                            overlays.add(pickupMarker)

                            // Store reference
                            mapView = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

            // Header with address and back button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.95f)
                    ),
                    elevation = CardDefaults.cardElevation(4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Back button on left
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.DarkGray
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Location",
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Live Tracking",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = pickupAddress,
                                fontSize = 12.sp,
                                color = Color.Gray,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            // ETA and Distance Info
                            if (routeInfo != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // ETA
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = "ETA",
                                            tint = PrimaryGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = formatDuration(routeInfo!!.duration),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = PrimaryGreen
                                        )
                                    }

                                    // Distance
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.DirectionsCar,
                                            contentDescription = "Distance",
                                            tint = PrimaryGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = formatDistance(routeInfo!!.distance),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = PrimaryGreen
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Zoom controls (right side, positioned behind bottom sheet)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 270.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Zoom In Button
                FloatingActionButton(
                    onClick = { mapView?.controller?.zoomIn() },
                    modifier = Modifier.size(48.dp),
                    containerColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom In",
                        tint = PrimaryGreen
                    )
                }

                // Zoom Out Button
                FloatingActionButton(
                    onClick = { mapView?.controller?.zoomOut() },
                    modifier = Modifier.size(48.dp),
                    containerColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Zoom Out",
                        tint = PrimaryGreen
                    )
                }
            }
            }
        }
    }
}

@Composable
fun StaticMapPreview(
    latitude: Double,
    longitude: Double
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
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
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
            update = { mapView ->
                // Update map center and marker when location changes
                val newLocation = GeoPoint(latitude, longitude)
                mapView.controller.setCenter(newLocation)

                // Update marker position
                if (mapView.overlays.isNotEmpty()) {
                    val marker = mapView.overlays.firstOrNull { it is Marker } as? Marker
                    marker?.position = newLocation
                }

                mapView.invalidate()
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RequestDetailScreenPreview() {
    SampahJujurTheme {
        RequestDetailScreen(
            request = PickupRequest(
                id = "req123456789",
                householdId = "user1",
                wasteItems = listOf(
                    WasteItem(
                        type = "plastic",
                        weight = 5.0,
                        estimatedValue = 25000.0,
                        description = "Clean plastic bottles and containers",
                        imageUrl = "https://via.placeholder.com/300"
                    ),
                    WasteItem(
                        type = "paper",
                        weight = 3.0,
                        estimatedValue = 15000.0,
                        description = "Newspapers and magazines"
                    ),
                    WasteItem(
                        type = "metal",
                        weight = 2.0,
                        estimatedValue = 18000.0,
                        description = "Aluminum cans",
                        imageUrl = "https://via.placeholder.com/300"
                    )
                ),
                pickupLocation = PickupRequest.Location(
                    latitude = -6.200000,
                    longitude = 106.816666,
                    address = "Jl. Sudirman No. 123, Jakarta Pusat, DKI Jakarta 10110"
                ),
                status = "accepted",
                notes = "Please ring the doorbell twice. Items are neatly arranged in the garage on the left side.",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            ),
            collectorName = "John Doe",
            collectorPhone = "+62 812-3456-7890",
            collectorVehicle = "Blue Truck - B 1234 XYZ"
        )
    }
}

// Helper function to calculate distance between two coordinates
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
