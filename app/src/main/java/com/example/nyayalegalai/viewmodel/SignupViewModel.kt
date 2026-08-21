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
        profilePhotoUrl: String = ""
    ) {
        Log.d("SIGNUP_DEBUG", "ViewModel Called: name=$name, email=$email, isLawyer=$isLawyer")

        // Validation
        if (name.isBlank() || email.isBlank() || phone.isBlank() || pass.isBlank()) {
            Log.e("SIGNUP_DEBUG", "Validation Failed: Blank fields")
            _signupState.value = SignupState.Error("All required fields must be filled")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            Log.e("SIGNUP_DEBUG", "Validation Failed: Invalid email format: '$email'")
            _signupState.value = SignupState.Error("Invalid email address")
            return
        }
        if (pass.length < 6) {
            Log.e("SIGNUP_DEBUG", "Validation Failed: Password too short")
            _signupState.value = SignupState.Error("Password must be at least 6 characters")
            return
        }
        if (pass != confirmPass) {
            Log.e("SIGNUP_DEBUG", "Validation Failed: Passwords do not match")
            _signupState.value = SignupState.Error("Passwords do not match")
            return
        }
        if (isLawyer) {
            if (barId.isBlank() || specialization.isBlank() || experience.isBlank() || location.isBlank()) {
                _signupState.value = SignupState.Error("Bar ID, Specialization, Experience & City are required for lawyers")
                return
            }
        }

        viewModelScope.launch {
            _signupState.value = SignupState.Loading
            try {
                Log.d("SIGNUP_DEBUG", "1. Attempting Auth Signup")
                val authResult = authRepo.signup(email.trim(), pass)
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
                    Log.e("SIGNUP_DEBUG", "Firestore save failed or timed out. Proceeding to success state.", e)
                    sessionManager.saveUser(LocalUser(userId, email.trim(), name.trim(), roleString, phone.trim()), pass)
                }
                
                viewModelScope.launch {
                    try {
                        authRepo.sendEmailVerification()
                    } catch (e: Exception) {
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
