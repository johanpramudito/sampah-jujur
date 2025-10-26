package com.melodi.sampahjujur.ui.screens.collector

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melodi.sampahjujur.model.PickupRequest
import com.melodi.sampahjujur.model.TransactionItem
import com.melodi.sampahjujur.model.WasteItem
import com.melodi.sampahjujur.ui.screens.household.StatusBadge
import com.melodi.sampahjujur.ui.screens.household.WasteItemDetailCard
import com.melodi.sampahjujur.ui.screens.household.formatDateTime
import com.melodi.sampahjujur.ui.theme.PrimaryGreen
import com.melodi.sampahjujur.viewmodel.CollectorViewModel
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectorRequestDetailRoute(
    requestId: String,
    onBackClick: () -> Unit,
    viewModel: CollectorViewModel = hiltViewModel()
) {
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

    if (currentRequest == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        CollectorRequestDetailScreen(
            request = currentRequest,
            householdName = currentRequest.householdId,
            householdPhone = null,
            isLoading = uiState.isLoading,
            snackbarHostState = snackbarHostState,
            onBackClick = onBackClick,
            onAcceptRequest = { viewModel.acceptPickupRequest(currentRequest) },
            onNavigateToLocation = { viewModel.getRouteToPickup(currentRequest) },
            onStartPickup = { viewModel.markRequestInProgress(currentRequest.id) },
            onCompletePickup = { showCompleteDialog = true },
            onContactHousehold = { /* TODO: Integrate contact action */ },
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
    householdName: String = "Unknown User",
    householdPhone: String? = null,
    isLoading: Boolean = false,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit = {},
    onAcceptRequest: () -> Unit = {},
    onNavigateToLocation: () -> Unit = {},
    onStartPickup: () -> Unit = {},
    onCompletePickup: () -> Unit = {},
    onContactHousehold: () -> Unit = {},
    onCancelRequest: () -> Unit = {}
) {
    var showCancelDialog by remember { mutableStateOf(false) }

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
                    .padding(bottom = 88.dp), // Space for bottom action buttons
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
                                        text = householdName,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (householdPhone != null) {
                                        Text(
                                            text = householdPhone,
                                            fontSize = 14.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }

                                IconButton(onClick = onContactHousehold) {
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

                            // Map Placeholder
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
                                Icon(
                                    imageVector = Icons.Default.Map,
                                    contentDescription = "Map",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(48.dp)
                                )
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
                                    text = request.pickupLocation.address,
                                    fontSize = 14.sp,
                                    color = Color.DarkGray,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (request.status == "accepted" || request.status == "in_progress") {
                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedButton(
                                    onClick = onNavigateToLocation,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = PrimaryGreen
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Navigation,
                                        contentDescription = "Navigate"
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Get Directions")
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
                                Text(
                                    text = "Special Instructions",
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

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }

            // Bottom Action Buttons
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (request.status) {
                        PickupRequest.STATUS_PENDING -> {
                            Button(
                                onClick = onAcceptRequest,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                enabled = !isLoading,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryGreen
                                ),
                                shape = RoundedCornerShape(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Accept"
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Accept Request",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                        PickupRequest.STATUS_ACCEPTED -> {
                            Button(
                                onClick = onStartPickup,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                enabled = !isLoading,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryGreen
                                ),
                                shape = RoundedCornerShape(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Start"
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Start Pickup",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }

                            OutlinedButton(
                                onClick = { showCancelDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                enabled = !isLoading,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.Red
                                ),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Text("Cancel Request")
                            }
                        }
                        PickupRequest.STATUS_IN_PROGRESS -> {
                            Button(
                                onClick = onCompletePickup,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                enabled = !isLoading,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryGreen
                                ),
                                shape = RoundedCornerShape(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Complete"
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Complete Pickup",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                        PickupRequest.STATUS_CANCELLED -> {
                            Text(
                                text = "This pickup was cancelled.",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
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
                    latitude = 0.0,
                    longitude = 0.0,
                    address = "123 Main Street, Springfield, IL 62701"
                ),
                status = "accepted",
                notes = "Please ring the doorbell twice. Items are in the garage.",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            ),
            householdName = "Jane Smith",
            householdPhone = "+1 (555) 987-6543",
            snackbarHostState = snackbarHostState
        )
    }
}
