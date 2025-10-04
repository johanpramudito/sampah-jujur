package com.melodi.sampahjujur.ui.screens.household

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.melodi.sampahjujur.model.WasteItem
import com.melodi.sampahjujur.ui.components.HouseholdBottomNavBar
import com.melodi.sampahjujur.ui.theme.PrimaryGreen
import com.melodi.sampahjujur.ui.theme.SampahJujurTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestPickupScreen(
    viewModel: com.melodi.sampahjujur.viewmodel.HouseholdViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    onNavigate: (String) -> Unit = {}
) {
    var notes by remember { mutableStateOf("") }
    var showAddItemDialog by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()
    val wasteItems = uiState.currentWasteItems
    val selectedAddress = uiState.selectedAddress

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
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
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
                            Text(
                                text = "Map preview placeholder",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = if (selectedAddress.isEmpty())
                                "Tap 'Get Current Location' to set your pickup address."
                            else selectedAddress,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = {
                                // TODO: Implement location picker
                                // For now, use a mock location
                                viewModel.setPickupLocation(
                                    com.google.firebase.firestore.GeoPoint(-7.7956, 110.3695),
                                    "Jl. Colombo No. 1, Yogyakarta, DIY 55281"
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = PrimaryGreen
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Location"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Get Current Location")
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
                                    onRemove = { viewModel.removeWasteItem(index) }
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
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Show Add Item Dialog
    if (showAddItemDialog) {
        AddWasteItemDialog(
            onDismiss = { showAddItemDialog = false },
            onAddItem = { type, weight, value, description ->
                viewModel.addWasteItem(
                    WasteItem(
                        type = type.lowercase(),
                        weight = weight,
                        estimatedValue = value,
                        description = description
                    )
                )
                showAddItemDialog = false
            }
        )
    }

    // Show success message
    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading && uiState.errorMessage == null) {
            // Request submitted successfully, reset form
            notes = ""
        }
    }

    // Show error message
    uiState.errorMessage?.let { error ->
        LaunchedEffect(error) {
            // Could show a Snackbar here if needed
        }
    }
}

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

@Preview(showBackground = true)
@Composable
fun RequestPickupScreenPreview() {
    SampahJujurTheme {
        RequestPickupScreen()
    }
}
