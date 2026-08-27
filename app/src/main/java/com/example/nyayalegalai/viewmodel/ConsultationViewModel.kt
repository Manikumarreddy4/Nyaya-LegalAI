package com.example.nyayalegalai.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nyayalegalai.models.Consultation
import com.example.nyayalegalai.models.LawyerProfile
import com.example.nyayalegalai.repository.FirestoreRepository
import com.example.nyayalegalai.repository.StorageRepository
import com.example.nyayalegalai.utils.SessionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class ConsultationViewModel(
    private val firestoreRepo: FirestoreRepository,
    private val storageRepo: StorageRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val authStateListener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { firebaseAuth ->
        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser == null) {
            lastUserId = null
            lastLawyerId = null
            userBookingsJob?.cancel()
            userBookingsJob = null
            lawyerRequestsJob?.cancel()
            lawyerRequestsJob = null
            _userBookings.value = emptyList()
            _lawyerRequests.value = emptyList()
            _uploadState.value = UploadState.Idle
            Log.d("MY_BOOKINGS", "Auth state changed to logged-out. Cleared booking session caches.")
        } else {
            val currentUid = firebaseUser.uid
            Log.d("MY_BOOKINGS", "Auth state changed to logged-in for UID: $currentUid")
            if (currentUid != lastUserId) {
                lastUserId = null
                userBookingsJob?.cancel()
                userBookingsJob = null
            }
            if (currentUid != lastLawyerId) {
                lastLawyerId = null
                lawyerRequestsJob?.cancel()
                lawyerRequestsJob = null
            }
        }
    }

    init {
        com.google.firebase.auth.FirebaseAuth.getInstance().addAuthStateListener(authStateListener)
        loadLawyers()
    }

    override fun onCleared() {
        super.onCleared()
        try {
            com.google.firebase.auth.FirebaseAuth.getInstance().removeAuthStateListener(authStateListener)
        } catch (e: Exception) {
            Log.e("ConsultationViewModel", "Error removing authStateListener onCleared", e)
        }
    }

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState

    fun resetUploadState() {
        _uploadState.value = UploadState.Idle
    }

    private val _allLawyers = MutableStateFlow<List<LawyerProfile>>(emptyList())
    val allLawyers: StateFlow<List<LawyerProfile>> = _allLawyers

    private fun loadLawyers() {
        viewModelScope.launch {
            try {
                _allLawyers.value = firestoreRepo.getAllLawyers().filterNotNull()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e("ConsultationViewModel", "Error loading lawyers", e)
            }
        }
    }

    private fun checkAndExpireConsultations(list: List<Consultation>) {
        val now = java.util.Date()
        viewModelScope.launch {
            list.forEach { c ->
                val apptTime = c.parsedAppointmentDate()
                val safeStatus = c.status.uppercase()
                
                Log.d("CONSULTATION_COMPLETION", "CONSULTATION_COMPLETION:")
                Log.d("CONSULTATION_COMPLETION", "Consultation ID = ${c.consultationId}")
                Log.d("CONSULTATION_COMPLETION", "Status = $safeStatus")
                Log.d("CONSULTATION_COMPLETION", "Appointment Date = ${c.resolvedDate}")
                Log.d("CONSULTATION_COMPLETION", "Appointment Time = ${c.resolvedTime}")
                Log.d("CONSULTATION_COMPLETION", "Parsed Appointment DateTime = $apptTime")
                Log.d("CONSULTATION_COMPLETION", "Current DateTime = $now")

                if (safeStatus == "PENDING") {
                    if (apptTime != null && apptTime.before(now)) {
                        Log.d("CONSULTATION_COMPLETION", "Eligible for completion = false")
                        try {
                            Log.d("EXPIRE_DEBUG", "Auto-expiring consultation ${c.consultationId}")
                            firestoreRepo.updateConsultationStatus(c.consultationId, "EXPIRED")
                        } catch (e: Exception) {
                            Log.e("EXPIRE_DEBUG", "Failed to auto-expire ${c.consultationId}", e)
                        }
                    } else {
                        Log.d("CONSULTATION_COMPLETION", "Eligible for completion = false")
                    }
                } else if (safeStatus == "ACCEPTED") {
                    if (apptTime != null && !now.before(apptTime)) {
                        Log.d("CONSULTATION_COMPLETION", "Eligible for completion = true")
                        try {
                            Log.d("COMPLETION_DEBUG", "Auto-completing consultation ${c.consultationId}")
                            firestoreRepo.updateConsultationStatus(c.consultationId, "COMPLETED")
                        } catch (e: Exception) {
                            Log.e("COMPLETION_DEBUG", "Failed to auto-complete ${c.consultationId}", e)
                        }
                    } else {
                        Log.d("CONSULTATION_COMPLETION", "Eligible for completion = false")
                    }
                } else {
                    Log.d("CONSULTATION_COMPLETION", "Eligible for completion = false")
                }
            }
        }
    }

    private var lastLawyerId: String? = null
    private var lawyerRequestsJob: kotlinx.coroutines.Job? = null
    private val _lawyerRequests = MutableStateFlow<List<Consultation>>(emptyList())
    val lawyerRequests: StateFlow<List<Consultation>> = _lawyerRequests

    private var lastUserId: String? = null
    private var userBookingsJob: kotlinx.coroutines.Job? = null
    private val _userBookings = MutableStateFlow<List<Consultation>>(emptyList())
    val userBookings: StateFlow<List<Consultation>> = _userBookings

    fun getLawyerRequestsFlow(): StateFlow<List<Consultation>> {
        val currentLawyerId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
        
        if (currentLawyerId.isBlank()) {
            _lawyerRequests.value = emptyList()
            lawyerRequestsJob?.cancel()
            lawyerRequestsJob = null
            lastLawyerId = null
            return _lawyerRequests
        }
        
        if (currentLawyerId != lastLawyerId) {
            lawyerRequestsJob?.cancel()
            lastLawyerId = currentLawyerId
            
            lawyerRequestsJob = viewModelScope.launch {
                firestoreRepo.getLawyerConsultations(currentLawyerId)
                    .map { list ->
                        checkAndExpireConsultations(list)
                        list
                    }
                    .catch { e -> Log.e("ConsultationViewModel", "Error in lawyer requests flow", e) }
                    .collect { list ->
                        _lawyerRequests.value = list
                    }
            }
        }
        return _lawyerRequests
    }

    fun getMyBookingsFlow(): StateFlow<List<Consultation>> {
        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
        Log.d("MY_BOOKINGS", "Current Firebase UID = $currentUserId")
        Log.d("MY_BOOKINGS", "Loading bookings after login")
        Log.d("MY_BOOKINGS", "MY_BOOKINGS: Current user ID = $currentUserId")
        
        if (currentUserId.isBlank()) {
            _userBookings.value = emptyList()
            userBookingsJob?.cancel()
            userBookingsJob = null
            lastUserId = null
            return _userBookings
        }
        
        if (currentUserId != lastUserId) {
            userBookingsJob?.cancel()
            lastUserId = currentUserId
            
            Log.d("MY_BOOKINGS", "MY_BOOKINGS: Firestore query started")
            userBookingsJob = viewModelScope.launch {
                firestoreRepo.getUserConsultations(currentUserId)
                    .map { list ->
                        checkAndExpireConsultations(list)
                        list
                    }
                    .catch { e -> 
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        Log.e("MY_BOOKINGS", "MY_BOOKINGS ERROR: ${e.message}", e)
                        Log.e("ConsultationViewModel", "Error in my bookings flow", e)
                    }
                    .collect { list ->
                        Log.d("MY_BOOKINGS", "MY_BOOKINGS: Total bookings received = ${list.size}")
                        _userBookings.value = list
                    }
            }
        }
        return _userBookings
    }

    fun getMyBookings(): Flow<List<Consultation>> {
        return getMyBookingsFlow()
    }

    fun getLawyerRequests(): Flow<List<Consultation>> {
        return getLawyerRequestsFlow()
    }

    fun bookConsultation(
        lawyerUid: String,
        userName: String,
        issueType: String,
        description: String,
        dateTime: String,
        contact: String,
        documentUris: List<Uri> = emptyList(),
        lawyerName: String = "",
        caseTitle: String = "",
        consultationType: String = "Online",
        date: String = "",
        time: String = "",
        preferredLanguage: String = "English",
        notes: String = "",
        fee: Double = 500.0
    ) {
        viewModelScope.launch {
            _uploadState.value = UploadState.Loading
            try {
                val user = sessionManager.getUser() ?: throw Exception("Not logged in")
                val cleanContact = contact.ifBlank { user.phone }.trim()
                val phonePattern = "^[0-9]{10}$".toRegex()
                if (!phonePattern.matches(cleanContact)) {
                    throw Exception("Phone number must contain exactly 10 digits.")
                }

                // Call backend consultations validation endpoint
                val serverError = withContext(Dispatchers.IO) {
                    validateBookingOnBackend(cleanContact)
                }
                if (serverError != null) {
                    throw Exception(serverError)
                }
                
                Log.d("BOOKING", "BOOKING: Checking lawyer availability")
                Log.d("BOOKING", "BOOKING: Lawyer ID = $lawyerUid")
                val lawyer = firestoreRepo.getLawyerProfile(lawyerUid)
                
                if (lawyer == null) {
                    Log.e("BOOKING", "BOOKING ERROR: Lawyer profile not found")
                    throw Exception("This lawyer is currently unavailable.")
                }

                val mainAvailable = lawyer.isAvailable
                val inPersonAvailable = lawyer.isInPersonAvailable
                
                Log.d("BOOKING", "BOOKING: Consultation type = ${consultationType.uppercase()}")
                Log.d("BOOKING", "BOOKING: Main availability = $mainAvailable")
                Log.d("BOOKING", "BOOKING: In-person availability = $inPersonAvailable")

                if (consultationType == "Online") {
                    if (!mainAvailable) {
                        Log.e("BOOKING", "BOOKING ERROR: Lawyer is unavailable")
                        throw Exception("This lawyer is currently unavailable.")
                    }
                } else if (consultationType == "In-Person") {
                    if (!mainAvailable) {
                        Log.e("BOOKING", "BOOKING ERROR: Lawyer is unavailable")
                        throw Exception("This lawyer is currently unavailable.")
                    }
                    if (!inPersonAvailable) {
                        Log.e("BOOKING", "BOOKING ERROR: Lawyer is not available for in-person consultations")
                        throw Exception("This lawyer is not currently available for in-person consultations. Please select Online consultation.")
                    }
                }

                val imageUrls = mutableListOf<String>()
                
                documentUris.forEachIndexed { index, uri ->
                    val path = "consultations/${user.uid}/${System.currentTimeMillis()}_$index.jpg"
                    val url = storageRepo.uploadFile(path, uri)
                    imageUrls.add(url)
                }

                val finalDate = date.ifBlank { dateTime }
                val finalTitle = caseTitle.ifBlank { issueType }
                val finalLawyerName = if (lawyerName.isNotBlank()) lawyerName else {
                    allLawyers.value.find { it.lawyerId == lawyerUid || it.userId == lawyerUid }?.name ?: "Advocate"
                }

                val tempConsultation = Consultation(
                    date = finalDate,
                    dateTime = finalDate,
                    time = time
                )
                val apptDate = tempConsultation.parsedAppointmentDate()
                val apptTimestamp = if (apptDate != null) com.google.firebase.Timestamp(apptDate) else null

                if (apptTimestamp != null) {
                    val isBusy = firestoreRepo.checkLawyerBusy(lawyerUid, apptTimestamp)
                    if (isBusy) {
                        throw Exception("Lawyer is busy at this time. Please select another available date or time.")
                    }
                }

                val consultation = Consultation(
                    clientId = user.uid,
                    userId = user.uid,
                    lawyerId = lawyerUid,
                    clientName = userName.ifBlank { user.name },
                    userName = userName.ifBlank { user.name },
                    lawyerName = finalLawyerName,
                    caseTitle = finalTitle,
                    issueType = finalTitle,
                    caseDescription = description,
                    issueDescription = description,
                    consultationType = consultationType,
                    date = finalDate,
                    dateTime = finalDate,
                    time = time,
                    preferredLanguage = preferredLanguage,
                    contactNumber = contact.ifBlank { user.phone },
                    documentUrls = imageUrls,
                    status = "PENDING",
                    fee = fee,
                    notes = notes,
                    appointmentDateTime = apptTimestamp,
                    createdAt = com.google.firebase.Timestamp.now(),
                    updatedAt = com.google.firebase.Timestamp.now()
                )
                firestoreRepo.createConsultation(consultation)
                _uploadState.value = UploadState.Success
            } catch (e: Exception) {
                _uploadState.value = UploadState.Error(e.message ?: "Failed to book consultation")
            }
        }
    }

    fun updateStatus(consultationId: String, status: String) {
        viewModelScope.launch {
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val docSnap = db.collection("consultations").document(consultationId).get().await()
                val currentStatus = docSnap.getString("status") ?: "PENDING"
                var isPastAppt = false
                try {
                    val apptDateTimeVal = docSnap.get("appointmentDateTime") as? com.google.firebase.Timestamp
                    if (apptDateTimeVal != null) {
                        isPastAppt = apptDateTimeVal.toDate().before(java.util.Date())
                    } else {
                        val dateStr = (docSnap.getString("date") ?: "").ifBlank {
                            (docSnap.getString("dateTime") ?: "").ifBlank {
                                (docSnap.getString("appointmentDate") ?: "").ifBlank {
                                    (docSnap.getString("consultationDate") ?: "")
                                }
                            }
                        }
                        val timeStr = (docSnap.getString("time") ?: "").ifBlank {
                            (docSnap.getString("appointmentTime") ?: "").ifBlank {
                                (docSnap.getString("consultationTime") ?: "").ifBlank { "Not scheduled" }
                            }
                        }
                        if (dateStr.isNotBlank()) {
                            var dateVal: java.util.Date? = null
                            var timeVal: java.util.Date? = null
                            val formats = listOf("dd/MM/yyyy", "dd MMMM yyyy", "dd MMM yyyy", "yyyy-MM-dd")
                            for (format in formats) {
                                try {
                                    val sdf = java.text.SimpleDateFormat(format, java.util.Locale.getDefault())
                                    sdf.isLenient = false
                                    val parsed = sdf.parse(dateStr)
                                    if (parsed != null) {
                                        dateVal = parsed
                                        break
                                    }
                                } catch (e: Exception) {}
                            }
                            if (timeStr.isNotBlank() && timeStr != "Not scheduled") {
                                val timeFormats = listOf("hh:mm a", "h:mm a", "HH:mm", "H:mm")
                                for (format in timeFormats) {
                                    try {
                                        val sdf = java.text.SimpleDateFormat(format, java.util.Locale.getDefault())
                                        sdf.isLenient = false
                                        timeVal = sdf.parse(timeStr)
                                        break
                                    } catch (e: Exception) {}
                                }
                            }
                            if (dateVal != null) {
                                val cal = java.util.Calendar.getInstance()
                                cal.time = dateVal
                                if (timeVal != null) {
                                    val timeCal = java.util.Calendar.getInstance()
                                    timeCal.time = timeVal
                                    cal.set(java.util.Calendar.HOUR_OF_DAY, timeCal.get(java.util.Calendar.HOUR_OF_DAY))
                                    cal.set(java.util.Calendar.MINUTE, timeCal.get(java.util.Calendar.MINUTE))
                                } else {
                                    cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
                                    cal.set(java.util.Calendar.MINUTE, 59)
                                }
                                cal.set(java.util.Calendar.SECOND, 0)
                                cal.set(java.util.Calendar.MILLISECOND, 0)
                                isPastAppt = cal.time.before(java.util.Date())
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ConsultationViewModel", "Error checking appointment date in updateStatus", e)
                }

                if (currentStatus.uppercase() == "EXPIRED" || (currentStatus.uppercase() == "PENDING" && isPastAppt)) {
                    Log.w("ConsultationViewModel", "Cannot update status of expired consultation.")
                    return@launch
                }
                firestoreRepo.updateConsultationStatus(consultationId, status)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e("ConsultationViewModel", "Error updating status", e)
            }
        }
    }

    fun submitReview(
        consultation: Consultation,
        rating: Double,
        comment: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        viewModelScope.launch {
            val user = sessionManager.getUser()
            if (user == null) {
                onError(Exception("Not logged in"))
                return@launch
            }
            if (consultation.userId != user.uid && consultation.clientId != user.uid) {
                onError(Exception("You are not authorized to review this consultation."))
                return@launch
            }
            if (consultation.status.uppercase() != "COMPLETED") {
                onError(Exception("You can only review completed consultations."))
                return@launch
            }
            if (consultation.hasReviewed) {
                onError(Exception("You have already reviewed this consultation."))
                return@launch
            }
            if (rating < 1.0 || rating > 5.0) {
                onError(Exception("Please select a rating between 1 and 5 stars."))
                return@launch
            }

            val docId = consultation.consultationId.ifBlank { consultation.bookingId }
            Log.d("REVIEW", "REVIEW:")
            Log.d("REVIEW", "Current User ID = ${user.uid}")
            Log.d("REVIEW", "Consultation ID = $docId")
            Log.d("REVIEW", "Lawyer ID = ${consultation.lawyerId}")
            Log.d("REVIEW", "Status = ${consultation.status}")
            Log.d("REVIEW", "Has Reviewed = ${consultation.hasReviewed}")
            Log.d("REVIEW", "Review Eligible = true")

            try {
                val review = com.example.nyayalegalai.models.LawyerReview(
                    reviewId = docId,
                    consultationId = docId,
                    userId = user.uid,
                    clientId = user.uid,
                    lawyerId = consultation.lawyerId,
                    userName = user.name.ifBlank { "Client" },
                    clientName = user.name.ifBlank { "Client" },
                    rating = rating,
                    comment = comment,
                    createdAt = com.google.firebase.Timestamp.now()
                )
                Log.d("REVIEW_SUBMISSION", "REVIEW_SUBMISSION:")
                Log.d("REVIEW_SUBMISSION", "Submitting rating = $rating")
                firestoreRepo.submitReview(review)
                Log.d("REVIEW_SUBMISSION", "Firestore write successful")
                onSuccess()
            } catch (e: Exception) {
                Log.e("REVIEW ERROR", "REVIEW ERROR: ${e.message ?: "Unknown review error"}", e)
                onError(e)
            }
        }
    }

    fun getLawyerReviewsFlow(lawyerId: String): kotlinx.coroutines.flow.Flow<List<com.example.nyayalegalai.models.LawyerReview>> {
        return firestoreRepo.getLawyerReviewsFlow(lawyerId)
    }

    private suspend fun validateBookingOnBackend(phone: String): String? {
        val client = OkHttpClient.Builder()
            .connectTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val json = JSONObject()
        json.put("phone", phone)

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = json.toString().toRequestBody(mediaType)
        
        // Target host machine localhost from Android Emulator: 10.0.2.2
        val request = Request.Builder()
            .url("http://10.0.2.2:5000/api/consultations/validate")
            .post(body)
            .build()

        return try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: ""
                val errJson = JSONObject(errBody)
                errJson.optString("error", "Server validation failed.")
            } else {
                null // Success!
            }
        } catch (e: Exception) {
            Log.w("BOOKING", "Backend booking validation endpoint unreachable: ${e.message}. Falling back to local validation.")
            null // Fallback to local validation when backend is offline
        }
    }
}

sealed class UploadState {
    object Idle : UploadState()
    object Loading : UploadState()
    object Success : UploadState()
    data class Error(val message: String) : UploadState()
}
