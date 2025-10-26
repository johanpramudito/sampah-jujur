package com.melodi.sampahjujur.ui.screens.household

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.melodi.sampahjujur.model.PickupRequest
import com.melodi.sampahjujur.model.WasteItem
import com.melodi.sampahjujur.ui.theme.PrimaryGreen
import com.melodi.sampahjujur.ui.theme.SampahJujurTheme
import com.melodi.sampahjujur.viewmodel.HouseholdViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseholdStatisticsScreen(
    viewModel: HouseholdViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {}
) {
    val requests by viewModel.userRequests.observeAsState(emptyList())

    // Calculate statistics
    val totalRequests = requests.size
    val pendingRequests = requests.count { it.status == PickupRequest.STATUS_PENDING }
    val acceptedRequests = requests.count { it.status == PickupRequest.STATUS_ACCEPTED || it.status == PickupRequest.STATUS_IN_PROGRESS }
    val completedRequests = requests.count { it.status == PickupRequest.STATUS_COMPLETED }
    val cancelledRequests = requests.count { it.status == PickupRequest.STATUS_CANCELLED }

    val totalWaste = requests.sumOf { it.wasteItems.sumOf { item -> item.weight } }
    val totalEarnings = requests.filter { it.status == PickupRequest.STATUS_COMPLETED }
        .sumOf { it.wasteItems.sumOf { item -> item.estimatedValue } }

    // Waste type breakdown
    val wasteByType = requests.flatMap { it.wasteItems }
        .groupBy { it.type }
        .mapValues { (_, items) ->
            items.sumOf { it.weight }
        }
        .toList()
        .sortedByDescending { it.second }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "My Statistics",
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

            // Overview Cards
            item {
                Text(
                    text = "Overview",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OverviewCard(
                        icon = Icons.Default.List,
                        value = totalRequests.toString(),
                        label = "Total\nRequests",
                        iconColor = PrimaryGreen,
                        modifier = Modifier.weight(1f)
                    )
                    OverviewCard(
                        icon = Icons.Default.Delete,
                        value = "${String.format("%.1f", totalWaste)} kg",
                        label = "Total\nWaste",
                        iconColor = Color(0xFF2196F3),
                        modifier = Modifier.weight(1f)
                    )
                    OverviewCard(
                        icon = Icons.Default.AttachMoney,
                        value = "Rp ${String.format("%,d", totalEarnings.toInt())}",
                        label = "Total\nEarned",
                        iconColor = Color(0xFFFF9800),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Request Status Breakdown
            item {
                Text(
                    text = "Request Status",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

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
                        StatusItem(
                            status = "Pending",
                            count = pendingRequests,
                            color = Color(0xFFFFC107),
                            icon = Icons.Default.Schedule
                        )
                        if (acceptedRequests > 0 || completedRequests > 0 || cancelledRequests > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = Color.LightGray.copy(alpha = 0.3f)
                            )
                        }
                        if (acceptedRequests > 0) {
                            StatusItem(
                                status = "In Progress",
                                count = acceptedRequests,
                                color = Color(0xFF2196F3),
                                icon = Icons.Default.LocalShipping
                            )
                            if (completedRequests > 0 || cancelledRequests > 0) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    color = Color.LightGray.copy(alpha = 0.3f)
                                )
                            }
                        }
                        if (completedRequests > 0) {
                            StatusItem(
                                status = "Completed",
                                count = completedRequests,
                                color = PrimaryGreen,
                                icon = Icons.Default.CheckCircle
                            )
                            if (cancelledRequests > 0) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    color = Color.LightGray.copy(alpha = 0.3f)
                                )
                            }
                        }
                        if (cancelledRequests > 0) {
                            StatusItem(
                                status = "Cancelled",
                                count = cancelledRequests,
                                color = Color(0xFFF44336),
                                icon = Icons.Default.Cancel
                            )
                        }
                    }
                }
            }

            // Waste Type Breakdown
            if (wasteByType.isNotEmpty()) {
                item {
                    Text(
                        text = "Waste by Type",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

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
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            wasteByType.take(6).forEach { (type, weight) ->
                                WasteTypeItem(
                                    type = type,
                                    weight = weight,
                                    percentage = (weight / totalWaste * 100).toFloat(),
                                    color = getColorForWasteType(type)
                                )
                            }
                        }
                    }
                }
            }

            // Achievement/Milestone Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = PrimaryGreen.copy(alpha = 0.1f)
                    ),
                    elevation = CardDefaults.cardElevation(0.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Achievement",
                            tint = PrimaryGreen,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Great Job!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = PrimaryGreen
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "You've recycled ${String.format("%.1f", totalWaste)} kg of waste so far!",
                                fontSize = 14.sp,
                                color = Color.DarkGray
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun OverviewCard(
    icon: ImageVector,
    value: String,
    label: String,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(iconColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun StatusItem(
    status: String,
    count: Int,
    color: Color,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = status,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = count.toString(),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun WasteTypeItem(
    type: String,
    weight: Double,
    percentage: Float,
    color: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = type.replaceFirstChar { it.uppercase() },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = "${String.format("%.1f", weight)} kg (${String.format("%.1f", percentage)}%)",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { percentage / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.2f),
        )
    }
}

fun getColorForWasteType(type: String): Color {
    return when (type.lowercase()) {
        "plastic" -> Color(0xFF2196F3) // Blue
        "paper" -> Color(0xFF8BC34A) // Light Green
        "metal" -> Color(0xFF9E9E9E) // Gray
        "glass" -> Color(0xFF00BCD4) // Cyan
        "electronics" -> Color(0xFFFF9800) // Orange
        "cardboard" -> Color(0xFF795548) // Brown
        else -> Color(0xFF607D8B) // Blue Gray
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun HouseholdStatisticsScreenPreview() {
    SampahJujurTheme {
        // Create mock data for preview
        val mockRequests = listOf(
            PickupRequest(
                id = "1",
                householdId = "user1",
                status = PickupRequest.STATUS_COMPLETED,
                wasteItems = listOf(
                    WasteItem(type = "plastic", weight = 5.0, estimatedValue = 20000.0, description = "Bottles"),
                    WasteItem(type = "paper", weight = 3.0, estimatedValue = 6000.0, description = "Newspapers")
                ),
                createdAt = System.currentTimeMillis()
            ),
            PickupRequest(
                id = "2",
                householdId = "user1",
                status = PickupRequest.STATUS_COMPLETED,
                wasteItems = listOf(
                    WasteItem(type = "metal", weight = 2.0, estimatedValue = 18000.0, description = "Cans"),
                    WasteItem(type = "cardboard", weight = 4.0, estimatedValue = 4800.0, description = "Boxes")
                ),
                createdAt = System.currentTimeMillis()
            ),
            PickupRequest(
                id = "3",
                householdId = "user1",
                status = PickupRequest.STATUS_PENDING,
                wasteItems = listOf(
                    WasteItem(type = "plastic", weight = 3.0, estimatedValue = 12000.0, description = "Containers"),
                    WasteItem(type = "glass", weight = 6.0, estimatedValue = 6000.0, description = "Bottles")
                ),
                createdAt = System.currentTimeMillis()
            ),
            PickupRequest(
                id = "4",
                householdId = "user1",
                status = PickupRequest.STATUS_ACCEPTED,
                wasteItems = listOf(
                    WasteItem(type = "electronics", weight = 1.5, estimatedValue = 3000.0, description = "Old cables")
                ),
                createdAt = System.currentTimeMillis()
            ),
            PickupRequest(
                id = "5",
                householdId = "user1",
                status = PickupRequest.STATUS_CANCELLED,
                wasteItems = listOf(
                    WasteItem(type = "paper", weight = 2.0, estimatedValue = 4000.0, description = "Magazines")
                ),
                createdAt = System.currentTimeMillis()
            )
        )

        // Calculate statistics
        val totalRequests = mockRequests.size
        val pendingRequests = mockRequests.count { it.status == PickupRequest.STATUS_PENDING }
        val acceptedRequests = mockRequests.count { it.status == PickupRequest.STATUS_ACCEPTED || it.status == PickupRequest.STATUS_IN_PROGRESS }
        val completedRequests = mockRequests.count { it.status == PickupRequest.STATUS_COMPLETED }
        val cancelledRequests = mockRequests.count { it.status == PickupRequest.STATUS_CANCELLED }

        val totalWaste = mockRequests.sumOf { it.wasteItems.sumOf { item -> item.weight } }
        val totalEarnings = mockRequests.filter { it.status == PickupRequest.STATUS_COMPLETED }
            .sumOf { it.wasteItems.sumOf { item -> item.estimatedValue } }

        // Waste type breakdown
        val wasteByType = mockRequests.flatMap { it.wasteItems }
            .groupBy { it.type }
            .mapValues { (_, items) -> items.sumOf { it.weight } }
            .toList()
            .sortedByDescending { it.second }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "My Statistics",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {}) {
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

                // Overview Cards
                item {
                    Text(
                        text = "Overview",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OverviewCard(
                            icon = Icons.Default.List,
                            value = totalRequests.toString(),
                            label = "Total\nRequests",
                            iconColor = PrimaryGreen,
                            modifier = Modifier.weight(1f)
                        )
                        OverviewCard(
                            icon = Icons.Default.Delete,
                            value = "${String.format("%.1f", totalWaste)} kg",
                            label = "Total\nWaste",
                            iconColor = Color(0xFF2196F3),
                            modifier = Modifier.weight(1f)
                        )
                        OverviewCard(
                            icon = Icons.Default.AttachMoney,
                            value = "Rp ${String.format("%,d", totalEarnings.toInt())}",
                            label = "Total\nEarned",
                            iconColor = Color(0xFFFF9800),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Request Status Breakdown
                item {
                    Text(
                        text = "Request Status",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

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
                            StatusItem(
                                status = "Pending",
                                count = pendingRequests,
                                color = Color(0xFFFFC107),
                                icon = Icons.Default.Schedule
                            )
                            if (acceptedRequests > 0 || completedRequests > 0 || cancelledRequests > 0) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    color = Color.LightGray.copy(alpha = 0.3f)
                                )
                            }
                            if (acceptedRequests > 0) {
                                StatusItem(
                                    status = "In Progress",
                                    count = acceptedRequests,
                                    color = Color(0xFF2196F3),
                                    icon = Icons.Default.LocalShipping
                                )
                                if (completedRequests > 0 || cancelledRequests > 0) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 12.dp),
                                        color = Color.LightGray.copy(alpha = 0.3f)
                                    )
                                }
                            }
                            if (completedRequests > 0) {
                                StatusItem(
                                    status = "Completed",
                                    count = completedRequests,
                                    color = PrimaryGreen,
                                    icon = Icons.Default.CheckCircle
                                )
                                if (cancelledRequests > 0) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 12.dp),
                                        color = Color.LightGray.copy(alpha = 0.3f)
                                    )
                                }
                            }
                            if (cancelledRequests > 0) {
                                StatusItem(
                                    status = "Cancelled",
                                    count = cancelledRequests,
                                    color = Color(0xFFF44336),
                                    icon = Icons.Default.Cancel
                                )
                            }
                        }
                    }
                }

                // Waste Type Breakdown
                if (wasteByType.isNotEmpty()) {
                    item {
                        Text(
                            text = "Waste by Type",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }

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
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                wasteByType.take(6).forEach { (type, weight) ->
                                    WasteTypeItem(
                                        type = type,
                                        weight = weight,
                                        percentage = (weight / totalWaste * 100).toFloat(),
                                        color = getColorForWasteType(type)
                                    )
                                }
                            }
                        }
                    }
                }

                // Achievement/Milestone Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = PrimaryGreen.copy(alpha = 0.1f)
                        ),
                        elevation = CardDefaults.cardElevation(0.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = "Achievement",
                                tint = PrimaryGreen,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Great Job!",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = PrimaryGreen
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "You've recycled ${String.format("%.1f", totalWaste)} kg of waste so far!",
                                    fontSize = 14.sp,
                                    color = Color.DarkGray
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}
