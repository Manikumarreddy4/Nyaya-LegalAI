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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        Log.i("APP_DEBUG", "Firebase Initialized")
        enableEdgeToEdge()

        // 1. Initialize Firebase safely
        FirebaseManager.init(this)

        // 2. Setup repositories and database
        val database = AppDatabase.getDatabase(this)
        val sessionManager = SessionManager(this)
        val factory = ViewModelFactory(database, sessionManager, this)

        setContent {
            var isLoading by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    try {
                        val authUser = FirebaseAuth.getInstance().currentUser
                        if (authUser != null) {
                            val uid = authUser.uid
                            Log.d("MainActivity", "User authenticated with UID: $uid. Syncing data from Firestore...")
                            val firestoreRepo = FirestoreRepository()

                            // 1. Sync profile
                             val profile = firestoreRepo.getUserProfile(uid)
                             if (profile != null) {
                                 val role = if (profile.role.equals("CLIENT", ignoreCase = true) || profile.role.equals("user", ignoreCase = true) || profile.role.equals("USER", ignoreCase = true)) "USER" else profile.role.uppercase()
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
                                 val lawyerProfile = firestoreRepo.getLawyerProfile(uid)
                                 if (lawyerProfile != null) {
                                     sessionManager.saveUser(LocalUser(uid, lawyerProfile.email, lawyerProfile.name, "LAWYER", lawyerProfile.phone, lawyerProfile.barCouncilNumber))
                                 }
                             }

                            // 2. Sync settings
                            val settings = firestoreRepo.getUserSettings(uid)
                            if (settings != null) {
                                sessionManager.setDarkMode(settings.darkMode)
                                sessionManager.setThemeColor(settings.themeColor)
                                sessionManager.setFontColor(settings.fontColor)
                                settings.language?.let { sessionManager.setLanguage(it) }
                            }

                            // 3. Sync chat sessions and messages
                            val chatSessions = firestoreRepo.getRoomChatSessionsList(uid)
                            for (session in chatSessions) {
                                database.unifiedHistoryDao().insertSession(session)
                                val messages = firestoreRepo.getRoomChatMessagesList(uid, session.sessionId)
                                for (msg in messages) {
                                    database.unifiedHistoryDao().insertMessage(msg)
                                }
                            }

                            // 4. Sync learning history
                            val learningList = firestoreRepo.getLearningHistoryList(uid)
                            for (item in learningList) {
                                database.learningHistoryDao().insertHistory(item)
                            }
                            Log.d("MainActivity", "Startup Firestore sync completed for UID: $uid")
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error during background startup sync", e)
                    } finally {
                        isLoading = false
                    }
                }

                if (!NetworkUtils.isInternetAvailable(this@MainActivity)) {
                    Toast.makeText(this@MainActivity, "No internet connection. Showing offline legal data.", Toast.LENGTH_LONG).show()
                }
            }

            NyayaAITheme {
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
                        if (isLoading) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else {
                            val navController = rememberNavController()
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
    }
}
