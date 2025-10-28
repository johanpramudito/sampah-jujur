package com.melodi.sampahjujur.ui.components

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melodi.sampahjujur.ui.theme.PrimaryGreen
import com.melodi.sampahjujur.viewmodel.AuthViewModel
import com.melodi.sampahjujur.viewmodel.PhoneAuthState

/**
 * Consolidated OTP Verification Bottom Sheet
 *
 * Used for both login and registration flows to verify phone numbers via SMS OTP.
 * Handles OTP input, verification, resend functionality with cooldown timer.
 *
 * @param phoneNumber The phone number being verified
 * @param viewModel AuthViewModel for handling verification logic
 * @param isRegistration Whether this is for registration (true) or login (false)
 * @param registrationData Optional data for registration flow (fullName, vehicleType, operatingArea)
 * @param onDismiss Callback when sheet is dismissed
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpVerificationSheet(
    phoneNumber: String,
    viewModel: AuthViewModel,
    isRegistration: Boolean = false,
    registrationData: RegistrationData? = null,
    onDismiss: () -> Unit
) {
    var otpValues by remember { mutableStateOf(List(6) { "" }) }
    val phoneAuthState by viewModel.phoneAuthState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val resendCooldown by viewModel.otpResendCooldown.collectAsState()
    val context = LocalContext.current
    val focusRequesters = remember { List(6) { FocusRequester() } }
    val focusManager = LocalFocusManager.current

    // Auto-register/login when verification completes
    LaunchedEffect(phoneAuthState) {
        if (phoneAuthState is PhoneAuthState.VerificationCompleted) {
            val credential = (phoneAuthState as PhoneAuthState.VerificationCompleted).credential
            if (isRegistration && registrationData != null) {
                viewModel.registerCollector(
                    credential,
                    registrationData.fullName,
                    phoneNumber,
                    registrationData.vehicleType,
                    registrationData.operatingArea
                )
            } else {
                viewModel.signInCollector(credential)
            }
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

            // Title
            Text(
                text = "Verify Phone Number",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Description
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
                    OutlinedTextField(
                        value = otpValues.getOrNull(index) ?: "",
                        onValueChange = { newValue ->
                            // Only allow single digit
                            if (newValue.length <= 1 && (newValue.isEmpty() || newValue.all { it.isDigit() })) {
                                otpValues = otpValues.toMutableList().apply {
                                    this[index] = newValue
                                }

                                // Move focus forward when digit entered
                                if (newValue.isNotEmpty() && index < 5) {
                                    focusRequesters[index + 1].requestFocus()
                                }

                                // Auto-verify when all digits entered
                                if (otpValues.all { it.isNotBlank() }) {
                                    focusManager.clearFocus()
                                    val otp = otpValues.joinToString("")
                                    viewModel.verifyPhoneCode(otp)
                                }
                            } else if (newValue.isEmpty() && index > 0) {
                                // Move focus backward when deleting
                                focusRequesters[index - 1].requestFocus()
                            }
                        },
                        modifier = Modifier
                            .width(50.dp)
                            .focusRequester(focusRequesters[index]),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = if (index == 5) ImeAction.Done else ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                if (index < 5) {
                                    focusRequesters[index + 1].requestFocus()
                                }
                            },
                            onDone = {
                                focusManager.clearFocus()
                                if (otpValues.all { it.isNotBlank() }) {
                                    val otp = otpValues.joinToString("")
                                    viewModel.verifyPhoneCode(otp)
                                }
                            }
                        ),
                        textStyle = LocalTextStyle.current.copy(
                            textAlign = TextAlign.Center,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGreen,
                            unfocusedBorderColor = if (otpValues.getOrNull(index)?.isNotBlank() == true)
                                PrimaryGreen else Color.LightGray,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Auto-focus first field when sheet opens
            LaunchedEffect(Unit) {
                focusRequesters[0].requestFocus()
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

            // Success Message (during verification)
            if (phoneAuthState is PhoneAuthState.VerificationCompleted && uiState.isLoading) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8F5E9)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = PrimaryGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isRegistration) "Verifying and creating account..." else "Verifying and logging in...",
                            color = PrimaryGreen,
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
                        text = if (isRegistration) "Verify & Register" else "Verify & Login",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Resend OTP Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Didn't receive code? ",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                TextButton(
                    onClick = {
                        val activity = context as? Activity
                        if (activity != null) {
                            viewModel.resendOtp(phoneNumber, activity)
                            // Clear OTP boxes on resend
                            otpValues = List(6) { "" }
                        }
                    },
                    enabled = resendCooldown == 0 && !uiState.isLoading
                ) {
                    Text(
                        text = if (resendCooldown > 0) "Resend ($resendCooldown)" else "Resend OTP",
                        fontSize = 14.sp,
                        color = if (resendCooldown > 0) Color.Gray else PrimaryGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Data class for registration information
 */
data class RegistrationData(
    val fullName: String,
    val vehicleType: String = "",
    val operatingArea: String = ""
)
