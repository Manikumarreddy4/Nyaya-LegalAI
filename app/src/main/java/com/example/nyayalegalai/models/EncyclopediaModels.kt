package com.example.nyayalegalai.models

enum class LegalStatus {
    IN_FORCE,
    REPEALED,
    AMENDED,
    SUPERSEDED,
    HISTORICAL,
    UNKNOWN
}

enum class LegalHierarchyType {
    CONSTITUTION,
    CENTRAL_ACT,
    STATE_ACT,
    UT_ACT,
    RULES,
    REGULATIONS,
    ORDINANCES,
    NOTIFICATIONS,
    REPEALED_ACT
}

data class LawAct(
    val id: String = "",
    val type: String = "CENTRAL_ACT",
    val actNumber: String = "",
    val name: String = "",
    val shortTitle: String = "",
    val longTitle: String = "",
    val description: String = "",
    val category: String = "",
    val stateOrUt: String = "Central / All India",
    val year: Int = 0,
    val enactmentDate: String = "",
    val enforcementDate: String = "",
    val ministry: String = "",
    val department: String = "",
    val status: String = "IN_FORCE",
    val source: String = "India Code (indiacode.nic.in)",
    val chapters: List<LawChapter> = emptyList(),
    val schedules: List<String> = emptyList()
)

data class LawChapter(
    val id: String = "",
    val number: String = "",
    val title: String = "",
    val description: String = "",
    val sections: List<LawSection> = emptyList()
)

data class LawSection(
    val id: String = "",
    val number: String = "",
    val title: String = "",
    val text: String = "",
    val shortMeaning: String = "",
    val detailedExplanation: String = "",
    val keyPoints: List<String> = emptyList(),
    val example: String = "",
    val relatedProvisions: List<String> = emptyList(),
    val importantNote: String = "",
    val status: String = "IN_FORCE"
)

data class EncyclopediaCategory(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val icon: String = "",
    val hierarchyType: String = "CENTRAL_ACT",
    val laws: List<LawAct> = emptyList()
)

data class EncyclopediaRoot(
    val categories: List<EncyclopediaCategory> = emptyList()
)

data class SectionNavigation(
    val currentSection: LawSection,
    val categoryId: String,
    val categoryName: String,
    val lawId: String,
    val lawName: String,
    val lawStatus: String = "IN_FORCE",
    val chapterId: String,
    val chapterTitle: String,
    val prevSection: LawSection? = null,
    val nextSection: LawSection? = null
)

data class EncyclopediaSearchResult(
    val section: LawSection,
    val categoryId: String,
    val categoryName: String,
    val lawId: String,
    val lawName: String,
    val lawStatus: String = "IN_FORCE",
    val chapterId: String,
    val chapterTitle: String,
    val ministry: String = "",
    val year: Int = 0
)

data class StateOrUtJurisdiction(
    val id: String = "",
    val name: String = "",
    val type: String = "STATE", // "STATE" or "UNION_TERRITORY"
    val capital: String = "",
    val highCourt: String = "",
    val actCount: Int = 0,
    val sampleActs: List<String> = emptyList()
)
