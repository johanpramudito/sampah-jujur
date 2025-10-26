package com.melodi.sampahjujur.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melodi.sampahjujur.MainActivity
import com.melodi.sampahjujur.ui.theme.PrimaryGreen
import com.melodi.sampahjujur.ui.theme.SampahJujurTheme
import com.melodi.sampahjujur.viewmodel.PhoneAuthState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectorRegistrationScreen(
    viewModel: com.melodi.sampahjujur.viewmodel.AuthViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    onRegisterSuccess: () -> Unit = {},
    onLoginClick: () -> Unit = {}
) {
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var vehicleType by remember { mutableStateOf("") }
    var operatingArea by remember { mutableStateOf("") }
    var termsAccepted by remember { mutableStateOf(false) }
    var showVehicleDropdown by remember { mutableStateOf(false) }
    var showOtpSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val uiState by viewModel.uiState.collectAsState()
    val authState by viewModel.authState.collectAsState()
    val phoneAuthState by viewModel.phoneAuthState.collectAsState()

    val vehicleOptions = listOf("Motorcycle", "Car", "Truck", "Other")

    // Handle successful registration
    LaunchedEffect(authState) {
        if (authState is com.melodi.sampahjujur.viewmodel.AuthViewModel.AuthState.Authenticated) {
            onRegisterSuccess()
        }
    }

    // Handle phone auth state changes
    LaunchedEffect(phoneAuthState) {
        when (phoneAuthState) {
            is PhoneAuthState.CodeSent -> {
                showOtpSheet = true
            }
            is PhoneAuthState.VerificationCompleted -> {
                // Auto-verification completed, register
                val credential = (phoneAuthState as PhoneAuthState.VerificationCompleted).credential
                viewModel.registerCollector(credential, fullName, phone, vehicleType, operatingArea)
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Title
            Text(
                text = "Create Collector Account",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Join our collector network",
                fontSize = 16.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Full Name
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full Name") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Person",
                        tint = PrimaryGreen
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = PrimaryGreen,
                    unfocusedBorderColor = Color.LightGray,
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(28.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Phone Number
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Phone",
                        tint = PrimaryGreen
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = PrimaryGreen,
                    unfocusedBorderColor = Color.LightGray,
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(28.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "We'll send a verification code",
                fontSize = 14.sp,
                color = PrimaryGreen,
                modifier = Modifier.padding(start = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Vehicle Type Dropdown
            ExposedDropdownMenuBox(
                expanded = showVehicleDropdown,
                onExpandedChange = { showVehicleDropdown = !showVehicleDropdown }
            ) {
                OutlinedTextField(
                    value = vehicleType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Vehicle Type (optional)") },
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
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = PrimaryGreen,
                        unfocusedBorderColor = Color.LightGray,
                        containerColor = Color.White
                    ),
                    shape = RoundedCornerShape(28.dp)
                )

                ExposedDropdownMenu(
                    expanded = showVehicleDropdown,
                    onDismissRequest = { showVehicleDropdown = false }
                ) {
                    vehicleOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                vehicleType = option
                                showVehicleDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Operating Area
            OutlinedTextField(
                value = operatingArea,
                onValueChange = { operatingArea = it },
                label = { Text("Operating Area (optional)") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = PrimaryGreen
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = PrimaryGreen,
                    unfocusedBorderColor = Color.LightGray,
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(28.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Terms Checkbox
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = termsAccepted,
                    onCheckedChange = { termsAccepted = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = PrimaryGreen,
                        uncheckedColor = Color.Gray
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "I agree to Collector Terms and Conditions",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Error Message
            if (phoneAuthState is PhoneAuthState.Error || uiState.errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFEBEE)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = Color.Red
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                phoneAuthState is PhoneAuthState.Error ->
                                    (phoneAuthState as PhoneAuthState.Error).message
                                else -> uiState.errorMessage ?: "An error occurred"
                            },
                            color = Color.Red,
                            fontSize = 14.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Send Verification Code Button
            Button(
                onClick = {
                    val activity = context as? MainActivity
                    if (activity != null) {
                        viewModel.sendPhoneVerificationCode(phone, activity)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen
                ),
                shape = RoundedCornerShape(28.dp),
                enabled = termsAccepted && fullName.isNotBlank() && phone.isNotBlank() &&
                        !uiState.isLoading && phoneAuthState !is PhoneAuthState.CodeSent
            ) {
                if (uiState.isLoading || phoneAuthState is PhoneAuthState.CodeSent) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "Send Verification Code",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Login Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Already registered? ",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                TextButton(onClick = onLoginClick) {
                    Text(
                        text = "Log in",
                        fontSize = 14.sp,
                        color = PrimaryGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // OTP Bottom Sheet
    if (showOtpSheet) {
        CollectorRegistrationOtpSheet(
            phoneNumber = phone,
            fullName = fullName,
            vehicleType = vehicleType,
            operatingArea = operatingArea,
            viewModel = viewModel,
            onDismiss = {
                showOtpSheet = false
                viewModel.resetPhoneAuthState()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollectorRegistrationOtpSheet(
    phoneNumber: String,
    fullName: String,
    vehicleType: String,
    operatingArea: String,
    viewModel: com.melodi.sampahjujur.viewmodel.AuthViewModel,
    onDismiss: () -> Unit
) {
    var otpValues by remember { mutableStateOf(List(6) { "" }) }
    val phoneAuthState by viewModel.phoneAuthState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    // Auto-register when verification completes
    LaunchedEffect(phoneAuthState) {
        if (phoneAuthState is PhoneAuthState.VerificationCompleted) {
            val credential = (phoneAuthState as PhoneAuthState.VerificationCompleted).credential
            viewModel.registerCollector(credential, fullName, phoneNumber, vehicleType, operatingArea)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag Handle
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .background(Color.LightGray, shape = RoundedCornerShape(2.dp))
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Verify Phone Number",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Enter 6-digit code sent to $phoneNumber",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // OTP Input Boxes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(6) { index ->
                    OtpBox(
                        value = otpValues.getOrNull(index) ?: "",
                        onValueChange = { newValue ->
                            if (newValue.length <= 1 && (newValue.isEmpty() || newValue.all { it.isDigit() })) {
                                otpValues = otpValues.toMutableList().apply {
                                    this[index] = newValue
                                }

                                // Auto-verify when all digits entered
                                if (otpValues.all { it.isNotBlank() }) {
                                    val otp = otpValues.joinToString("")
                                    viewModel.verifyPhoneCode(otp)
                                }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Error Message
            if (phoneAuthState is PhoneAuthState.Error) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFEBEE)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = Color.Red,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = (phoneAuthState as PhoneAuthState.Error).message,
                            color = Color.Red,
                            fontSize = 14.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Verify Button
            Button(
                onClick = {
                    val otp = otpValues.joinToString("")
                    viewModel.verifyPhoneCode(otp)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen
                ),
                shape = RoundedCornerShape(28.dp),
                enabled = otpValues.all { it.isNotBlank() } && !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "Verify & Register",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun OtpBox(
    value: String,
    onValueChange: (String) -> Unit
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier
            .size(50.dp)
            .border(
                width = 2.dp,
                color = if (value.isNotBlank()) PrimaryGreen else Color.LightGray,
                shape = CircleShape
            )
            .background(Color.White, CircleShape),
        decorationBox = { innerTextField ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = "",
                        color = Color.LightGray,
                        fontSize = 20.sp
                    )
                }
                innerTextField()
            }
        },
        textStyle = MaterialTheme.typography.headlineSmall.copy(
            textAlign = TextAlign.Center,
            color = Color.Black
        )
    )
}

@Preview(showBackground = true)
@Composable
fun CollectorRegistrationScreenPreview() {
    SampahJujurTheme {
        CollectorRegistrationScreen()
    }
}
