package com.melodi.sampahjujur.ui.screens.shared

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melodi.sampahjujur.repository.AuthRepository
import com.melodi.sampahjujur.ui.theme.PrimaryGreen
import com.melodi.sampahjujur.ui.theme.SampahJujurTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

    fun changePassword(currentPassword: String, newPassword: String, confirmPassword: String) {
        viewModelScope.launch {
            // Validate inputs
            if (currentPassword.isBlank()) {
                _uiState.value = _uiState.value.copy(
                    error = "Current password is required",
                    isLoading = false
                )
                return@launch
            }

            if (newPassword.isBlank()) {
                _uiState.value = _uiState.value.copy(
                    error = "New password is required",
                    isLoading = false
                )
                return@launch
            }

            if (newPassword != confirmPassword) {
                _uiState.value = _uiState.value.copy(
                    error = "New passwords do not match",
                    isLoading = false
                )
                return@launch
            }

            if (currentPassword == newPassword) {
                _uiState.value = _uiState.value.copy(
                    error = "New password must be different from current password",
                    isLoading = false
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val result = authRepository.changePassword(currentPassword, newPassword)

            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    success = true,
                    error = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    success = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to change password"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(success = false)
    }
}

data class ChangePasswordUiState(
    val isLoading: Boolean = false,
    val success: Boolean = false,
    val error: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    viewModel: ChangePasswordViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    onBackClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var currentPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var showSuccessDialog by remember { mutableStateOf(false) }

    // Show success dialog
    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            showSuccessDialog = true
            viewModel.clearSuccess()
        }
    }

    // Show error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Long
            )
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Change Password",
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
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = PrimaryGreen.copy(alpha = 0.1f)
                ),
                elevation = CardDefaults.cardElevation(0.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "For your security, please enter your current password to confirm your identity before setting a new password.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = Color.DarkGray,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Current Password
            OutlinedTextField(
                value = currentPassword,
                onValueChange = { currentPassword = it },
                label = { Text("Current Password") },
                visualTransformation = if (currentPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { currentPasswordVisible = !currentPasswordVisible }) {
                        Icon(
                            imageVector = if (currentPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (currentPasswordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryGreen,
                    focusedLabelColor = PrimaryGreen,
                    cursorColor = PrimaryGreen
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // New Password
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("New Password") },
                visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                        Icon(
                            imageVector = if (newPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (newPasswordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryGreen,
                    focusedLabelColor = PrimaryGreen,
                    cursorColor = PrimaryGreen
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Confirm New Password
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm New Password") },
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryGreen,
                    focusedLabelColor = PrimaryGreen,
                    cursorColor = PrimaryGreen
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Change Password Button
            Button(
                onClick = {
                    viewModel.changePassword(currentPassword, newPassword, confirmPassword)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen
                ),
                shape = RoundedCornerShape(28.dp),
                enabled = !uiState.isLoading &&
                        currentPassword.isNotBlank() &&
                        newPassword.isNotBlank() &&
                        confirmPassword.isNotBlank()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Changing Password...",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Text(
                        text = "Change Password",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    // Success Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                currentPassword = ""
                newPassword = ""
                confirmPassword = ""
                onBackClick()
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Success",
                    tint = PrimaryGreen,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "Password Changed!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "Your password has been changed successfully. Please use your new password the next time you sign in.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center,
                    color = Color.DarkGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        currentPassword = ""
                        newPassword = ""
                        confirmPassword = ""
                        onBackClick()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryGreen
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done", fontWeight = FontWeight.SemiBold)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ChangePasswordScreenPreview() {
    SampahJujurTheme {
        ChangePasswordScreen()
    }
}
