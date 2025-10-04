package com.example.handsonpapb_15sep.ui.screens.household

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.handsonpapb_15sep.model.PickupRequest
import com.example.handsonpapb_15sep.model.WasteItem
import com.example.handsonpapb_15sep.ui.components.HouseholdBottomNavBar
import com.example.handsonpapb_15sep.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyRequestsScreen(
    requests: List<PickupRequest> = emptyList(),
    onRequestClick: (String) -> Unit = {},
    onNavigate: (String) -> Unit = {}
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "My Requests",
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
                selectedRoute = "my_requests",
                onNavigate = onNavigate
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->
        if (requests.isEmpty()) {
            // Empty State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Inbox,
                        contentDescription = "No requests",
                        tint = Color.Gray.copy(alpha = 0.5f),
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Pickup Requests",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You haven't made any pickup requests yet.",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${requests.size} pickup request${if (requests.size != 1) "s" else ""}",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }

                items(requests) { request ->
                    RequestCard(
                        request = request,
                        onClick = { onRequestClick(request.id) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun RequestCard(
    request: PickupRequest,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row - Status Badge and Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(status = request.status)

                Text(
                    text = formatDate(request.createdAt),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Request ID
            Text(
                text = "Request #${request.id.take(8).uppercase()}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Location
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location",
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = request.pickupLocation.address,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Waste Items Summary
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Items",
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${request.wasteItems.size} item${if (request.wasteItems.size != 1) "s" else ""} • ${request.wasteItems.sumOf { it.weight }} kg",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Divider(color = Color.LightGray.copy(alpha = 0.3f))

            Spacer(modifier = Modifier.height(12.dp))

            // Footer Row - Value and Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Estimated Value",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "$${request.wasteItems.sumOf { it.estimatedValue }}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )
                }

                // Action based on status
                when (request.status) {
                    "pending" -> {
                        TextButton(onClick = onClick) {
                            Text(
                                text = "View Details",
                                color = PrimaryGreen,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "View",
                                tint = PrimaryGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    "accepted", "in_progress" -> {
                        OutlinedButton(
                            onClick = onClick,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = PrimaryGreen
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = "Track",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    else -> {
                        TextButton(onClick = onClick) {
                            Text(
                                text = "View",
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (backgroundColor, textColor, displayText) = when (status) {
        "pending" -> Triple(StatusPending.copy(alpha = 0.1f), StatusPending, "Pending")
        "accepted" -> Triple(StatusAccepted.copy(alpha = 0.1f), StatusAccepted, "Accepted")
        "in_progress" -> Triple(StatusInProgress.copy(alpha = 0.1f), StatusInProgress, "In Progress")
        "completed" -> Triple(StatusCompleted.copy(alpha = 0.1f), StatusCompleted, "Completed")
        "cancelled" -> Triple(StatusCancelled.copy(alpha = 0.1f), StatusCancelled, "Cancelled")
        else -> Triple(Color.Gray.copy(alpha = 0.1f), Color.Gray, status.replaceFirstChar { it.uppercase() })
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor
    ) {
        Text(
            text = displayText,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Preview(showBackground = true)
@Composable
fun MyRequestsScreenPreview() {
    SampahJujurTheme {
        MyRequestsScreen(
            requests = listOf(
                PickupRequest(
                    id = "req123456789",
                    householdId = "user1",
                    wasteItems = listOf(
                        WasteItem("plastic", 5.0, 10.0, "Clean bottles"),
                        WasteItem("paper", 3.0, 6.0, "Newspapers")
                    ),
                    pickupLocation = PickupRequest.Location(
                        latitude = 0.0,
                        longitude = 0.0,
                        address = "123 Main Street, Springfield, IL 62701"
                    ),
                    status = "pending",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                ),
                PickupRequest(
                    id = "req987654321",
                    householdId = "user1",
                    wasteItems = listOf(
                        WasteItem("metal", 10.0, 25.0, "Aluminum cans")
                    ),
                    pickupLocation = PickupRequest.Location(
                        latitude = 0.0,
                        longitude = 0.0,
                        address = "456 Oak Avenue, Downtown"
                    ),
                    status = "accepted",
                    createdAt = System.currentTimeMillis() - 86400000,
                    updatedAt = System.currentTimeMillis()
                )
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MyRequestsScreenEmptyPreview() {
    SampahJujurTheme {
        MyRequestsScreen()
    }
}
