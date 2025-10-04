package com.melodi.sampahjujur.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.melodi.sampahjujur.ui.theme.PrimaryGreen

// Navigation items for Household
sealed class HouseholdNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Request : HouseholdNavItem("request", Icons.Default.Add, "Request")
    object MyRequests : HouseholdNavItem("my_requests", Icons.Default.List, "My Requests")
    object Profile : HouseholdNavItem("household_profile", Icons.Default.Person, "Profile")
}

// Navigation items for Collector
sealed class CollectorNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Requests : CollectorNavItem("collector_dashboard", Icons.Default.List, "Requests")
    object Map : CollectorNavItem("map_view", Icons.Default.Map, "Map")
    object Profile : CollectorNavItem("collector_profile", Icons.Default.Person, "Profile")
}

@Composable
fun HouseholdBottomNavBar(
    selectedRoute: String,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        HouseholdNavItem.Request,
        HouseholdNavItem.MyRequests,
        HouseholdNavItem.Profile
    )

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                label = { Text(item.label) },
                selected = selectedRoute == item.route,
                onClick = { onNavigate(item.route) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryGreen,
                    selectedTextColor = PrimaryGreen,
                    indicatorColor = PrimaryGreen.copy(alpha = 0.1f),
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )
        }
    }
}

@Composable
fun CollectorBottomNavBar(
    selectedRoute: String,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        CollectorNavItem.Requests,
        CollectorNavItem.Map,
        CollectorNavItem.Profile
    )

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                label = { Text(item.label) },
                selected = selectedRoute == item.route,
                onClick = { onNavigate(item.route) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryGreen,
                    selectedTextColor = PrimaryGreen,
                    indicatorColor = PrimaryGreen.copy(alpha = 0.1f),
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )
        }
    }
}
