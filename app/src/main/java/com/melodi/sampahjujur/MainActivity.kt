package com.melodi.sampahjujur

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.melodi.sampahjujur.navigation.SampahJujurNavGraph
import com.melodi.sampahjujur.ui.theme.SampahJujurTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * Initializes the activity: installs the splash screen and sets the Jetpack Compose content.
     *
     * Calls the lifecycle super implementation, installs the splash screen before super, and sets the
     * activity's UI to the root composable.
     *
     * @param savedInstanceState A Bundle containing the activity's previously saved state, or `null` if none.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setContent {
            SampahJujurApp()
        }
    }
}

@Composable
fun SampahJujurApp() {
    SampahJujurTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val navController = rememberNavController()
            SampahJujurNavGraph(navController = navController)
        }
    }
}