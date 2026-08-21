package com.example.nyayalegalai.models

data class AIChatHistory(
    val id: String,
    val question: String,
    val answerPreview: String,
    val timestamp: String,
    val isPinned: Boolean = false,
    val timeInMillis: Long = System.currentTimeMillis()
)

val dummyChatHistory = listOf(
    AIChatHistory(
        id = "1",
        question = "What are the penalties for cyber defamation in India?",
        answerPreview = "Under the Information Technology Act, 2000, and the BNS, cyber defamation can lead to imprisonment...",
        timestamp = "10:30 AM",
        timeInMillis = System.currentTimeMillis() - 3600000
    ),
    AIChatHistory(
        id = "2",
        question = "How to file a consumer complaint for a faulty laptop?",
        answerPreview = "You can file a complaint through the E-Daakhil portal or visit your local District Consumer Commission...",
        timestamp = "Yesterday",
        timeInMillis = System.currentTimeMillis() - 86400000
    ),
    AIChatHistory(
        id = "3",
        question = "Difference between BNS and IPC?",
        answerPreview = "The Bharatiya Nyaya Sanhita (BNS) has replaced the Indian Penal Code (IPC) with several modifications in...",
        timestamp = "2 days ago",
        timeInMillis = System.currentTimeMillis() - 172800000
    ),
    AIChatHistory(
        id = "4",
        question = "Is a digital signature legally valid for property documents?",
        answerPreview = "Yes, under the IT Act, digital signatures are valid, but specific property registration laws might require...",
        timestamp = "5 May",
        timeInMillis = System.currentTimeMillis() - 500000000
    )
)
