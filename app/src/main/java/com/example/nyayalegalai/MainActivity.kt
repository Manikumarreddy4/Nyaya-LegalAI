package com.example.nyayalegalai

import com.google.firebase.FirebaseApp
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.nyayalegalai.database.AppDatabase
import com.example.nyayalegalai.repository.FirebaseManager
import com.example.nyayalegalai.repository.FirestoreRepository
import com.example.nyayalegalai.ui.navigation.NyayaNavGraph
import com.example.nyayalegalai.ui.theme.NyayaAITheme
import com.example.nyayalegalai.utils.LocaleHelper
import com.example.nyayalegalai.utils.LocalUser
import com.example.nyayalegalai.utils.NetworkUtils
import com.example.nyayalegalai.utils.SessionManager
import com.example.nyayalegalai.viewmodel.*
import com.google.firebase.auth.FirebaseAuth
import androidx.lifecycle.lifecycleScope
import com.example.nyayalegalai.repository.ChatHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import com.example.nyayalegalai.database.LearningHistory
import com.example.nyayalegalai.database.ChatSession
import com.google.firebase.firestore.DocumentSnapshot

fun DocumentSnapshot.getSafeId(fieldName: String = "id"): String {
    return try {
        when (val value = get(fieldName)) {
            null -> this.id
            is String -> if (value.isBlank()) this.id else value
            is Number -> value.toString()
            else -> value.toString().ifBlank { this.id }
        }
    } catch (e: Exception) {
        Log.e("FIRESTORE_HISTORY", "Error parsing ID for field $fieldName in doc $id", e)
        this.id
    }
}

fun DocumentSnapshot.getSafeLong(fieldName: String, defaultValue: Long = 0L): Long {
    return try {
        val value = get(fieldName)
        when (value) {
            null -> defaultValue
            is com.google.firebase.Timestamp -> value.toDate().time
            is Number -> value.toLong()
            is String -> {
                val directLong = value.toLongOrNull()
                if (directLong != null) {
                    directLong
                } else {
                    try {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            java.time.Instant.parse(value).toEpochMilli()
                        } else {
                            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
                            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                            sdf.parse(value)?.time ?: defaultValue
                        }
                    } catch (parseEx: Exception) {
                        defaultValue
                    }
                }
            }
            else -> defaultValue
        }
    } catch (e: Exception) {
        Log.e("FIRESTORE_HISTORY", "Invalid timestamp/Long in document $id for field $fieldName", e)
        defaultValue
    }
}

fun DocumentSnapshot.toRoomChatSession(): ChatSession? {
    if (!exists()) return null
    return try {
        val title = getString("title") ?: ""
        val type = getString("chatbotType") ?: "AI_ASSISTANT"
        if (type == "ENCYCLOPEDIA") return null
        val createdAt = getSafeLong("createdAt", System.currentTimeMillis())
        val updatedAt = getSafeLong("updatedAt", System.currentTimeMillis())
        val isPinned = getBoolean("isPinned") ?: false
        
        val sessionIdVal = get("sessionId")
        val sessionId = when (sessionIdVal) {
            is Number -> sessionIdVal.toLong()
            is String -> sessionIdVal.toLongOrNull() ?: 0L
            else -> id.toLongOrNull() ?: 0L
        }
        
        ChatSession(
            sessionId = sessionId,
            title = title,
            chatbotType = type,
            createdAt = createdAt,
            updatedAt = updatedAt,
            isPinned = isPinned
        )
    } catch (e: Exception) {
        Log.e("MainActivity", "Error parsing Room ChatSession", e)
        null
    }
}

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onStart() {
        super.onStart()
        android.util.Log.d("APP_CRASH_TRACE", "MainActivity onStart")
        android.util.Log.i("APP_LIFECYCLE", "MainActivity onStart")
    }

    override fun onResume() {
        super.onResume()
        android.util.Log.d("APP_CRASH_TRACE", "MainActivity onResume")
        android.util.Log.i("APP_LIFECYCLE", "MainActivity onResume")
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (!uid.isNullOrBlank()) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val database = AppDatabase.getDatabase(this@MainActivity)
                    val sessionManager = SessionManager(this@MainActivity)
                    val firestoreRepo = FirestoreRepository()
                    val repo = ChatHistoryRepository(database.unifiedHistoryDao(), sessionManager, firestoreRepo, database.learningHistoryDao())
                    repo.refreshHistoryFromServer(uid)
                } catch (e: Exception) {
                    Log.e("SYNC_DEBUG", "onResume history refresh failed", e)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Step 8: Register global uncaught exception handler
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("APP_FATAL", "UNCAUGHT EXCEPTION on thread: ${thread.name}", throwable)
            android.util.Log.e("APP_CRASH_TRACE", "UNCAUGHT EXCEPTION on thread: ${thread.name}", throwable)
            android.util.Log.e("APP_CRASH", "Uncaught exception", throwable)
            android.util.Log.e("APP_CRASH", "Exception class: ${throwable::class.java.name}")
            Log.e("APP_CRASH", "Message: ${throwable.message}")
            Log.e("APP_CRASH", "Full stack trace:\n${Log.getStackTraceString(throwable)}")
            
            defaultHandler?.uncaughtException(thread, throwable)
        }

        super.onCreate(savedInstanceState)
        android.util.Log.d("APP_CRASH_TRACE", "Application started")
        android.util.Log.d("APP_CRASH_TRACE", "MainActivity onCreate")
        android.util.Log.d("APP_START", "MainActivity created")
        android.util.Log.i("APP_LIFECYCLE", "MainActivity onCreate")
        android.util.Log.i("NYAYA_CRASH_DEBUG", "MainActivity onCreate - App startup initialized")
        Log.d("NYAYA_STARTUP", "NYAYA_STARTUP: Application started")
        Log.d("NYAYA_STARTUP", "NYAYA_STARTUP: MainActivity created")
        
        try {
            android.util.Log.i("NYAYA_CRASH_DEBUG", "MainActivity onCreate - Firebase init starting")
            Log.d("NYAYA_STARTUP", "NYAYA_STARTUP: Firebase initialization started")
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
                Log.i("APP_DEBUG", "Firebase Initialized in onCreate")
            }
            FirebaseManager.init(this)
            android.util.Log.d("APP_START", "Firebase initialized")
            android.util.Log.i("NYAYA_CRASH_DEBUG", "MainActivity onCreate - Firebase init finished")
            Log.d("NYAYA_STARTUP", "NYAYA_STARTUP: Firebase initialized successfully")
        } catch (e: Exception) {
            android.util.Log.e("NYAYA_CRASH_DEBUG", "MainActivity onCreate - Firebase init failed", e)
            Log.e("NYAYA_STARTUP", "NYAYA_STARTUP ERROR: Firebase init failed = ${e.message}", e)
        }
        
        enableEdgeToEdge()

        val database: AppDatabase
        val sessionManager: SessionManager
        val factory: ViewModelFactory

        try {
            android.util.Log.i("NYAYA_CRASH_DEBUG", "MainActivity onCreate - DB/Factory creation starting")
            database = AppDatabase.getDatabase(this)
            sessionManager = SessionManager(this)
            factory = ViewModelFactory(database, sessionManager, this)
            android.util.Log.i("NYAYA_CRASH_DEBUG", "MainActivity onCreate - DB/Factory creation finished")
        } catch (e: Exception) {
            android.util.Log.e("NYAYA_CRASH_DEBUG", "MainActivity onCreate - DB/Factory initialization failed", e)
            Log.e("NYAYA_STARTUP", "NYAYA_STARTUP ERROR: DB/Factory initialization failed = ${e.message}", e)
            throw e
        }

        setContent {
            android.util.Log.d("APP_CRASH_TRACE", "Compose started")
            val scope = rememberCoroutineScope()
            var currentUid by remember {
                val uid = try {
                    android.util.Log.d("APP_CRASH_TRACE", "Checking Firebase user")
                    Log.d("NYAYA_STARTUP", "NYAYA_STARTUP: Checking authentication")
                    val firebaseAuth = FirebaseAuth.getInstance()
                    val u = firebaseAuth.currentUser?.uid
                    if (u != null) {
                        android.util.Log.d("APP_CRASH_TRACE", "Firebase UID = $u")
                        android.util.Log.d("APP_START", "Current Firebase user = $u")
                    } else {
                        android.util.Log.d("APP_START", "Current Firebase user = Null")
                    }
                    android.util.Log.i("NYAYA_CRASH_DEBUG", "MainActivity: Initial auth state loaded currentUid=${u ?: "Null"}")
                    Log.d("NYAYA_STARTUP", "NYAYA_STARTUP: Current user = ${u ?: "Null"}")
                    u
                } catch (e: Exception) {
                    android.util.Log.e("NYAYA_CRASH_DEBUG", "MainActivity: Initial auth check failed", e)
                    Log.e("NYAYA_STARTUP", "NYAYA_STARTUP ERROR: Auth check failed = ${e.message}", e)
                    null
                }
                mutableStateOf(uid)
            }

            DisposableEffect(Unit) {
                val authStateListener = FirebaseAuth.AuthStateListener { auth ->
                    val newUid = auth.currentUser?.uid
                    android.util.Log.i("NYAYA_CRASH_DEBUG", "MainActivity: authStateListener triggered - newUid=${newUid ?: "Null"}")
                    if (newUid != currentUid) {
                        currentUid = newUid
                    }
                }
                try {
                    FirebaseAuth.getInstance().addAuthStateListener(authStateListener)
                } catch (e: Exception) {
                    Log.e("NYAYA_STARTUP", "NYAYA_STARTUP ERROR: Adding authStateListener failed = ${e.message}", e)
                }
                onDispose {
                    try {
                        FirebaseAuth.getInstance().removeAuthStateListener(authStateListener)
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error removing authStateListener", e)
                    }
                }
            }

            LaunchedEffect(currentUid) {
                val uid = currentUid
                android.util.Log.i("NYAYA_CRASH_DEBUG", "MainActivity: LaunchedEffect(currentUid) triggered for UID=${uid ?: "Null"}")
                if (uid != null && uid.isNotEmpty()) {
                    launch(Dispatchers.IO) {
                        try {
                            Log.d("MainActivity", "Initial data cleanup and profile sync started for UID: $uid")
                            database.unifiedHistoryDao().deleteAllSessions()
                            database.unifiedHistoryDao().deleteAllMessages()
                            database.learningHistoryDao().clearHistory()

                            val firestoreRepo = FirestoreRepository()

                            // 1. Sync profile
                            android.util.Log.d("APP_CRASH_TRACE", "Loading user profile")
                            android.util.Log.d("APP_START", "Loading user profile")
                            val profile = firestoreRepo.getUserProfile(uid)
                            if (profile != null) {
                                android.util.Log.d("APP_CRASH_TRACE", "Loading user role")
                                android.util.Log.d("APP_START", "User profile loaded")
                                val role = if (profile.role.equals("CLIENT", ignoreCase = true) || profile.role.equals("user", ignoreCase = true) || profile.role.equals("USER", ignoreCase = true)) "USER" else profile.role.uppercase()
                                android.util.Log.d("APP_CRASH_TRACE", "User role = $role")
                                android.util.Log.d("APP_START", "User role = $role")
                                android.util.Log.i("NYAYA_CRASH_DEBUG", "MainActivity: Syncing user profile. name=${profile.name}, role=$role")
                                sessionManager.saveUser(LocalUser(uid, profile.email, profile.name, role, profile.phone))
                                database.userProfileDao().updateProfile(
                                    com.example.nyayalegalai.database.UserProfile(
                                        id = 1,
                                        name = profile.name,
                                        language = sessionManager.getLanguage() ?: "en",
                                        learningProgress = 0
                                    )
                                )
                            } else {
                                android.util.Log.w("NYAYA_CRASH_DEBUG", "MainActivity: User profile document not found in Firestore users/$uid")
                            }

                            // 2. Sync settings
                            val settings = firestoreRepo.getUserSettings(uid)
                            if (settings != null) {
                                sessionManager.setDarkMode(settings.darkMode)
                                sessionManager.setThemeColor(settings.themeColor)
                                sessionManager.setFontColor(settings.fontColor)
                                settings.language?.let { sessionManager.setLanguage(it) }
                            }
                            
                            // 3. Initial server fetch
                            val repo = ChatHistoryRepository(database.unifiedHistoryDao(), sessionManager, firestoreRepo, database.learningHistoryDao())
                            repo.refreshHistoryFromServer(uid)
                            
                            Log.d("MainActivity", "Initial profile/settings sync finished for UID: $uid")
                        } catch (e: Exception) {
                            Log.e("NYAYA_STARTUP", "NYAYA_STARTUP ERROR: Initial sync error = ${e.message}", e)
                        }
                    }
                }
            }

            DisposableEffect(currentUid) {
                val uid = currentUid
                var unsubChatSessions: com.google.firebase.firestore.ListenerRegistration? = null
                var unsubLearningHistory: com.google.firebase.firestore.ListenerRegistration? = null

                if (uid != null && uid.isNotEmpty()) {
                    try {
                        Log.d("NYAYA_STARTUP", "NYAYA_STARTUP: Firestore listener started")
                        val dbInstance = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        val firestoreRepo = FirestoreRepository()

                        unsubChatSessions = dbInstance.collection("users").document(uid).collection("chatSessions")
                            .addSnapshotListener { snapshot, error ->
                                if (error != null) {
                                    Log.e("MainActivity", "Error listening to chatSessions", error)
                                    return@addSnapshotListener
                                }
                                if (snapshot != null) {
                                    if (snapshot.metadata.isFromCache) {
                                        Log.d("ACTIVITY_SYNC", "Ignoring cached snapshot for chatSessions")
                                        return@addSnapshotListener
                                    }
                                    Log.d("ACTIVITY_SYNC", "ACTIVITY_SYNC: Firestore update received for chatSessions")
                                    scope.launch(Dispatchers.IO) {
                                        try {
                                            val firestoreSessionIds = mutableSetOf<Long>()
                                            for (doc in snapshot.documents) {
                                                val session = doc.toRoomChatSession()
                                                if (session != null) {
                                                    firestoreSessionIds.add(session.sessionId)
                                                    database.unifiedHistoryDao().insertSession(session)
                                                    
                                                    val messages = firestoreRepo.getRoomChatMessagesList(uid, session.sessionId)
                                                    for (msg in messages) {
                                                        database.unifiedHistoryDao().insertMessage(msg)
                                                    }
                                                }
                                            }
                                            
                                            val localSessions = database.unifiedHistoryDao().getAllSessionsList()
                                            for (localSess in localSessions) {
                                                if (localSess.sessionId !in firestoreSessionIds) {
                                                    database.unifiedHistoryDao().deleteSession(localSess)
                                                    database.unifiedHistoryDao().deleteMessagesForSession(localSess.sessionId)
                                                    
                                                    // ALSO delete corresponding LearningHistory item from Room!
                                                    if (localSess.chatbotType == "LEGAL_LEARNING") {
                                                        val firstMsg = database.unifiedHistoryDao().getMessagesForSessionList(localSess.sessionId)
                                                            .firstOrNull { it.sender == "User" }
                                                        if (firstMsg != null) {
                                                            val queryText = firstMsg.message.trim()
                                                            val matchingItems = database.learningHistoryDao().getAllHistoryList()
                                                                .filter { it.question.trim().equals(queryText, ignoreCase = true) }
                                                            for (item in matchingItems) {
                                                                database.learningHistoryDao().deleteHistory(item)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        } catch (e: Exception) {
                                            Log.e("NYAYA_STARTUP", "NYAYA_STARTUP ERROR: error in chatSessions snapshot listener = ${e.message}", e)
                                        }
                                    }
                                }
                            }

                        unsubLearningHistory = dbInstance.collection("users").document(uid).collection("learningHistory")
                            .addSnapshotListener { snapshot, error ->
                                if (error != null) {
                                    Log.e("MainActivity", "Error listening to learningHistory", error)
                                    return@addSnapshotListener
                                }
                                if (snapshot != null) {
                                    if (snapshot.metadata.isFromCache) {
                                        Log.d("ACTIVITY_SYNC", "Ignoring cached snapshot for learningHistory")
                                        return@addSnapshotListener
                                    }
                                    Log.d("ACTIVITY_SYNC", "ACTIVITY_SYNC: Firestore update received for learningHistory")
                                    scope.launch(Dispatchers.IO) {
                                        try {
                                            val firestoreHistoryIds = mutableSetOf<String>()
                                            for (doc in snapshot.documents) {
                                                try {
                                                    val question = doc.getString("question") ?: doc.getString("query") ?: ""
                                                    val answer = doc.getString("answer") ?: doc.getString("explanation") ?: ""
                                                    val timestamp = doc.getSafeLong("timestamp", System.currentTimeMillis())
                                                    val id = doc.getSafeId("id")
                                                    
                                                    val item = LearningHistory(id = id, question = question, answer = answer, timestamp = timestamp)
                                                    firestoreHistoryIds.add(id)
                                                    database.learningHistoryDao().insertHistory(item)
                                                } catch (innerEx: Exception) {
                                                    Log.e("FIRESTORE_HISTORY", "Error parsing learning history document ${doc.id}", innerEx)
                                                }
                                            }
                                            
                                            val localHistory = database.learningHistoryDao().getAllHistoryList()
                                            for (localItem in localHistory) {
                                                if (localItem.id !in firestoreHistoryIds) {
                                                    database.learningHistoryDao().deleteHistory(localItem)
                                                    
                                                    // ALSO delete corresponding ChatSession from Room!
                                                    val queryText = localItem.question.trim()
                                                    val localSessions = database.unifiedHistoryDao().getAllSessionsList()
                                                    for (session in localSessions) {
                                                        if (session.chatbotType == "LEGAL_LEARNING") {
                                                            val messages = database.unifiedHistoryDao().getMessagesForSessionList(session.sessionId)
                                                            val firstMsg = messages.firstOrNull { it.sender == "User" }
                                                            if (firstMsg != null && firstMsg.message.trim().equals(queryText, ignoreCase = true)) {
                                                                database.unifiedHistoryDao().deleteSession(session)
                                                                database.unifiedHistoryDao().deleteMessagesForSession(session.sessionId)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        } catch (e: Exception) {
                                            Log.e("NYAYA_STARTUP", "NYAYA_STARTUP ERROR: error in learningHistory snapshot listener = ${e.message}", e)
                                        }
                                    }
                                }
                            }
                    } catch (e: Exception) {
                        Log.e("NYAYA_STARTUP", "NYAYA_STARTUP ERROR: registering firestore snapshot listeners failed = ${e.message}", e)
                    }
                }

                onDispose {
                    unsubChatSessions?.remove()
                    unsubLearningHistory?.remove()
                    Log.d("MainActivity", "Real-time sync listeners disposed")
                }
            }

            LaunchedEffect(Unit) {
                if (!NetworkUtils.isInternetAvailable(this@MainActivity)) {
                    Toast.makeText(this@MainActivity, "Offline mode. Showing local data.", Toast.LENGTH_SHORT).show()
                }
            }

            val themeViewModel: ThemeViewModel = viewModel(factory = factory)
            val isDarkMode by themeViewModel.isDarkMode
            val themeColorName by themeViewModel.themeColor
            val fontColorName by themeViewModel.fontColor

            NyayaAITheme(
                darkTheme = isDarkMode,
                themeColorName = themeColorName,
                fontColorName = fontColorName,
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    android.util.Log.d("APP_START", "Navigation graph created")
                    Log.d("NYAYA_STARTUP", "NYAYA_STARTUP: Navigation initialized")
                    
                    // Create ViewModels. They will be shared using the same factory.
                    // Accessing them here makes them available to the NavGraph.
                    val chatViewModel: ChatViewModel = viewModel(factory = factory)
                    val lawyerViewModel: LawyerViewModel = viewModel(factory = factory)
                    val learningViewModel: LegalLearningViewModel = viewModel(factory = factory)
                    val lawViewModel: LawViewModel = viewModel(factory = factory)
                    val loginViewModel: LoginViewModel = viewModel(factory = factory)
                    val signupViewModel: SignupViewModel = viewModel(factory = factory)
                    val authViewModel: AuthViewModel = viewModel(factory = factory)
                    val consultationViewModel: ConsultationViewModel = viewModel(factory = factory)
                    val profileViewModel: ProfileViewModel = viewModel(factory = factory)
                    val historyViewModel: HistoryViewModel = viewModel(factory = factory)

                    NyayaNavGraph(
                        navController = navController, 
                        chatViewModel = chatViewModel,
                        lawyerViewModel = lawyerViewModel,
                        learningViewModel = learningViewModel,
                        lawViewModel = lawViewModel,
                        loginViewModel = loginViewModel,
                        signupViewModel = signupViewModel,
                        authViewModel = authViewModel,
                        consultationViewModel = consultationViewModel,
                        profileViewModel = profileViewModel,
                        themeViewModel = themeViewModel,
                        historyViewModel = historyViewModel
                    )
                }
            }
        }
    }
}
