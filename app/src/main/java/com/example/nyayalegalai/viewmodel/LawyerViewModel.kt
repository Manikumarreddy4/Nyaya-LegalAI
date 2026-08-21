package com.example.nyayalegalai.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nyayalegalai.models.LawyerProfile
import com.example.nyayalegalai.repository.FirestoreRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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
    
    init {
        loadLawyers()
    }

    fun loadLawyers() {
        viewModelScope.launch {
            _isLoading.value = true
            firestoreRepo.getAllLawyersFlow()
                .catch { e ->
                    Log.e("LawyerViewModel", "Failed to listen to lawyers flow", e)
                    _isLoading.value = false
                }
                .collect { list ->
                    _allLawyers.value = list
                    _isLoading.value = false
                }
        }
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

        var filtered = rawLawyers.filter { lawyer ->
            val matchesQuery = query.isBlank() ||
                    lawyer.name.contains(query, ignoreCase = true) ||
                    lawyer.displayLocation.contains(query, ignoreCase = true) ||
                    lawyer.specialization.contains(query, ignoreCase = true) ||
                    lawyer.languages.contains(query, ignoreCase = true)

            val matchesSpec = spec == "All" || lawyer.specialization.contains(spec, ignoreCase = true)
            
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

            matchesQuery && matchesSpec && matchesExp && matchesFee && matchesOnline && matchesInPerson
        }

        filtered = when (sort) {
            "Highest Rated" -> filtered.sortedByDescending { it.rating }
            "Most Experienced" -> filtered.sortedByDescending { it.experience.filter { char -> char.isDigit() }.toIntOrNull() ?: 0 }
            "Lowest Fee" -> filtered.sortedBy { it.consultationFee }
            "Highest Fee" -> filtered.sortedByDescending { it.consultationFee }
            else -> filtered // Recommended
        }

        filtered
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
