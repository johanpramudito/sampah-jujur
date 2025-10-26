package com.melodi.sampahjujur.ui.screens.household

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.melodi.sampahjujur.ui.components.HouseholdBottomNavBar
import com.melodi.sampahjujur.ui.theme.PrimaryGreen
import com.melodi.sampahjujur.ui.theme.SampahJujurTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseholdProfileScreen(
    user: User,
    totalRequests: Int = 0,
    totalWasteCollected: Double = 0.0,
    totalEarnings: Double = 0.0,
    onEditProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
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
            HouseholdBottomNavBar(
                selectedRoute = "household_profile",
                onNavigate = onNavigate
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Profile Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                        // Profile Picture
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            if (user.profileImageUrl.isNotEmpty()) {
                                Image(
                                    painter = rememberAsyncImagePainter(user.profileImageUrl),
                                    contentDescription = "Profile picture",
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Profile",
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
                            text = user.email,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )

                        if (user.phone.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = user.phone,
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
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

            // Statistics Card
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
                            text = "My Statistics",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatItem(
                                icon = Icons.Default.List,
                                value = totalRequests.toString(),
                                label = "Requests"
                            )
                            VerticalDivider(
                                modifier = Modifier.height(60.dp),
                                color = Color.LightGray.copy(alpha = 0.3f)
                            )
                            StatItem(
                                icon = Icons.Default.Delete,
                                value = "${totalWasteCollected} kg",
                                label = "Waste Collected"
                            )
                            VerticalDivider(
                                modifier = Modifier.height(60.dp),
                                color = Color.LightGray.copy(alpha = 0.3f)
                            )
                            StatItem(
                                icon = Icons.Default.AttachMoney,
                                value = "$$totalEarnings",
                                label = "Total Earned"
                            )
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
                    color = Color.Gray
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SettingsItem(
                            icon = Icons.Default.Settings,
                            title = "Settings",
                            subtitle = "Notifications, privacy, and more",
                            onClick = onSettingsClick
                        )
                        Divider(color = Color.LightGray.copy(alpha = 0.2f))
                        SettingsItem(
                            icon = Icons.Default.HelpOutline,
                            title = "Help & Support",
                            subtitle = "FAQs, contact us, feedback",
                            onClick = onHelpSupportClick
                        )
                        Divider(color = Color.LightGray.copy(alpha = 0.2f))
                        SettingsItem(
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

            // Bottom spacer for better scrollability
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun StatItem(
    icon: ImageVector,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = PrimaryGreen,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun SettingsItem(
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
fun HouseholdProfileScreenPreview() {
    SampahJujurTheme {
        HouseholdProfileScreen(
            user = User(
                id = "user1",
                fullName = "Jane Smith",
                email = "jane.smith@example.com",
                phone = "+1 (555) 987-6543",
                userType = "household"
            ),
            totalRequests = 12,
            totalWasteCollected = 145.5,
            totalEarnings = 287.50
        )
    }
}
