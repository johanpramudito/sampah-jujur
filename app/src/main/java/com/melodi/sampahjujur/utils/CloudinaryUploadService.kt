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
     * Initialize the Cloudinary MediaManager with credentials from BuildConfig.
     *
     * This is idempotent: if Cloudinary is already initialized, the call is a no-op.
     *
     * @param context Android Context used to initialize MediaManager.
     * @throws CloudinaryException if initialization fails.
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
     * Uploads an image to Cloudinary and returns its accessible URL.
     *
     * @param context Android Context used to initialize Cloudinary and access content.
     * @param imageUri The content Uri of the image to upload.
     * @param folder Optional target folder path in Cloudinary; defaults to the BuildConfig upload folder.
     * @return The URL of the uploaded image.
     * @throws CloudinaryException If the URI cannot be converted to a file, the upload fails, or another Cloudinary-related error occurs.
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
                    /**
                     * Called when an upload request is started.
                     *
                     * @param requestId The unique identifier of the upload request.
                     */
                    override fun onStart(requestId: String) {
                        Log.d(TAG, "Upload started: $requestId")
                    }

                    /**
                     * Reports upload progress for a given request by logging the percentage completed.
                     *
                     * @param requestId Identifier of the upload request.
                     * @param bytes Number of bytes uploaded so far.
                     * @param totalBytes Total number of bytes to be uploaded.
                     */
                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        val progress = (bytes.toDouble() / totalBytes * 100).toInt()
                        Log.d(TAG, "Upload progress: $progress%")
                    }

                    /**
                     * Handles a successful upload by extracting the uploaded image URL, deleting the temporary file, and resuming the awaiting continuation with the URL.
                     *
                     * @param requestId The upload request identifier.
                     * @param resultData The response data returned by Cloudinary; expected to contain `secure_url` or `url`.
                     */
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

                    /**
                     * Handles an upload error by logging the failure, deleting the temporary upload file, and
                     * resuming the awaiting continuation with a CloudinaryException if it is still active.
                     *
                     * @param requestId The identifier of the upload request that failed.
                     * @param error Details about the upload error.
                     */
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

                    /**
                     * Handles an upload reschedule event.
                     *
                     * Called when a previously started upload has been rescheduled by the uploader.
                     *
                     * @param requestId The identifier for the upload request that was rescheduled.
                     * @param error An ErrorInfo describing why the upload was rescheduled (contains a human-readable description).
                     */
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
     * Remove the image referenced by the provided Cloudinary URL from Cloudinary.
     *
     * @param imageUrl The full Cloudinary image URL whose corresponding remote resource should be deleted.
     * @return `true` if the image was deleted successfully, `false` otherwise.
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
     * Derives the Cloudinary public_id (the path under the `/upload/` segment) from a Cloudinary image URL.
     *
     * @param imageUrl The full Cloudinary image URL.
     * @return The `public_id` (e.g., `folder/.../filename`) without any version segment or file extension, or `null` if it cannot be determined.
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
     * Computes the SHA-1 hash of the given input and returns it as a lowercase hexadecimal string.
     *
     * @param input The string to hash.
     * @return The SHA-1 digest of `input` encoded as a lowercase hex string.
     */
    private fun sha1(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-1").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Converts a content `Uri` into a temporary JPG file stored in the app cache directory.
     *
     * @param context Android context used to access the content resolver and cache directory.
     * @param uri The content `Uri` pointing to the source data to copy.
     * @return A `File` referencing the created temporary file, or `null` if the conversion failed.
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