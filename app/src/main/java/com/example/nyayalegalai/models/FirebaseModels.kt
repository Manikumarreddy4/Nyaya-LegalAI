package com.example.nyayalegalai.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class UserProfile(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "USER",
    val profilePhotoUrl: String = "",
    val rating: Double = 4.5,
    @get:PropertyName("isAvailable")
    val isAvailable: Boolean = true,
    val availabilityUpdatedAt: Timestamp? = null,
    @get:PropertyName("isInPersonAvailable")
    val isInPersonAvailable: Boolean = true,
    val inPersonAvailabilityUpdatedAt: Timestamp? = null,
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
    val onlineAvailable: Boolean = false,
    val inPersonAvailable: Boolean = false,
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
    @get:PropertyName("isAvailable")
    val isAvailable: Boolean = false,
    @get:PropertyName("availability_status")
    val availability_status: Boolean? = null,
    @get:PropertyName("video_consultation_available")
    val video_consultation_available: Boolean? = null,
    val availabilityUpdatedAt: Timestamp? = null,
    @get:PropertyName("isInPersonAvailable")
    val isInPersonAvailable: Boolean = false,
    @get:PropertyName("in_person_consultation_available")
    val in_person_consultation_available: Boolean? = null,
    val inPersonAvailabilityUpdatedAt: Timestamp? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
) {
    val isOnlineAvailable: Boolean
        get() {
            val status = availability_status ?: isAvailable
            if (!status) return false
            return video_consultation_available ?: onlineAvailable
        }

    val isInPersonOnlineAvailable: Boolean
        get() {
            val status = availability_status ?: isAvailable
            if (!status) return false
            return in_person_consultation_available ?: inPersonAvailable
        }

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
    val appointmentDate: String = "",
    val appointmentTime: String = "",
    val consultationDate: String = "",
    val consultationTime: String = "",
    val preferredLanguage: String = "English",
    val contactNumber: String = "",
    val documentUrls: List<String> = emptyList(),
    val status: String = "PENDING", // PENDING, ACCEPTED, REJECTED, CANCELLED, COMPLETED
    val fee: Double = 0.0,
    val notes: String = "",
    val bookingId: String = "",
    val userPhone: String = "",
    val userEmail: String = "",
    val language: String = "",
    val additionalNotes: String = "",
    val acceptedAt: Timestamp? = null,
    val rejectedAt: Timestamp? = null,
    val appointmentDateTime: Timestamp? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    @get:PropertyName("hasReviewed")
    val hasReviewed: Boolean = false,
    @get:PropertyName("reviewId")
    val reviewId: String = ""
) {
    init {
        val safeId = consultationId ?: ""
        val safeDate = resolvedDate
        val safeTime = resolvedTime
        val safeStatus = status ?: "PENDING"
        android.util.Log.d(
            "CONSULTATION_DATA",
            "CONSULTATION_DATA: ID = $safeId, Date = $safeDate, Time = $safeTime, Status = $safeStatus"
        )
    }

    val resolvedDate: String
        get() = (date ?: "").ifBlank { (dateTime ?: "").ifBlank { (appointmentDate ?: "").ifBlank { (consultationDate ?: "") } } }

    val resolvedTime: String
        get() = (time ?: "").ifBlank { (appointmentTime ?: "").ifBlank { (consultationTime ?: "").ifBlank { "Not scheduled" } } }

    fun parsedAppointmentDate(): java.util.Date? {
        val stored = appointmentDateTime
        if (stored != null) {
            return stored.toDate()
        }
        
        val parsedDate = parseDateOnly() ?: return null
        val parsedTime = parseTimeOnly()
        
        val cal = java.util.Calendar.getInstance()
        cal.time = parsedDate
        
        if (parsedTime != null) {
            val timeCal = java.util.Calendar.getInstance()
            timeCal.time = parsedTime
            cal.set(java.util.Calendar.HOUR_OF_DAY, timeCal.get(java.util.Calendar.HOUR_OF_DAY))
            cal.set(java.util.Calendar.MINUTE, timeCal.get(java.util.Calendar.MINUTE))
        } else {
            cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
            cal.set(java.util.Calendar.MINUTE, 59)
        }
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.time
    }

    val displayClientName: String
        get() = (clientName ?: "").ifBlank { (userName ?: "").ifBlank { "Client" } }

    val displayCaseTitle: String
        get() = (caseTitle ?: "").ifBlank { (issueTitle ?: "").ifBlank { (issueType ?: "").ifBlank { "Legal Consultation" } } }

    val displayDescription: String
        get() = (caseDescription ?: "").ifBlank { (issueDescription ?: "") }

    val displayDate: String
        get() = resolvedDate

    val effectiveClientId: String
        get() = (clientId ?: "").ifBlank { (userId ?: "") }

    fun parseDateOnly(): java.util.Date? {
        val dateStr = resolvedDate
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
        if (resolvedTime.isBlank()) return null
        val cleanTime = resolvedTime.trim()
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
    val consultationId: String = "",
    val userId: String = "",
    val clientId: String = "",
    val lawyerId: String = "",
    val userName: String = "",
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
