package com.example.nyayalegalai.ui.navigation

sealed class Route(val route: String) {
    object Splash : Route("splash")
    object LanguageSelection : Route("language_selection")
    object Welcome : Route("welcome")
    object Dashboard : Route("dashboard") // Default User Dashboard
    object LawyerDashboard : Route("lawyer_dashboard")
    object AiChatbot : Route("ai_chatbot")
    object LegalLearning : Route("legal_learning")
    object LawEncyclopedia : Route("law_encyclopedia")
    object LawyerConsultation : Route("lawyer_consultation")
    object Profile : Route("profile")
    object EditProfile : Route("edit_profile")
    object AppTutorial : Route("app_tutorial")
    object LawyerHistory : Route("lawyer_history")
    object LearningHistory : Route("learning_history")
    object Settings : Route("settings")
    object Login : Route("login")
    object Signup : Route("signup")
    object ChatHistory : Route("chat_history")
    object LawyerDetail : Route("lawyer_detail/{lawyerId}")
    object BookingDetail : Route("booking_detail/{consultationId}")
}
