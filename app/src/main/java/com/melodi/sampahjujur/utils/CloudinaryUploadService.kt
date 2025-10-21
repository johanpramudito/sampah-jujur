package com.melodi.sampahjujur.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.melodi.sampahjujur.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Service for uploading images to Cloudinary
 * Handles initialization and upload operations for waste item images
 */
object CloudinaryUploadService {
    private const val TAG = "CloudinaryUploadService"
    private var isInitialized = false

    /**
     * Initialize Cloudinary MediaManager with credentials from BuildConfig
     * Must be called before any upload operations
     */
    fun initialize(context: Context) {
        if (isInitialized) {
            Log.d(TAG, "Cloudinary already initialized")
            return
        }

        try {
            val config = mapOf(
                "cloud_name" to BuildConfig.CLOUDINARY_CLOUD_NAME,
                "api_key" to BuildConfig.CLOUDINARY_API_KEY,
                "api_secret" to BuildConfig.CLOUDINARY_API_SECRET
            )

            MediaManager.init(context, config)
            isInitialized = true
            Log.d(TAG, "Cloudinary initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Cloudinary", e)
            throw CloudinaryException("Failed to initialize Cloudinary: ${e.message}", e)
        }
    }

    /**
     * Upload an image to Cloudinary
     * @param context Android context
     * @param imageUri URI of the image to upload
     * @param folder Optional folder path in Cloudinary (defaults to BuildConfig value)
     * @return URL of the uploaded image
     */
    suspend fun uploadImage(
        context: Context,
        imageUri: Uri,
        folder: String = BuildConfig.CLOUDINARY_UPLOAD_FOLDER
    ): String = suspendCancellableCoroutine { continuation ->
        if (!isInitialized) {
            initialize(context)
        }

        try {
            // Convert URI to file path for Cloudinary upload
            val file = getFileFromUri(context, imageUri)
                ?: throw CloudinaryException("Failed to convert URI to file")

            val uploadOptions = mapOf(
                "folder" to folder,
                "resource_type" to "image",
                "quality" to "auto:good",
                "fetch_format" to "auto"
            )

            val requestId = MediaManager.get().upload(file.absolutePath)
                .options(uploadOptions)
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        Log.d(TAG, "Upload started: $requestId")
                    }

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        val progress = (bytes.toDouble() / totalBytes * 100).toInt()
                        Log.d(TAG, "Upload progress: $progress%")
                    }

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val url = resultData["secure_url"] as? String
                            ?: resultData["url"] as? String
                            ?: ""

                        Log.d(TAG, "Upload successful: $url")

                        // Clean up temporary file
                        file.delete()

                        if (continuation.isActive) {
                            continuation.resume(url)
                        }
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        Log.e(TAG, "Upload failed: ${error.description}")

                        // Clean up temporary file
                        file.delete()

                        if (continuation.isActive) {
                            continuation.resumeWithException(
                                CloudinaryException("Upload failed: ${error.description}")
                            )
                        }
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        Log.w(TAG, "Upload rescheduled: ${error.description}")
                    }
                })
                .dispatch()

            continuation.invokeOnCancellation {
                MediaManager.get().cancelRequest(requestId)
                file.delete()
                Log.d(TAG, "Upload cancelled: $requestId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during upload", e)
            if (continuation.isActive) {
                continuation.resumeWithException(
                    CloudinaryException("Upload error: ${e.message}", e)
                )
            }
        }
    }

    /**
     * Delete an image from Cloudinary by its URL
     * @param imageUrl The full Cloudinary URL of the image
     * @return true if deletion was successful, false otherwise
     */
    suspend fun deleteImage(imageUrl: String): Boolean = withContext(Dispatchers.IO) {
        if (imageUrl.isBlank()) {
            Log.w(TAG, "Cannot delete image: empty URL")
            return@withContext false
        }

        try {
            // Extract public_id from Cloudinary URL
            val publicId = extractPublicId(imageUrl)
            if (publicId == null) {
                Log.e(TAG, "Failed to extract public_id from URL: $imageUrl")
                return@withContext false
            }

            Log.d(TAG, "Attempting to delete image with public_id: $publicId")

            // Generate timestamp for signature
            val timestamp = (System.currentTimeMillis() / 1000).toString()

            // Create signature: SHA1(public_id=xxx&timestamp=xxx + api_secret)
            val stringToSign = "public_id=$publicId&timestamp=$timestamp${BuildConfig.CLOUDINARY_API_SECRET}"
            val signature = sha1(stringToSign)

            // Make DELETE request to Cloudinary
            val cloudName = BuildConfig.CLOUDINARY_CLOUD_NAME
            val apiKey = BuildConfig.CLOUDINARY_API_KEY
            val url = URL("https://api.cloudinary.com/v1_1/$cloudName/image/destroy")

            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

            // Build request body
            val postData = "public_id=$publicId&timestamp=$timestamp&api_key=$apiKey&signature=$signature"
            connection.outputStream.use { os ->
                os.write(postData.toByteArray())
            }

            val responseCode = connection.responseCode
            val responseMessage = connection.inputStream.bufferedReader().use { it.readText() }

            Log.d(TAG, "Delete response code: $responseCode")
            Log.d(TAG, "Delete response: $responseMessage")

            connection.disconnect()

            if (responseCode == 200) {
                Log.d(TAG, "Successfully deleted image: $publicId")
                true
            } else {
                Log.e(TAG, "Failed to delete image: $responseCode - $responseMessage")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting image from Cloudinary", e)
            false
        }
    }

    /**
     * Extract public_id from Cloudinary URL
     * Example: https://res.cloudinary.com/cloud/image/upload/v123/folder/image.jpg
     * Returns: folder/image
     */
    private fun extractPublicId(imageUrl: String): String? {
        return try {
            // Cloudinary URL format: .../upload/v{version}/{folder}/{filename}.{ext}
            // or .../upload/{folder}/{filename}.{ext}
            val uploadIndex = imageUrl.indexOf("/upload/")
            if (uploadIndex == -1) return null

            val afterUpload = imageUrl.substring(uploadIndex + "/upload/".length)

            // Remove version if present (starts with v followed by numbers)
            val withoutVersion = if (afterUpload.matches(Regex("^v\\d+/.*"))) {
                afterUpload.substring(afterUpload.indexOf("/") + 1)
            } else {
                afterUpload
            }

            // Remove file extension
            val publicId = withoutVersion.substringBeforeLast(".")

            Log.d(TAG, "Extracted public_id: $publicId from URL: $imageUrl")
            publicId
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting public_id from URL: $imageUrl", e)
            null
        }
    }

    /**
     * Generate SHA-1 hash for Cloudinary signature
     */
    private fun sha1(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-1").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Convert URI to File
     * Copies content from URI to a temporary file in cache directory
     */
    private fun getFileFromUri(context: Context, uri: Uri): File? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            inputStream?.use { input ->
                val tempFile = File.createTempFile(
                    "waste_image_${System.currentTimeMillis()}",
                    ".jpg",
                    context.cacheDir
                )

                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }

                tempFile
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error converting URI to file", e)
            null
        }
    }
}

/**
 * Custom exception for Cloudinary operations
 */
class CloudinaryException(message: String, cause: Throwable? = null) : Exception(message, cause)
