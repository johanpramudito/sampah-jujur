package com.melodi.sampahjujur.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melodi.sampahjujur.MainActivity
import com.melodi.sampahjujur.ui.components.OtpVerificationSheet
import com.melodi.sampahjujur.ui.theme.PrimaryGreen
import com.melodi.sampahjujur.ui.theme.SampahJujurTheme
import com.melodi.sampahjujur.viewmodel.PhoneAuthState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectorLoginScreen(
    viewModel: com.melodi.sampahjujur.viewmodel.AuthViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    onLoginSuccess: () -> Unit = {},
    onSignUpClick: () -> Unit = {}
) {
    var phone by remember { mutableStateOf("") }
    var showOtpSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val uiState by viewModel.uiState.collectAsState()
    val authState by viewModel.authState.collectAsState()
    val phoneAuthState by viewModel.phoneAuthState.collectAsState()

    // Handle successful login
    LaunchedEffect(authState) {
        if (authState is com.melodi.sampahjujur.viewmodel.AuthViewModel.AuthState.Authenticated) {
            onLoginSuccess()
        }
    }

    // Handle phone auth state changes
    LaunchedEffect(phoneAuthState) {
        when (phoneAuthState) {
            is PhoneAuthState.CodeSent -> {
                showOtpSheet = true
            }
            is PhoneAuthState.VerificationCompleted -> {
                // Auto-verification completed, sign in
                val credential = (phoneAuthState as PhoneAuthState.VerificationCompleted).credential
                viewModel.signInCollector(credential)
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo/Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        PrimaryGreen.copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalShipping,
                    contentDescription = "Collector",
                    tint = PrimaryGreen,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Title
            Text(
                text = "Collector Login",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Enter your registered phone number",
                fontSize = 16.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Phone Number Field
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Phone",
                        tint = Color.Gray
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = PrimaryGreen,
                    unfocusedBorderColor = Color.LightGray,
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

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

            // Send OTP Button
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
                enabled = phone.isNotBlank() && !uiState.isLoading &&
                        phoneAuthState !is PhoneAuthState.CodeSent
            ) {
                if (uiState.isLoading || phoneAuthState is PhoneAuthState.CodeSent) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "Send OTP",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sign Up Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "New collector? ",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                TextButton(onClick = onSignUpClick) {
                    Text(
                        text = "Register here",
                        fontSize = 14.sp,
                        color = PrimaryGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    // OTP Bottom Sheet
    if (showOtpSheet) {
        OtpVerificationSheet(
            phoneNumber = phone,
            viewModel = viewModel,
            isRegistration = false,
            onDismiss = {
                showOtpSheet = false
                viewModel.resetPhoneAuthState()
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CollectorLoginScreenPreview() {
    SampahJujurTheme {
        CollectorLoginScreen()
    }
}
