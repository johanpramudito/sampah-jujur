package com.melodi.sampahjujur.ui.components

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.melodi.sampahjujur.ui.theme.PrimaryGreen
import java.io.File

/**
 * Profile image picker component for circular profile pictures
 * Allows users to take a photo or select from gallery
 *
 * @param currentImageUrl Current profile image URL (from Cloudinary or empty)
 * @param previewUri URI of newly selected image for preview (takes precedence over currentImageUrl)
 * @param onImageSelected Callback when new image is selected with URI
 * @param modifier Modifier for the container
 */
@Composable
fun ProfileImagePicker(
    currentImageUrl: String?,
    previewUri: Uri? = null,
    onImageSelected: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            onImageSelected(tempCameraUri!!)
        }
    }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onImageSelected(it) }
    }

    // Permission launcher for camera
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Create a temporary file for the camera image
            val photoFile = File.createTempFile(
                "profile_image_${System.currentTimeMillis()}",
                ".jpg",
                context.cacheDir
            )
            tempCameraUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            cameraLauncher.launch(tempCameraUri!!)
        }
    }

    // Permission launcher for gallery (for Android 13+)
    val galleryPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            galleryLauncher.launch("image/*")
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Profile picture circle
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    if (previewUri == null && currentImageUrl.isNullOrEmpty()) {
                        PrimaryGreen.copy(alpha = 0.1f)
                    } else {
                        Color.Transparent
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                // Show preview of newly selected image first
                previewUri != null -> {
                    Image(
                        painter = rememberAsyncImagePainter(previewUri),
                        contentDescription = "Profile picture preview",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                // Show current uploaded image
                !currentImageUrl.isNullOrEmpty() -> {
                    Image(
                        painter = rememberAsyncImagePainter(currentImageUrl),
                        contentDescription = "Profile picture",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                // Show placeholder icon
                else -> {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile placeholder",
                        tint = PrimaryGreen,
                        modifier = Modifier.size(60.dp)
                    )
                }
            }
        }

        // Camera button overlay
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(36.dp)
                .clip(CircleShape)
                .background(PrimaryGreen)
                .clickable { showImageSourceDialog = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Change photo",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }

    // Image source selection dialog
    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("Change Profile Picture") },
            text = {
                Column {
                    Text("Choose how to update your profile picture:")
                    Spacer(modifier = Modifier.height(16.dp))

                    // Camera option
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showImageSourceDialog = false
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddAPhoto,
                                contentDescription = "Camera",
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Take Photo",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Use camera to capture photo",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Gallery option
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showImageSourceDialog = false
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    galleryPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                                } else {
                                    galleryLauncher.launch("image/*")
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = "Gallery",
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Choose from Gallery",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Select existing photo",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showImageSourceDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
