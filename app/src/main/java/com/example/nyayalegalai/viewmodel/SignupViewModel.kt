package com.example.nyayalegalai.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nyayalegalai.models.LawyerProfile
import com.example.nyayalegalai.models.SavedSettings
import com.example.nyayalegalai.models.UserProfile
import com.example.nyayalegalai.repository.FirebaseAuthRepository
import com.example.nyayalegalai.repository.FirestoreRepository
import com.example.nyayalegalai.utils.LocalUser
import com.example.nyayalegalai.utils.SessionManager
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class SignupViewModel(
    private val authRepo: FirebaseAuthRepository,
    private val firestoreRepo: FirestoreRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _signupState = MutableStateFlow<SignupState>(SignupState.Idle)
    val signupState: StateFlow<SignupState> = _signupState

    fun signupUser(
        name: String, 
        email: String, 
        phone: String, 
        pass: String,
        confirmPass: String,
        isLawyer: Boolean = false,
        barId: String = "",
        stateBarCouncil: String = "",
        specialization: String = "",
        additionalSpecializations: String = "",
        experience: String = "",
        qualification: String = "",
        university: String = "",
        location: String = "",
        languages: String = "",
        bio: String = "",
        consultationFee: Double = 500.0,
        onlineAvailable: Boolean = true,
        inPersonAvailable: Boolean = true,
        emergencyAvailable: Boolean = false,
        officeAddress: String = "",
        availableDays: String = "Mon - Sat",
        availableTime: String = "09:00 AM - 06:00 PM",
        profilePhotoUrl: String = "",
        fcmToken: String = ""
    ) {
        Log.d("SIGNUP_DEBUG", "ViewModel Called: name=$name, email=$email, isLawyer=$isLawyer")

        // 1. Blank fields validation
        if (name.isBlank() || email.isBlank() || phone.isBlank() || pass.isBlank()) {
            _signupState.value = SignupState.Error("All required fields must be filled")
            return
        }
        // 2. Email pattern validation
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            _signupState.value = SignupState.Error("Invalid email address")
            return
        }
        // 3. Phone pattern validation
        val phonePattern = "^[0-9]{10}$".toRegex()
        if (!phonePattern.matches(phone.trim())) {
            _signupState.value = SignupState.Error("Phone number must contain exactly 10 digits.")
            return
        }
        // 4. Password strong criteria validation
        val passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{6,}$".toRegex()
        if (!passwordPattern.matches(pass)) {
            _signupState.value = SignupState.Error("Password must contain at least 6 characters, including one uppercase letter, one lowercase letter, one number, and one special character.")
            return
        }
        // 5. Matching confirmation passwords
        if (pass != confirmPass) {
            _signupState.value = SignupState.Error("Passwords do not match")
            return
        }
        // 6. Professional field validation for lawyers
        if (isLawyer) {
            if (barId.isBlank() || specialization.isBlank() || experience.isBlank() || location.isBlank()) {
                _signupState.value = SignupState.Error("Bar ID, Specialization, Experience & City are required for lawyers")
                return
            }
        }

        viewModelScope.launch {
            _signupState.value = SignupState.Loading
            
            // Server-side validation check
            val serverError = withContext(Dispatchers.IO) {
                validateSignupOnBackend(phone.trim(), pass)
            }
            if (serverError != null) {
                _signupState.value = SignupState.Error(serverError)
                return@launch
            }

            try {
                Log.d("SIGNUP_DEBUG", "1. Attempting Auth Signup")
                val authResult = authRepo.signup(email.trim(), pass.trim())
                val firebaseUser = authResult.user ?: FirebaseAuth.getInstance().currentUser ?: throw Exception("Auth UID is null")
                val userId = firebaseUser.uid
                
                Log.d("SIGNUP_DEBUG", "2. Auth SUCCESS: $userId")

                // Clear any leftover local cache directly so new account is clean
                sessionManager.clearLocalCacheDirectly()

                val roleString = if (isLawyer) "LAWYER" else "USER"

                // Save profile and default settings under users/{uid}
                try {
                    withTimeout(10000) {
                        if (isLawyer) {
                            val profile = LawyerProfile(
                                lawyerId = userId,
                                userId = userId,
                                name = name.trim(),
                                email = email.trim(),
                                phone = phone.trim(),
                                profilePhotoUrl = profilePhotoUrl.trim(),
                                profileImage = profilePhotoUrl.trim(),
                                barCouncilNumber = barId.trim(),
                                enrollmentNumber = barId.trim(),
                                stateBarCouncil = stateBarCouncil.trim(),
                                barCouncil = stateBarCouncil.trim(),
                                specialization = specialization.trim(),
                                additionalSpecializations = additionalSpecializations.trim(),
                                experience = experience.trim(),
                                qualification = qualification.trim(),
                                university = university.trim(),
                                location = location.trim(),
                                city = location.trim(),
                                languages = languages.trim(),
                                bio = bio.trim(),
                                consultationFee = consultationFee,
                                onlineAvailable = onlineAvailable,
                                inPersonAvailable = inPersonAvailable,
                                isAvailable = onlineAvailable,
                                availability_status = onlineAvailable,
                                video_consultation_available = onlineAvailable,
                                isInPersonAvailable = inPersonAvailable,
                                in_person_consultation_available = inPersonAvailable,
                                emergencyAvailable = emergencyAvailable,
                                officeAddress = officeAddress.trim(),
                                availableDays = availableDays.ifBlank { "Mon - Sat" },
                                availableTime = availableTime.ifBlank { "09:00 AM - 06:00 PM" },
                                verificationStatus = "PENDING", // PENDING initially for new lawyer signups
                                role = "LAWYER",
                                createdAt = Timestamp.now(),
                                updatedAt = Timestamp.now()
                            )
                            Log.d("SIGNUP_DEBUG", "3a. Saving Lawyer Profile under users/$userId and lawyers/$userId")
                            firestoreRepo.saveLawyerProfile(profile)
                            firestoreRepo.saveSettings(userId, SavedSettings())
                            sessionManager.saveUser(LocalUser(userId, email.trim(), name.trim(), "LAWYER", phone.trim(), barId.trim()), pass)
                        } else {
                            val profile = UserProfile(
                                userId = userId,
                                name = name.trim(),
                                email = email.trim(),
                                phone = phone.trim(),
                                role = "USER",
                                createdAt = Timestamp.now()
                            )
                            Log.d("SIGNUP_DEBUG", "3b. Saving User Profile under users/$userId")
                            firestoreRepo.saveUserProfile(profile)
                            firestoreRepo.saveSettings(userId, SavedSettings())
                            sessionManager.saveUser(LocalUser(userId, email.trim(), name.trim(), "USER", phone.trim()), pass)
                        }
                    }
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    Log.e("SIGNUP_DEBUG", "Firestore save failed or timed out. Proceeding to success state.", e)
                    sessionManager.saveUser(LocalUser(userId, email.trim(), name.trim(), roleString, phone.trim()), pass)
                }
                
                viewModelScope.launch {
                    try {
                        authRepo.sendEmailVerification()
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        Log.e("SIGNUP_DEBUG", "Verification background error", e)
                    }
                }

                _signupState.value = SignupState.Success(roleString)
                
            } catch (e: FirebaseAuthWeakPasswordException) {
                _signupState.value = SignupState.Error("The password is too weak.")
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                _signupState.value = SignupState.Error("The email address is badly formatted.")
            } catch (e: FirebaseAuthUserCollisionException) {
                _signupState.value = SignupState.Error("An account already exists with this email address.")
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _signupState.value = SignupState.Error(e.localizedMessage ?: "Signup failed. Check connection.")
            }
        }
    }

    fun signupLawyer(
        name: String, email: String, phone: String, pass: String,
        specialization: String, experience: String, barId: String, city: String, state: String
    ) {
        signupUser(
            name = name,
            email = email,
            phone = phone,
            pass = pass,
            confirmPass = pass,
            isLawyer = true,
            barId = barId,
            stateBarCouncil = state,
            specialization = specialization,
            experience = experience,
            location = city
        )
    }

    private suspend fun validateSignupOnBackend(phone: String, pass: String): String? {
        val client = OkHttpClient.Builder()
            .connectTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val json = JSONObject()
        json.put("phone", phone)
        json.put("password", pass)

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = json.toString().toRequestBody(mediaType)
        
        // Target host machine localhost from Android Emulator: 10.0.2.2
        val request = Request.Builder()
            .url("http://10.0.2.2:5000/api/auth/signup/validate")
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
            Log.w("SIGNUP_DEBUG", "Backend validation endpoint unreachable: ${e.message}. Falling back to local validation.")
            null // Fallback to local validation when backend is offline
        }
    }

    fun resetState() {
        _signupState.value = SignupState.Idle
    }
}



sealed class SignupState {
    object Idle : SignupState()
    object Loading : SignupState()
    data class Success(val role: String) : SignupState()
    data class Error(val message: String) : SignupState()
}
