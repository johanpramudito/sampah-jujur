package com.melodi.sampahjujur.ui.screens.collector

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melodi.sampahjujur.model.WasteItem
import com.melodi.sampahjujur.ui.theme.PrimaryGreen
import com.melodi.sampahjujur.ui.theme.SampahJujurTheme

data class ActualWasteItem(
    val type: String,
    val estimatedWeight: Double,
    val estimatedValue: Double,
    var actualWeight: String = "",
    var actualValue: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompleteTransactionDialog(
    wasteItems: List<WasteItem>,
    onDismiss: () -> Unit = {},
    onComplete: (List<ActualWasteItem>, String) -> Unit = { _, _ -> }
) {
    var actualItems by remember {
        mutableStateOf(
            wasteItems.map { item ->
                ActualWasteItem(
                    type = item.type,
                    estimatedWeight = item.weight,
                    estimatedValue = item.estimatedValue,
                    actualWeight = item.weight.toString(),
                    actualValue = item.estimatedValue.toString()
                )
            }
        )
    }
    var selectedPaymentMethod by remember { mutableStateOf("Cash") }
    var showPaymentDropdown by remember { mutableStateOf(false) }

    val paymentMethods = listOf("Cash", "Bank Transfer", "Digital Wallet")

    val totalEstimatedWeight = wasteItems.sumOf { it.weight }
    val totalEstimatedValue = wasteItems.sumOf { it.estimatedValue }
    val totalActualWeight = actualItems.sumOf { it.actualWeight.toDoubleOrNull() ?: 0.0 }
    val totalActualValue = actualItems.sumOf { it.actualValue.toDoubleOrNull() ?: 0.0 }

    val isFormValid = actualItems.all {
        it.actualWeight.toDoubleOrNull() != null &&
        it.actualWeight.toDouble() > 0 &&
        it.actualValue.toDoubleOrNull() != null &&
        it.actualValue.toDouble() >= 0
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Text(
                    text = "Complete Pickup",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Verify actual weights and values",
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Waste Items Verification
            item {
                Text(
                    text = "Item Verification",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            itemsIndexed(actualItems) { index, item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = item.type.replaceFirstChar { it.uppercase() },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Actual Weight
                            OutlinedTextField(
                                value = item.actualWeight,
                                onValueChange = { newValue ->
                                    actualItems = actualItems.toMutableList().also {
                                        it[index] = item.copy(actualWeight = newValue)
                                    }
                                },
                                label = { Text("Actual Weight (kg)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryGreen,
                                    unfocusedBorderColor = Color.LightGray,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true,
                                supportingText = {
                                    Text(
                                        text = "Est: ${item.estimatedWeight} kg",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                            )

                            // Actual Value
                            OutlinedTextField(
                                value = item.actualValue,
                                onValueChange = { newValue ->
                                    actualItems = actualItems.toMutableList().also {
                                        it[index] = item.copy(actualValue = newValue)
                                    }
                                },
                                label = { Text("Actual Value (Rp)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryGreen,
                                    unfocusedBorderColor = Color.LightGray,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true,
                                supportingText = {
                                    Text(
                                        text = "Est: Rp ${String.format("%,.0f", item.estimatedValue)}",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Totals Summary
            item {
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = PrimaryGreen.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Summary",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

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
                                    text = "$totalActualWeight kg",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Est: $totalEstimatedWeight kg",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Total Value",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = "Rp ${String.format("%,.0f", totalActualValue)}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreen
                                )
                                Text(
                                    text = "Est: Rp ${String.format("%,.0f", totalEstimatedValue)}",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Payment Method
            item {
                Text(
                    text = "Payment Method",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = showPaymentDropdown,
                    onExpandedChange = { showPaymentDropdown = !showPaymentDropdown }
                ) {
                    OutlinedTextField(
                        value = selectedPaymentMethod,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Payment Method") },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown",
                                tint = Color.Gray
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGreen,
                            unfocusedBorderColor = Color.LightGray,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = showPaymentDropdown,
                        onDismissRequest = { showPaymentDropdown = false }
                    ) {
                        paymentMethods.forEach { method ->
                            DropdownMenuItem(
                                text = { Text(method) },
                                onClick = {
                                    selectedPaymentMethod = method
                                    showPaymentDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            // Action Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.Gray
                        )
                    ) {
                        Text(
                            text = "Cancel",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = {
                            onComplete(actualItems, selectedPaymentMethod)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryGreen
                        ),
                        shape = RoundedCornerShape(28.dp),
                        enabled = isFormValid
                    ) {
                        Text(
                            text = "Complete",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Preview
@Composable
fun CompleteTransactionDialogPreview() {
    SampahJujurTheme {
        CompleteTransactionDialog(
            wasteItems = listOf(
                WasteItem("plastic", 5.0, 10.0, "Bottles"),
                WasteItem("paper", 3.0, 6.0, "Newspapers"),
                WasteItem("metal", 2.0, 8.0, "Cans")
            )
        )
    }
}
