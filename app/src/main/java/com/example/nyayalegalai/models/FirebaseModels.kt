package com.example.nyayalegalai.models

import com.google.firebase.Timestamp

data class UserProfile(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "USER",
    val profilePhotoUrl: String = "",
    val rating: Double = 4.5,
    val createdAt: Timestamp = Timestamp.now()
)

data class LawyerProfile(
    val lawyerId: String = "",
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val profileImage: String = "",
    val profilePhotoUrl: String = "",
    val enrollmentNumber: String = "",
    val barCouncilNumber: String = "",
    val barCouncil: String = "",
    val stateBarCouncil: String = "",
    val specialization: String = "",
    val additionalSpecializations: String = "",
    val experience: String = "",
    val qualification: String = "",
    val university: String = "",
    val location: String = "",
    val city: String = "",
    val state: String = "",
    val languages: String = "",
    val bio: String = "",
    val consultationFee: Double = 500.0,
    val onlineAvailable: Boolean = true,
    val inPersonAvailable: Boolean = true,
    val emergencyAvailable: Boolean = false,
    val officeAddress: String = "",
    val availableDays: String = "Mon - Sat",
    val availableTime: String = "09:00 AM - 06:00 PM",
    val verificationStatus: String = "PENDING", // PENDING, VERIFIED, REJECTED
    val availability: String = "Available",
    val rating: Double = 4.8,
    val reviewCount: Int = 0,
    val consultationCount: Int = 0,
    val role: String = "LAWYER",
    val fullName: String = "",
    val displayName: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
) {
    val displayNameString: String
        get() = name.ifBlank { fullName.ifBlank { displayName.ifBlank { "Advocate" } } }

    // Helper computed properties for UI backward compatibility
    val displayLocation: String
        get() = location.ifBlank { city.ifBlank { "Location not specified" } }

    val displayBarNumber: String
        get() = enrollmentNumber.ifBlank { barCouncilNumber }

    val displayPhoto: String
        get() = profileImage.ifBlank { profilePhotoUrl }

    val displayBarCouncil: String
        get() = barCouncil.ifBlank { stateBarCouncil }
}

data class Consultation(
    val consultationId: String = "",
    val clientId: String = "",
    val userId: String = "",
    val lawyerId: String = "",
    val clientName: String = "",
    val userName: String = "",
    val lawyerName: String = "",
    val caseTitle: String = "",
    val issueType: String = "",
    val issueTitle: String = "",
    val caseDescription: String = "",
    val issueDescription: String = "",
    val consultationType: String = "Online", // Online or In-Person
    val date: String = "",
    val dateTime: String = "",
    val time: String = "",
    val preferredLanguage: String = "English",
    val contactNumber: String = "",
    val documentUrls: List<String> = emptyList(),
    val status: String = "PENDING", // PENDING, ACCEPTED, REJECTED, CANCELLED, COMPLETED
    val fee: Double = 0.0,
    val notes: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
) {
    val displayClientName: String
        get() = clientName.ifBlank { userName.ifBlank { "Client" } }

    val displayCaseTitle: String
        get() = caseTitle.ifBlank { issueTitle.ifBlank { issueType.ifBlank { "Legal Consultation" } } }

    val displayDescription: String
        get() = caseDescription.ifBlank { issueDescription }

    val displayDate: String
        get() = date.ifBlank { dateTime }

    val effectiveClientId: String
        get() = clientId.ifBlank { userId }

    fun parseDateOnly(): java.util.Date? {
        val dateStr = date.ifBlank { dateTime }
        if (dateStr.isBlank()) return null
        
        val cleanDateStr = dateStr.trim()
        
        if (cleanDateStr.equals("today", ignoreCase = true)) {
            val cal = java.util.Calendar.getInstance()
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            return cal.time
        }
        if (cleanDateStr.equals("tomorrow", ignoreCase = true)) {
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            return cal.time
        }
        
        var datePart = cleanDateStr
        if (cleanDateStr.contains(",")) {
            datePart = cleanDateStr.split(",").first().trim()
        } else if (cleanDateStr.contains(" at ", ignoreCase = true)) {
            datePart = cleanDateStr.split(Regex("(?i) at ")).first().trim()
        }
        
        val formats = listOf(
            "dd/MM/yyyy",
            "dd MMMM yyyy",
            "dd MMM yyyy",
            "yyyy-MM-dd"
        )
        
        for (format in formats) {
            try {
                val sdf = java.text.SimpleDateFormat(format, java.util.Locale.getDefault())
                sdf.isLenient = false
                val parsed = sdf.parse(datePart)
                if (parsed != null) {
                    val cal = java.util.Calendar.getInstance()
                    cal.time = parsed
                    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    cal.set(java.util.Calendar.MINUTE, 0)
                    cal.set(java.util.Calendar.SECOND, 0)
                    cal.set(java.util.Calendar.MILLISECOND, 0)
                    return cal.time
                }
            } catch (e: Exception) {
                // continue
            }
        }
        
        for (format in formats) {
            try {
                val sdf = java.text.SimpleDateFormat(format, java.util.Locale.getDefault())
                sdf.isLenient = false
                val parsed = sdf.parse(cleanDateStr)
                if (parsed != null) {
                    val cal = java.util.Calendar.getInstance()
                    cal.time = parsed
                    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    cal.set(java.util.Calendar.MINUTE, 0)
                    cal.set(java.util.Calendar.SECOND, 0)
                    cal.set(java.util.Calendar.MILLISECOND, 0)
                    return cal.time
                }
            } catch (e: Exception) {
                // continue
            }
        }
        
        return null
    }

    fun parseTimeOnly(): java.util.Date? {
        if (time.isBlank()) return null
        val cleanTime = time.trim()
        val formats = listOf(
            "hh:mm a",
            "h:mm a",
            "HH:mm",
            "H:mm"
        )
        for (format in formats) {
            try {
                val sdf = java.text.SimpleDateFormat(format, java.util.Locale.getDefault())
                sdf.isLenient = false
                return sdf.parse(cleanTime)
            } catch (e: Exception) {
                // continue
            }
        }
        return null
    }
}

data class LawyerReview(
    val reviewId: String = "",
    val lawyerId: String = "",
    val clientId: String = "",
    val clientName: String = "",
    val rating: Double = 5.0,
    val comment: String = "",
    val createdAt: Timestamp = Timestamp.now()
)

data class FirestoreChatSession(
    val sessionId: String = "",
    val userId: String = "",
    val chatbotType: String = "",
    val title: String = "",
    val updatedAt: Timestamp = Timestamp.now(),
    val isPinned: Boolean = false
)

data class FirestoreChatMessage(
    val messageId: String = "",
    val sessionId: String = "",
    val sender: String = "", // "User" or "Bot"
    val message: String = "",
    val timestamp: Timestamp = Timestamp.now()
)

data class Bookmark(
    val bookmarkId: String = "",
    val userId: String = "",
    val lawTitle: String = "",
    val sectionNumber: String = "",
    val createdAt: Timestamp = Timestamp.now()
)

data class SavedDocument(
    val documentId: String = "",
    val userId: String = "",
    val title: String = "",
    val fileUrl: String = "",
    val createdAt: Timestamp = Timestamp.now()
)

data class SavedSettings(
    val darkMode: Boolean = false,
    val themeColor: String = "Default",
    val fontColor: String = "Default",
    val language: String = "en"
)
