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
import kotlinx.coroutines.launch

class ConsultationViewModel(
    private val firestoreRepo: FirestoreRepository,
    private val storageRepo: StorageRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState

    private val _allLawyers = MutableStateFlow<List<LawyerProfile>>(emptyList())
    val allLawyers: StateFlow<List<LawyerProfile>> = _allLawyers

    init {
        loadLawyers()
    }

    private fun loadLawyers() {
        viewModelScope.launch {
            try {
                _allLawyers.value = firestoreRepo.getAllLawyers().filterNotNull()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun getMyBookings(): Flow<List<Consultation>> {
        val user = sessionManager.getUser() ?: return flowOf(emptyList())
        return firestoreRepo.getUserConsultations(user.uid)
            .catch { e -> Log.e("ConsultationViewModel", "Error in my bookings flow", e) }
    }

    fun getLawyerRequests(): Flow<List<Consultation>> {
        val user = sessionManager.getUser() ?: return flowOf(emptyList())
        return firestoreRepo.getLawyerConsultations(user.uid)
            .catch { e -> Log.e("ConsultationViewModel", "Error in lawyer requests flow", e) }
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
                    createdAt = com.google.firebase.Timestamp.now(),
                    updatedAt = com.google.firebase.Timestamp.now()
                )
                firestoreRepo.createConsultation(consultation)
                _uploadState.value = UploadState.Success
            } catch (e: Exception) {
                _uploadState.value = UploadState.Error(e.localizedMessage ?: "Failed to book consultation")
            }
        }
    }

    fun updateStatus(consultationId: String, status: String) {
        viewModelScope.launch {
            try {
                firestoreRepo.updateConsultationStatus(consultationId, status)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}

sealed class UploadState {
    object Idle : UploadState()
    object Loading : UploadState()
    object Success : UploadState()
    data class Error(val message: String) : UploadState()
}
