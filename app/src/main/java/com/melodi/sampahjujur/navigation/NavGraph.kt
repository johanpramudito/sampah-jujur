package com.melodi.sampahjujur.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.melodi.sampahjujur.model.PickupRequest
import com.melodi.sampahjujur.model.User
import com.melodi.sampahjujur.model.WasteItem
import com.melodi.sampahjujur.ui.screens.*
import com.melodi.sampahjujur.ui.screens.auth.*
import com.melodi.sampahjujur.ui.screens.collector.*
import com.melodi.sampahjujur.ui.screens.household.*
import com.melodi.sampahjujur.ui.screens.shared.*
import com.melodi.sampahjujur.viewmodel.AuthViewModel


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

    // Collector Main
    object CollectorDashboard : Screen("collector_dashboard")
    object CollectorRequestDetail : Screen("collector_request_detail/{requestId}") {
        fun createRoute(requestId: String) = "collector_request_detail/$requestId"
    }
    object CollectorProfile : Screen("collector_profile")
    object CollectorEditProfile : Screen("collector_edit_profile")

    // Shared
    object Settings : Screen("settings")
    object HelpSupport : Screen("help_support")
    object PrivacyPolicy : Screen("privacy_policy")
    object TermsAndConditions : Screen("terms_and_conditions")
}

@Composable
fun SampahJujurNavGraph(
    navController: NavHostController,
    authViewModel: com.melodi.sampahjujur.viewmodel.AuthViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val authState by authViewModel.authState.collectAsState()

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
                        val user = (authState as com.melodi.sampahjujur.viewmodel.AuthViewModel.AuthState.Authenticated).user
                        val destination = if (user.isHousehold()) {
                            Screen.HouseholdRequest.route
                        } else {
                            Screen.CollectorDashboard.route
                        }
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
            val viewModel: com.melodi.sampahjujur.viewmodel.HouseholdViewModel =
                androidx.hilt.navigation.compose.hiltViewModel()
            val requests by viewModel.userRequests.observeAsState(emptyList())

            // Find the request by ID
            val request = requests.find { it.id == requestId }

            if (request != null) {
                RequestDetailScreen(
                    request = request,
                    collectorName = null, // TODO: Load collector info from Firestore if assigned
                    collectorPhone = null,
                    collectorVehicle = null,
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onCancelRequest = {
                        viewModel.cancelPickupRequest(requestId)
                        navController.popBackStack()
                    },
                    onContactCollector = {
                        // TODO: Implement phone call or messaging
                    }
                )
            } else {
                // Request not found - show error or go back
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            }
        }

        // Household Profile Screen
        composable(Screen.HouseholdProfile.route) {
            val user = authViewModel.getCurrentUser() ?: User(
                id = "user1",
                fullName = "Test User",
                email = "test@example.com",
                phone = "+1234567890",
                userType = "household"
            )

            HouseholdProfileScreen(
                user = user,
                totalRequests = 0,
                totalWasteCollected = 0.0,
                totalEarnings = 0.0,
                onEditProfileClick = {
                    navController.navigate(Screen.HouseholdEditProfile.route)
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                },
                onHelpSupportClick = {
                    navController.navigate(Screen.HelpSupport.route)
                },
                onAboutClick = {
                    // TODO: Handle about
                },
                onLogoutClick = {
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
            // TODO: Get from ViewModel
            val user = User(
                id = "user1",
                fullName = "Test User",
                email = "test@example.com",
                phone = "+1234567890",
                address = "",
                userType = "household"
            )

            EditProfileScreen(
                user = user,
                onBackClick = {
                    navController.popBackStack()
                },
                onSaveClick = { fullName, email, phone, address, profileImageUrl ->
                    // TODO: Update in ViewModel
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
                        "collector_dashboard" -> { /* Already here */ }
                        "map_view" -> { /* TODO: Navigate to map */ }
                        "collector_profile" -> navController.navigate(Screen.CollectorProfile.route)
                    }
                }
            )
        }

        // Collector Request Detail Screen
        composable(
            route = Screen.CollectorRequestDetail.route,
            arguments = listOf(navArgument("requestId") { type = NavType.StringType })
        ) { backStackEntry ->
            val requestId = backStackEntry.arguments?.getString("requestId") ?: ""
            // TODO: Get request from ViewModel by ID
            val dummyRequest = PickupRequest(
                id = requestId,
                householdId = "user1",
                wasteItems = listOf(WasteItem("plastic", 5.0, 10.0, "Bottles")),
                pickupLocation = PickupRequest.Location(0.0, 0.0, "123 Main St"),
                status = "pending"
            )

            CollectorRequestDetailScreen(
                request = dummyRequest,
                householdName = "Household User",
                householdPhone = null,
                onBackClick = {
                    navController.popBackStack()
                },
                onAcceptRequest = {
                    // TODO: Accept in ViewModel
                    navController.popBackStack()
                },
                onNavigateToLocation = {
                    // TODO: Handle navigation
                },
                onStartPickup = {
                    // TODO: Start pickup in ViewModel
                },
                onCompletePickup = {
                    // TODO: Show complete transaction dialog
                },
                onContactHousehold = {
                    // TODO: Handle contact
                },
                onCancelRequest = {
                    // TODO: Cancel in ViewModel
                    navController.popBackStack()
                }
            )
        }

        // Collector Profile Screen
        composable(Screen.CollectorProfile.route) {
            val user = authViewModel.getCurrentUser() ?: User(
                id = "collector1",
                fullName = "Test Collector",
                email = "",
                phone = "+1234567890",
                userType = "collector"
            )

            CollectorProfileScreen(
                user = user,
                totalCollections = 0,
                totalWasteCollected = 0.0,
                totalEarnings = 0.0,
                completionRate = 0.0,
                vehicleInfo = "",
                onEditProfileClick = {
                    navController.navigate(Screen.CollectorEditProfile.route)
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                },
                onEarningsClick = {
                    // TODO: Handle earnings
                },
                onPerformanceClick = {
                    // TODO: Handle performance
                },
                onHelpSupportClick = {
                    navController.navigate(Screen.HelpSupport.route)
                },
                onAboutClick = {
                    // TODO: Handle about
                },
                onLogoutClick = {
                    authViewModel.signOut()
                    navController.navigate(Screen.RoleSelection.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigate = { route ->
                    when (route) {
                        "collector_dashboard" -> navController.navigate(Screen.CollectorDashboard.route)
                        "map_view" -> { /* TODO: Navigate to map */ }
                        "collector_profile" -> { /* Already here */ }
                    }
                }
            )
        }

        // Collector Edit Profile Screen
        composable(Screen.CollectorEditProfile.route) {
            // TODO: Get from ViewModel
            val user = User(
                id = "collector1",
                fullName = "Test Collector",
                email = "",
                phone = "+1234567890",
                userType = "collector"
            )

            CollectorEditProfileScreen(
                user = user,
                vehicleType = "",
                vehiclePlateNumber = "",
                operatingArea = "",
                onBackClick = {
                    navController.popBackStack()
                },
                onSaveClick = { fullName, phone, vehicleType, plateNumber, operatingArea ->
                    // TODO: Update in ViewModel
                    navController.popBackStack()
                }
            )
        }

        // Settings Screen
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onLanguageClick = {
                    // TODO: Handle language selection
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
                },
                onSubmitFeedback = { name, email, message ->
                    // TODO: Handle feedback submission
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
    }
}