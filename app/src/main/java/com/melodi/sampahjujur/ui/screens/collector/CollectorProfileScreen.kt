package com.melodi.sampahjujur.ui.screens.collector

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.melodi.sampahjujur.model.User
import com.melodi.sampahjujur.ui.components.CollectorBottomNavBar
import com.melodi.sampahjujur.ui.theme.PrimaryGreen
import com.melodi.sampahjujur.ui.theme.SampahJujurTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectorProfileScreen(
    user: User,
    totalCollections: Int = 0,
    totalWasteCollected: Double = 0.0,
    totalSpent: Double = 0.0,
    completionRate: Double = 0.0,
    vehicleType: String = "",
    vehiclePlateNumber: String = "",
    profileImageUrl: String? = null,
    onEditProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onPerformanceClick: () -> Unit = {},
    onHelpSupportClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onNavigate: (String) -> Unit = {}
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Profile",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = {
            CollectorBottomNavBar(
                selectedRoute = "collector_profile",
                onNavigate = onNavigate
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Profile Header Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = PrimaryGreen
                    ),
                    elevation = CardDefaults.cardElevation(4.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (!profileImageUrl.isNullOrBlank()) {
                            Image(
                                painter = rememberAsyncImagePainter(profileImageUrl),
                                contentDescription = "Collector profile photo",
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .background(Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalShipping,
                                    contentDescription = "Collector",
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = user.fullName,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = user.phone,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )

                        val hasVehicleInfo = vehicleType.isNotBlank() || vehiclePlateNumber.isNotBlank()
                        if (hasVehicleInfo) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = "Vehicle information",
                                    tint = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                val label = buildString {
                                    if (vehicleType.isNotBlank()) append(vehicleType)
                                    if (vehiclePlateNumber.isNotBlank()) {
                                        if (isNotEmpty()) append(" • ")
                                        append(vehiclePlateNumber.uppercase())
                                    }
                                }
                                Text(
                                    text = label,
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedButton(
                            onClick = onEditProfileClick,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                width = 1.dp
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Edit Profile",
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Performance Overview Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
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
                                text = "Performance",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            TextButton(onClick = onPerformanceClick) {
                                Text(
                                    text = "View All",
                                    color = PrimaryGreen,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Total Purchases",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Rp ${String.format("%,.0f", totalSpent)}",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF6B35)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))

                        Spacer(modifier = Modifier.height(16.dp))

                        // Stats Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Collections",
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = totalCollections.toString(),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Collections",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }

                            VerticalDivider(
                                modifier = Modifier.height(70.dp),
                                color = Color.LightGray.copy(alpha = 0.3f)
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Waste",
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${totalWasteCollected} kg",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Collected",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }

                            VerticalDivider(
                                modifier = Modifier.height(70.dp),
                                color = Color.LightGray.copy(alpha = 0.3f)
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = "Rate",
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${(completionRate * 100).toInt()}%",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Success Rate",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }

            // Settings Section
            item {
                Text(
                    text = "Settings & Preferences",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        CollectorSettingsItem(
                            icon = Icons.Default.Settings,
                            title = "Settings",
                            subtitle = "Notifications, privacy, and more",
                            onClick = onSettingsClick
                        )
                        Divider(color = Color.LightGray.copy(alpha = 0.2f))
                        CollectorSettingsItem(
                            icon = Icons.Default.HelpOutline,
                            title = "Help & Support",
                            subtitle = "FAQs, contact us, feedback",
                            onClick = onHelpSupportClick
                        )
                        Divider(color = Color.LightGray.copy(alpha = 0.2f))
                        CollectorSettingsItem(
                            icon = Icons.Default.Info,
                            title = "About",
                            subtitle = "App version and information",
                            onClick = onAboutClick
                        )
                    }
                }
            }

            // Logout Button
            item {
                OutlinedButton(
                    onClick = onLogoutClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.Red
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "Logout"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Log Out",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun CollectorSettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
                imageVector = icon,
                contentDescription = title,
                tint = PrimaryGreen,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Navigate",
            tint = Color.Gray,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CollectorProfileScreenPreview() {
    SampahJujurTheme {
        CollectorProfileScreen(
            user = User(
                id = "collector1",
                fullName = "John Collector",
                email = "john.collector@example.com",
                phone = "+1 (555) 123-4567",
                userType = "collector",
                profileImageUrl = ""
            ),
            totalCollections = 47,
            totalWasteCollected = 523.5,
            totalSpent = 1287500.0,
            completionRate = 0.96,
            vehicleType = "Truck",
            vehiclePlateNumber = "ABC 123",
            profileImageUrl = null
        )
    }
}
