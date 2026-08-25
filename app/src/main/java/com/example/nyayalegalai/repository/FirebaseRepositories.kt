package com.example.nyayalegalai.repository

import android.net.Uri
import android.util.Log
import com.example.nyayalegalai.models.*
import com.example.nyayalegalai.database.LearningHistory
import com.example.nyayalegalai.database.ChatSession
import com.example.nyayalegalai.database.ChatHistoryMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

fun DocumentSnapshot.getSafeLong(fieldName: String, defaultValue: Long = 0L): Long {
    return try {
        when (val value = get(fieldName)) {
            is com.google.firebase.Timestamp -> value.toDate().time
            is Number -> value.toLong()
            is String -> value.toLongOrNull() ?: defaultValue
            else -> defaultValue
        }
    } catch (e: Exception) {
        Log.e("FIRESTORE_HISTORY", "Invalid timestamp/Long in document $id for field $fieldName", e)
        defaultValue
    }
}

fun DocumentSnapshot.toLawyerProfile(): LawyerProfile? {
    if (!exists()) return null
    return try {
        val lawyerId = getString("lawyerId") ?: getString("userId") ?: id
        val userId = getString("userId") ?: getString("lawyerId") ?: id
        val name = getString("name") ?: ""
        val email = getString("email") ?: ""
        val phone = getString("phone") ?: ""
        val profileImage = getString("profileImage") ?: getString("profilePhotoUrl") ?: ""
        val profilePhotoUrl = getString("profilePhotoUrl") ?: getString("profileImage") ?: ""
        val enrollmentNumber = getString("enrollmentNumber") ?: getString("barCouncilNumber") ?: ""
        val barCouncilNumber = getString("barCouncilNumber") ?: getString("enrollmentNumber") ?: ""
        val barCouncil = getString("barCouncil") ?: getString("stateBarCouncil") ?: ""
        val stateBarCouncil = getString("stateBarCouncil") ?: getString("barCouncil") ?: ""
        val specialization = getString("specialization") ?: ""
        val additionalSpecializations = getString("additionalSpecializations") ?: ""
        val experience = getString("experience") ?: ""
        val qualification = getString("qualification") ?: ""
        val university = getString("university") ?: ""
        val location = getString("location") ?: getString("city") ?: ""
        val city = getString("city") ?: getString("location") ?: ""
        val state = getString("state") ?: ""
        val languages = getString("languages") ?: ""
        val bio = getString("bio") ?: ""
        
        val feeVal = get("consultationFee")
        val consultationFee = when (feeVal) {
            is Number -> feeVal.toDouble()
            is String -> feeVal.toDoubleOrNull() ?: 500.0
            else -> 500.0
        }
        
        val onlineAvailable = getBoolean("onlineAvailable") ?: false
        val inPersonAvailable = getBoolean("inPersonAvailable") ?: false
        val emergencyAvailable = getBoolean("emergencyAvailable") ?: false
        val officeAddress = getString("officeAddress") ?: ""
        val availableDays = getString("availableDays") ?: "Mon - Sat"
        val availableTime = getString("availableTime") ?: "09:00 AM - 06:00 PM"
        val verificationStatus = getString("verificationStatus") ?: "PENDING"
        val availability = getString("availability") ?: "Available"
        
        val ratingVal = get("rating")
        val rating = when (ratingVal) {
            is Number -> ratingVal.toDouble()
            is String -> ratingVal.toDoubleOrNull() ?: 4.8
            else -> 4.8
        }
        
        val rcVal = get("reviewCount")
        val reviewCount = when (rcVal) {
            is Number -> rcVal.toInt()
            else -> 0
        }
        
        val ccVal = get("consultationCount")
        val consultationCount = when (ccVal) {
            is Number -> ccVal.toInt()
            else -> 0
        }
        
        val role = getString("role") ?: "LAWYER"
        val fullName = getString("fullName") ?: ""
        val displayName = getString("displayName") ?: ""
        val isAvailable = getBoolean("isAvailable") ?: getBoolean("onlineAvailable") ?: false
        val availabilityUpdatedAt = getTimestamp("availabilityUpdatedAt")
        val isInPersonAvailable = getBoolean("isInPersonAvailable") ?: getBoolean("inPersonAvailable") ?: false
        val inPersonAvailabilityUpdatedAt = getTimestamp("inPersonAvailabilityUpdatedAt")
        val createdAt = getTimestamp("createdAt") ?: com.google.firebase.Timestamp.now()
        val updatedAt = getTimestamp("updatedAt") ?: com.google.firebase.Timestamp.now()
        
        LawyerProfile(
            lawyerId = lawyerId,
            userId = userId,
            name = name,
            email = email,
            phone = phone,
            profileImage = profileImage,
            profilePhotoUrl = profilePhotoUrl,
            enrollmentNumber = enrollmentNumber,
            barCouncilNumber = barCouncilNumber,
            barCouncil = barCouncil,
            stateBarCouncil = stateBarCouncil,
            specialization = specialization,
            additionalSpecializations = additionalSpecializations,
            experience = experience,
            qualification = qualification,
            university = university,
            location = location,
            city = city,
            state = state,
            languages = languages,
            bio = bio,
            consultationFee = consultationFee,
            onlineAvailable = onlineAvailable,
            inPersonAvailable = inPersonAvailable,
            emergencyAvailable = emergencyAvailable,
            officeAddress = officeAddress,
            availableDays = availableDays,
            availableTime = availableTime,
            verificationStatus = verificationStatus,
            availability = availability,
            rating = rating,
            reviewCount = reviewCount,
            consultationCount = consultationCount,
            role = role,
            fullName = fullName,
            displayName = displayName,
            isAvailable = isAvailable,
            availabilityUpdatedAt = availabilityUpdatedAt,
            isInPersonAvailable = isInPersonAvailable,
            inPersonAvailabilityUpdatedAt = inPersonAvailabilityUpdatedAt,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    } catch (e: Exception) {
        Log.e("FIND_LAWYER", "Error deserializing document ${id}", e)
        null
    }
}

class FirebaseAuthRepository(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) {
    fun getCurrentUser() = auth.currentUser
    
    suspend fun signup(email: String, pass: String): com.google.firebase.auth.AuthResult {
        return auth.createUserWithEmailAndPassword(email, pass).await()
    }
    
    suspend fun login(email: String, pass: String) = auth.signInWithEmailAndPassword(email, pass).await()
    
    suspend fun logout() = auth.signOut()
    
    suspend fun sendPasswordResetEmail(email: String) = auth.sendPasswordResetEmail(email).await()

    suspend fun sendEmailVerification() {
        auth.currentUser?.sendEmailVerification()?.await()
    }
}

fun DocumentSnapshot.toFirestoreChatSession(): FirestoreChatSession? {
    if (!exists()) return null
    return try {
        val sessionIdVal = get("sessionId")
        val sessionId = when (sessionIdVal) {
            is Number -> sessionIdVal.toLong().toString()
            is String -> sessionIdVal
            else -> id
        }
        val userId = getString("userId") ?: ""
        val chatbotType = getString("chatbotType") ?: "AI_ASSISTANT"
        val title = getString("title") ?: ""
        val isPinned = getBoolean("isPinned") ?: false
        
        val updatedAtVal = get("updatedAt")
        val updatedAt = when (updatedAtVal) {
            is Timestamp -> updatedAtVal
            is Number -> Timestamp(java.util.Date(updatedAtVal.toLong()))
            else -> Timestamp.now()
        }
        
        FirestoreChatSession(
            sessionId = sessionId,
            userId = userId,
            chatbotType = chatbotType,
            title = title,
            updatedAt = updatedAt,
            isPinned = isPinned
        )
    } catch (e: Exception) {
        Log.e("FIRESTORE_DEBUG", "Error parsing FirestoreChatSession for doc $id", e)
        null
    }
}

class FirestoreRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    // ==========================================
    // 1. Profile Management (users/{uid})
    // ==========================================

    suspend fun getUserProfile(userId: String): UserProfile? {
        if (userId.isBlank()) return null
        return try {
            val doc = db.collection("users").document(userId).get().await()
            if (!doc.exists()) return null
            val profile = doc.toObject(UserProfile::class.java)
            if (profile != null) {
                val normalizedRole = if (profile.role.equals("CLIENT", ignoreCase = true) || profile.role.equals("user", ignoreCase = true) || profile.role.equals("USER", ignoreCase = true)) "USER" else profile.role.uppercase()
                profile.copy(role = normalizedRole)
            } else {
                val roleVal = doc.getString("role") ?: "USER"
                val normalizedRole = if (roleVal.equals("CLIENT", ignoreCase = true) || roleVal.equals("user", ignoreCase = true) || roleVal.equals("USER", ignoreCase = true)) "USER" else roleVal.uppercase()
                UserProfile(
                    userId = userId,
                    name = doc.getString("name") ?: "",
                    email = doc.getString("email") ?: "",
                    phone = doc.getString("phone") ?: "",
                    role = normalizedRole,
                    profilePhotoUrl = doc.getString("profilePhotoUrl") ?: "",
                    rating = doc.getDouble("rating") ?: 4.5
                )
            }
        } catch (e: Exception) {
            Log.e("FIRESTORE_DEBUG", "Error fetching user profile for UID: $userId", e)
            null
        }
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        if (profile.userId.isBlank()) return
        db.collection("users").document(profile.userId).set(profile).await()
    }

    suspend fun updateUserProfile(userId: String, updates: Map<String, Any>) {
        if (userId.isBlank()) return
        db.collection("users").document(userId).update(updates).await()
        try {
            val lawyerDoc = db.collection("lawyers").document(userId).get().await()
            if (lawyerDoc.exists()) {
                db.collection("lawyers").document(userId).update(updates).await()
            }
        } catch (e: Exception) {
            Log.e("FIRESTORE_DEBUG", "Failed to update lawyers collection profile", e)
        }
    }

    suspend fun deleteUserProfile(userId: String) {
        if (userId.isBlank()) return
        db.collection("users").document(userId).delete().await()
    }

    suspend fun deleteLawyerProfile(userId: String) {
        if (userId.isBlank()) return
        db.collection("lawyers").document(userId).delete().await()
    }

    suspend fun getLawyerProfile(lawyerId: String): LawyerProfile? {
        if (lawyerId.isBlank()) return null
        return try {
            val doc = db.collection("users").document(lawyerId).get().await()
            if (doc.exists()) {
                doc.toLawyerProfile()
            } else {
                val lawyerDoc = db.collection("lawyers").document(lawyerId).get().await()
                if (lawyerDoc.exists()) lawyerDoc.toLawyerProfile() else null
            }
        } catch (e: Exception) {
            Log.e("FIRESTORE_DEBUG", "Error fetching lawyer profile for UID: $lawyerId", e)
            null
        }
    }

    suspend fun saveLawyerProfile(profile: LawyerProfile) {
        val id = profile.lawyerId.ifBlank { profile.userId }
        if (id.isBlank()) return
        val finalProfile = profile.copy(lawyerId = id, userId = id, updatedAt = com.google.firebase.Timestamp.now())
        db.collection("users").document(id).set(finalProfile).await()
        db.collection("lawyers").document(id).set(finalProfile).await()
    }

    fun getLawyerProfileFlow(lawyerId: String): Flow<LawyerProfile?> = callbackFlow {
        if (lawyerId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val listener = db.collection("lawyers").document(lawyerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("LAWYER_AVAILABILITY", "LAWYER_AVAILABILITY ERROR: ${error.message}", error)
                    trySend(null)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val profile = snapshot.toLawyerProfile()
                    Log.d("LAWYER_AVAILABILITY", "LAWYER_AVAILABILITY: Listener received = ${profile?.isAvailable ?: true}")
                    trySend(profile)
                } else {
                    // Fallback to check users collection
                    db.collection("users").document(lawyerId).get()
                        .addOnSuccessListener { userDoc ->
                            if (userDoc.exists()) {
                                val profile = userDoc.toLawyerProfile()
                                Log.d("LAWYER_AVAILABILITY", "LAWYER_AVAILABILITY: Listener received = ${profile?.isAvailable ?: true}")
                                trySend(profile)
                            } else {
                                trySend(null)
                            }
                        }
                        .addOnFailureListener {
                            trySend(null)
                        }
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun updateLawyerAvailability(lawyerId: String, isAvailable: Boolean) {
        if (lawyerId.isBlank()) return
        val updates = mapOf(
            "isAvailable" to isAvailable,
            "onlineAvailable" to isAvailable,
            "availabilityUpdatedAt" to com.google.firebase.Timestamp.now(),
            "updatedAt" to com.google.firebase.Timestamp.now()
        )
        try {
            db.collection("users").document(lawyerId).update(updates).await()
        } catch (e: Exception) {
            Log.e("FIRESTORE_DEBUG", "Failed to update users collection availability", e)
        }
        try {
            val lawyerDoc = db.collection("lawyers").document(lawyerId).get().await()
            if (lawyerDoc.exists()) {
                db.collection("lawyers").document(lawyerId).update(updates).await()
            }
        } catch (e: Exception) {
            Log.e("FIRESTORE_DEBUG", "Failed to update lawyers collection availability", e)
        }
    }

    suspend fun updateLawyerInPersonAvailability(lawyerId: String, isInPersonAvailable: Boolean) {
        if (lawyerId.isBlank()) return
        val updates = mapOf(
            "isInPersonAvailable" to isInPersonAvailable,
            "inPersonAvailabilityUpdatedAt" to com.google.firebase.Timestamp.now(),
            "updatedAt" to com.google.firebase.Timestamp.now()
        )
        try {
            db.collection("users").document(lawyerId).update(updates).await()
        } catch (e: Exception) {
            Log.e("FIRESTORE_DEBUG", "Failed to update users collection in-person availability", e)
        }
        try {
            val lawyerDoc = db.collection("lawyers").document(lawyerId).get().await()
            if (lawyerDoc.exists()) {
                db.collection("lawyers").document(lawyerId).update(updates).await()
            }
        } catch (e: Exception) {
            Log.e("FIRESTORE_DEBUG", "Failed to update lawyers collection in-person availability", e)
        }
    }

    fun getAllLawyersFlow(): Flow<List<LawyerProfile>> {
        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
        Log.d("FIND_LAWYERS", "FIND_LAWYERS: Fetching lawyers")
        Log.d("FIND_LAWYERS", "FIND_LAWYERS: Current user ID = $currentUserId")

        val flowLawyers = callbackFlow {
            val listener = db.collection("lawyers")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("FIND_LAWYERS", "FIND_LAWYERS ERROR: Firestore listener error (lawyers)", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = mutableListOf<LawyerProfile>()
                        for (document in snapshot.documents) {
                            val profile = document.toLawyerProfile()
                            if (profile != null) {
                                list.add(profile)
                            }
                        }
                        trySend(list)
                    }
                }
            awaitClose { listener.remove() }
        }

        val flowUsersUpper = callbackFlow {
            val listener = db.collection("users")
                .whereEqualTo("role", "LAWYER")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("FIND_LAWYERS", "FIND_LAWYERS ERROR: Firestore listener error (users LAWYER)", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = mutableListOf<LawyerProfile>()
                        for (document in snapshot.documents) {
                            val profile = document.toLawyerProfile()
                            if (profile != null) {
                                list.add(profile)
                            }
                        }
                        trySend(list)
                    }
                }
            awaitClose { listener.remove() }
        }

        val flowUsersLower = callbackFlow {
            val listener = db.collection("users")
                .whereEqualTo("role", "lawyer")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("FIND_LAWYERS", "FIND_LAWYERS ERROR: Firestore listener error (users lawyer)", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = mutableListOf<LawyerProfile>()
                        for (document in snapshot.documents) {
                            val profile = document.toLawyerProfile()
                            if (profile != null) {
                                list.add(profile)
                            }
                        }
                        trySend(list)
                    }
                }
            awaitClose { listener.remove() }
        }

        return kotlinx.coroutines.flow.combine(flowLawyers, flowUsersUpper, flowUsersLower) { list1, list2, list3 ->
            val mergedMap = mutableMapOf<String, LawyerProfile>()
            val validUserIds = (list2 + list3).map { it.userId.ifBlank { it.lawyerId } }.filter { it.isNotBlank() }.toSet()
            
            val totalDocs = list1.size + list2.size + list3.size
            Log.d("FIND_LAWYERS", "FIND_LAWYERS: Total Firestore documents = $totalDocs")

            list1.forEach { profile ->
                val id = profile.userId.ifBlank { profile.lawyerId }
                Log.d("FIND_LAWYERS", "FIND_LAWYERS: Processing lawyer ID = $id")
                if (id.isNotBlank()) {
                    if (validUserIds.contains(id)) {
                        mergedMap[id] = profile
                    } else {
                        Log.d("FIND_LAWYERS", "FIND_LAWYERS: Deleted/orphan profile skipped = $id")
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            try {
                                db.collection("lawyers").document(id).delete().await()
                                Log.d("FIND_LAWYERS", "FIND_LAWYERS: Successfully deleted orphan profile doc: $id")
                            } catch (e: Exception) {
                                Log.w("FIND_LAWYERS", "FIND_LAWYERS: Failed to delete orphan profile doc $id: ${e.message}")
                            }
                        }
                    }
                } else {
                    Log.d("FIND_LAWYERS", "FIND_LAWYERS: Invalid lawyer skipped = null/empty ID")
                }
            }

            (list2 + list3).forEach { profile ->
                val id = profile.userId.ifBlank { profile.lawyerId }
                Log.d("FIND_LAWYERS", "FIND_LAWYERS: Processing lawyer ID = $id")
                if (id.isNotBlank()) {
                    val existing = mergedMap[id]
                    if (existing == null) {
                        mergedMap[id] = profile
                    } else {
                        val mergedProfile = if (profile.specialization.isNotBlank() && existing.specialization.isBlank()) {
                            profile
                        } else {
                            existing
                        }
                        mergedMap[id] = mergedProfile
                    }
                } else {
                    Log.d("FIND_LAWYERS", "FIND_LAWYERS: Invalid lawyer skipped = null/empty ID")
                }
            }

            val rawMerged = mergedMap.values.toList()
            val filteredList = rawMerged.filter { profile ->
                val id = profile.userId.ifBlank { profile.lawyerId }
                val roleLower = profile.role.lowercase().trim()
                val isLawyer = roleLower == "lawyer" || roleLower == "advocate"
                val resolvedName = profile.displayNameString.trim()
                val isValid = isLawyer && resolvedName.isNotBlank() && id.isNotBlank()
                if (isValid) {
                    Log.d("FIND_LAWYERS", "FIND_LAWYERS: Valid lawyer added = $resolvedName")
                } else {
                    Log.d("FIND_LAWYERS", "FIND_LAWYERS: Invalid lawyer skipped = $id")
                }
                isValid
            }
            Log.d("FIND_LAWYERS", "FIND_LAWYERS: Listener updated")
            filteredList
        }
    }

    suspend fun getAllLawyers(): List<LawyerProfile> {
        return try {
            Log.d("FIND_LAWYERS", "FIND_LAWYERS: Fetching lawyers")
            
            val list1 = try {
                val snapshot = db.collection("lawyers").get().await()
                snapshot.documents.mapNotNull { document ->
                    document.toLawyerProfile()
                }
            } catch (e: Exception) {
                Log.e("FIND_LAWYERS", "FIND_LAWYERS ERROR: Firestore lawyer query failed (lawyers)", e)
                emptyList()
            }

            val list2 = try {
                val snapshot = db.collection("users").whereEqualTo("role", "LAWYER").get().await()
                snapshot.documents.mapNotNull { document ->
                    document.toLawyerProfile()
                }
            } catch (e: Exception) {
                Log.e("FIND_LAWYERS", "FIND_LAWYERS ERROR: Firestore lawyer query failed (LAWYER in users)", e)
                emptyList()
            }

            val list3 = try {
                val snapshot = db.collection("users").whereEqualTo("role", "lawyer").get().await()
                snapshot.documents.mapNotNull { document ->
                    document.toLawyerProfile()
                }
            } catch (e: Exception) {
                Log.e("FIND_LAWYERS", "FIND_LAWYERS ERROR: Firestore lawyer query failed (lawyer in users)", e)
                emptyList()
            }

            val mergedMap = mutableMapOf<String, LawyerProfile>()
            val validUserIds = (list2 + list3).map { it.userId.ifBlank { it.lawyerId } }.filter { it.isNotBlank() }.toSet()
            
            val totalDocs = list1.size + list2.size + list3.size
            Log.d("FIND_LAWYERS", "FIND_LAWYERS: Total Firestore documents = $totalDocs")

            list1.forEach { profile ->
                val id = profile.userId.ifBlank { profile.lawyerId }
                Log.d("FIND_LAWYERS", "FIND_LAWYERS: Processing lawyer ID = $id")
                if (id.isNotBlank()) {
                    if (validUserIds.contains(id)) {
                        mergedMap[id] = profile
                    } else {
                        Log.d("FIND_LAWYERS", "FIND_LAWYERS: Deleted/orphan profile skipped = $id")
                        try {
                            db.collection("lawyers").document(id).delete()
                            Log.d("FIND_LAWYERS", "FIND_LAWYERS: Successfully triggered delete for orphan profile doc: $id")
                        } catch (e: Exception) {
                            Log.w("FIND_LAWYERS", "FIND_LAWYERS: Failed to delete orphan profile doc $id: ${e.message}")
                        }
                    }
                } else {
                    Log.d("FIND_LAWYERS", "FIND_LAWYERS: Invalid lawyer skipped = null/empty ID")
                }
            }

            (list2 + list3).forEach { profile ->
                val id = profile.userId.ifBlank { profile.lawyerId }
                Log.d("FIND_LAWYERS", "FIND_LAWYERS: Processing lawyer ID = $id")
                if (id.isNotBlank()) {
                    val existing = mergedMap[id]
                    if (existing == null) {
                        mergedMap[id] = profile
                    } else {
                        val mergedProfile = if (profile.specialization.isNotBlank() && existing.specialization.isBlank()) {
                            profile
                        } else {
                            existing
                        }
                        mergedMap[id] = mergedProfile
                    }
                } else {
                    Log.d("FIND_LAWYERS", "FIND_LAWYERS: Invalid lawyer skipped = null/empty ID")
                }
            }

            val rawMerged = mergedMap.values.toList()
            val filteredList = rawMerged.filter { profile ->
                val id = profile.userId.ifBlank { profile.lawyerId }
                val roleLower = profile.role.lowercase().trim()
                val isLawyer = roleLower == "lawyer" || roleLower == "advocate"
                val resolvedName = profile.displayNameString.trim()
                val isValid = isLawyer && resolvedName.isNotBlank() && id.isNotBlank()
                if (isValid) {
                    Log.d("FIND_LAWYERS", "FIND_LAWYERS: Valid lawyer added = $resolvedName")
                } else {
                    Log.d("FIND_LAWYERS", "FIND_LAWYERS: Invalid lawyer skipped = $id")
                }
                isValid
            }
            filteredList
        } catch (e: Exception) {
            Log.e("FIND_LAWYERS", "FIND_LAWYERS ERROR: Error fetching all lawyers", e)
            emptyList()
        }
    }

    suspend fun getVerifiedLawyers(): List<LawyerProfile> {
        val all = getAllLawyers()
        return all.filter { lawyer ->
            lawyer.verificationStatus.equals("VERIFIED", ignoreCase = true) || lawyer.verificationStatus.isBlank()
        }
    }


    // ==========================================
    // 2. Settings (users/{uid}/settings/config)
    // ==========================================

    suspend fun saveSettings(uid: String, settings: SavedSettings) {
        if (uid.isBlank()) return
        db.collection("users").document(uid).collection("settings").document("config").set(settings).await()
    }

    suspend fun getUserSettings(uid: String): SavedSettings? {
        if (uid.isBlank()) return null
        return try {
            db.collection("users").document(uid).collection("settings").document("config").get().await().toObject(SavedSettings::class.java)
        } catch (e: Exception) {
            Log.e("FIRESTORE_DEBUG", "Error fetching user settings for UID: $uid", e)
            null
        }
    }

    // ==========================================
    // 3. Learning History (users/{uid}/learningHistory)
    // ==========================================

    suspend fun saveLearningHistory(uid: String, item: LearningHistory) {
        if (uid.isBlank()) return
        val docId = if (item.id == 0) {
            db.collection("users").document(uid).collection("learningHistory").document().id
        } else {
            item.id.toString()
        }
        db.collection("users").document(uid).collection("learningHistory").document(docId).set(item).await()
    }

    suspend fun getLearningHistoryList(uid: String): List<LearningHistory> {
        if (uid.isBlank()) return emptyList()
        return try {
            val snapshot = db.collection("users").document(uid).collection("learningHistory")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get().await()
            snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(LearningHistory::class.java)
                } catch (e: Exception) {
                    try {
                        val question = doc.getString("question") ?: doc.getString("query") ?: ""
                        val answer = doc.getString("answer") ?: doc.getString("explanation") ?: ""
                        val timestamp = doc.getSafeLong("timestamp", System.currentTimeMillis())
                        val idVal = doc.get("id")
                        val id = when (idVal) {
                            is Number -> idVal.toInt()
                            is String -> idVal.toIntOrNull() ?: doc.id.hashCode()
                            else -> doc.id.hashCode()
                        }
                        LearningHistory(id = id, question = question, answer = answer, timestamp = timestamp)
                    } catch (innerEx: Exception) {
                        Log.e("FIRESTORE_HISTORY", "Error parsing learning history document ${doc.id}", innerEx)
                        null
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FIRESTORE_DEBUG", "Error getting learning history list for UID: $uid", e)
            emptyList()
        }
    }

    fun getLearningHistoryFlow(uid: String): Flow<List<LearningHistory>> = callbackFlow {
        if (uid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = db.collection("users").document(uid).collection("learningHistory")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                snapshot?.let {
                    val list = it.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(LearningHistory::class.java)
                        } catch (e: Exception) {
                            try {
                                val question = doc.getString("question") ?: doc.getString("query") ?: ""
                                val answer = doc.getString("answer") ?: doc.getString("explanation") ?: ""
                                val timestamp = doc.getSafeLong("timestamp", System.currentTimeMillis())
                                val idVal = doc.get("id")
                                val id = when (idVal) {
                                    is Number -> idVal.toInt()
                                    is String -> idVal.toIntOrNull() ?: doc.id.hashCode()
                                    else -> doc.id.hashCode()
                                }
                                LearningHistory(id = id, question = question, answer = answer, timestamp = timestamp)
                            } catch (innerEx: Exception) {
                                Log.e("FIRESTORE_HISTORY", "Error parsing learning history document ${doc.id}", innerEx)
                                null
                            }
                        }
                    }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun deleteLearningHistoryItem(uid: String, docId: String) {
        if (uid.isBlank() || docId.isBlank()) return
        db.collection("users").document(uid).collection("learningHistory").document(docId).delete().await()
    }

    suspend fun deleteLearningHistoryItemByTimestamp(uid: String, timestamp: Long) {
        if (uid.isBlank()) return
        try {
            val snapshot = db.collection("users").document(uid).collection("learningHistory")
                .whereEqualTo("timestamp", timestamp)
                .get().await()
            for (doc in snapshot.documents) {
                doc.reference.delete().await()
            }
        } catch (e: Exception) {
            Log.e("FIRESTORE_DEBUG", "Error deleting learning history item by timestamp", e)
            throw e
        }
    }

    // ==========================================
    // 4. AI Problem Assistant History (users/{uid}/problemHistory)
    // ==========================================

    suspend fun saveChatSession(session: FirestoreChatSession) {
        if (session.userId.isBlank() || session.sessionId.isBlank()) return
        try {
            db.collection("users").document(session.userId)
                .collection("problemHistory").document(session.sessionId).set(session).await()
            Log.d("FIRESTORE", "Firestore saveChatSession success for UID ${session.userId}, Session: ${session.sessionId}")
        } catch (e: Exception) {
            Log.e("FIRESTORE", "Exception saving chat session", e)
            throw e
        }
    }

    fun getChatSessions(userId: String): Flow<List<FirestoreChatSession>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("users").document(userId)
            .collection("chatSessions")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val sessions = snapshot.documents.mapNotNull { it.toFirestoreChatSession() }
                    trySend(sessions)
                }
            }
        awaitClose { listener.remove() }
    }

    fun getProblemHistoryFlow(userId: String): Flow<List<FirestoreChatSession>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("users").document(userId)
            .collection("problemHistory")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val sessions = snapshot.documents.mapNotNull { it.toFirestoreChatSession() }
                    trySend(sessions)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun deleteChatSession(sessionId: String, userId: String? = null) {
        val uid = userId ?: FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (sessionId.isBlank()) return
        try {
            val sessionRef = db.collection("users").document(uid)
                .collection("problemHistory").document(sessionId)
            
            val messages = sessionRef.collection("messages").get().await()
            for (doc in messages) {
                doc.reference.delete().await()
            }
            sessionRef.delete().await()
        } catch (e: Exception) {
            Log.e("FIRESTORE", "Exception deleting session $sessionId", e)
            throw e
        }
    }

    suspend fun saveChatMessage(message: FirestoreChatMessage, userId: String? = null) {
        val uid = userId ?: FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (message.sessionId.isBlank()) return
        try {
            val sessionRef = db.collection("users").document(uid)
                .collection("problemHistory").document(message.sessionId)
            val doc = if (message.messageId.isEmpty()) {
                sessionRef.collection("messages").document()
            } else {
                sessionRef.collection("messages").document(message.messageId)
            }
            val finalMessage = message.copy(messageId = doc.id)
            doc.set(finalMessage).await()
        } catch (e: Exception) {
            Log.e("FIRESTORE", "Exception saving chat message", e)
            throw e
        }
    }

    fun getChatMessages(sessionId: String, userId: String? = null): Flow<List<FirestoreChatMessage>> = callbackFlow {
        val uid = userId ?: FirebaseAuth.getInstance().currentUser?.uid ?: ""
        if (uid.isBlank() || sessionId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("users").document(uid)
            .collection("problemHistory").document(sessionId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.toObjects(FirestoreChatMessage::class.java)
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun checkLawyerBusy(lawyerId: String, appointmentDateTime: com.google.firebase.Timestamp): Boolean {
        if (lawyerId.isBlank()) return false
        return try {
            val snapshot = db.collection("consultations")
                .whereEqualTo("lawyerId", lawyerId)
                .whereEqualTo("status", "ACCEPTED")
                .get()
                .await()
            for (doc in snapshot.documents) {
                val booking = doc.toObject(Consultation::class.java)
                if (booking != null) {
                    val apptDate = booking.parsedAppointmentDate()
                    val targetDate = appointmentDateTime.toDate()
                    if (apptDate != null && apptDate.time == targetDate.time) {
                        return true
                    }
                }
            }
            false
        } catch (e: Exception) {
            Log.e("FIRESTORE_DEBUG", "Error checking if lawyer is busy", e)
            false
        }
    }

    suspend fun createConsultation(consultation: Consultation) {
        val uid = consultation.clientId.ifBlank { consultation.userId }
        val docId = if (consultation.consultationId.isNotBlank()) {
            consultation.consultationId
        } else {
            db.collection("consultations").document().id
        }
        val finalConsultation = consultation.copy(
            consultationId = docId,
            clientId = uid,
            userId = uid,
            bookingId = "",
            userPhone = consultation.userPhone.ifBlank { consultation.contactNumber },
            userEmail = consultation.userEmail,
            language = consultation.language.ifBlank { consultation.preferredLanguage },
            additionalNotes = consultation.additionalNotes.ifBlank { consultation.notes },
            createdAt = com.google.firebase.Timestamp.now(),
            updatedAt = com.google.firebase.Timestamp.now()
        )

        // Save to root consultations collection
        db.collection("consultations").document(docId).set(finalConsultation).await()

        // Save to client's subcollection
        if (uid.isNotBlank()) {
            db.collection("users").document(uid)
                .collection("consultations").document(docId).set(finalConsultation).await()
        }

        // Save to lawyer's subcollection
        if (finalConsultation.lawyerId.isNotBlank() && finalConsultation.lawyerId != uid) {
            db.collection("users").document(finalConsultation.lawyerId)
                .collection("consultations").document(docId).set(finalConsultation).await()
        }
    }

    fun getUserConsultations(userId: String): Flow<List<Consultation>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val query = db.collection("consultations")
            .where(
                Filter.or(
                    Filter.equalTo("userId", userId),
                    Filter.equalTo("clientId", userId)
                )
            )
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("MY_BOOKINGS", "Firestore booking query failed", error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                Log.d("MY_BOOKINGS", "Firestore query started")
                Log.d("MY_BOOKINGS", "Current user ID = $userId")
                Log.d("MY_BOOKINGS", "Total bookings received = ${snapshot.size()}")

                val list = mutableListOf<Consultation>()
                for (document in snapshot.documents) {
                    try {
                        Log.d("MY_BOOKINGS", "Booking loaded: id=${document.id}, data=${document.data}")
                        var booking = document.toObject(Consultation::class.java)
                        if (booking != null) {
                            if (booking.consultationId.isBlank()) {
                                booking = booking.copy(consultationId = document.id)
                            }
                            list.add(booking)
                        }
                    } catch (e: Exception) {
                        Log.e("MY_BOOKINGS", "Firestore booking query failed", e)
                    }
                }
                val sortedList = list.sortedByDescending { it.createdAt }
                trySend(sortedList)
            }
        }
        awaitClose { listener.remove() }
    }

    fun getLawyerConsultations(lawyerId: String): Flow<List<Consultation>> = callbackFlow {
        if (lawyerId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        Log.d("CONSULTATION_SYNC", "CONSULTATION_SYNC: Listener attached for lawyerId = $lawyerId")
        val listener = db.collection("consultations")
            .whereEqualTo("lawyerId", lawyerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("MY_BOOKINGS", "MY BOOKINGS CRASH / ERROR", error)
                    Log.e("FIRESTORE_DEBUG", "Error listening to lawyer consultations", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    Log.d("CONSULTATION_SYNC", "CONSULTATION_SYNC: Firestore snapshot received")
                    Log.d("CONSULTATION_SYNC", "CONSULTATION_SYNC: Total consultations = ${snapshot.size()}")

                    val list = mutableListOf<Consultation>()
                    val docIds = mutableListOf<String>()
                    
                    for (document in snapshot.documents) {
                        try {
                            var booking = document.toObject(Consultation::class.java)
                            if (booking != null) {
                                if (booking.consultationId.isBlank()) {
                                    booking = booking.copy(consultationId = document.id)
                                }
                                docIds.add(booking.consultationId)
                                Log.d(
                                    "CONSULTATION_SYNC",
                                    "consultationId=${booking.consultationId}, " +
                                    "userId=${booking.userId}, " +
                                    "lawyerId=${booking.lawyerId}, " +
                                    "status=${booking.status}, " +
                                    "appointmentDate=${booking.date}, " +
                                    "appointmentTime=${booking.time}"
                                )
                                list.add(booking)
                            }
                        } catch (e: Exception) {
                            Log.e("MY_BOOKINGS", "MY BOOKINGS CRASH / deserialization error for document ${document.id}", e)
                        }
                    }
                    val sortedList = list.sortedByDescending { it.createdAt }
                    val pendingCount = sortedList.count { it.status.uppercase() == "PENDING" }
                    Log.d("CONSULTATION_SYNC", "CONSULTATION_SYNC: Pending consultations = $pendingCount")
                    trySend(sortedList)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun updateConsultationStatus(id: String, status: String, currentUid: String? = null) {
        val uid = currentUid ?: FirebaseAuth.getInstance().currentUser?.uid ?: ""
        try {
            val docRef = db.collection("consultations").document(id)
            
            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                if (snapshot.exists()) {
                    val existingStatus = snapshot.getString("status") ?: "PENDING"
                    if (existingStatus.uppercase() == "EXPIRED") {
                        return@runTransaction
                    }

                    val updateMap = mutableMapOf<String, Any>(
                        "status" to status,
                        "updatedAt" to com.google.firebase.Timestamp.now()
                    )

                    if (status.equals("ACCEPTED", ignoreCase = true)) {
                        val existingBookingId = snapshot.getString("bookingId")
                        val bookingId = if (!existingBookingId.isNullOrBlank()) {
                            existingBookingId
                        } else {
                            "NYA-2026-${java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8).uppercase()}"
                        }
                        updateMap["bookingId"] = bookingId
                        updateMap["acceptedAt"] = com.google.firebase.Timestamp.now()
                    } else if (status.equals("REJECTED", ignoreCase = true)) {
                        updateMap["rejectedAt"] = com.google.firebase.Timestamp.now()
                    }

                    transaction.update(docRef, updateMap)
                }
            }.await()

            // Update subcollections if they exist
            val rootDoc = db.collection("consultations").document(id).get().await()
            val consultation = rootDoc.toObject(Consultation::class.java)

            if (consultation != null) {
                val updateMap = mutableMapOf<String, Any>(
                    "status" to consultation.status,
                    "updatedAt" to (consultation.updatedAt ?: com.google.firebase.Timestamp.now())
                )
                if (consultation.bookingId.isNotBlank()) {
                    updateMap["bookingId"] = consultation.bookingId
                }
                if (consultation.acceptedAt != null) {
                    updateMap["acceptedAt"] = consultation.acceptedAt
                }
                if (consultation.rejectedAt != null) {
                    updateMap["rejectedAt"] = consultation.rejectedAt
                }

                val client = consultation.clientId.ifBlank { consultation.userId }
                if (client.isNotBlank()) {
                    val clientDoc = db.collection("users").document(client)
                        .collection("consultations").document(id)
                    if (clientDoc.get().await().exists()) {
                        clientDoc.update(updateMap).await()
                    }
                }
                if (consultation.lawyerId.isNotBlank()) {
                    val lawyerDoc = db.collection("users").document(consultation.lawyerId)
                        .collection("consultations").document(id)
                    if (lawyerDoc.get().await().exists()) {
                        lawyerDoc.update(updateMap).await()
                    }
                }
            } else if (uid.isNotBlank()) {
                val updateMap = mapOf(
                    "status" to status,
                    "updatedAt" to com.google.firebase.Timestamp.now()
                )
                val fallbackDoc = db.collection("users").document(uid)
                    .collection("consultations").document(id)
                if (fallbackDoc.get().await().exists()) {
                    fallbackDoc.update(updateMap).await()
                }
            }
        } catch (e: Exception) {
            Log.e("FIRESTORE_DEBUG", "Error updating consultation status", e)
        }
    }

    // ==========================================
    // 6. Bookmarks (users/{uid}/bookmarks)
    // ==========================================

    suspend fun saveBookmark(uid: String, bookmark: Bookmark) {
        if (uid.isBlank()) return
        val docId = if (bookmark.bookmarkId.isEmpty()) {
            db.collection("users").document(uid).collection("bookmarks").document().id
        } else {
            bookmark.bookmarkId
        }
        val finalBookmark = bookmark.copy(bookmarkId = docId, userId = uid)
        db.collection("users").document(uid).collection("bookmarks").document(docId).set(finalBookmark).await()
    }

    fun getBookmarks(uid: String): Flow<List<Bookmark>> = callbackFlow {
        if (uid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = db.collection("users").document(uid).collection("bookmarks")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                snapshot?.let { trySend(it.toObjects(Bookmark::class.java)) }
            }
        awaitClose { listener.remove() }
    }

    suspend fun deleteBookmark(uid: String, bookmarkId: String) {
        if (uid.isBlank() || bookmarkId.isBlank()) return
        db.collection("users").document(uid).collection("bookmarks").document(bookmarkId).delete().await()
    }

    // ==========================================
    // 7. Documents (users/{uid}/documents)
    // ==========================================

    suspend fun saveDocument(uid: String, document: SavedDocument) {
        if (uid.isBlank()) return
        val docId = if (document.documentId.isEmpty()) {
            db.collection("users").document(uid).collection("documents").document().id
        } else {
            document.documentId
        }
        val finalDoc = document.copy(documentId = docId, userId = uid)
        db.collection("users").document(uid).collection("documents").document(docId).set(finalDoc).await()
    }

    fun getDocuments(uid: String): Flow<List<SavedDocument>> = callbackFlow {
        if (uid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = db.collection("users").document(uid).collection("documents")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                snapshot?.let { trySend(it.toObjects(SavedDocument::class.java)) }
            }
        awaitClose { listener.remove() }
    }

    suspend fun deleteDocument(uid: String, documentId: String) {
        if (uid.isBlank() || documentId.isBlank()) return
        db.collection("users").document(uid).collection("documents").document(documentId).delete().await()
    }

    // ==========================================
    // 8. Room Unified Chat History Cross-Device Sync (users/{uid}/chatSessions)
    // ==========================================

    suspend fun saveRoomChatSession(uid: String, session: ChatSession) {
        if (uid.isBlank()) return
        db.collection("users").document(uid)
            .collection("chatSessions").document(session.sessionId.toString()).set(session).await()
    }

    suspend fun deleteRoomChatSession(uid: String, sessionId: Long) {
        if (uid.isBlank()) return
        val sessionRef = db.collection("users").document(uid)
            .collection("chatSessions").document(sessionId.toString())
        val msgs = sessionRef.collection("messages").get().await()
        for (doc in msgs) {
            doc.reference.delete().await()
        }
        sessionRef.delete().await()
    }

    suspend fun renameRoomChatSession(uid: String, sessionId: Long, newTitle: String) {
        if (uid.isBlank()) return
        db.collection("users").document(uid)
            .collection("chatSessions").document(sessionId.toString())
            .update("title", newTitle, "updatedAt", System.currentTimeMillis()).await()
    }

    suspend fun pinRoomChatSession(uid: String, sessionId: Long, pinned: Boolean) {
        if (uid.isBlank()) return
        db.collection("users").document(uid)
            .collection("chatSessions").document(sessionId.toString())
            .update("isPinned", pinned).await()
    }

    suspend fun saveRoomChatMessage(uid: String, message: ChatHistoryMessage) {
        if (uid.isBlank()) return
        val sessionRef = db.collection("users").document(uid)
            .collection("chatSessions").document(message.sessionId.toString())
        sessionRef.collection("messages").document(message.messageId.toString()).set(message).await()
    }

    suspend fun getRoomChatSessionsList(uid: String): List<ChatSession> {
        if (uid.isBlank()) return emptyList()
        return try {
            val snapshot = db.collection("users").document(uid).collection("chatSessions")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .get().await()
            snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(ChatSession::class.java)
                } catch (e: Exception) {
                    try {
                        val title = doc.getString("title") ?: ""
                        val type = doc.getString("chatbotType") ?: "AI_ASSISTANT"
                        val createdAt = doc.getSafeLong("createdAt", System.currentTimeMillis())
                        val updatedAt = doc.getSafeLong("updatedAt", System.currentTimeMillis())
                        val isPinned = doc.getBoolean("isPinned") ?: false
                        val id = doc.getSafeLong("sessionId", doc.id.toLongOrNull() ?: 0L)
                        ChatSession(sessionId = id, title = title, chatbotType = type, createdAt = createdAt, updatedAt = updatedAt, isPinned = isPinned)
                    } catch (innerEx: Exception) {
                        Log.e("FIRESTORE_HISTORY", "Error parsing chat session document ${doc.id}", innerEx)
                        null
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FIRESTORE_DEBUG", "Error getting room chat sessions list for UID: $uid", e)
            emptyList()
        }
    }

    suspend fun getRoomChatMessagesList(uid: String, sessionId: Long): List<ChatHistoryMessage> {
        if (uid.isBlank()) return emptyList()
        return try {
            val snapshot = db.collection("users").document(uid)
                .collection("chatSessions").document(sessionId.toString())
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get().await()
            snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(ChatHistoryMessage::class.java)
                } catch (e: Exception) {
                    try {
                        val sender = doc.getString("sender") ?: "Bot"
                        val message = doc.getString("message") ?: ""
                        val timestamp = doc.getSafeLong("timestamp", System.currentTimeMillis())
                        val msgId = doc.getSafeLong("messageId", doc.id.toLongOrNull() ?: 0L)
                        val sessId = doc.getSafeLong("sessionId", sessionId)
                        ChatHistoryMessage(messageId = msgId, sessionId = sessId, sender = sender, message = message, timestamp = timestamp)
                    } catch (innerEx: Exception) {
                        Log.e("FIRESTORE_HISTORY", "Error parsing chat message document ${doc.id}", innerEx)
                        null
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FIRESTORE_DEBUG", "Error getting room chat messages list for UID: $uid", e)
            emptyList()
        }
    }

    fun getLawyerReviewsFlow(lawyerId: String): Flow<List<LawyerReview>> = callbackFlow {
        if (lawyerId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val query = db.collection("reviews")
            .whereEqualTo("lawyerId", lawyerId)
            
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("FIRESTORE_REVIEWS", "Error listening to reviews", error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = mutableListOf<LawyerReview>()
                for (doc in snapshot.documents) {
                    try {
                        val review = doc.toObject(LawyerReview::class.java)
                        if (review != null) {
                            val finalReview = if (review.reviewId.isBlank()) {
                                review.copy(reviewId = doc.id)
                            } else {
                                review
                            }
                            list.add(finalReview)
                        }
                    } catch (e: Exception) {
                        Log.e("FIRESTORE_REVIEWS", "Error deserializing review document", e)
                    }
                }
                val sorted = list.sortedByDescending { it.createdAt }
                trySend(sorted)
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun submitReview(review: LawyerReview) {
        val docId = review.reviewId.ifBlank { review.consultationId }
        if (docId.isBlank()) return
        
        db.collection("reviews").document(docId).set(review).await()
        
        val updates = mapOf(
            "hasReviewed" to true,
            "reviewId" to docId,
            "updatedAt" to com.google.firebase.Timestamp.now()
        )
        db.collection("consultations").document(review.consultationId).update(updates).await()
        
        try {
            val consultation = db.collection("consultations").document(review.consultationId).get().await()
            if (consultation.exists()) {
                val clientId = consultation.getString("clientId") ?: ""
                val userId = consultation.getString("userId") ?: ""
                val lawyerId = consultation.getString("lawyerId") ?: ""
                val cId = clientId.ifBlank { userId }
                if (cId.isNotBlank()) {
                    db.collection("users").document(cId).collection("consultations").document(review.consultationId).update(updates).await()
                }
                if (lawyerId.isNotBlank()) {
                    db.collection("users").document(lawyerId).collection("consultations").document(review.consultationId).update(updates).await()
                }
            }
        } catch (e: Exception) {
            Log.w("REVIEW_SUBMISSION", "Failed to update nested subcollections for reviews", e)
        }
    }
}

class StorageRepository(private val storage: FirebaseStorage = FirebaseStorage.getInstance()) {
    suspend fun uploadFile(path: String, uri: Uri): String {
        val ref = storage.reference.child(path)
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }
}
