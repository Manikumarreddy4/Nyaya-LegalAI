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
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

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
    }

    suspend fun getLawyerProfile(lawyerId: String): LawyerProfile? {
        if (lawyerId.isBlank()) return null
        return try {
            val doc = db.collection("users").document(lawyerId).get().await()
            if (doc.exists()) {
                doc.toObject(LawyerProfile::class.java)
            } else {
                val lawyerDoc = db.collection("lawyers").document(lawyerId).get().await()
                if (lawyerDoc.exists()) lawyerDoc.toObject(LawyerProfile::class.java) else null
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
                    Log.e("FIRESTORE_DEBUG", "Error listening to lawyer profile", error)
                    trySend(null)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    trySend(snapshot.toObject(LawyerProfile::class.java))
                } else {
                    // Fallback to check users collection
                    db.collection("users").document(lawyerId).get()
                        .addOnSuccessListener { userDoc ->
                            if (userDoc.exists()) {
                                trySend(userDoc.toObject(LawyerProfile::class.java))
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
            "onlineAvailable" to isAvailable,
            "updatedAt" to com.google.firebase.Timestamp.now()
        )
        try {
            db.collection("users").document(lawyerId).update(updates).await()
        } catch (e: Exception) {
            Log.e("FIRESTORE_DEBUG", "Failed to update users collection availability", e)
        }
        try {
            db.collection("lawyers").document(lawyerId).update(updates).await()
        } catch (e: Exception) {
            Log.e("FIRESTORE_DEBUG", "Failed to update lawyers collection availability", e)
        }
    }

    fun getAllLawyersFlow(): Flow<List<LawyerProfile>> = callbackFlow {
        val listener = db.collection("lawyers")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FIND_LAWYER", "Failed to load lawyers snapshot. Firestore listener error: ${error.message}", error)
                    // Do NOT close flow or send emptyList. Keep listener active.
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val rawList = snapshot.toObjects(LawyerProfile::class.java)
                    Log.d("FIND_LAWYER", "Collection name: lawyers")
                    Log.d("FIND_LAWYER", "Number of documents loaded: ${rawList.size}")
                    
                    val filteredList = rawList.filter { profile ->
                        val roleLower = profile.role.lowercase().trim()
                        val isLawyer = roleLower == "lawyer" || roleLower == "advocate"
                        val resolvedName = profile.displayNameString.trim()
                        
                        if (isLawyer && resolvedName.isNotBlank() && !resolvedName.equals("Advocate", ignoreCase = true)) {
                            Log.d("FIND_LAWYER", "Loaded lawyer: uid=${profile.userId.ifBlank { profile.lawyerId }}, name=$resolvedName, role=${profile.role}, specialization=${profile.specialization}, location=${profile.displayLocation}")
                            true
                        } else {
                            Log.w("FIND_LAWYER", "Skipping non-lawyer or incomplete profile: uid=${profile.userId.ifBlank { profile.lawyerId }}, name=${profile.name}, role=${profile.role}")
                            false
                        }
                    }
                    trySend(filteredList)
                } else {
                    trySend(emptyList())
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun getAllLawyers(): List<LawyerProfile> {
        return try {
            val usersList = db.collection("users")
                .whereIn("role", listOf("lawyer", "LAWYER"))
                .get()
                .await()
                .toObjects(LawyerProfile::class.java)

            val rawList = if (usersList.isNotEmpty()) {
                usersList
            } else {
                db.collection("lawyers")
                    .get()
                    .await()
                    .toObjects(LawyerProfile::class.java)
            }
            Log.d("FIND_LAWYER", "Collection name: lawyers (fallback/suspend)")
            Log.d("FIND_LAWYER", "Number of documents loaded: ${rawList.size}")
            
            rawList.filter { profile ->
                val roleLower = profile.role.lowercase().trim()
                val isLawyer = roleLower == "lawyer" || roleLower == "advocate"
                val resolvedName = profile.displayNameString.trim()
                
                if (isLawyer && resolvedName.isNotBlank() && !resolvedName.equals("Advocate", ignoreCase = true)) {
                    Log.d("FIND_LAWYER", "Loaded lawyer: uid=${profile.userId.ifBlank { profile.lawyerId }}, name=$resolvedName, role=${profile.role}, specialization=${profile.specialization}, location=${profile.displayLocation}")
                    true
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            Log.e("FIRESTORE_DEBUG", "Error fetching all lawyers", e)
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
                    val question = doc.getString("question") ?: ""
                    val answer = doc.getString("answer") ?: ""
                    val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    val id = doc.getLong("id")?.toInt() ?: doc.id.toIntOrNull() ?: 0
                    LearningHistory(id = id, question = question, answer = answer, timestamp = timestamp)
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
                snapshot?.let { trySend(it.toObjects(LearningHistory::class.java)) }
            }
        awaitClose { listener.remove() }
    }

    suspend fun deleteLearningHistoryItem(uid: String, docId: String) {
        if (uid.isBlank() || docId.isBlank()) return
        db.collection("users").document(uid).collection("learningHistory").document(docId).delete().await()
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
            .collection("problemHistory")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val sessions = snapshot.toObjects(FirestoreChatSession::class.java)
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

    // ==========================================
    // 5. Consultations (users/{uid}/consultations)
    // ==========================================

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
                Log.e("FIRESTORE_DEBUG", "Error listening to user consultations", error)
                trySend(emptyList())
                return@addSnapshotListener
            }
            if (snapshot != null) {
                Log.d("BOOKING_SYNC", "Logged in UID: $userId")
                Log.d("BOOKING_SYNC", "Collection being queried: consultations")
                Log.d("BOOKING_SYNC", "Number of bookings found: ${snapshot.size()}")

                val list = mutableListOf<Consultation>()
                for (document in snapshot.documents) {
                    var booking = document.toObject(Consultation::class.java)
                    if (booking != null) {
                        if (booking.consultationId.isBlank()) {
                            booking = booking.copy(consultationId = document.id)
                        }
                        Log.d(
                            "BOOKING_SYNC",
                            "Document=${document.id}, " +
                            "userId=${booking.userId}, " +
                            "clientId=${booking.clientId}, " +
                            "lawyerId=${booking.lawyerId}, " +
                            "status=${booking.status}"
                        )
                        list.add(booking)
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
        val listener = db.collection("consultations")
            .whereEqualTo("lawyerId", lawyerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FIRESTORE_DEBUG", "Error listening to lawyer consultations", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    Log.d("BOOKING_SYNC", "Logged in Lawyer UID: $lawyerId")
                    Log.d("BOOKING_SYNC", "Collection being queried: consultations")
                    Log.d("BOOKING_SYNC", "Number of bookings found: ${snapshot.size()}")

                    val list = mutableListOf<Consultation>()
                    for (document in snapshot.documents) {
                        var booking = document.toObject(Consultation::class.java)
                        if (booking != null) {
                            if (booking.consultationId.isBlank()) {
                                booking = booking.copy(consultationId = document.id)
                            }
                            Log.d(
                                "BOOKING_SYNC",
                                "Document=${document.id}, " +
                                "userId=${booking.userId}, " +
                                "clientId=${booking.clientId}, " +
                                "lawyerId=${booking.lawyerId}, " +
                                "status=${booking.status}"
                            )
                            list.add(booking)
                        }
                    }
                    val sortedList = list.sortedByDescending { it.createdAt }
                    trySend(sortedList)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun updateConsultationStatus(id: String, status: String, currentUid: String? = null) {
        val uid = currentUid ?: FirebaseAuth.getInstance().currentUser?.uid ?: ""
        try {
            val updateMap = mapOf(
                "status" to status,
                "updatedAt" to com.google.firebase.Timestamp.now()
            )

            // Update root collection
            db.collection("consultations").document(id).update(updateMap).await()

            // Update client/lawyer subcollections if they exist
            val rootDoc = db.collection("consultations").document(id).get().await()
            val consultation = rootDoc.toObject(Consultation::class.java)

            if (consultation != null) {
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
                    val title = doc.getString("title") ?: ""
                    val type = doc.getString("chatbotType") ?: "AI_ASSISTANT"
                    val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    val updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                    val isPinned = doc.getBoolean("isPinned") ?: false
                    val id = doc.getLong("sessionId") ?: doc.id.toLongOrNull() ?: 0L
                    ChatSession(sessionId = id, title = title, chatbotType = type, createdAt = createdAt, updatedAt = updatedAt, isPinned = isPinned)
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
                    val sender = doc.getString("sender") ?: "Bot"
                    val message = doc.getString("message") ?: ""
                    val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    val msgId = doc.getLong("messageId") ?: doc.id.toLongOrNull() ?: 0L
                    val sessId = doc.getLong("sessionId") ?: sessionId
                    ChatHistoryMessage(messageId = msgId, sessionId = sessId, sender = sender, message = message, timestamp = timestamp)
                }
            }
        } catch (e: Exception) {
            Log.e("FIRESTORE_DEBUG", "Error getting room chat messages list for UID: $uid", e)
            emptyList()
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
