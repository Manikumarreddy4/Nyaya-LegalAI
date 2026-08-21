package com.example.nyayalegalai.ui.navigation

import androidx.navigation.NavController
import com.example.nyayalegalai.database.ChatSession

fun navigateToChat(navController: NavController, session: ChatSession) {
    val route = when (session.chatbotType) {
        "AI_ASSISTANT" -> Route.AiChatbot.route
        "LEGAL_LEARNING" -> Route.LegalLearning.route
        "CONSULTATION" -> Route.LawyerConsultation.route
        "ENCYCLOPEDIA" -> Route.LawEncyclopedia.route
        else -> Route.AiChatbot.route
    }
    navController.navigate("$route?sessionId=${session.sessionId}")
}
