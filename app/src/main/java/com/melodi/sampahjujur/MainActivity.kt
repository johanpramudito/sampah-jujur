package com.melodi.sampahjujur

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.SideEffect
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.melodi.sampahjujur.navigation.SampahJujurNavGraph
import com.melodi.sampahjujur.ui.theme.SampahJujurTheme
import dagger.hilt.android.AndroidEntryPoint
import android.app.Activity

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val pendingNavigation = mutableStateOf<PendingNavigation?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        requestRequiredPermissions()
        handleIntent(intent)

        setContent {
            val currentNavigation by pendingNavigation // Observe state changes
            SampahJujurApp(
                pendingNavigation = currentNavigation,
                onNavigationHandled = {
                    Log.d("MainActivity", "Clearing pending navigation")
                    pendingNavigation.value = null
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Update the current intent
        Log.d("MainActivity", "onNewIntent called, handling intent")
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) {
            Log.d("MainActivity", "Intent is null, skipping")
            return
        }

        Log.d("MainActivity", "=== Handling Intent ===")
        Log.d("MainActivity", "Action: ${intent.action}")
        Log.d("MainActivity", "Data: ${intent.data}")

        // Log all extras
        intent.extras?.let { bundle ->
            Log.d("MainActivity", "Extras:")
            for (key in bundle.keySet()) {
                Log.d("MainActivity", "  $key = ${bundle.get(key)}")
            }
        }

        // Handle both custom extras (when app is in foreground) and FCM data payload (when app is killed)
        val destination = intent.getStringExtra("destination")
        val type = intent.getStringExtra("type") // FCM data payload key
        val requestId = intent.getStringExtra("requestId")
        val chatId = intent.getStringExtra("chatId")

        Log.d("MainActivity", "Destination: $destination")
        Log.d("MainActivity", "Type: $type")
        Log.d("MainActivity", "Request ID: $requestId")
        Log.d("MainActivity", "Chat ID: $chatId")

        // Handle FCM data payload (when app was killed)
        if (destination == null && type != null) {
            Log.d("MainActivity", "Handling FCM data payload (app was killed)")
            when (type) {
                "new_message" -> {
                    if (requestId != null) {
                        Log.d("MainActivity", "✅ Setting pending navigation to Chat from FCM data: $requestId")
                        pendingNavigation.value = PendingNavigation.Chat(requestId)
                    } else {
                        Log.e("MainActivity", "❌ FCM new_message but requestId is null!")
                    }
                    return
                }
                "new_request" -> {
                    if (requestId != null) {
                        Log.d("MainActivity", "✅ Setting pending navigation to Collector Request Detail from FCM data: $requestId")
                        pendingNavigation.value = PendingNavigation.CollectorRequestDetail(requestId)
                    } else {
                        Log.e("MainActivity", "❌ FCM new_request but requestId is null!")
                    }
                    return
                }
                "request_status" -> {
                    if (requestId != null) {
                        Log.d("MainActivity", "✅ Setting pending navigation to Household Request Detail from FCM data: $requestId")
                        pendingNavigation.value = PendingNavigation.HouseholdRequestDetail(requestId)
                    } else {
                        Log.e("MainActivity", "❌ FCM request_status but requestId is null!")
                    }
                    return
                }
            }
        }

        // Handle custom extras (when app is in foreground)
        if (destination == null) {
            Log.d("MainActivity", "No destination or type found in intent, skipping navigation")
            return
        }

        Log.d("MainActivity", "Handling custom destination (app was in foreground)")
        when (destination) {
            "chat" -> {
                // For chat, we need requestId (not chatId) to navigate
                if (requestId != null) {
                    Log.d("MainActivity", "✅ Setting pending navigation to Chat with requestId: $requestId")
                    pendingNavigation.value = PendingNavigation.Chat(requestId)
                } else {
                    Log.e("MainActivity", "❌ Chat destination but requestId is null!")
                }
            }
            "household_request_detail" -> {
                if (requestId != null) {
                    Log.d("MainActivity", "✅ Setting pending navigation to Household Request Detail: $requestId")
                    pendingNavigation.value = PendingNavigation.HouseholdRequestDetail(requestId)
                } else {
                    Log.e("MainActivity", "❌ Household request detail destination but requestId is null!")
                }
            }
            "collector_request_detail" -> {
                if (requestId != null) {
                    Log.d("MainActivity", "✅ Setting pending navigation to Collector Request Detail: $requestId")
                    pendingNavigation.value = PendingNavigation.CollectorRequestDetail(requestId)
                } else {
                    Log.e("MainActivity", "❌ Collector request detail destination but requestId is null!")
                }
            }
            else -> {
                Log.w("MainActivity", "⚠️ Unknown destination: $destination")
            }
        }

        Log.d("MainActivity", "Current pending navigation: ${pendingNavigation.value}")
    }

    private fun requestRequiredPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        // Check location permissions (required for all Android versions)
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        // Check notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Request all missing permissions at once
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                REQUEST_CODE_PERMISSIONS
            )
        }
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 1001
    }
}

sealed class PendingNavigation {
    data class Chat(val requestId: String) : PendingNavigation() // Changed from chatId to requestId
    data class HouseholdRequestDetail(val requestId: String) : PendingNavigation()
    data class CollectorRequestDetail(val requestId: String) : PendingNavigation()
}

@Composable
fun SampahJujurApp(
    pendingNavigation: PendingNavigation? = null,
    onNavigationHandled: () -> Unit = {}
) {
    SampahJujurTheme {
        StatusBarAppearance()
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val navController = rememberNavController()
            SampahJujurNavGraph(
                navController = navController,
                pendingNavigation = pendingNavigation,
                onNavigationHandled = onNavigationHandled
            )
        }
    }
}

@Composable
private fun StatusBarAppearance() {
    val view = LocalView.current
    if (view.isInEditMode) return

    val window = (view.context as? Activity)?.window ?: return
    val backgroundColor = MaterialTheme.colorScheme.background
    val isLightBackground = backgroundColor.luminance() > 0.5f

    SideEffect {
        window.statusBarColor = backgroundColor.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = isLightBackground
    }
}
