package com.melodi.sampahjujur.ui.screens.collector

import android.net.Uri
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.melodi.sampahjujur.BuildConfig
import com.melodi.sampahjujur.model.User
import com.melodi.sampahjujur.ui.components.ProfileImagePicker
import com.melodi.sampahjujur.ui.theme.PrimaryGreen
import com.melodi.sampahjujur.ui.theme.SampahJujurTheme
import com.melodi.sampahjujur.utils.CloudinaryUploadService
import com.melodi.sampahjujur.utils.ValidationUtils
import com.melodi.sampahjujur.viewmodel.AuthViewModel
import com.melodi.sampahjujur.viewmodel.AuthViewModel.AuthState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Route composable that wires the edit profile screen to [AuthViewModel] and Cloudinary uploads.
 */
@Composable
fun CollectorEditProfileRoute(
    onBackClick: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authState by authViewModel.authState.collectAsState()
    val uiState by authViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val authenticatedUser = when (authState) {
        is AuthState.Authenticated -> (authState as AuthState.Authenticated).user
        AuthState.Loading -> null
        AuthState.Unauthenticated -> null
    }

    if (authenticatedUser == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var pendingUploadedUrl by remember { mutableStateOf<String?>(null) }
    var previousImageUrl by remember { mutableStateOf<String?>(null) }

    val isSaving = uiState.isLoading || isUploading

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            authViewModel.clearError()
            pendingUploadedUrl?.let { url ->
                CloudinaryUploadService.deleteImage(url)
                pendingUploadedUrl = null
            }
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            val newUrl = pendingUploadedUrl
            val oldUrl = previousImageUrl
            pendingUploadedUrl = null
            previousImageUrl = null
            selectedImageUri = null

            if (!oldUrl.isNullOrBlank() && !newUrl.isNullOrBlank() && oldUrl != newUrl) {
                launch {
                    CloudinaryUploadService.deleteImage(oldUrl)
                }
            }

            authViewModel.clearSuccessMessage()
            onBackClick()
        }
    }

    CollectorEditProfileScreen(
        user = authenticatedUser,
        currentImageUrl = authenticatedUser.profileImageUrl,
        previewImageUri = selectedImageUri,
        profileImageChanged = selectedImageUri != null,
        isSaving = isSaving,
        snackbarHostState = snackbarHostState,
        onImageSelected = { selectedImageUri = it },
        vehicleType = authenticatedUser.vehicleType,
        vehiclePlateNumber = authenticatedUser.vehiclePlateNumber,
        operatingArea = authenticatedUser.operatingArea,
        onBackClick = { if (!isSaving) onBackClick() },
        onSaveClick = { fullName, phone, vehicleType, plateNumber, operatingArea ->
            if (isSaving) return@CollectorEditProfileScreen
            scope.launch {
                var uploadedUrl: String? = null
                try {
                    if (selectedImageUri != null) {
                        isUploading = true
                        uploadedUrl = CloudinaryUploadService.uploadImage(
                            context = context,
                            imageUri = selectedImageUri!!,
                            folder = "${BuildConfig.CLOUDINARY_UPLOAD_FOLDER}/profiles"
                        )
                    }
                    pendingUploadedUrl = uploadedUrl
                    previousImageUrl = authenticatedUser.profileImageUrl

                    authViewModel.updateCollectorProfile(
                        fullName = fullName,
                        phone = phone,
                        vehicleType = vehicleType,
                        vehiclePlateNumber = plateNumber,
                        operatingArea = operatingArea,
                        profileImageUrl = uploadedUrl
                    )
                } catch (e: Exception) {
                    uploadedUrl?.let { url ->
                        CloudinaryUploadService.deleteImage(url)
                    }
                    snackbarHostState.showSnackbar(
                        e.message ?: "Failed to update profile photo"
                    )
                } finally {
                    isUploading = false
                }
            }
        }
    )
}

/**
 * UI for the collector edit profile screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectorEditProfileScreen(
    user: User,
    currentImageUrl: String,
    previewImageUri: Uri?,
    profileImageChanged: Boolean,
    isSaving: Boolean,
    snackbarHostState: SnackbarHostState,
    onImageSelected: (Uri) -> Unit,
    vehicleType: String = "",
    vehiclePlateNumber: String = "",
    operatingArea: String = "",
    onBackClick: () -> Unit = {},
    onSaveClick: (String, String, String, String, String) -> Unit = { _, _, _, _, _ -> }
) {
    var fullName by remember { mutableStateOf(user.fullName) }
    var phone by remember { mutableStateOf(user.phone) }
    var vehicleTypeField by remember { mutableStateOf(vehicleType) }
    var plateNumber by remember { mutableStateOf(vehiclePlateNumber) }
    var operatingAreaField by remember { mutableStateOf(operatingArea) }
    var showVehicleDropdown by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showPhoneDialog by remember { mutableStateOf(false) }
    var phoneDialogInput by remember { mutableStateOf("") }
    var phoneDialogError by remember { mutableStateOf<String?>(null) }

    val vehicleOptions = listOf("Motorcycle", "Car", "Truck", "Van", "Other")

    val hasChanges = profileImageChanged ||
        fullName != user.fullName ||
        phone != user.phone ||
        vehicleTypeField != vehicleType ||
        plateNumber != vehiclePlateNumber ||
        operatingAreaField != operatingArea

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isSaving) return@IconButton
                            if (hasChanges) {
                                showDiscardDialog = true
                            } else {
                                onBackClick()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    val saveEnabled = hasChanges && fullName.isNotBlank() && phone.isNotBlank() && !isSaving
                    TextButton(
                        onClick = {
                            if (saveEnabled) {
                                onSaveClick(
                                    fullName,
                                    phone,
                                    vehicleTypeField,
                                    plateNumber,
                                    operatingAreaField
                                )
                            }
                        },
                        enabled = saveEnabled
                    ) {
                        Text(
                            text = "Save",
                            color = if (saveEnabled) PrimaryGreen else Color.Gray,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isSaving) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                ProfileImagePicker(
                    currentImageUrl = currentImageUrl,
                    previewUri = previewImageUri,
                    onImageSelected = onImageSelected,
                    modifier = Modifier.size(140.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                SectionLabel("Personal Information")

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name *") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Name",
                            tint = PrimaryGreen
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { /* Read-only field */ },
                    label = { Text("Phone Number *") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Phone",
                            tint = PrimaryGreen
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = readOnlyTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(32.dp))

                SectionLabel("Vehicle Information")

                ExposedDropdownMenuBox(
                    expanded = showVehicleDropdown,
                    onExpandedChange = { showVehicleDropdown = !showVehicleDropdown }
                ) {
                    OutlinedTextField(
                        value = vehicleTypeField,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Vehicle Type") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = "Vehicle",
                                tint = PrimaryGreen
                            )
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = showVehicleDropdown)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = textFieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    DropdownMenu(
                        expanded = showVehicleDropdown,
                        onDismissRequest = { showVehicleDropdown = false }
                    ) {
                        vehicleOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    vehicleTypeField = option
                                    showVehicleDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = plateNumber,
                    onValueChange = { plateNumber = it },
                    label = { Text("License Plate Number") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = "Plate",
                            tint = PrimaryGreen
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    placeholder = { Text("e.g. ABC 1234") }
                )

                Spacer(modifier = Modifier.height(32.dp))

                SectionLabel("Operating Information")

                OutlinedTextField(
                    value = operatingAreaField,
                    onValueChange = { operatingAreaField = it },
                    label = { Text("Operating Area") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Area",
                            tint = PrimaryGreen
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2,
                    maxLines = 3,
                    placeholder = { Text("e.g. Downtown, North District") }
                )

                Spacer(modifier = Modifier.height(32.dp))

                SectionLabel("Account Security")

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isSaving) {
                                phoneDialogInput = phone
                                phoneDialogError = null
                                showPhoneDialog = true
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Phone Security",
                            tint = PrimaryGreen
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Change Phone Number",
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Update your registered phone number",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Navigate",
                            tint = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            if (showPhoneDialog) {
                AlertDialog(
                    onDismissRequest = { showPhoneDialog = false },
                    title = { Text("Change Phone Number") },
                    text = {
                        Column {
                            Text(
                                text = "Enter the new phone number you want to use for this account.",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = phoneDialogInput,
                                onValueChange = {
                                    phoneDialogInput = it
                                    phoneDialogError = null
                                },
                                label = { Text("New Phone Number") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = "Phone",
                                        tint = PrimaryGreen
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                isError = phoneDialogError != null,
                                colors = textFieldColors()
                            )
                            if (phoneDialogError != null) {
                                Text(
                                    text = phoneDialogError!!,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val sanitizedInput = phoneDialogInput.trim()
                            val validation = ValidationUtils.validateIndonesianPhone(sanitizedInput)
                            if (!validation.isValid) {
                                phoneDialogError = validation.errorMessage
                                return@TextButton
                            }

                            val formattedPhone = ValidationUtils.formatPhoneForFirebase(sanitizedInput)
                            if (formattedPhone == null) {
                                phoneDialogError = "Unable to format phone number"
                                return@TextButton
                            }

                            phone = formattedPhone
                            showPhoneDialog = false
                        }) {
                            Text("Update", color = PrimaryGreen)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPhoneDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            if (showDiscardDialog) {
                AlertDialog(
                    onDismissRequest = { showDiscardDialog = false },
                    title = { Text("Discard changes?") },
                    text = { Text("You have unsaved changes. Are you sure you want to discard them?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDiscardDialog = false
                                onBackClick()
                            }
                        ) {
                            Text("Discard", color = PrimaryGreen)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDiscardDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color.Gray,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun textFieldColors() = TextFieldDefaults.outlinedTextFieldColors(
    focusedBorderColor = PrimaryGreen,
    unfocusedBorderColor = Color.LightGray,
    containerColor = Color.White
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun readOnlyTextFieldColors() = TextFieldDefaults.outlinedTextFieldColors(
    focusedBorderColor = Color.LightGray,
    unfocusedBorderColor = Color.LightGray,
    containerColor = Color(0xFFEDEDED),
    focusedTextColor = Color.DarkGray,
    unfocusedTextColor = Color.DarkGray,
    disabledTextColor = Color.DarkGray,
    disabledBorderColor = Color.LightGray,
    focusedLabelColor = Color.Gray,
    unfocusedLabelColor = Color.Gray
)

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun CollectorEditProfileScreenPreview() {
    SampahJujurTheme {
        val snackbarHostState = remember { SnackbarHostState() }
        CollectorEditProfileScreen(
            user = User(
                id = "collector1",
                fullName = "John Collector",
                phone = "+62 812 3456 7890",
                userType = User.ROLE_COLLECTOR,
                vehicleType = "Truck",
                vehiclePlateNumber = "B 1234 XYZ",
                operatingArea = "Central Jakarta"
            ),
            currentImageUrl = "",
            previewImageUri = null,
            profileImageChanged = false,
            isSaving = false,
            snackbarHostState = snackbarHostState,
            onImageSelected = {}
        )
    }
}
