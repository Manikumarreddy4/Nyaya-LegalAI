package com.example.nyayalegalai.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nyayalegalai.models.*
import com.example.nyayalegalai.repository.ChatHistoryRepository
import com.example.nyayalegalai.repository.EncyclopediaRepository
import com.example.nyayalegalai.repository.ImportStats
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class EncyclopediaNavLevel {
    ROOT,
    CATEGORY,
    LAW,
    CHAPTER,
    SECTION,
    JURISDICTION_DETAIL
}

class LawViewModel(
    private val encyclopediaRepo: EncyclopediaRepository,
    private val chatHistoryRepository: ChatHistoryRepository
) : ViewModel() {

    private val _navLevel = MutableStateFlow(EncyclopediaNavLevel.ROOT)
    val navLevel: StateFlow<EncyclopediaNavLevel> = _navLevel.asStateFlow()

    private val _selectedTab = MutableStateFlow(0) // 0: Categories, 1: Central Acts, 2: States & UTs
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _categories = MutableStateFlow<List<EncyclopediaCategory>>(emptyList())
    val categories: StateFlow<List<EncyclopediaCategory>> = _categories.asStateFlow()

    private val _centralActsCatalogue = MutableStateFlow<List<LawAct>>(emptyList())
    val centralActsCatalogue: StateFlow<List<LawAct>> = _centralActsCatalogue.asStateFlow()

    private val _stateUtJurisdictions = MutableStateFlow<List<StateOrUtJurisdiction>>(emptyList())
    val stateUtJurisdictions: StateFlow<List<StateOrUtJurisdiction>> = _stateUtJurisdictions.asStateFlow()

    private val _importStats = MutableStateFlow<ImportStats?>(null)
    val importStats: StateFlow<ImportStats?> = _importStats.asStateFlow()

    private val _selectedCategory = MutableStateFlow<EncyclopediaCategory?>(null)
    val selectedCategory: StateFlow<EncyclopediaCategory?> = _selectedCategory.asStateFlow()

    private val _selectedLaw = MutableStateFlow<LawAct?>(null)
    val selectedLaw: StateFlow<LawAct?> = _selectedLaw.asStateFlow()

    private val _selectedChapter = MutableStateFlow<LawChapter?>(null)
    val selectedChapter: StateFlow<LawChapter?> = _selectedChapter.asStateFlow()

    private val _selectedJurisdiction = MutableStateFlow<StateOrUtJurisdiction?>(null)
    val selectedJurisdiction: StateFlow<StateOrUtJurisdiction?> = _selectedJurisdiction.asStateFlow()

    private val _currentSectionNav = MutableStateFlow<SectionNavigation?>(null)
    val currentSectionNav: StateFlow<SectionNavigation?> = _currentSectionNav.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<EncyclopediaSearchResult>>(emptyList())
    val searchResults: StateFlow<List<EncyclopediaSearchResult>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _isBookmarked = MutableStateFlow(false)
    val isBookmarked: StateFlow<Boolean> = _isBookmarked.asStateFlow()

    private val _lastRead = MutableStateFlow<Triple<String, String, String>?>(null)
    val lastRead: StateFlow<Triple<String, String, String>?> = _lastRead.asStateFlow()

    private val _bookmarkedSections = MutableStateFlow<List<EncyclopediaSearchResult>>(emptyList())
    val bookmarkedSections: StateFlow<List<EncyclopediaSearchResult>> = _bookmarkedSections.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            try {
                Log.d("CENTRAL_ACT_DEBUG", "ACT_LOAD: Loading encyclopedia initial data")
                val cats = encyclopediaRepo.getCategories()
                _categories.value = cats
                val catalogue = encyclopediaRepo.getCentralActsCatalogue()
                _centralActsCatalogue.value = catalogue
                val jurisdictions = encyclopediaRepo.getStateUtJurisdictions()
                _stateUtJurisdictions.value = jurisdictions
                val stats = encyclopediaRepo.getImportStats()
                _importStats.value = stats
                _lastRead.value = encyclopediaRepo.getLastReadSection()
                refreshBookmarks()
                Log.d("CENTRAL_ACT_DEBUG", "ACT_LOAD: Loaded ${cats.size} categories, ${catalogue.size} catalogue acts, ${jurisdictions.size} jurisdictions")
            } catch (e: Exception) {
                Log.e("CENTRAL_ACT_ERROR", "Failed to load encyclopedia data", e)
            }
        }
    }

    fun selectTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    private suspend fun refreshBookmarks() {
        try {
            val ids = encyclopediaRepo.getBookmarkedSectionIds()
            val list = mutableListOf<EncyclopediaSearchResult>()
            for (id in ids) {
                val result = encyclopediaRepo.getSectionById(id)
                if (result != null) {
                    val (nav, sec) = result
                    list.add(
                        EncyclopediaSearchResult(
                            section = sec,
                            categoryId = nav.categoryId,
                            categoryName = nav.categoryName,
                            lawId = nav.lawId,
                            lawName = nav.lawName,
                            lawStatus = nav.lawStatus,
                            chapterId = nav.chapterId,
                            chapterTitle = nav.chapterTitle
                        )
                    )
                }
            }
            _bookmarkedSections.value = list
        } catch (e: Exception) {
            Log.e("CENTRAL_ACT_ERROR", "Failed to refresh bookmarks", e)
        }
    }

    fun selectCategory(category: EncyclopediaCategory) {
        Log.d("CENTRAL_ACT_DEBUG", "ACT_CLICK: Selected category id=${category.id}, name=${category.name}, laws=${category.laws.size}")
        _selectedCategory.value = category
        _selectedLaw.value = null
        _selectedChapter.value = null
        _navLevel.value = EncyclopediaNavLevel.CATEGORY
    }

    fun selectLaw(law: LawAct) {
        val actId = law.id
        val actName = law.shortTitle.ifBlank { law.name }
        Log.d("CENTRAL_ACT_DEBUG", "ACT_CLICK: Clicked actId=$actId, name=$actName")
        viewModelScope.launch {
            try {
                Log.d("CENTRAL_ACT_DEBUG", "ACT_LOAD: Loading act=$actId")
                val categoryId = _selectedCategory.value?.id ?: ""
                val fullLaw = encyclopediaRepo.getLawById(categoryId, actId) ?: law
                
                val foundCategory = encyclopediaRepo.findCategoryForLaw(fullLaw.id)
                if (foundCategory != null) {
                    _selectedCategory.value = foundCategory
                }

                _selectedLaw.value = fullLaw
                _selectedChapter.value = null
                Log.d("CENTRAL_ACT_DEBUG", "CHAPTER_LOAD: Chapters found=${fullLaw.chapters.size} for actId=$actId (${fullLaw.shortTitle.ifBlank { fullLaw.name }})")
                _navLevel.value = EncyclopediaNavLevel.LAW
            } catch (e: Exception) {
                Log.e("CENTRAL_ACT_ERROR", "Failed to load chapters for actId=$actId", e)
                _selectedLaw.value = law
                _navLevel.value = EncyclopediaNavLevel.LAW
            }
        }
    }

    fun selectChapter(chapter: LawChapter) {
        val chapId = chapter.id
        Log.d("CENTRAL_ACT_DEBUG", "CHAPTER_LOAD: Clicked chapterId=$chapId, title=${chapter.title}, sections=${chapter.sections.size}")
        _selectedChapter.value = chapter
        _navLevel.value = EncyclopediaNavLevel.CHAPTER
    }

    fun selectJurisdiction(jurisdiction: StateOrUtJurisdiction) {
        _selectedJurisdiction.value = jurisdiction
        _navLevel.value = EncyclopediaNavLevel.JURISDICTION_DETAIL
    }

    fun openSection(
        categoryId: String,
        lawId: String,
        chapterId: String,
        sectionId: String
    ) {
        Log.d("CENTRAL_ACT_DEBUG", "SECTION_LOAD: Opening sectionId=$sectionId for lawId=$lawId, chapterId=$chapterId, categoryId=$categoryId")
        viewModelScope.launch {
            try {
                val nav = encyclopediaRepo.getSectionNavigation(categoryId, lawId, chapterId, sectionId)
                if (nav != null) {
                    _currentSectionNav.value = nav
                    _selectedCategory.value = encyclopediaRepo.getCategoryById(nav.categoryId) ?: _selectedCategory.value
                    _selectedLaw.value = encyclopediaRepo.getLawById(nav.categoryId, nav.lawId) ?: _selectedLaw.value
                    _selectedChapter.value = encyclopediaRepo.getChapterById(nav.categoryId, nav.lawId, nav.chapterId) ?: _selectedChapter.value
                    _isBookmarked.value = encyclopediaRepo.isSectionBookmarked(sectionId)
                    encyclopediaRepo.saveLastReadSection(sectionId, nav.currentSection.number, nav.currentSection.title)
                    _lastRead.value = encyclopediaRepo.getLastReadSection()
                    _navLevel.value = EncyclopediaNavLevel.SECTION
                    Log.d("CENTRAL_ACT_DEBUG", "SECTION_LOAD: Successfully displayed section ${nav.currentSection.number} (${nav.currentSection.title})")

                    try {
                        val sessionId = chatHistoryRepository.createSession(nav.currentSection.number, "ENCYCLOPEDIA")
                        chatHistoryRepository.addMessage(sessionId, "User", "Viewed ${nav.currentSection.number}: ${nav.currentSection.title}")
                        chatHistoryRepository.addMessage(sessionId, "Bot", "Explaining ${nav.currentSection.number}: ${nav.currentSection.shortMeaning}")
                    } catch (e: Exception) {
                        Log.e("CENTRAL_ACT_ERROR", "Chat session creation warning for section $sectionId", e)
                    }
                } else {
                    Log.e("CENTRAL_ACT_ERROR", "SECTION_LOAD: Navigation null for sectionId=$sectionId, lawId=$lawId, chapterId=$chapterId")
                }
            } catch (e: Exception) {
                Log.e("CENTRAL_ACT_ERROR", "Failed to open sectionId=$sectionId", e)
            }
        }
    }

    fun openSectionFromSearch(result: EncyclopediaSearchResult) {
        Log.d("CENTRAL_ACT_DEBUG", "ACT_CLICK: Opened search result sectionId=${result.section.id}, lawId=${result.lawId}")
        openSection(result.categoryId, result.lawId, result.chapterId, result.section.id)
    }

    fun openLastRead() {
        val last = _lastRead.value ?: return
        val sectionId = last.first
        viewModelScope.launch {
            try {
                val found = encyclopediaRepo.getSectionById(sectionId)
                if (found != null) {
                    val (nav, sec) = found
                    openSection(nav.categoryId, nav.lawId, nav.chapterId, sec.id)
                }
            } catch (e: Exception) {
                Log.e("CENTRAL_ACT_ERROR", "Failed to open last read section $sectionId", e)
            }
        }
    }

    fun goToPrevSection() {
        val nav = _currentSectionNav.value ?: return
        val prev = nav.prevSection ?: return
        openSection(nav.categoryId, nav.lawId, nav.chapterId, prev.id)
    }

    fun goToNextSection() {
        val nav = _currentSectionNav.value ?: return
        val next = nav.nextSection ?: return
        openSection(nav.categoryId, nav.lawId, nav.chapterId, next.id)
    }

    fun toggleBookmark() {
        val nav = _currentSectionNav.value ?: return
        val sectionId = nav.currentSection.id
        val bookmarked = encyclopediaRepo.toggleSectionBookmark(sectionId)
        _isBookmarked.value = bookmarked
        viewModelScope.launch {
            refreshBookmarks()
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        if (query.trim().isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }

        viewModelScope.launch {
            try {
                _isSearching.value = true
                val results = encyclopediaRepo.searchSections(query)
                _searchResults.value = results
                _isSearching.value = false
                Log.d("CENTRAL_ACT_DEBUG", "Search query '$query' found ${results.size} results")
            } catch (e: Exception) {
                Log.e("CENTRAL_ACT_ERROR", "Search error for query '$query'", e)
                _searchResults.value = emptyList()
                _isSearching.value = false
            }
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _isSearching.value = false
    }

    fun navigateBack(): Boolean {
        if (_searchQuery.value.isNotBlank()) {
            clearSearch()
            return true
        }

        return when (_navLevel.value) {
            EncyclopediaNavLevel.SECTION -> {
                _navLevel.value = EncyclopediaNavLevel.CHAPTER
                true
            }
            EncyclopediaNavLevel.CHAPTER -> {
                _navLevel.value = EncyclopediaNavLevel.LAW
                true
            }
            EncyclopediaNavLevel.LAW -> {
                _navLevel.value = if (_selectedTab.value == 1) {
                    EncyclopediaNavLevel.ROOT
                } else {
                    EncyclopediaNavLevel.CATEGORY
                }
                true
            }
            EncyclopediaNavLevel.CATEGORY,
            EncyclopediaNavLevel.JURISDICTION_DETAIL -> {
                _navLevel.value = EncyclopediaNavLevel.ROOT
                true
            }
            EncyclopediaNavLevel.ROOT -> {
                false
            }
        }
    }
}
