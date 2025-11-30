package com.melodi.sampahjujur.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.melodi.sampahjujur.PendingNavigation
import com.melodi.sampahjujur.di.GoogleSignInModule
import com.melodi.sampahjujur.model.PickupRequest
import com.melodi.sampahjujur.model.User
import com.melodi.sampahjujur.model.WasteItem
import com.melodi.sampahjujur.ui.screens.*
import com.melodi.sampahjujur.ui.screens.auth.*
import com.melodi.sampahjujur.ui.screens.collector.*
import com.melodi.sampahjujur.ui.screens.household.*
import com.melodi.sampahjujur.ui.screens.shared.*
import com.melodi.sampahjujur.viewmodel.AuthViewModel
import com.melodi.sampahjujur.viewmodel.CollectorViewModel


sealed class Screen(val route: String) {
    // Initial Flow
    object Loading : Screen("loading")
    object Onboarding : Screen("onboarding")
    object RoleSelection : Screen("role_selection")

    // Household Auth
    object HouseholdLogin : Screen("household_login")
    object HouseholdRegistration : Screen("household_registration")
    object ForgotPassword : Screen("forgot_password")

    // Collector Auth
    object CollectorLogin : Screen("collector_login")
    object CollectorRegistration : Screen("collector_registration")

    // Household Main
    object HouseholdRequest : Screen("household_request")
    object HouseholdMyRequests : Screen("household_my_requests")
    object HouseholdRequestDetail : Screen("household_request_detail/{requestId}") {
        fun createRoute(requestId: String) = "household_request_detail/$requestId"
    }
    object HouseholdLocationPicker : Screen("household_location_picker")
    object HouseholdProfile : Screen("household_profile")
    object HouseholdEditProfile : Screen("household_edit_profile")
    object HouseholdStatistics : Screen("household_statistics")

    // Collector Main
    object CollectorDashboard : Screen("collector_dashboard")
    object CollectorRequestDetail : Screen("collector_request_detail/{requestId}") {
        fun createRoute(requestId: String) = "collector_request_detail/$requestId"
    }
    object CollectorMap : Screen("collector_map")
    object CollectorProfile : Screen("collector_profile")
    object CollectorPerformance : Screen("collector_performance")
    object CollectorEditProfile : Screen("collector_edit_profile")

    // Chat
    object ChatList : Screen("chat_list")
    object Chat : Screen("chat/{requestId}") {
        fun createRoute(requestId: String) = "chat/$requestId"
    }

    // Shared
    object Settings : Screen("settings")
    object HelpSupport : Screen("help_support")
    object About : Screen("about")
    object ChangePassword : Screen("change_password")
    object LanguageSelection : Screen("language_selection")
    object PrivacyPolicy : Screen("privacy_policy")
    object TermsAndConditions : Screen("terms_and_conditions")
}

@Composable
fun SampahJujurNavGraph(
    navController: NavHostController,
    authViewModel: com.melodi.sampahjujur.viewmodel.AuthViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    pendingNavigation: PendingNavigation? = null,
    onNavigationHandled: () -> Unit = {}
) {
    val authState by authViewModel.authState.collectAsState()
    val context = LocalContext.current

    // Create GoogleSignInClient for sign-out functionality
    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(GoogleSignInModule.getWebClientId())
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    // Handle deep linking from notifications
    LaunchedEffect(pendingNavigation, authState) {
        if (pendingNavigation != null && authState is AuthViewModel.AuthState.Authenticated) {
            Log.d("NavGraph", "=== Handling Deep Link ===")
            Log.d("NavGraph", "Pending Navigation: $pendingNavigation")
            Log.d("NavGraph", "Auth State: Authenticated")

            val route = when (pendingNavigation) {
                is PendingNavigation.Chat -> {
                    Log.d("NavGraph", "Navigating to Chat with requestId: ${pendingNavigation.requestId}")
                    Screen.Chat.createRoute(pendingNavigation.requestId)
                }
                is PendingNavigation.HouseholdRequestDetail -> {
                    Log.d("NavGraph", "Navigating to Household Request Detail: ${pendingNavigation.requestId}")
                    Screen.HouseholdRequestDetail.createRoute(pendingNavigation.requestId)
                }
                is PendingNavigation.CollectorRequestDetail -> {
                    Log.d("NavGraph", "Navigating to Collector Request Detail: ${pendingNavigation.requestId}")
                    Screen.CollectorRequestDetail.createRoute(pendingNavigation.requestId)
                }
            }

            navController.navigate(route)
            onNavigationHandled()
            Log.d("NavGraph", "Navigation completed, pending navigation cleared")
        }
    }

    // Handle auth state changes during runtime (e.g., after login/logout)
    LaunchedEffect(authState) {
        when (authState) {
            is com.melodi.sampahjujur.viewmodel.AuthViewModel.AuthState.Authenticated -> {
                val user = (authState as com.melodi.sampahjujur.viewmodel.AuthViewModel.AuthState.Authenticated).user
                val destination = if (user.isHousehold()) {
                    Screen.HouseholdRequest.route
                } else {
                    Screen.CollectorDashboard.route
                }

                // Only navigate if we're on auth/onboarding screens
                val currentRoute = navController.currentBackStackEntry?.destination?.route
                if (currentRoute == Screen.Onboarding.route ||
                    currentRoute == Screen.RoleSelection.route ||
                    currentRoute == Screen.HouseholdLogin.route ||
                    currentRoute == Screen.HouseholdRegistration.route ||
                    currentRoute == Screen.CollectorLogin.route ||
                    currentRoute == Screen.CollectorRegistration.route) {
                    navController.navigate(destination) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            else -> {
                // Not authenticated or loading
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Loading.route
    ) {
        // Loading Screen - shows while checking auth
        composable(Screen.Loading.route) {
            LoadingScreen()

            // Navigate based on auth state
            LaunchedEffect(authState) {
                when (authState) {
                    is com.melodi.sampahjujur.viewmodel.AuthViewModel.AuthState.Authenticated -> {
                        // If there's a pending deep link navigation, let it be handled instead
                        // Don't add pendingNavigation to LaunchedEffect keys or it will re-run when cleared
                        if (pendingNavigation != null) {
                            Log.d("NavGraph", "Loading screen: Skipping home navigation due to pending deep link: $pendingNavigation")
                            return@LaunchedEffect
                        }

                        val user = (authState as com.melodi.sampahjujur.viewmodel.AuthViewModel.AuthState.Authenticated).user
                        val destination = if (user.isHousehold()) {
                            Screen.HouseholdRequest.route
                        } else {
                            Screen.CollectorDashboard.route
                        }
                        Log.d("NavGraph", "Loading screen: Navigating to home screen: $destination")
                        navController.navigate(destination) {
                            popUpTo(Screen.Loading.route) { inclusive = true }
                        }
                    }
                    is com.melodi.sampahjujur.viewmodel.AuthViewModel.AuthState.Unauthenticated -> {
                        navController.navigate(Screen.Onboarding.route) {
                            popUpTo(Screen.Loading.route) { inclusive = true }
                        }
                    }
                    else -> {
                        // Still loading, do nothing
                    }
                }
            }
        }
        // Onboarding Screen
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onSkip = {
                    navController.navigate(Screen.RoleSelection.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
                onFinish = {
                    navController.navigate(Screen.RoleSelection.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        // Role Selection Screen
        composable(Screen.RoleSelection.route) {
            RoleSelectionScreen(
                onHouseholdSelected = {
                    navController.navigate(Screen.HouseholdLogin.route)
                },
                onCollectorSelected = {
                    navController.navigate(Screen.CollectorLogin.route)
                }
            )
        }

        // Household Login
        composable(Screen.HouseholdLogin.route) {
            HouseholdLoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    // AuthViewModel will handle navigation via LaunchedEffect above
                },
                onGoogleSignInClick = {
                    // TODO: Handle Google sign-in
                },
                onForgotPasswordClick = {
                    navController.navigate(Screen.ForgotPassword.route)
                },
                onSignUpClick = {
                    navController.navigate(Screen.HouseholdRegistration.route)
                }
            )
        }

        // Forgot Password
        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                viewModel = authViewModel,
                onBackClick = {
                    navController.popBackStack()
                },
                onEmailSent = {
                    // User can navigate back manually
                }
            )
        }

        // Household Registration
        composable(Screen.HouseholdRegistration.route) {
            HouseholdRegistrationScreen(
                viewModel = authViewModel,
                onRegisterSuccess = {
                    // AuthViewModel will handle navigation via LaunchedEffect above
                },
                onLoginClick = {
                    navController.popBackStack()
                },
                onTermsClick = {
                    navController.navigate(Screen.TermsAndConditions.route)
                },
                onPrivacyPolicyClick = {
                    navController.navigate(Screen.PrivacyPolicy.route)
                }
            )
        }

        // Collector Login
        composable(Screen.CollectorLogin.route) {
            CollectorLoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    // AuthViewModel will handle navigation via LaunchedEffect above
                },
                onSignUpClick = {
                    navController.navigate(Screen.CollectorRegistration.route)
                }
            )
        }

        // Collector Registration
        composable(Screen.CollectorRegistration.route) {
            CollectorRegistrationScreen(
                viewModel = authViewModel,
                onRegisterSuccess = {
                    // AuthViewModel will handle navigation via LaunchedEffect above
                },
                onLoginClick = {
                    navController.popBackStack()
                },
                onTermsClick = {
                    navController.navigate(Screen.TermsAndConditions.route)
                },
                onPrivacyPolicyClick = {
                    navController.navigate(Screen.PrivacyPolicy.route)
                }
            )
        }

        // Household Request Pickup Screen
        composable(Screen.HouseholdRequest.route) { backStackEntry ->
            // Use navController.getBackStackEntry to share ViewModel between Request and LocationPicker
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.HouseholdRequest.route)
            }

            RequestPickupScreen(
                viewModel = androidx.hilt.navigation.compose.hiltViewModel(parentEntry),
                onNavigate = { route ->
                    when (route) {
                        "request" -> { /* Already here */ }
                        "my_requests" -> navController.navigate(Screen.HouseholdMyRequests.route)
                        "household_profile" -> navController.navigate(Screen.HouseholdProfile.route)
                        "location_picker" -> navController.navigate(Screen.HouseholdLocationPicker.route)
                    }
                }
            )
        }

        // Household Location Picker Screen
        composable(Screen.HouseholdLocationPicker.route) { backStackEntry ->
            // Share the same ViewModel instance with RequestPickupScreen
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.HouseholdRequest.route)
            }

            LocationPickerScreen(
                viewModel = androidx.hilt.navigation.compose.hiltViewModel(parentEntry),
                onLocationSelected = { geoPoint, address ->
                    // Location is already set in ViewModel via viewModel.setPickupLocation()
                    // This callback is redundant but kept for flexibility
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // Household My Requests Screen
        composable(Screen.HouseholdMyRequests.route) {
            val viewModel: com.melodi.sampahjujur.viewmodel.HouseholdViewModel =
                androidx.hilt.navigation.compose.hiltViewModel()
            val requests by viewModel.userRequests.observeAsState(emptyList())

            MyRequestsScreen(
                requests = requests,
                onRequestClick = { requestId ->
                    navController.navigate(Screen.HouseholdRequestDetail.createRoute(requestId))
                },
                onNavigate = { route ->
                    when (route) {
                        "request" -> navController.navigate(Screen.HouseholdRequest.route)
                        "my_requests" -> { /* Already here */ }
                        "household_profile" -> navController.navigate(Screen.HouseholdProfile.route)
                    }
                }
            )
        }

        // Household Request Detail Screen
        composable(
            route = Screen.HouseholdRequestDetail.route,
            arguments = listOf(navArgument("requestId") { type = NavType.StringType })
        ) { backStackEntry ->
            val requestId = backStackEntry.arguments?.getString("requestId") ?: ""

            HouseholdRequestDetailRoute(
                requestId = requestId,
                onBackClick = {
                    navController.popBackStack()
                },
                onNavigateToChat = { reqId ->
                    navController.navigate(Screen.Chat.createRoute(reqId))
                }
            )
        }

        // Household Profile Screen
        composable(Screen.HouseholdProfile.route) {
            val authState by authViewModel.authState.collectAsState()
            val user = when (authState) {
                is com.melodi.sampahjujur.viewmodel.AuthViewModel.AuthState.Authenticated -> {
                    (authState as com.melodi.sampahjujur.viewmodel.AuthViewModel.AuthState.Authenticated).user
                }
                else -> User(
                    id = "",
                    fullName = "",
                    email = "",
                    phone = "",
                    userType = "household"
                )
            }

            // Get HouseholdViewModel to fetch user requests for statistics
            val householdViewModel: com.melodi.sampahjujur.viewmodel.HouseholdViewModel =
                androidx.hilt.navigation.compose.hiltViewModel()
            val requests by householdViewModel.userRequests.observeAsState(emptyList())

            // Calculate statistics from requests
            val totalRequests = requests.size
            val totalWasteCollected = requests.sumOf { it.wasteItems.sumOf { item -> item.weight } }
            val totalEarnings = requests.filter { it.status == PickupRequest.STATUS_COMPLETED }
                .sumOf { it.wasteItems.sumOf { item -> item.estimatedValue } }

            HouseholdProfileScreen(
                user = user,
                totalRequests = totalRequests,
                totalWasteCollected = totalWasteCollected,
                totalEarnings = totalEarnings,
                onEditProfileClick = {
                    navController.navigate(Screen.HouseholdEditProfile.route)
                },
                onStatisticsClick = {
                    navController.navigate(Screen.HouseholdStatistics.route)
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                },
                onHelpSupportClick = {
                    navController.navigate(Screen.HelpSupport.route)
                },
                onAboutClick = {
                    navController.navigate(Screen.About.route)
                },
                onLogoutClick = {
                    // Sign out from both Firebase and Google
                    googleSignInClient.signOut().addOnCompleteListener {
                        android.util.Log.d("NavGraph", "Google sign-out completed")
                    }
                    authViewModel.signOut()
                    navController.navigate(Screen.RoleSelection.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigate = { route ->
                    when (route) {
                        "request" -> navController.navigate(Screen.HouseholdRequest.route)
                        "my_requests" -> navController.navigate(Screen.HouseholdMyRequests.route)
                        "household_profile" -> { /* Already here */ }
                    }
                }
            )
        }

        // Household Edit Profile Screen
        composable(Screen.HouseholdEditProfile.route) {
            val authState by authViewModel.authState.collectAsState()
            val user = when (authState) {
                is com.melodi.sampahjujur.viewmodel.AuthViewModel.AuthState.Authenticated -> {
                    (authState as com.melodi.sampahjujur.viewmodel.AuthViewModel.AuthState.Authenticated).user
                }
                else -> User(
                    id = "",
                    fullName = "",
                    email = "",
                    phone = "",
                    address = "",
                    userType = "household"
                )
            }

            EditProfileScreen(
                user = user,
                onBackClick = {
                    navController.popBackStack()
                },
                onChangePasswordClick = {
                    navController.navigate(Screen.ChangePassword.route)
                },
                onSaveClick = { fullName, phone, address, profileImageUrl ->
                    // TODO: Update in ViewModel (master has updateHouseholdProfile method)
                    navController.popBackStack()
                }
            )
        }

        // Household Statistics Screen
        composable(Screen.HouseholdStatistics.route) {
            HouseholdStatisticsScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // Collector Dashboard Screen
        composable(Screen.CollectorDashboard.route) {
            CollectorDashboardScreen(
                onRequestClick = { requestId ->
                    navController.navigate(Screen.CollectorRequestDetail.createRoute(requestId))
                },
                onNavigate = { route ->
                    when (route) {
                        Screen.CollectorDashboard.route -> { /* Already here */ }
                        Screen.CollectorMap.route -> navController.navigate(Screen.CollectorMap.route)
                        Screen.CollectorProfile.route -> navController.navigate(Screen.CollectorProfile.route)
                    }
                }
            )
        }

        // Collector Map Screen
        composable(Screen.CollectorMap.route) {
            CollectorMapScreen(
                onNavigate = { route ->
                    when (route) {
                        Screen.CollectorDashboard.route -> navController.navigate(Screen.CollectorDashboard.route)
                        Screen.CollectorMap.route -> { /* Already here */ }
                        Screen.CollectorProfile.route -> navController.navigate(Screen.CollectorProfile.route)
                    }
                },
                onRequestSelected = { requestId ->
                    navController.navigate(Screen.CollectorRequestDetail.createRoute(requestId))
                }
            )
        }

        // Collector Request Detail Screen
        composable(
            route = Screen.CollectorRequestDetail.route,
            arguments = listOf(navArgument("requestId") { type = NavType.StringType })
        ) { backStackEntry ->
            val requestId = backStackEntry.arguments?.getString("requestId") ?: ""
            CollectorRequestDetailRoute(
                requestId = requestId,
                onBackClick = { navController.popBackStack() },
                onNavigateToChat = { reqId ->
                    navController.navigate(Screen.Chat.createRoute(reqId))
                }
            )
        }

        // Collector Profile Screen
        composable(Screen.CollectorProfile.route) {
            val authState by authViewModel.authState.collectAsState()
            val user = when (authState) {
                is com.melodi.sampahjujur.viewmodel.AuthViewModel.AuthState.Authenticated -> {
                    (authState as com.melodi.sampahjujur.viewmodel.AuthViewModel.AuthState.Authenticated).user
                }
                else -> User(
                    id = "",
                    fullName = "",
                    email = "",
                    phone = "",
                    userType = "collector"
                )
            }
            val collectorViewModel: CollectorViewModel = hiltViewModel()
            val performanceMetrics by collectorViewModel.performanceMetrics.collectAsState()

            CollectorProfileScreen(
                user = user,
                totalCollections = performanceMetrics.totalCompleted,
                totalWasteCollected = performanceMetrics.totalWasteKg,
                totalSpent = performanceMetrics.totalSpent,
                completionRate = performanceMetrics.completionRate,
                vehicleType = user.vehicleType,
                vehiclePlateNumber = user.vehiclePlateNumber,
                profileImageUrl = user.profileImageUrl,
                onEditProfileClick = {
                    navController.navigate(Screen.CollectorEditProfile.route)
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                },
                onPerformanceClick = {
                    navController.navigate(Screen.CollectorPerformance.route)
                },
                onHelpSupportClick = {
                    navController.navigate(Screen.HelpSupport.route)
                },
                onAboutClick = {
                    navController.navigate(Screen.About.route)
                },
                onLogoutClick = {
                    // Sign out from both Firebase and Google
                    googleSignInClient.signOut().addOnCompleteListener {
                        android.util.Log.d("NavGraph", "Google sign-out completed")
                    }
                    authViewModel.signOut()
                    navController.navigate(Screen.RoleSelection.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigate = { route ->
                    when (route) {
                        Screen.CollectorDashboard.route -> navController.navigate(Screen.CollectorDashboard.route)
                        Screen.CollectorMap.route -> navController.navigate(Screen.CollectorMap.route)
                        Screen.CollectorProfile.route -> { /* Already here */ }
                    }
                }
            )
        }

        composable(Screen.CollectorPerformance.route) {
            CollectorPerformanceRoute(
                onBackClick = { navController.popBackStack() }
            )
        }

        // Collector Edit Profile Screen
        composable(Screen.CollectorEditProfile.route) {
            CollectorEditProfileRoute(
                onBackClick = { navController.popBackStack() },
                authViewModel = authViewModel
            )
        }

        // Settings Screen
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onLanguageClick = {
                    navController.navigate(Screen.LanguageSelection.route)
                },
                onPrivacyPolicyClick = {
                    navController.navigate(Screen.PrivacyPolicy.route)
                },
                onTermsClick = {
                    navController.navigate(Screen.TermsAndConditions.route)
                },
                onDeleteAccountClick = {
                    // TODO: Handle delete account
                }
            )
        }

        // Help & Support Screen
        composable(Screen.HelpSupport.route) {
            HelpSupportScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onLiveChatClick = {
                    // TODO: Handle live chat
                }
            )
        }

        // About Screen
        composable(Screen.About.route) {
            AboutScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // Change Password Screen
        composable(Screen.ChangePassword.route) {
            ChangePasswordScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // Language Selection Screen
        composable(Screen.LanguageSelection.route) {
            LanguageSelectionScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // Privacy Policy Screen
        composable(Screen.PrivacyPolicy.route) {
            PrivacyPolicyScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // Terms & Conditions Screen
        composable(Screen.TermsAndConditions.route) {
            TermsAndConditionsScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // Chat List Screen
        composable(Screen.ChatList.route) {
            com.melodi.sampahjujur.ui.screens.chat.ChatListScreen(
                onNavigateToChat = { requestId ->
                    navController.navigate(Screen.Chat.createRoute(requestId))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Chat Screen
        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("requestId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val requestId = backStackEntry.arguments?.getString("requestId") ?: ""
            com.melodi.sampahjujur.ui.screens.chat.ChatScreen(
                requestId = requestId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
