package com.melodi.sampahjujur

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
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

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        requestNotificationPermissionIfNeeded()

        setContent {
            SampahJujurApp()
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val alreadyGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!alreadyGranted) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_CODE_POST_NOTIFICATIONS
            )
        }
    }

    companion object {
        private const val REQUEST_CODE_POST_NOTIFICATIONS = 1001
    }
}

@Composable
fun SampahJujurApp() {
    SampahJujurTheme {
        StatusBarAppearance()
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val navController = rememberNavController()
            SampahJujurNavGraph(navController = navController)
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
