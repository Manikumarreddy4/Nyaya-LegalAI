package com.example.nyayalegalai.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nyayalegalai.models.LawyerProfile
import com.example.nyayalegalai.repository.FirestoreRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class LawyerViewModel(private val firestoreRepo: FirestoreRepository) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedSpecialization = MutableStateFlow("All")
    val selectedSpecialization: StateFlow<String> = _selectedSpecialization.asStateFlow()
    val selectedFilter: StateFlow<String> = _selectedSpecialization.asStateFlow()

    private val _selectedExperience = MutableStateFlow("All")
    val selectedExperience: StateFlow<String> = _selectedExperience.asStateFlow()

    private val _selectedFeeRange = MutableStateFlow("All")
    val selectedFeeRange: StateFlow<String> = _selectedFeeRange.asStateFlow()

    private val _onlineOnly = MutableStateFlow(false)
    val onlineOnly: StateFlow<Boolean> = _onlineOnly.asStateFlow()

    private val _inPersonOnly = MutableStateFlow(false)
    val inPersonOnly: StateFlow<Boolean> = _inPersonOnly.asStateFlow()

    private val _selectedSort = MutableStateFlow("Recommended")
    val selectedSort: StateFlow<String> = _selectedSort.asStateFlow()

    private val _allLawyers = MutableStateFlow<List<LawyerProfile>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var collectJob: kotlinx.coroutines.Job? = null

    private val authStateListener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { firebaseAuth ->
        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser == null) {
            collectJob?.cancel()
            collectJob = null
            _allLawyers.value = emptyList()
            _isLoading.value = false
            _error.value = null
            Log.d("FIND_LAWYERS", "FIND_LAWYERS: User logged out, cleared state and listener")
        } else {
            Log.d("FIND_LAWYERS", "FIND_LAWYERS: User logged in, loading lawyers")
            loadLawyers(force = true)
        }
    }

    init {
        com.google.firebase.auth.FirebaseAuth.getInstance().addAuthStateListener(authStateListener)
    }

    override fun onCleared() {
        super.onCleared()
        try {
            com.google.firebase.auth.FirebaseAuth.getInstance().removeAuthStateListener(authStateListener)
        } catch (e: Exception) {
            Log.e("LawyerViewModel", "Error removing authStateListener onCleared", e)
        }
        collectJob?.cancel()
    }

    private fun updateAllLawyers(newList: List<LawyerProfile>) {
        _allLawyers.value = newList
    }

    fun loadLawyers(force: Boolean = false) {
        if (!force && collectJob?.isActive == true) {
            Log.d("FIND_LAWYERS", "FIND_LAWYERS: Collection is already active, skipping reload")
            return
        }
        collectJob?.cancel()
        _isLoading.value = true
        _error.value = null
        
        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
        Log.d("FIND_LAWYERS", "FIND_LAWYERS: Screen opened")
        Log.d("FIND_LAWYERS", "FIND_LAWYERS: Current user ID = $currentUserId")
        Log.d("FIND_LAWYERS", "FIND_LAWYERS: Fetching lawyers")
        
        collectJob = viewModelScope.launch {
            try {
                val firstList = withTimeout(6000) {
                    firestoreRepo.getAllLawyersFlow()
                        .first()
                }
                
                updateAllLawyers(firstList)
                _isLoading.value = false
                Log.d("FIND_LAWYERS", "FIND_LAWYERS: UI state = ${if (firstList.isEmpty()) "Empty" else "Success"}")
            } catch (te: kotlinx.coroutines.TimeoutCancellationException) {
                Log.e("FIND_LAWYERS", "FIND_LAWYERS ERROR: Firestore query timed out", te)
                _error.value = "Unable to load lawyers. Request timed out."
                _isLoading.value = false
                Log.d("FIND_LAWYERS", "FIND_LAWYERS: UI state = Error")
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e("FIND_LAWYERS", "FIND_LAWYERS ERROR: Firestore error = ${e.message}", e)
                _error.value = "Unable to load lawyers. Please check your internet connection and try again."
                _isLoading.value = false
                Log.d("FIND_LAWYERS", "FIND_LAWYERS: UI state = Error")
            }

            // Continue collecting updates in real-time
            firestoreRepo.getAllLawyersFlow()
                .catch { e ->
                    Log.e("FIND_LAWYERS", "FIND_LAWYERS ERROR: Real-time query error: ${e.message}", e)
                }
                .collect { list ->
                    updateAllLawyers(list)
                }
        }
    }

    fun matchesCategory(specialization: String, selectedCategory: String): Boolean {
        val cat = selectedCategory.trim().lowercase()
        if (cat == "all") return true
        
        val spec = specialization.trim().lowercase()
        if (spec.isBlank()) return false
        
        val cleanCat = cat.replace(" law", "").replace(" lawyer", "").replace(" advocate", "").replace("&", "").replace("  ", " ").trim()
        val cleanSpec = spec.replace(" law", "").replace(" lawyer", "").replace(" advocate", "").replace("&", "").replace("  ", " ").trim()
        
        if (cleanCat.isBlank() || cleanSpec.isBlank()) return false
        
        val catWords = cleanCat.split("\\s+".toRegex())
        val specWords = cleanSpec.split("\\s+".toRegex())
        
        for (catWord in catWords) {
            if (catWord.isNotBlank() && catWord != "and") {
                for (specWord in specWords) {
                    if (specWord.isNotBlank() && specWord != "and") {
                        if (catWord == specWord || catWord.contains(specWord) || specWord.contains(catWord)) {
                            return true
                        }
                    }
                }
            }
        }
        
        return cleanSpec.contains(cleanCat) || cleanCat.contains(cleanSpec)
    }

    val lawyers: StateFlow<List<LawyerProfile>> = combine(
        _allLawyers,
        _searchQuery,
        _selectedSpecialization,
        _selectedExperience,
        _selectedFeeRange,
        _onlineOnly,
        _inPersonOnly,
        _selectedSort
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val rawLawyers = args[0] as List<LawyerProfile>
        val query = args[1] as String
        val spec = args[2] as String
        val exp = args[3] as String
        val fee = args[4] as String
        val online = args[5] as Boolean
        val inPerson = args[6] as Boolean
        val sort = args[7] as String

        try {
            Log.d("FIND_LAWYERS", "FIND_LAWYER: Total lawyers loaded = ${rawLawyers.size}")
            val availableCount = rawLawyers.count { it.isAvailable }
            Log.d("FIND_LAWYERS", "FIND_LAWYER: Available lawyers = $availableCount")
            Log.d("FIND_LAWYERS", "FIND_LAWYER: Selected category = $spec")
            
            rawLawyers.forEach { lawyer ->
                Log.d("FIND_LAWYERS", "FIND_LAWYER: Lawyer name = ${lawyer.name}")
                Log.d("FIND_LAWYERS", "FIND_LAWYER: Lawyer role = ${lawyer.role}")
                Log.d("FIND_LAWYERS", "FIND_LAWYER: Lawyer category = ${lawyer.specialization}")
            }

            var filtered = rawLawyers.filter { lawyer ->
                val roleLower = lawyer.role.lowercase().trim()
                val isLawyer = roleLower == "lawyer" || roleLower == "advocate"
                val isAvailable = lawyer.isAvailable
                
                val matchesQuery = query.isBlank() ||
                        lawyer.name.contains(query, ignoreCase = true) ||
                        lawyer.displayLocation.contains(query, ignoreCase = true) ||
                        lawyer.specialization.contains(query, ignoreCase = true) ||
                        lawyer.languages.contains(query, ignoreCase = true)

                val matchesSpec = spec == "All" || matchesCategory(lawyer.specialization, spec)
                
                val years = lawyer.experience.filter { it.isDigit() }.toIntOrNull() ?: 0
                val matchesExp = when (exp) {
                    "1-3 Yrs" -> years in 1..3
                    "3-5 Yrs" -> years in 3..5
                    "5-10 Yrs" -> years in 5..10
                    "10+ Yrs" -> years >= 10
                    else -> true
                }

                val matchesFee = when (fee) {
                    "Under ₹500" -> lawyer.consultationFee <= 500
                    "₹500 - ₹1000" -> lawyer.consultationFee in 500.0..1000.0
                    "Above ₹1000" -> lawyer.consultationFee > 1000
                    else -> true
                }

                val matchesOnline = !online || lawyer.onlineAvailable
                val matchesInPerson = !inPerson || lawyer.inPersonAvailable

                isLawyer && isAvailable && matchesQuery && matchesSpec && matchesExp && matchesFee && matchesOnline && matchesInPerson
            }

            filtered = when (sort) {
                "Highest Rated" -> filtered.sortedByDescending { it.rating }
                "Most Experienced" -> filtered.sortedByDescending { it.experience.filter { char -> char.isDigit() }.toIntOrNull() ?: 0 }
                "Lowest Fee" -> filtered.sortedBy { it.consultationFee }
                "Highest Fee" -> filtered.sortedByDescending { it.consultationFee }
                else -> filtered // Recommended
            }

            Log.d("FIND_LAWYERS", "FIND_LAWYER: Final filtered lawyers = ${filtered.size}")
            filtered
        } catch (e: Exception) {
            Log.e("FIND_LAWYERS", "FIND_LAWYER ERROR: ${e.message}", e)
            emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onSpecializationSelected(spec: String) {
        _selectedSpecialization.value = spec
    }

    fun onFilterSelected(filter: String) {
        _selectedSpecialization.value = filter
    }

    fun onExperienceSelected(exp: String) {
        _selectedExperience.value = exp
    }

    fun onFeeRangeSelected(fee: String) {
        _selectedFeeRange.value = fee
    }

    fun onOnlineToggled(checked: Boolean) {
        _onlineOnly.value = checked
    }

    fun onInPersonToggled(checked: Boolean) {
        _inPersonOnly.value = checked
    }

    fun onSortSelected(sort: String) {
        _selectedSort.value = sort
    }

}
