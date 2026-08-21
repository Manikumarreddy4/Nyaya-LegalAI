package com.example.nyayalegalai.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.nyayalegalai.models.*
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.InputStreamReader

data class ImportStats(
    val totalActsDiscovered: Int,
    val totalActsImported: Int,
    val totalSectionsImported: Int,
    val constitutionArticlesImported: Int,
    val currentCriminalSectionsImported: Int,
    val historicalCriminalSectionsImported: Int,
    val stateUtJurisdictionsCatalogued: Int,
    val totalSkipped: Int,
    val skipReasons: List<String>
)

class EncyclopediaRepository(private val context: Context) {

    private val gson = Gson()
    private val mutex = Mutex()
    private val prefs: SharedPreferences = context.getSharedPreferences("nyaya_encyclopedia_prefs", Context.MODE_PRIVATE)

    // Memory caches for high performance
    private var cachedCategories: List<EncyclopediaCategory>? = null
    private var cachedCentralCatalogue: List<LawAct>? = null
    private var cachedJurisdictions: List<StateOrUtJurisdiction>? = null
    private val actCache = mutableMapOf<String, LawAct>()

    // Statistics Tracker
    private var importStats: ImportStats? = null

    private fun normalizeActId(rawId: String): String {
        return when (rawId.lowercase().trim()) {
            "act_constitution_1950", "constitution", "constitution_of_india" -> "constitution_of_india"
            "act_ipc_1860_repealed", "ipc", "ipc_1860", "act_ipc_1860" -> "act_ipc_1860"
            "act_crpc_1973_repealed", "crpc", "crpc_1973", "act_crpc_1973" -> "act_crpc_1973"
            "act_evidence_1872_repealed", "evidence", "act_evidence_1872" -> "act_evidence_1872"
            else -> rawId.trim()
        }
    }

    suspend fun getCategories(): List<EncyclopediaCategory> = withContext(Dispatchers.IO) {
        mutex.withLock {
            cachedCategories?.let { return@withContext it }

            val categoriesList = mutableListOf<EncyclopediaCategory>()

            // 1. Constitution of India (395+ Articles)
            val constitutionAct = loadConstitutionAct()
            categoriesList.add(
                EncyclopediaCategory(
                    id = "cat_constitution",
                    name = "Constitution of India",
                    description = "Supreme law of India establishing fundamental rights, directive principles, Union/State government structure, and constitutional remedies.",
                    icon = "AccountBalance",
                    hierarchyType = "CONSTITUTION",
                    laws = listOf(constitutionAct)
                )
            )

            // 2. Current Criminal Law Framework (BNS / BNSS / BSA - 2023)
            val bnsAct = loadBnsAct()
            val bnssAct = loadBnssAct()
            val bsaAct = loadBsaAct()
            categoriesList.add(
                EncyclopediaCategory(
                    id = "cat_criminal_law_current",
                    name = "Criminal Law (BNS, BNSS, BSA - 2023)",
                    description = "The substantive, procedural, and evidence statutes of India in force from 1 July 2024, superseding IPC, CrPC, and Indian Evidence Act.",
                    icon = "Gavel",
                    hierarchyType = "CENTRAL_ACT",
                    laws = listOf(bnsAct, bnssAct, bsaAct)
                )
            )

            // 3. Civil, Corporate & Commercial Laws
            val mvaAct = loadMvaAct()
            val cpcAct = loadCpcAct()
            val civilActs = loadCivilAndCommercialActs(mvaAct, cpcAct)
            categoriesList.add(
                EncyclopediaCategory(
                    id = "cat_civil_commercial",
                    name = "Civil, Commercial & Transport Laws",
                    description = "Key Central Acts regulating contracts, corporate governance, consumer rights, cyber law, civil procedure, data protection, and motor vehicles.",
                    icon = "Balance",
                    hierarchyType = "CENTRAL_ACT",
                    laws = civilActs
                )
            )

            // 4. Historical & Repealed Criminal Legislation
            val ipcAct = loadIpcAct()
            val crpcAct = loadCrpcAct()
            val evidenceAct = loadEvidenceAct()
            categoriesList.add(
                EncyclopediaCategory(
                    id = "cat_historical_repealed",
                    name = "Historical & Repealed Legislation",
                    description = "Historical penal, procedural, and evidence statutes (IPC 1860, CrPC 1973, Evidence Act 1872) applicable only to pre-1 July 2024 matters.",
                    icon = "History",
                    hierarchyType = "REPEALED_ACT",
                    laws = listOf(ipcAct, crpcAct, evidenceAct)
                )
            )

            // Cache all loaded acts by primary and alias IDs
            for (cat in categoriesList) {
                for (law in cat.laws) {
                    actCache[law.id] = law
                    actCache[normalizeActId(law.id)] = law
                }
            }

            cachedCategories = categoriesList
            calculateStats(categoriesList)
            return@withContext categoriesList
        }
    }

    suspend fun getImportStats(): ImportStats {
        if (importStats == null) {
            getCategories()
        }
        return importStats ?: ImportStats(0, 0, 0, 0, 0, 0, 0, 0, emptyList())
    }

    private fun calculateStats(categories: List<EncyclopediaCategory>) {
        var totalSecs = 0
        var constArticles = 0
        var currentCrimSecs = 0
        var histCrimSecs = 0
        var totalActs = 0

        for (cat in categories) {
            for (law in cat.laws) {
                totalActs++
                var lawSecs = 0
                for (chap in law.chapters) {
                    lawSecs += chap.sections.size
                }
                totalSecs += lawSecs
                when (law.id) {
                    "constitution_of_india" -> constArticles = lawSecs
                    "act_bns_2023", "act_bnss_2023", "act_bsa_2023" -> currentCrimSecs += lawSecs
                    "act_ipc_1860", "act_crpc_1973", "act_evidence_1872" -> histCrimSecs += lawSecs
                }
            }
        }

        val catalogueSize = cachedCentralCatalogue?.size ?: 17
        val jurisdictionsCount = cachedJurisdictions?.size ?: 36

        importStats = ImportStats(
            totalActsDiscovered = totalActs + catalogueSize,
            totalActsImported = totalActs,
            totalSectionsImported = totalSecs,
            constitutionArticlesImported = constArticles,
            currentCriminalSectionsImported = currentCrimSecs,
            historicalCriminalSectionsImported = histCrimSecs,
            stateUtJurisdictionsCatalogued = jurisdictionsCount,
            totalSkipped = 0,
            skipReasons = emptyList()
        )
    }

    // 1. CONSTITUTION OF INDIA (395+ Articles)
    private fun loadConstitutionAct(): LawAct {
        actCache["constitution_of_india"]?.let { return it }

        try {
            context.assets.open("constitution_of_india.json").use { stream ->
                InputStreamReader(stream).use { reader ->
                    val array = gson.fromJson(reader, JsonArray::class.java)
                    val chaptersMap = mutableMapOf<String, MutableList<LawSection>>()

                    for (i in 0 until array.size()) {
                        val obj = array.get(i).asJsonObject
                        val artNumVal = obj.get("article")
                        val artNum = if (artNumVal != null && !artNumVal.isJsonNull) {
                            if (artNumVal.asString == "0") "Preamble" else "Article ${artNumVal.asString}"
                        } else "Article $i"
                        val title = obj.get("title")?.asString ?: "Untitled Article"
                        val desc = obj.get("description")?.asString ?: ""

                        val partKey = determineConstitutionPart(artNum)

                        val section = LawSection(
                            id = "const_art_${artNum.replace(" ", "_").lowercase()}",
                            number = artNum,
                            title = title,
                            text = desc,
                            shortMeaning = desc.take(250).let { if (desc.length > 250) "$it..." else it },
                            detailedExplanation = desc,
                            keyPoints = listOf(
                                "Part of the supreme constitutional framework of India.",
                                "Binding on the Union, States, and all statutory authorities."
                            ),
                            status = "IN_FORCE"
                        )

                        chaptersMap.getOrPut(partKey) { mutableListOf() }.add(section)
                    }

                    val chaptersList = chaptersMap.map { (partName, sections) ->
                        LawChapter(
                            id = "chap_${partName.replace(" ", "_").lowercase()}",
                            number = partName.substringBefore(":"),
                            title = partName,
                            description = "Constitutional provisions and articles under $partName.",
                            sections = sections
                        )
                    }

                    val act = LawAct(
                        id = "constitution_of_india",
                        type = "CONSTITUTION",
                        actNumber = "Constituent Assembly of India",
                        name = "Constitution of India",
                        shortTitle = "Constitution of India",
                        longTitle = "The Constitution of the Sovereign Socialist Secular Democratic Republic of India.",
                        description = "The fundamental supreme legal document of the Republic of India containing all 395+ Articles and Parts.",
                        year = 1950,
                        enactmentDate = "26-11-1949",
                        enforcementDate = "26-01-1950",
                        ministry = "Ministry of Law and Justice",
                        department = "Legislative Department",
                        category = "Constitutional Law",
                        status = "IN_FORCE",
                        source = "India Code (indiacode.nic.in) & Legislative Department",
                        chapters = chaptersList
                    )

                    actCache["constitution_of_india"] = act
                    actCache["act_constitution_1950"] = act
                    return act
                }
            }
        } catch (e: Exception) {
            Log.e("CENTRAL_ACT_ERROR", "Error loading constitution_of_india.json", e)
            return LawAct(id = "constitution_of_india", name = "Constitution of India")
        }
    }

    private fun determineConstitutionPart(artNum: String): String {
        if (artNum.equals("Preamble", ignoreCase = true)) return "Preamble"
        val numOnly = artNum.replace("Article", "").trim().filter { it.isDigit() }.toIntOrNull() ?: return "Part Miscellaneous"

        return when {
            numOnly in 1..4 -> "Part I: The Union and its Territory"
            numOnly in 5..11 -> "Part II: Citizenship"
            numOnly in 12..35 -> "Part III: Fundamental Rights"
            numOnly in 36..51 -> "Part IV: Directive Principles of State Policy"
            numOnly in 52..151 -> "Part V: The Union Executive and Parliament"
            numOnly in 152..237 -> "Part VI: The States and High Courts"
            numOnly in 239..242 -> "Part VIII: The Union Territories"
            numOnly in 243..243 -> "Part IX: The Panchayats and Municipalities"
            numOnly in 244..263 -> "Part X & XI: Relations between Union and States"
            numOnly in 264..300 -> "Part XII: Finance, Property, Contracts and Suits"
            numOnly in 301..307 -> "Part XIII: Trade, Commerce and Intercourse"
            numOnly in 308..323 -> "Part XIV: Services and Public Service Commissions"
            numOnly in 324..329 -> "Part XV: Elections"
            numOnly in 330..342 -> "Part XVI: Special Provisions relating to Certain Classes"
            numOnly in 343..351 -> "Part XVII: Official Language"
            numOnly in 352..360 -> "Part XVIII: Emergency Provisions"
            numOnly in 361..367 -> "Part XIX: Miscellaneous Provisions"
            numOnly in 368..368 -> "Part XX: Amendment of the Constitution"
            numOnly in 369..392 -> "Part XXI: Temporary, Transitional and Special Provisions"
            else -> "Part XXII: Short Title, Commencement and Repeals"
        }
    }

    // 2. BHARATIYA NYAYA SANHITA, 2023 (358 Sections)
    private fun loadBnsAct(): LawAct {
        actCache["act_bns_2023"]?.let { return it }
        return loadBnsFormatAct(
            assetFileName = "bns_en.json",
            actId = "act_bns_2023",
            actNumber = "Act No. 45 of 2023",
            shortTitle = "Bharatiya Nyaya Sanhita, 2023",
            longTitle = "An Act to consolidate and amend the provisions relating to offences and for matters connected therewith or incidental thereto.",
            description = "The substantive penal code of India governing criminal offenses, liability, and statutory punishments (in force from 1 July 2024).",
            year = 2023,
            ministry = "Ministry of Home Affairs",
            department = "Internal Security Division",
            category = "Criminal Law"
        )
    }

    // 3. BHARATIYA NAGARIK SURAKSHA SANHITA, 2023 (531 Sections)
    private fun loadBnssAct(): LawAct {
        actCache["act_bnss_2023"]?.let { return it }
        return loadBnsFormatAct(
            assetFileName = "bnss_en.json",
            actId = "act_bnss_2023",
            actNumber = "Act No. 46 of 2023",
            shortTitle = "Bharatiya Nagarik Suraksha Sanhita, 2023",
            longTitle = "An Act to consolidate and amend the law relating to Criminal Procedure.",
            description = "The procedural code of India regulating police investigation, arrest, e-FIR, Zero FIR, forensics, bail, and trials (in force from 1 July 2024).",
            year = 2023,
            ministry = "Ministry of Home Affairs",
            department = "Judicial Division",
            category = "Criminal Procedure"
        )
    }

    // 4. BHARATIYA SAKSHYA ADHINIYAM, 2023 (170 Sections)
    private fun loadBsaAct(): LawAct {
        actCache["act_bsa_2023"]?.let { return it }
        return loadBnsFormatAct(
            assetFileName = "bsa_en.json",
            actId = "act_bsa_2023",
            actNumber = "Act No. 47 of 2023",
            shortTitle = "Bharatiya Sakshya Adhiniyam, 2023",
            longTitle = "An Act to consolidate and to provide for general rules and principles of evidence for fair trial.",
            description = "The statutory law of evidence governing electronic records, admissibility, and witness examination (in force from 1 July 2024).",
            year = 2023,
            ministry = "Ministry of Law and Justice",
            department = "Legislative Department",
            category = "Evidence"
        )
    }

    private fun loadBnsFormatAct(
        assetFileName: String,
        actId: String,
        actNumber: String,
        shortTitle: String,
        longTitle: String,
        description: String,
        year: Int,
        ministry: String,
        department: String,
        category: String
    ): LawAct {
        try {
            context.assets.open(assetFileName).use { stream ->
                InputStreamReader(stream).use { reader ->
                    val rootObj = gson.fromJson(reader, JsonObject::class.java)
                    val sectionsArray = rootObj.getAsJsonArray("sections")
                    val chaptersMap = mutableMapOf<String, MutableList<LawSection>>()

                    for (i in 0 until sectionsArray.size()) {
                        val secObj = sectionsArray.get(i).asJsonObject
                        val secNum = secObj.get("section_number")?.asString ?: "${i + 1}"
                        val heading = secObj.get("heading")?.asString ?: "Section $secNum"
                        val text = secObj.get("text")?.asString ?: ""

                        val chapObj = secObj.getAsJsonObject("chapter")
                        val chapCode = chapObj?.get("code")?.asString ?: "General"
                        val chapTitle = chapObj?.get("title")?.asString ?: "Provisions"
                        val chapKey = "Chapter $chapCode: $chapTitle"

                        val section = LawSection(
                            id = "${actId}_sec_$secNum",
                            number = "Section $secNum",
                            title = heading.substringBefore(".—").substringBefore(".-").trim(),
                            text = text,
                            shortMeaning = text.take(250).let { if (text.length > 250) "$it..." else it },
                            detailedExplanation = text,
                            keyPoints = listOf(
                                "Official statutory provision under $shortTitle.",
                                "In force across India effective 1 July 2024."
                            ),
                            status = "IN_FORCE"
                        )

                        chaptersMap.getOrPut(chapKey) { mutableListOf() }.add(section)
                    }

                    val chaptersList = chaptersMap.map { (chapTitle, sections) ->
                        LawChapter(
                            id = "chap_${actId}_${chapTitle.replace(" ", "_").lowercase()}",
                            number = chapTitle.substringBefore(":"),
                            title = chapTitle,
                            description = "Provisions under $chapTitle.",
                            sections = sections
                        )
                    }

                    val act = LawAct(
                        id = actId,
                        type = "CENTRAL_ACT",
                        actNumber = actNumber,
                        name = shortTitle,
                        shortTitle = shortTitle,
                        longTitle = longTitle,
                        description = description,
                        year = year,
                        enactmentDate = "25-12-2023",
                        enforcementDate = "01-07-2024",
                        ministry = ministry,
                        department = department,
                        category = category,
                        status = "IN_FORCE",
                        source = "India Code (indiacode.nic.in)",
                        chapters = chaptersList
                    )

                    actCache[actId] = act
                    return act
                }
            }
        } catch (e: Exception) {
            Log.e("CENTRAL_ACT_ERROR", "Error loading $assetFileName", e)
            return LawAct(id = actId, name = shortTitle)
        }
    }

    // 5. INDIAN PENAL CODE, 1860 (511 Sections - Historical / Repealed)
    private fun loadIpcAct(): LawAct {
        actCache["act_ipc_1860"]?.let { return it }

        try {
            context.assets.open("ipc.json").use { stream ->
                InputStreamReader(stream).use { reader ->
                    val array = gson.fromJson(reader, JsonArray::class.java)
                    val chaptersMap = mutableMapOf<String, MutableList<LawSection>>()

                    for (i in 0 until array.size()) {
                        val obj = array.get(i).asJsonObject
                        val chapNum = obj.get("chapter")?.asString ?: "1"
                        val chapTitle = obj.get("chapter_title")?.asString ?: "General"
                        val secNum = obj.get("Section")?.asString ?: "${i + 1}"
                        val title = obj.get("section_title")?.asString ?: "Section $secNum"
                        val desc = obj.get("section_desc")?.asString ?: ""

                        val chapKey = "Chapter $chapNum: ${chapTitle.uppercase()}"

                        val section = LawSection(
                            id = "ipc_sec_$secNum",
                            number = "Section $secNum (IPC)",
                            title = title,
                            text = desc,
                            shortMeaning = desc.take(250).let { if (desc.length > 250) "$it..." else it },
                            detailedExplanation = desc,
                            keyPoints = listOf(
                                "Historical penal provision of the erstwhile Indian Penal Code (1860).",
                                "REPEALED by Section 358 of Bharatiya Nyaya Sanhita, 2023 with effect from 1 July 2024.",
                                "Applies only to offenses committed prior to 1 July 2024."
                            ),
                            importantNote = "Repealed statute. Superseded by Bharatiya Nyaya Sanhita, 2023.",
                            status = "REPEALED"
                        )

                        chaptersMap.getOrPut(chapKey) { mutableListOf() }.add(section)
                    }

                    val chaptersList = chaptersMap.map { (chapTitle, sections) ->
                        LawChapter(
                            id = "chap_ipc_${chapTitle.replace(" ", "_").lowercase()}",
                            number = chapTitle.substringBefore(":"),
                            title = chapTitle,
                            description = "Former IPC provisions under $chapTitle.",
                            sections = sections
                        )
                    }

                    val act = LawAct(
                        id = "act_ipc_1860",
                        type = "REPEALED_ACT",
                        actNumber = "Act No. 45 of 1860",
                        name = "Indian Penal Code, 1860 (Historical / Repealed)",
                        shortTitle = "Indian Penal Code, 1860",
                        longTitle = "An Act to provide a general Penal Code for India (Repealed and superseded by Bharatiya Nyaya Sanhita, 2023).",
                        description = "Former substantive criminal code containing all 511 Sections. Preserved for historical reference and pending pre-transition trials.",
                        year = 1860,
                        enactmentDate = "06-10-1860",
                        enforcementDate = "01-01-1862",
                        ministry = "Ministry of Home Affairs",
                        department = "Historical Archives",
                        category = "Historical Criminal Law",
                        status = "REPEALED",
                        source = "India Code (Repealed Acts Archive)",
                        chapters = chaptersList
                    )

                    actCache["act_ipc_1860"] = act
                    actCache["act_ipc_1860_repealed"] = act
                    return act
                }
            }
        } catch (e: Exception) {
            Log.e("CENTRAL_ACT_ERROR", "Error loading ipc.json", e)
            return LawAct(id = "act_ipc_1860", name = "Indian Penal Code, 1860")
        }
    }

    // 6. CODE OF CRIMINAL PROCEDURE, 1973 (484 Sections - Historical / Repealed)
    private fun loadCrpcAct(): LawAct {
        actCache["act_crpc_1973"]?.let { return it }

        try {
            context.assets.open("crpc.json").use { stream ->
                InputStreamReader(stream).use { reader ->
                    val array = gson.fromJson(reader, JsonArray::class.java)
                    val chaptersMap = mutableMapOf<String, MutableList<LawSection>>()

                    for (i in 0 until array.size()) {
                        val obj = array.get(i).asJsonObject
                        val chapNum = obj.get("chapter")?.asString ?: "1"
                        val secNum = obj.get("section")?.asString ?: "${i + 1}"
                        val title = obj.get("section_title")?.asString ?: "Section $secNum"
                        val desc = obj.get("section_desc")?.asString ?: ""

                        val chapKey = "Chapter $chapNum: Criminal Procedure"

                        val section = LawSection(
                            id = "crpc_sec_$secNum",
                            number = "Section $secNum (CrPC)",
                            title = title,
                            text = desc,
                            shortMeaning = desc.take(250).let { if (desc.length > 250) "$it..." else it },
                            detailedExplanation = desc,
                            keyPoints = listOf(
                                "Historical procedural code of India (1973).",
                                "REPEALED by Bharatiya Nagarik Suraksha Sanhita, 2023 with effect from 1 July 2024.",
                                "Applies only to proceedings commenced prior to the transition date."
                            ),
                            importantNote = "Repealed statute. Superseded by Bharatiya Nagarik Suraksha Sanhita, 2023.",
                            status = "REPEALED"
                        )

                        chaptersMap.getOrPut(chapKey) { mutableListOf() }.add(section)
                    }

                    val chaptersList = chaptersMap.map { (chapTitle, sections) ->
                        LawChapter(
                            id = "chap_crpc_${chapTitle.replace(" ", "_").lowercase()}",
                            number = chapTitle.substringBefore(":"),
                            title = chapTitle,
                            description = "Former CrPC provisions under $chapTitle.",
                            sections = sections
                        )
                    }

                    val act = LawAct(
                        id = "act_crpc_1973",
                        type = "REPEALED_ACT",
                        actNumber = "Act No. 2 of 1974",
                        name = "Code of Criminal Procedure, 1973 (Historical / Repealed)",
                        shortTitle = "Code of Criminal Procedure, 1973",
                        longTitle = "An Act to consolidate and amend the law relating to Criminal Procedure (Repealed by BNSS 2023).",
                        description = "Former criminal procedural code containing all 484 Sections. Preserved for historical reference and pending pre-transition trials.",
                        year = 1973,
                        enactmentDate = "25-01-1974",
                        enforcementDate = "01-04-1974",
                        ministry = "Ministry of Home Affairs",
                        department = "Historical Archives",
                        category = "Historical Criminal Procedure",
                        status = "REPEALED",
                        source = "India Code (Repealed Acts Archive)",
                        chapters = chaptersList
                    )

                    actCache["act_crpc_1973"] = act
                    actCache["act_crpc_1973_repealed"] = act
                    return act
                }
            }
        } catch (e: Exception) {
            Log.e("CENTRAL_ACT_ERROR", "Error loading crpc.json", e)
            return LawAct(id = "act_crpc_1973", name = "Code of Criminal Procedure, 1973")
        }
    }

    // 7. CODE OF CIVIL PROCEDURE, 1908 (158 Sections)
    private fun loadCpcAct(): LawAct {
        actCache["act_cpc_1908"]?.let { return it }

        try {
            context.assets.open("cpc.json").use { stream ->
                InputStreamReader(stream).use { reader ->
                    val array = gson.fromJson(reader, JsonArray::class.java)
                    val chaptersMap = mutableMapOf<String, MutableList<LawSection>>()

                    for (i in 0 until array.size()) {
                        val obj = array.get(i).asJsonObject
                        val secNumVal = obj.get("section")
                        val secNumStr = if (secNumVal != null && !secNumVal.isJsonNull) secNumVal.asString else "${i + 1}"
                        val secNumInt = secNumStr.filter { it.isDigit() }.toIntOrNull() ?: (i + 1)
                        val title = obj.get("title")?.asString ?: "Section $secNumStr"
                        val desc = obj.get("description")?.asString ?: ""

                        val partKey = determineCpcPart(secNumInt)

                        val section = LawSection(
                            id = "cpc_sec_$secNumStr",
                            number = "Section $secNumStr (CPC)",
                            title = title,
                            text = desc,
                            shortMeaning = desc.take(250).let { if (desc.length > 250) "$it..." else it },
                            detailedExplanation = desc,
                            keyPoints = listOf(
                                "Governs procedural law for civil courts and litigation in India.",
                                "Enacted in 1908 under the Ministry of Law and Justice."
                            ),
                            status = "IN_FORCE"
                        )

                        chaptersMap.getOrPut(partKey) { mutableListOf() }.add(section)
                    }

                    val chaptersList = chaptersMap.map { (partName, sections) ->
                        LawChapter(
                            id = "chap_cpc_${partName.replace(" ", "_").replace(":", "").lowercase()}",
                            number = partName.substringBefore(":"),
                            title = partName,
                            description = "Provisions under $partName.",
                            sections = sections
                        )
                    }

                    val act = LawAct(
                        id = "act_cpc_1908",
                        type = "CENTRAL_ACT",
                        actNumber = "Act No. 5 of 1908",
                        name = "Code of Civil Procedure, 1908",
                        shortTitle = "Code of Civil Procedure, 1908",
                        longTitle = "An Act to consolidate and amend the laws relating to the procedure of the Courts of Civil Judicature.",
                        description = "Comprehensive statute containing all 158 Sections regulating civil court procedure, suits, plaints, execution, appeals, and revision.",
                        year = 1908,
                        enactmentDate = "21-03-1908",
                        enforcementDate = "01-01-1909",
                        ministry = "Ministry of Law and Justice",
                        department = "Department of Legal Affairs",
                        category = "Civil Law",
                        status = "IN_FORCE",
                        source = "India Code (indiacode.nic.in)",
                        chapters = chaptersList
                    )

                    actCache["act_cpc_1908"] = act
                    return act
                }
            }
        } catch (e: Exception) {
            Log.e("CENTRAL_ACT_ERROR", "Error loading cpc.json", e)
            return LawAct(id = "act_cpc_1908", name = "Code of Civil Procedure, 1908")
        }
    }

    private fun determineCpcPart(secNum: Int): String {
        return when {
            secNum in 1..8 -> "Preliminary"
            secNum in 9..35 -> "Part I: Suits in General"
            secNum in 36..74 -> "Part II: Execution"
            secNum in 75..78 -> "Part III: Incidental Proceedings"
            secNum in 79..88 -> "Part IV: Suits in Particular Cases"
            secNum in 89..93 -> "Part V: Special Proceedings"
            secNum in 94..95 -> "Part VI: Supplemental Proceedings"
            secNum in 96..112 -> "Part VII: Appeals"
            secNum in 113..115 -> "Part VIII: Reference, Review and Revision"
            secNum in 116..120 -> "Part IX: Special Provisions relating to High Courts"
            secNum in 121..131 -> "Part X: Rules"
            else -> "Part XI: Miscellaneous"
        }
    }

    // 8. MOTOR VEHICLES ACT, 1988 (217 Sections)
    private fun loadMvaAct(): LawAct {
        actCache["act_mva_1988"]?.let { return it }

        try {
            context.assets.open("MVA.json").use { stream ->
                InputStreamReader(stream).use { reader ->
                    val array = gson.fromJson(reader, JsonArray::class.java)
                    val sections = mutableListOf<LawSection>()

                    for (i in 0 until array.size()) {
                        val obj = array.get(i).asJsonObject
                        val secNum = obj.get("section")?.asString ?: "${i + 1}"
                        val title = obj.get("title")?.asString ?: "Section $secNum"
                        val desc = obj.get("description")?.asString ?: ""

                        val section = LawSection(
                            id = "mva_sec_$secNum",
                            number = "Section $secNum (MVA)",
                            title = title,
                            text = desc,
                            shortMeaning = desc.take(250).let { if (desc.length > 250) "$it..." else it },
                            detailedExplanation = desc,
                            keyPoints = listOf(
                                "Governs motor vehicle licensing, registration, and road safety.",
                                "Enforced under the Ministry of Road Transport and Highways."
                            ),
                            status = "IN_FORCE"
                        )
                        sections.add(section)
                    }

                    val chapter = LawChapter(
                        id = "chap_mva_all",
                        number = "Chapters I - XIV",
                        title = "Motor Vehicles Act Provisions",
                        description = "Licensing, registration, transport vehicles, traffic safety, and accident claims tribunals.",
                        sections = sections
                    )

                    val act = LawAct(
                        id = "act_mva_1988",
                        type = "CENTRAL_ACT",
                        actNumber = "Act No. 59 of 1988",
                        name = "Motor Vehicles Act, 1988",
                        shortTitle = "Motor Vehicles Act, 1988",
                        longTitle = "An Act to consolidate and amend the law relating to motor vehicles.",
                        description = "Comprehensive statute containing all 217 Sections regulating road transport, vehicle safety, insurance, and compensation.",
                        year = 1988,
                        enactmentDate = "14-10-1988",
                        enforcementDate = "01-07-1989",
                        ministry = "Ministry of Road Transport and Highways",
                        department = "Road Transport Division",
                        category = "Motor Vehicles",
                        status = "IN_FORCE",
                        source = "India Code (indiacode.nic.in)",
                        chapters = listOf(chapter)
                    )

                    actCache["act_mva_1988"] = act
                    return act
                }
            }
        } catch (e: Exception) {
            Log.e("CENTRAL_ACT_ERROR", "Error loading MVA.json", e)
            return LawAct(id = "act_mva_1988", name = "Motor Vehicles Act, 1988")
        }
    }

    // 9. INDIAN EVIDENCE ACT, 1872 (Repealed / Historical)
    private fun loadEvidenceAct(): LawAct {
        actCache["act_evidence_1872"]?.let { return it }

        val act = LawAct(
            id = "act_evidence_1872",
            type = "REPEALED_ACT",
            actNumber = "Act No. 1 of 1872",
            name = "Indian Evidence Act, 1872 (Historical / Repealed)",
            shortTitle = "Indian Evidence Act, 1872",
            longTitle = "An Act to consolidate, define and amend the law of Evidence (Repealed by Bharatiya Sakshya Adhiniyam, 2023 with effect from 1 July 2024).",
            description = "Former statutory law of evidence. Repealed and superseded by the Bharatiya Sakshya Adhiniyam, 2023.",
            year = 1872,
            enactmentDate = "15-03-1872",
            enforcementDate = "01-09-1872",
            ministry = "Ministry of Law and Justice",
            department = "Historical Archives",
            category = "Historical Criminal Laws",
            status = "REPEALED",
            source = "India Code (Repealed Acts Archive)",
            chapters = listOf(
                LawChapter(
                    id = "chap_evidence_relevancy",
                    number = "Part I",
                    title = "Relevancy of Facts",
                    description = "Statutory principles governing relevant facts, admissions, and confessions.",
                    sections = listOf(
                        LawSection(
                            id = "evidence_sec_3",
                            number = "Section 3",
                            title = "Interpretation Clause",
                            text = "Defines 'Court', 'Fact', 'Relevant', 'Facts in issue', 'Document', 'Evidence', 'Proved', 'Disproved', and 'Not proved'.",
                            shortMeaning = "Foundational definitions for evidentiary interpretation in Indian courts.",
                            detailedExplanation = "Establishes standards of proof including preponderance of probability and beyond reasonable doubt.",
                            keyPoints = listOf("Defines core legal standards of proof", "Superseded by Section 2 BSA 2023"),
                            status = "REPEALED"
                        ),
                        LawSection(
                            id = "evidence_sec_25",
                            number = "Section 25",
                            title = "Confession to Police Officer Not to be Proved",
                            text = "No confession made to a police officer shall be proved as against a person accused of any offence.",
                            shortMeaning = "Inadmissibility of police confessions to protect against custodial coercion.",
                            detailedExplanation = "Fundamental safeguard preventing involuntary or coerced confessions made while in police custody.",
                            keyPoints = listOf("Absolute statutory bar on police confessions", "Re-enacted in BSA 2023"),
                            status = "REPEALED"
                        )
                    )
                ),
                LawChapter(
                    id = "chap_evidence_proof",
                    number = "Part II",
                    title = "On Proof & Electronic Evidence",
                    description = "Proof of documents and admissibility of electronic records.",
                    sections = listOf(
                        LawSection(
                            id = "evidence_sec_65b",
                            number = "Section 65B",
                            title = "Admissibility of Electronic Records",
                            text = "Any information contained in an electronic record which is printed on a paper, stored, recorded or copied in optical or magnetic media shall be deemed to be also a document, subject to conditions specified.",
                            shortMeaning = "Historical provision governing conditions and certification for electronic evidence admissibility.",
                            detailedExplanation = "Laid down mandatory certificate requirement under Section 65B(4) (Anvar P.V. v. P.K. Basheer). Superseded by Section 63 BSA 2023.",
                            keyPoints = listOf("Mandatory 65B certificate rule", "Superseded by BSA 2023 Section 63"),
                            status = "REPEALED"
                        )
                    )
                )
            )
        )
        actCache["act_evidence_1872"] = act
        actCache["act_evidence_1872_repealed"] = act
        return act
    }

    private fun loadCivilAndCommercialActs(mvaAct: LawAct, cpcAct: LawAct): List<LawAct> {
        val list = mutableListOf<LawAct>()

        // 1. Contract Act 1872
        val contractAct = LawAct(
            id = "act_contract_1872",
            type = "CENTRAL_ACT",
            actNumber = "Act No. 9 of 1872",
            name = "Indian Contract Act, 1872",
            shortTitle = "Indian Contract Act, 1872",
            longTitle = "An Act to define and amend certain parts of the law relating to contracts.",
            description = "Foundational commercial statute governing contract formation, free consent, consideration, indemnity, guarantee, bailment, and agency.",
            year = 1872,
            enactmentDate = "25-04-1872",
            enforcementDate = "01-09-1872",
            ministry = "Ministry of Law and Justice",
            department = "Legislative Department",
            category = "Contract Law",
            status = "IN_FORCE",
            source = "India Code (indiacode.nic.in)",
            chapters = listOf(
                LawChapter(
                    id = "chap_contract_formation",
                    number = "Chapter I & II",
                    title = "Formation of Contract & Free Consent",
                    description = "Essential elements of valid agreements and competency to contract.",
                    sections = listOf(
                        LawSection(
                            id = "contract_sec_2",
                            number = "Section 2",
                            title = "Interpretation Clause",
                            text = "Defines proposal, acceptance, promise, promisor, promisee, consideration, agreement, and contract.",
                            shortMeaning = "Core statutory definitions of Indian contract law.",
                            detailedExplanation = "Every promise and set of promises forming the consideration for each other is an agreement; an agreement enforceable by law is a contract.",
                            keyPoints = listOf("Defines proposal and acceptance", "Enforceability is key to contract"),
                            status = "IN_FORCE"
                        ),
                        LawSection(
                            id = "contract_sec_10",
                            number = "Section 10",
                            title = "What agreements are contracts",
                            text = "All agreements are contracts if they are made by the free consent of parties competent to contract, for a lawful consideration and with a lawful object, and are not hereby expressly declared to be void.",
                            shortMeaning = "Defines the essential legal criteria for an agreement to be enforceable as a binding contract in law.",
                            detailedExplanation = "Requires free consent without coercion or fraud, lawful consideration, and lawful object.",
                            keyPoints = listOf("Requires free consent", "Competency of parties", "Lawful consideration"),
                            status = "IN_FORCE"
                        ),
                        LawSection(
                            id = "contract_sec_25",
                            number = "Section 25",
                            title = "Agreement without consideration, void",
                            text = "An agreement made without consideration is void, unless it is in writing and registered, or is a promise to compensate for something done, or is a promise to pay a debt barred by limitation law.",
                            shortMeaning = "General rule 'no consideration, no contract' along with statutory exceptions.",
                            detailedExplanation = "Consideration is essential to validate commercial agreements in Indian law.",
                            keyPoints = listOf("Consideration is mandatory", "Exceptions for natural love & affection and time-barred debts"),
                            status = "IN_FORCE"
                        )
                    )
                ),
                LawChapter(
                    id = "chap_contract_breach_remedies",
                    number = "Chapter VI & VIII",
                    title = "Breach of Contract, Indemnity & Guarantee",
                    description = "Damages, compensation for breach, contracts of indemnity and guarantee.",
                    sections = listOf(
                        LawSection(
                            id = "contract_sec_73",
                            number = "Section 73",
                            title = "Compensation for Loss or Damage Caused by Breach",
                            text = "When a contract has been broken, the party who suffers by such breach is entitled to receive, from the party who has broken the contract, compensation for any loss or damage caused to him thereby, which naturally arose in the usual course of things.",
                            shortMeaning = "Codifies the rule of Hadley v. Baxendale for claiming contractual damages.",
                            detailedExplanation = "Damages are compensatory, not punitive, covering direct and foreseeable losses arising from breach.",
                            keyPoints = listOf("Entitlement to compensation for natural losses", "No compensation for remote losses"),
                            status = "IN_FORCE"
                        ),
                        LawSection(
                            id = "contract_sec_124",
                            number = "Section 124",
                            title = "Contract of Indemnity Defined",
                            text = "A contract by which one party promises to save the other from loss caused to him by the conduct of the promisor himself, or by the conduct of any other person, is called a contract of indemnity.",
                            shortMeaning = "Statutory definition of indemnity obligations in commercial contracts.",
                            detailedExplanation = "Indemnity protects against losses caused by human agency.",
                            keyPoints = listOf("Promisor promises to save promisee from loss", "Covers conduct of promisor or third parties"),
                            status = "IN_FORCE"
                        )
                    )
                )
            )
        )
        list.add(contractAct)

        // 2. Companies Act 2013
        val companiesAct = LawAct(
            id = "act_companies_2013",
            type = "CENTRAL_ACT",
            actNumber = "Act No. 18 of 2013",
            name = "Companies Act, 2013",
            shortTitle = "Companies Act, 2013",
            longTitle = "An Act to consolidate and amend the law relating to companies.",
            description = "Primary statute governing incorporation, corporate governance, directors' duties, CSR, and audit compliance of companies.",
            year = 2013,
            enactmentDate = "29-08-2013",
            enforcementDate = "12-09-2013",
            ministry = "Ministry of Corporate Affairs",
            department = "Department of Corporate Affairs",
            category = "Corporate Law",
            status = "IN_FORCE",
            source = "India Code (indiacode.nic.in)",
            chapters = listOf(
                LawChapter(
                    id = "chap_companies_governance",
                    number = "Chapter IX & XI",
                    title = "Corporate Governance, CSR & Directors' Duties",
                    description = "Corporate accounts, social responsibility mandates, and fiduciary duties of directors.",
                    sections = listOf(
                        LawSection(
                            id = "companies_sec_135",
                            number = "Section 135",
                            title = "Corporate Social Responsibility (CSR)",
                            text = "Every qualifying company shall spend, in every financial year, at least two per cent of the average net profits of the company made during the three immediately preceding financial years on Corporate Social Responsibility.",
                            shortMeaning = "Mandates qualifying corporate entities to allocate at least 2% of average net profits towards statutory CSR activities.",
                            detailedExplanation = "Applies to companies meeting net worth, turnover, or net profit criteria under Section 135(1).",
                            keyPoints = listOf("Mandatory 2% net profit spending on CSR", "Requires Board CSR Committee"),
                            status = "IN_FORCE"
                        ),
                        LawSection(
                            id = "companies_sec_166",
                            number = "Section 166",
                            title = "Duties of Directors",
                            text = "A director of a company shall act in good faith in order to promote the objects of the company for the benefit of its members as a whole, and in the best interests of the company, its employees, the shareholders, the community and for the protection of environment.",
                            shortMeaning = "Codifies statutory fiduciary duties and standard of care expected from directors.",
                            detailedExplanation = "Prohibits conflicts of interest and unauthorized personal gains by directors.",
                            keyPoints = listOf("Duty of good faith and reasonable care", "Prohibits secret profit or conflict of interest"),
                            status = "IN_FORCE"
                        )
                    )
                )
            )
        )
        list.add(companiesAct)

        // 3. Consumer Protection Act 2019
        val cpaAct = LawAct(
            id = "act_cpa_2019",
            type = "CENTRAL_ACT",
            actNumber = "Act No. 35 of 2019",
            name = "Consumer Protection Act, 2019",
            shortTitle = "Consumer Protection Act, 2019",
            longTitle = "An Act to provide for protection of the interests of consumers and establish authorities for timely and effective settlement of consumers' disputes.",
            description = "Governs consumer rights, product liability, CCPA, e-commerce protections, and dispute redressal commissions.",
            year = 2019,
            enactmentDate = "09-08-2019",
            enforcementDate = "20-07-2020",
            ministry = "Ministry of Consumer Affairs, Food and Public Distribution",
            department = "Department of Consumer Affairs",
            category = "Consumer Protection",
            status = "IN_FORCE",
            source = "India Code (indiacode.nic.in)",
            chapters = listOf(
                LawChapter(
                    id = "chap_cpa_rights_and_commissions",
                    number = "Chapter I & IV",
                    title = "Consumer Rights & Redressal Commissions",
                    description = "Statutory rights guaranteed to buyers and three-tier dispute commissions.",
                    sections = listOf(
                        LawSection(
                            id = "cpa_sec_2_9",
                            number = "Section 2(9)",
                            title = "Consumer Rights",
                            text = "Consumer rights includes the right to be protected against hazardous goods, informed about quality, assured access to competitive prices, and right to seek redressal against unfair trade practices.",
                            shortMeaning = "Statutory guarantee of six fundamental consumer rights in India.",
                            detailedExplanation = "Covers product liability, unfair trade practices, and e-commerce platforms.",
                            keyPoints = listOf("Right to safety & information", "Strict product liability"),
                            status = "IN_FORCE"
                        ),
                        LawSection(
                            id = "cpa_sec_28",
                            number = "Section 28",
                            title = "Establishment of District Consumer Disputes Redressal Commission",
                            text = "The State Government shall, by notification, establish a District Consumer Disputes Redressal Commission in each district of the State.",
                            shortMeaning = "Provides accessible local consumer dispute resolution forum for claims up to Rs. 50 Lakhs.",
                            detailedExplanation = "Empowered to order refund, replacement, damages, and punitive costs.",
                            keyPoints = listOf("District-level accessible dispute forum", "Time-bound summary proceedings"),
                            status = "IN_FORCE"
                        )
                    )
                )
            )
        )
        list.add(cpaAct)

        // 4. Information Technology Act 2000
        val itAct = LawAct(
            id = "act_it_2000",
            type = "CENTRAL_ACT",
            actNumber = "Act No. 21 of 2000",
            name = "Information Technology Act, 2000",
            shortTitle = "Information Technology Act, 2000",
            longTitle = "An Act to provide legal recognition for electronic transactions and e-commerce.",
            description = "Regulates cyber offenses, electronic signatures, digital transactions, intermediaries, and computer security.",
            year = 2000,
            enactmentDate = "09-06-2000",
            enforcementDate = "17-10-2000",
            ministry = "Ministry of Electronics and Information Technology",
            department = "Cyber Law Division",
            category = "Cyber Law",
            status = "IN_FORCE",
            source = "India Code (indiacode.nic.in)",
            chapters = listOf(
                LawChapter(
                    id = "chap_it_offenses_and_intermediaries",
                    number = "Chapter XI & XII",
                    title = "Cyber Offenses & Intermediary Liability",
                    description = "Electronic fraud, hacking, identity theft, and safe harbor for platforms.",
                    sections = listOf(
                        LawSection(
                            id = "it_sec_66c",
                            number = "Section 66C",
                            title = "Punishment for Identity Theft",
                            text = "Whoever, fraudulently or dishonestly make use of the electronic signature, password or any other unique identification feature of any other person, shall be punished with imprisonment up to three years and fine.",
                            shortMeaning = "Penalizes dishonest use of passwords, OTPs, digital signatures, or biometric identifiers.",
                            detailedExplanation = "Addresses digital credential theft, phishing, and impersonation.",
                            keyPoints = listOf("Imprisonment up to 3 years and fine", "Cognizable and bailable"),
                            status = "IN_FORCE"
                        ),
                        LawSection(
                            id = "it_sec_79",
                            number = "Section 79",
                            title = "Exemption from Liability of Intermediary (Safe Harbor)",
                            text = "An intermediary shall not be liable for any third party information, data, or communication link made available or hosted by him provided the intermediary exercises due diligence.",
                            shortMeaning = "Safe harbor protection for digital platforms, social media, and telecom operators.",
                            detailedExplanation = "Protects intermediaries that act as passive conduits and comply with takedown rules upon actual knowledge (Shreya Singhal v. UOI).",
                            keyPoints = listOf("Statutory safe harbor for platforms", "Requires observance of IT due diligence rules"),
                            status = "IN_FORCE"
                        )
                    )
                )
            )
        )
        list.add(itAct)

        // 5. Digital Personal Data Protection Act, 2023
        val dpdpAct = LawAct(
            id = "act_dpdp_2023",
            type = "CENTRAL_ACT",
            actNumber = "Act No. 22 of 2023",
            name = "Digital Personal Data Protection Act, 2023",
            shortTitle = "Digital Personal Data Protection Act, 2023",
            longTitle = "An Act to provide for the processing of digital personal data in a manner that recognises both the right of individuals to protect their personal data and the need to process such personal data for lawful purposes.",
            description = "Comprehensive statute establishing personal data privacy obligations for Data Fiduciaries and rights of Data Principals.",
            year = 2023,
            enactmentDate = "11-08-2023",
            enforcementDate = "Notified in phases",
            ministry = "Ministry of Electronics and Information Technology",
            department = "Data Protection Board Division",
            category = "Digital/Data Protection",
            status = "IN_FORCE",
            source = "India Code (indiacode.nic.in)",
            chapters = listOf(
                LawChapter(
                    id = "chap_dpdp_obligations",
                    number = "Chapter II",
                    title = "Obligations of Data Fiduciary",
                    description = "Consent requirements, notice mandates, and data security obligations.",
                    sections = listOf(
                        LawSection(
                            id = "dpdp_sec_4",
                            number = "Section 4",
                            title = "Grounds for Processing Digital Personal Data",
                            text = "A person may process the digital personal data of an individual only in accordance with the provisions of this Act and for a lawful purpose for which the Data Principal has given consent or for certain legitimate uses.",
                            shortMeaning = "Processing requires valid consent or specified statutory legitimate uses.",
                            detailedExplanation = "Mandates clear notice before seeking consent and requires purpose limitation.",
                            keyPoints = listOf("Consent-based data processing", "Strict purpose limitation"),
                            status = "IN_FORCE"
                        ),
                        LawSection(
                            id = "dpdp_sec_8",
                            number = "Section 8",
                            title = "General Obligations of Data Fiduciary",
                            text = "A Data Fiduciary shall implement appropriate technical and organisational measures to ensure compliance, protect personal data against breach, and erase data when purpose is served.",
                            shortMeaning = "Mandatory data security safeguards, breach notification, and data erasure obligations.",
                            detailedExplanation = "Requires fiduciaries to notify the Data Protection Board and affected users in case of personal data breach.",
                            keyPoints = listOf("Mandatory breach notification", "Data erasure upon withdrawal of consent"),
                            status = "IN_FORCE"
                        )
                    )
                ),
                LawChapter(
                    id = "chap_dpdp_rights",
                    number = "Chapter III",
                    title = "Rights & Duties of Data Principal",
                    description = "Rights of citizens to access, correct, erase personal data and seek grievance redressal.",
                    sections = listOf(
                        LawSection(
                            id = "dpdp_sec_11",
                            number = "Section 11",
                            title = "Right to Access Information About Personal Data",
                            text = "The Data Principal shall have the right to obtain from the Data Fiduciary a summary of personal data being processed and the identities of third parties with whom data is shared.",
                            shortMeaning = "Guarantees citizen transparency regarding who holds and processes their personal information.",
                            detailedExplanation = "Enables individuals to track data flows and ensure compliance.",
                            keyPoints = listOf("Right to summary of processed data", "Right to know third-party recipients"),
                            status = "IN_FORCE"
                        ),
                        LawSection(
                            id = "dpdp_sec_12",
                            number = "Section 12",
                            title = "Right to Correction and Erasure of Personal Data",
                            text = "A Data Principal shall have the right to correction, completion, updating and erasure of personal data for the processing of which he had previously given consent.",
                            shortMeaning = "Statutory 'right to be forgotten' and right to rectify inaccurate records.",
                            detailedExplanation = "Data fiduciaries must delete records once retention is no longer necessary for legal or business purposes.",
                            keyPoints = listOf("Right to correction and updating", "Right to erasure upon request"),
                            status = "IN_FORCE"
                        )
                    )
                )
            )
        )
        list.add(dpdpAct)

        // 6. Insolvency and Bankruptcy Code, 2016
        val ibcAct = LawAct(
            id = "act_ibc_2016",
            type = "CENTRAL_ACT",
            actNumber = "Act No. 31 of 2016",
            name = "Insolvency and Bankruptcy Code, 2016",
            shortTitle = "Insolvency and Bankruptcy Code, 2016",
            longTitle = "An Act to consolidate and amend the laws relating to reorganisation and insolvency resolution of corporate persons, partnership firms and individuals.",
            description = "Time-bound framework for corporate insolvency resolution process (CIRP) and liquidation before the NCLT.",
            year = 2016,
            enactmentDate = "28-05-2016",
            enforcementDate = "05-08-2016",
            ministry = "Ministry of Corporate Affairs",
            department = "Insolvency Section",
            category = "Insolvency & Bankruptcy",
            status = "IN_FORCE",
            source = "India Code (indiacode.nic.in)",
            chapters = listOf(
                LawChapter(
                    id = "chap_ibc_cirp",
                    number = "Part II: Chapter II",
                    title = "Corporate Insolvency Resolution Process (CIRP)",
                    description = "Initiation of CIRP by financial/operational creditors and moratorium.",
                    sections = listOf(
                        LawSection(
                            id = "ibc_sec_7",
                            number = "Section 7",
                            title = "Initiation of CIRP by Financial Creditor",
                            text = "A financial creditor either by itself or jointly with other financial creditors may file an application for initiating corporate insolvency resolution process against a corporate debtor before the Adjudicating Authority when a default has occurred.",
                            shortMeaning = "Enables banks and financial institutions to initiate time-bound insolvency resolution before NCLT.",
                            detailedExplanation = "NCLT must ascertain default within 14 days and admit the application upon satisfaction.",
                            keyPoints = listOf("Initiation upon default of threshold amount", "Triggered before NCLT"),
                            status = "IN_FORCE"
                        ),
                        LawSection(
                            id = "ibc_sec_14",
                            number = "Section 14",
                            title = "Moratorium",
                            text = "On the insolvency commencement date, the Adjudicating Authority shall by order declare moratorium prohibiting the institution of suits, transfer of assets, and foreclosure of security interest against the corporate debtor.",
                            shortMeaning = "Statutory shield halting all lawsuits, recoveries, and asset foreclosures during CIRP.",
                            detailedExplanation = "Provides breathing space to the corporate debtor to enable viable revival and resolution.",
                            keyPoints = listOf("Halts all debt recovery and litigation", "Applies throughout the CIRP period"),
                            status = "IN_FORCE"
                        )
                    )
                )
            )
        )
        list.add(ibcAct)

        // 7. Arbitration and Conciliation Act, 1996
        val arbitrationAct = LawAct(
            id = "act_arbitration_1996",
            type = "CENTRAL_ACT",
            actNumber = "Act No. 26 of 1996",
            name = "Arbitration and Conciliation Act, 1996",
            shortTitle = "Arbitration and Conciliation Act, 1996",
            longTitle = "An Act to consolidate and amend the law relating to domestic arbitration, international commercial arbitration and enforcement of foreign arbitral awards.",
            description = "Model UNCITRAL-based statute regulating alternative dispute resolution, arbitral tribunals, interim relief, and arbitral award enforcement.",
            year = 1996,
            enactmentDate = "16-08-1996",
            enforcementDate = "22-08-1996",
            ministry = "Ministry of Law and Justice",
            department = "Department of Legal Affairs",
            category = "Arbitration & Commercial Law",
            status = "IN_FORCE",
            source = "India Code (indiacode.nic.in)",
            chapters = listOf(
                LawChapter(
                    id = "chap_arbitration_general",
                    number = "Part I",
                    title = "Arbitration Agreement & Judicial Intervention",
                    description = "Validity of arbitration clauses, reference to arbitration, and setting aside awards.",
                    sections = listOf(
                        LawSection(
                            id = "arbitration_sec_7",
                            number = "Section 7",
                            title = "Arbitration Agreement",
                            text = "An 'arbitration agreement' means an agreement by the parties to submit to arbitration all or certain disputes which have arisen or which may arise between them in respect of a defined legal relationship, whether contractual or not.",
                            shortMeaning = "Statutory definition of valid arbitration agreements in Indian commercial practice.",
                            detailedExplanation = "Must be in writing, including electronic communications, emails, or contract reference clauses.",
                            keyPoints = listOf("Must be in writing", "Can be in the form of an arbitration clause or separate agreement"),
                            status = "IN_FORCE"
                        ),
                        LawSection(
                            id = "arbitration_sec_34",
                            number = "Section 34",
                            title = "Application for Setting Aside Arbitral Award",
                            text = "Recourse to a Court against an arbitral award may be made only by an application for setting aside such award in accordance with sub-section (2) and sub-section (3).",
                            shortMeaning = "Restricted statutory grounds for challenging arbitral awards (patent illegality, public policy violation).",
                            detailedExplanation = "Courts cannot review awards on merit or reappreciate evidence; challenge is limited to statutory grounds.",
                            keyPoints = listOf("Strict time limit of 3 months (+30 days)", "Limited non-merit judicial review"),
                            status = "IN_FORCE"
                        )
                    )
                )
            )
        )
        list.add(arbitrationAct)

        // 8. Prevention of Money Laundering Act, 2002
        val pmlaAct = LawAct(
            id = "act_pmla_2002",
            type = "CENTRAL_ACT",
            actNumber = "Act No. 15 of 2003",
            name = "Prevention of Money Laundering Act, 2002",
            shortTitle = "Prevention of Money Laundering Act, 2002",
            longTitle = "An Act to prevent money-laundering and to provide for confiscation of property derived from, or involved in, money-laundering.",
            description = "Statute penalizing money laundering, tracing proceeds of crime, attachment of illicit assets, and ED special court proceedings.",
            year = 2002,
            enactmentDate = "17-01-2003",
            enforcementDate = "01-07-2005",
            ministry = "Ministry of Finance",
            department = "Department of Revenue (Enforcement Directorate)",
            category = "Anti-Corruption & Financial Crime",
            status = "IN_FORCE",
            source = "India Code (indiacode.nic.in)",
            chapters = listOf(
                LawChapter(
                    id = "chap_pmla_offences",
                    number = "Chapter II & III",
                    title = "Offence of Money-Laundering & Attachment of Property",
                    description = "Statutory definition of money laundering, provisional attachment, and powers of Enforcement Directorate.",
                    sections = listOf(
                        LawSection(
                            id = "pmla_sec_3",
                            number = "Section 3",
                            title = "Offence of Money-Laundering",
                            text = "Whosoever directly or indirectly attempts to indulge or knowingly assists or knowingly is a party or is actually involved in any process or activity connected with the proceeds of crime including its concealment, possession, acquisition or use and projecting or claiming it as untainted property shall be guilty of offence of money-laundering.",
                            shortMeaning = "Wide statutory definition penalizing involvement with proceeds of scheduled crimes.",
                            detailedExplanation = "Money laundering is a continuing offense as long as a person enjoys or projects illicit proceeds.",
                            keyPoints = listOf("Covers concealment, possession, and acquisition of crime proceeds", "Rigorous imprisonment up to 7 years (10 years for NDPS)"),
                            status = "IN_FORCE"
                        ),
                        LawSection(
                            id = "pmla_sec_5",
                            number = "Section 5",
                            title = "Attachment of Property Involved in Money-Laundering",
                            text = "Where the Director or any officer has reason to believe on the basis of material in his possession that any person is in possession of proceeds of crime, he may by order in writing provisionally attach such property for a period not exceeding one hundred and eighty days.",
                            shortMeaning = "Power of Enforcement Directorate to freeze and provisionally attach illicit crime assets.",
                            detailedExplanation = "Provisional attachment requires confirmation by the PMLA Adjudicating Authority within 180 days.",
                            keyPoints = listOf("Provisional attachment up to 180 days", "Subject to confirmation by Adjudicating Authority"),
                            status = "IN_FORCE"
                        )
                    )
                )
            )
        )
        list.add(pmlaAct)

        // 9. Code of Civil Procedure 1908 (Full 158 Sections)
        list.add(cpcAct)

        // 10. Motor Vehicles Act 1988 (Full 217 Sections)
        list.add(mvaAct)

        return list
    }

    suspend fun getCentralActsCatalogue(): List<LawAct> = withContext(Dispatchers.IO) {
        cachedCentralCatalogue?.let { return@withContext it }
        try {
            context.assets.open("india_code/central_acts_catalogue.json").use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    val type = object : TypeToken<List<LawAct>>() {}.type
                    val list: List<LawAct> = gson.fromJson(reader, type)
                    cachedCentralCatalogue = list
                    list
                }
            }
        } catch (e: Exception) {
            Log.e("CENTRAL_ACT_ERROR", "Error loading central_acts_catalogue.json", e)
            emptyList()
        }
    }

    suspend fun getStateUtJurisdictions(): List<StateOrUtJurisdiction> = withContext(Dispatchers.IO) {
        cachedJurisdictions?.let { return@withContext it }
        try {
            context.assets.open("india_code/state_ut_jurisdictions.json").use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    val type = object : TypeToken<List<StateOrUtJurisdiction>>() {}.type
                    val list: List<StateOrUtJurisdiction> = gson.fromJson(reader, type)
                    cachedJurisdictions = list
                    list
                }
            }
        } catch (e: Exception) {
            Log.e("CENTRAL_ACT_ERROR", "Error loading state_ut_jurisdictions.json", e)
            emptyList()
        }
    }

    suspend fun getCategoryById(categoryId: String): EncyclopediaCategory? {
        return getCategories().find { it.id.equals(categoryId, ignoreCase = true) }
    }

    suspend fun findCategoryForLaw(lawId: String): EncyclopediaCategory? {
        val normId = normalizeActId(lawId)
        val categories = getCategories()
        return categories.find { cat ->
            cat.laws.any { normalizeActId(it.id) == normId }
        }
    }

    suspend fun getLawById(categoryId: String, lawId: String): LawAct? {
        val normId = normalizeActId(lawId)
        Log.d("CENTRAL_ACT_DEBUG", "ACT_LOAD: getLawById requested categoryId='$categoryId', rawLawId='$lawId', normalized='$normId'")

        val categories = getCategories()

        // 1. Search within specified category if provided
        if (categoryId.isNotBlank()) {
            val cat = categories.find { it.id.equals(categoryId, ignoreCase = true) }
            val law = cat?.laws?.find { normalizeActId(it.id) == normId }
            if (law != null && law.chapters.isNotEmpty()) {
                Log.d("CENTRAL_ACT_DEBUG", "ACT_LOAD: Found full law '${law.shortTitle.ifBlank { law.name }}' with ${law.chapters.size} chapters in category '$categoryId'")
                return law
            }
        }

        // 2. Search all categories for a law with chapters
        for (cat in categories) {
            val law = cat.laws.find { normalizeActId(it.id) == normId }
            if (law != null && law.chapters.isNotEmpty()) {
                Log.d("CENTRAL_ACT_DEBUG", "ACT_LOAD: Found full law '${law.shortTitle.ifBlank { law.name }}' with ${law.chapters.size} chapters in category '${cat.id}'")
                return law
            }
        }

        // 3. Search in actCache
        actCache[normId]?.let {
            if (it.chapters.isNotEmpty()) {
                Log.d("CENTRAL_ACT_DEBUG", "ACT_LOAD: Found cached law '${it.shortTitle.ifBlank { it.name }}' with ${it.chapters.size} chapters")
                return it
            }
        }
        actCache[lawId]?.let {
            if (it.chapters.isNotEmpty()) {
                Log.d("CENTRAL_ACT_DEBUG", "ACT_LOAD: Found cached law '${it.shortTitle.ifBlank { it.name }}' with ${it.chapters.size} chapters")
                return it
            }
        }

        // 4. Fallback to catalogue item
        val catalogueItem = getCentralActsCatalogue().find { normalizeActId(it.id) == normId }
        if (catalogueItem != null) {
            Log.w("CENTRAL_ACT_DEBUG", "ACT_LOAD: Returning catalogue item for '$lawId' (chapters count: ${catalogueItem.chapters.size})")
            return catalogueItem
        }

        Log.e("CENTRAL_ACT_ERROR", "ACT_LOAD: Law not found for id '$lawId'")
        return null
    }

    suspend fun getChapterById(categoryId: String, lawId: String, chapterId: String): LawChapter? {
        Log.d("CENTRAL_ACT_DEBUG", "CHAPTER_LOAD: getChapterById lawId=$lawId, chapterId=$chapterId")
        val law = getLawById(categoryId, lawId) ?: run {
            Log.e("CENTRAL_ACT_ERROR", "CHAPTER_LOAD: Could not find law $lawId to load chapter $chapterId")
            return null
        }
        val chapter = law.chapters.find { it.id.equals(chapterId, ignoreCase = true) }
        if (chapter == null) {
            Log.e("CENTRAL_ACT_ERROR", "CHAPTER_LOAD: Chapter $chapterId not found in law ${law.name}")
        } else {
            Log.d("CENTRAL_ACT_DEBUG", "CHAPTER_LOAD: Found chapter '${chapter.title}' with ${chapter.sections.size} sections")
        }
        return chapter
    }

    suspend fun getSectionById(sectionId: String): Pair<SectionNavigation, LawSection>? {
        val categories = getCategories()
        for (category in categories) {
            for (law in category.laws) {
                for (chapter in law.chapters) {
                    val sec = chapter.sections.find { it.id.equals(sectionId, ignoreCase = true) }
                    if (sec != null) {
                        val nav = getSectionNavigation(category.id, law.id, chapter.id, sec.id) ?: continue
                        return Pair(nav, sec)
                    }
                }
            }
        }
        Log.e("CENTRAL_ACT_ERROR", "SECTION_LOAD: getSectionById found no section for sectionId=$sectionId")
        return null
    }

    suspend fun getSectionNavigation(
        categoryId: String,
        lawId: String,
        chapterId: String,
        sectionId: String
    ): SectionNavigation? {
        Log.d("CENTRAL_ACT_DEBUG", "SECTION_LOAD: getSectionNavigation categoryId=$categoryId, lawId=$lawId, chapterId=$chapterId, sectionId=$sectionId")
        val law = getLawById(categoryId, lawId) ?: run {
            Log.e("CENTRAL_ACT_ERROR", "SECTION_LOAD: Law $lawId not found for section $sectionId")
            return null
        }
        val cat = if (categoryId.isNotBlank()) {
            getCategoryById(categoryId)
        } else {
            findCategoryForLaw(law.id)
        } ?: EncyclopediaCategory(id = "cat_central", name = law.category.ifBlank { "Central Acts" })

        val chapter = law.chapters.find { it.id.equals(chapterId, ignoreCase = true) } ?: run {
            Log.e("CENTRAL_ACT_ERROR", "SECTION_LOAD: Chapter $chapterId not found in law ${law.name}")
            return null
        }

        val allSectionsInChapter = chapter.sections
        val currentIndex = allSectionsInChapter.indexOfFirst { it.id.equals(sectionId, ignoreCase = true) }
        if (currentIndex == -1) {
            Log.e("CENTRAL_ACT_ERROR", "SECTION_LOAD: Section $sectionId not found in chapter ${chapter.title}")
            return null
        }

        val currentSection = allSectionsInChapter[currentIndex]
        val prevSection = if (currentIndex > 0) allSectionsInChapter[currentIndex - 1] else null
        val nextSection = if (currentIndex < allSectionsInChapter.size - 1) allSectionsInChapter[currentIndex + 1] else null

        Log.d("CENTRAL_ACT_DEBUG", "SECTION_LOAD: Success navigating section ${currentSection.number} (${currentSection.title})")
        return SectionNavigation(
            currentSection = currentSection,
            categoryId = cat.id,
            categoryName = cat.name,
            lawId = law.id,
            lawName = law.name.ifBlank { law.shortTitle },
            lawStatus = law.status,
            chapterId = chapter.id,
            chapterTitle = chapter.title,
            prevSection = prevSection,
            nextSection = nextSection
        )
    }

    suspend fun searchSections(query: String): List<EncyclopediaSearchResult> {
        val trimmed = query.trim().lowercase()
        if (trimmed.isBlank()) return emptyList()

        val results = mutableListOf<EncyclopediaSearchResult>()
        val categories = getCategories()

        for (category in categories) {
            for (law in category.laws) {
                for (chapter in law.chapters) {
                    for (section in chapter.sections) {
                        val matches = section.number.lowercase().contains(trimmed) ||
                                section.title.lowercase().contains(trimmed) ||
                                section.text.lowercase().contains(trimmed) ||
                                section.shortMeaning.lowercase().contains(trimmed) ||
                                section.detailedExplanation.lowercase().contains(trimmed) ||
                                section.keyPoints.any { it.lowercase().contains(trimmed) } ||
                                section.relatedProvisions.any { it.lowercase().contains(trimmed) } ||
                                law.name.lowercase().contains(trimmed) ||
                                law.shortTitle.lowercase().contains(trimmed) ||
                                law.actNumber.lowercase().contains(trimmed) ||
                                law.ministry.lowercase().contains(trimmed) ||
                                law.status.lowercase().contains(trimmed) ||
                                chapter.title.lowercase().contains(trimmed) ||
                                category.name.lowercase().contains(trimmed)

                        if (matches) {
                            results.add(
                                EncyclopediaSearchResult(
                                    section = section,
                                    categoryId = category.id,
                                    categoryName = category.name,
                                    lawId = law.id,
                                    lawName = law.name.ifBlank { law.shortTitle },
                                    lawStatus = law.status,
                                    chapterId = chapter.id,
                                    chapterTitle = chapter.title,
                                    ministry = law.ministry,
                                    year = law.year
                                )
                            )
                        }
                    }
                }
            }
        }
        return results
    }

    // Bookmarks & Continue Reading
    fun isSectionBookmarked(sectionId: String): Boolean {
        val bookmarks = prefs.getStringSet("bookmarked_sections", emptySet()) ?: emptySet()
        return bookmarks.contains(sectionId)
    }

    fun toggleSectionBookmark(sectionId: String): Boolean {
        val bookmarks = (prefs.getStringSet("bookmarked_sections", emptySet()) ?: emptySet()).toMutableSet()
        val willBeBookmarked = !bookmarks.contains(sectionId)
        if (willBeBookmarked) {
            bookmarks.add(sectionId)
        } else {
            bookmarks.remove(sectionId)
        }
        prefs.edit().putStringSet("bookmarked_sections", bookmarks).apply()
        return willBeBookmarked
    }

    fun getBookmarkedSectionIds(): Set<String> {
        return prefs.getStringSet("bookmarked_sections", emptySet()) ?: emptySet()
    }

    fun saveLastReadSection(sectionId: String, number: String, title: String) {
        prefs.edit()
            .putString("last_read_id", sectionId)
            .putString("last_read_number", number)
            .putString("last_read_title", title)
            .putLong("last_read_timestamp", System.currentTimeMillis())
            .apply()
    }

    fun getLastReadSection(): Triple<String, String, String>? {
        val id = prefs.getString("last_read_id", null) ?: return null
        val number = prefs.getString("last_read_number", "") ?: ""
        val title = prefs.getString("last_read_title", "") ?: ""
        return Triple(id, number, title)
    }
}
