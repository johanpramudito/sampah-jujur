package com.melodi.sampahjujur.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melodi.sampahjujur.MainActivity
import com.melodi.sampahjujur.ui.components.OtpVerificationSheet
import com.melodi.sampahjujur.ui.components.RegistrationData
import com.melodi.sampahjujur.ui.theme.PrimaryGreen
import com.melodi.sampahjujur.ui.theme.SampahJujurTheme
import com.melodi.sampahjujur.viewmodel.PhoneAuthState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectorRegistrationScreen(
    viewModel: com.melodi.sampahjujur.viewmodel.AuthViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    onRegisterSuccess: () -> Unit = {},
    onLoginClick: () -> Unit = {},
    onTermsClick: () -> Unit = {},
    onPrivacyPolicyClick: () -> Unit = {}
) {
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var vehicleType by remember { mutableStateOf("") }
    var operatingArea by remember { mutableStateOf("") }
    var termsAccepted by remember { mutableStateOf(false) }
    var showVehicleDropdown by remember { mutableStateOf(false) }
    var showOtpSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

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
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
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
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
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
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
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
                verticalAlignment = Alignment.Top,
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

                val annotatedText = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = Color.Gray, fontSize = 14.sp)) {
                        append("I agree to the ")
                    }
                    pushStringAnnotation(tag = "terms", annotation = "terms")
                    withStyle(
                        style = SpanStyle(
                            color = PrimaryGreen,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    ) {
                        append("Terms & Conditions")
                    }
                    pop()
                    withStyle(style = SpanStyle(color = Color.Gray, fontSize = 14.sp)) {
                        append(" and ")
                    }
                    pushStringAnnotation(tag = "privacy", annotation = "privacy")
                    withStyle(
                        style = SpanStyle(
                            color = PrimaryGreen,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    ) {
                        append("Privacy Policy")
                    }
                    pop()
                }

                ClickableText(
                    text = annotatedText,
                    modifier = Modifier.weight(1f),
                    onClick = { offset ->
                        annotatedText.getStringAnnotations(tag = "terms", start = offset, end = offset)
                            .firstOrNull()?.let {
                                onTermsClick()
                            }
                        annotatedText.getStringAnnotations(tag = "privacy", start = offset, end = offset)
                            .firstOrNull()?.let {
                                onPrivacyPolicyClick()
                            }
                    }
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
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already registered? ",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                TextButton(
                    onClick = onLoginClick,
                    contentPadding = PaddingValues(0.dp)
                ) {
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
        OtpVerificationSheet(
            phoneNumber = phone,
            viewModel = viewModel,
            isRegistration = true,
            registrationData = RegistrationData(
                fullName = fullName,
                vehicleType = vehicleType,
                operatingArea = operatingArea
            ),
            onDismiss = {
                showOtpSheet = false
                viewModel.resetPhoneAuthState()
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CollectorRegistrationScreenPreview() {
    SampahJujurTheme {
        CollectorRegistrationScreen()
    }
}
