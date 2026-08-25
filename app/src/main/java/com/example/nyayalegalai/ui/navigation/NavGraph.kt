package com.example.nyayalegalai.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.nyayalegalai.ui.screens.*
import com.example.nyayalegalai.viewmodel.*

@Composable
fun NyayaNavGraph(
    navController: NavHostController, 
    chatViewModel: ChatViewModel,
    lawyerViewModel: LawyerViewModel,
    learningViewModel: LegalLearningViewModel,
    lawViewModel: LawViewModel,
    loginViewModel: LoginViewModel,
    signupViewModel: SignupViewModel,
    authViewModel: AuthViewModel,
    consultationViewModel: ConsultationViewModel,
    profileViewModel: ProfileViewModel,
    themeViewModel: ThemeViewModel,
    historyViewModel: HistoryViewModel
) {
    androidx.compose.runtime.DisposableEffect(navController) {
        val listener = androidx.navigation.NavController.OnDestinationChangedListener { _, destination, arguments ->
            android.util.Log.i("NYAYA_CRASH_DEBUG", "NavGraph: Navigation destination changed to route = ${destination.route}, args = $arguments")
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }

    NavHost(
        navController = navController,
        startDestination = Route.Splash.route
    ) {
        composable(Route.Splash.route) { SplashScreen(navController) }
        composable(Route.Login.route) { LoginScreen(navController, loginViewModel) }
        composable(Route.Signup.route) { SignupScreen(navController, signupViewModel) }
        composable(Route.LawyerDashboard.route) { LawyerDashboardScreen(navController, consultationViewModel) }
        composable(Route.LanguageSelection.route) { LanguageSelectionScreen(navController, profileViewModel) }
        composable(Route.Welcome.route) { WelcomeScreen(navController) }
        composable(Route.Dashboard.route) { DashboardScreen(navController, historyViewModel) }
        
        composable(
            route = Route.AiChatbot.route + "?sessionId={sessionId}",
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType; defaultValue = "-1" })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId")
            AiChatbotScreen(navController, chatViewModel, if (sessionId == "-1") null else sessionId)
        }

        composable(
            route = Route.LegalLearning.route + "?sessionId={sessionId}&query={query}",
            arguments = listOf(
                navArgument("sessionId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("query") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong("sessionId")
            val query = backStackEntry.arguments?.getString("query")
            LegalLearningScreen(
                navController = navController, 
                viewModel = learningViewModel, 
                sessionId = if (sessionId == -1L) null else sessionId,
                query = if (query.isNullOrBlank()) null else query
            )
        }
        
        composable(Route.LawEncyclopedia.route) { 
            LawEncyclopediaScreen(navController, lawViewModel) 
        }
        
        composable(
            route = "law_detail/{lawId}",
            arguments = listOf(navArgument("lawId") { type = NavType.IntType })
        ) { backStackEntry ->
            val lawId = backStackEntry.arguments?.getInt("lawId") ?: 0
            LawDetailScreen(navController, lawViewModel, lawId)
        }
        
        composable(Route.LawyerConsultation.route) { 
            LawyerListingScreen(navController, consultationViewModel, lawyerViewModel) 
        }

        composable("lawyer_detail/{lawyerId}") { backStackEntry ->
            val lawyerId = backStackEntry.arguments?.getString("lawyerId") ?: ""
            LawyerProfileDetailScreen(navController, consultationViewModel, lawyerId)
        }

        composable("booking_form/{lawyerId}") { backStackEntry ->
            val lawyerId = backStackEntry.arguments?.getString("lawyerId") ?: ""
            BookingFormScreen(navController, consultationViewModel, lawyerId)
        }

        composable(Route.LawyerHistory.route) {
            BookingStatusScreen(navController, consultationViewModel)
        }

        composable(
            route = "booking_detail/{consultationId}",
            arguments = listOf(navArgument("consultationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val consultationId = backStackEntry.arguments?.getString("consultationId") ?: ""
            BookingDetailScreen(navController, consultationViewModel, consultationId)
        }

        composable(Route.LearningHistory.route) {
            ChatHistoryScreen(navController, historyViewModel) // Redirect to unified history
        }

        composable(Route.Profile.route) {
            ProfileScreen(navController, profileViewModel)
        }

        composable(Route.EditProfile.route) {
            EditProfileScreen(navController, profileViewModel)
        }

        composable(Route.AppTutorial.route) {
            AppTutorialScreen(navController)
        }

        composable(Route.ChatHistory.route) {
            ChatHistoryScreen(navController, historyViewModel)
        }

        composable(Route.Settings.route) {
            SettingsScreen(navController, themeViewModel)
        }
    }
}
