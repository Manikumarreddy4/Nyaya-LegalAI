package com.example.nyayalegalai.models

data class ConsultationHistory(
    val lawyerName: String,
    val date: String,
    val topic: String,
    val status: String = "Completed"
)

val dummyConsultationHistory = listOf(
    ConsultationHistory(
        lawyerName = "Adv. Rajesh Kumar",
        date = "24 May 2024",
        topic = "Property Dispute Inquiry regarding ancestral land in Punjab."
    ),
    ConsultationHistory(
        lawyerName = "Adv. Sneha Sharma",
        date = "15 May 2024",
        topic = "Consumer Court complaint for defective electronic goods."
    ),
    ConsultationHistory(
        lawyerName = "Adv. Amit Verma",
        date = "02 May 2024",
        topic = "Drafting of Rental Agreement for commercial property."
    ),
    ConsultationHistory(
        lawyerName = "Adv. Priya Singh",
        date = "20 April 2024",
        topic = "Legal advice on Income Tax notice received for FY 2022-23."
    )
)
