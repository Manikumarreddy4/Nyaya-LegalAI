package com.example.nyayalegalai.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.nyayalegalai.model.ConstitutionArticle
import com.example.nyayalegalai.repository.ConstitutionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ConstitutionViewModel(
    application: Application
) : AndroidViewModel(
    application
) {

    private val repository =
        ConstitutionRepository(
            application
        )

    private val _articles =
        MutableStateFlow<
                List<ConstitutionArticle>
                >(emptyList())

    val articles =
        _articles.asStateFlow()

    init {
        _articles.value =
            repository
                .getAllArticles()
    }

    fun search(
        query: String
    ) {
        _articles.value =
            repository
                .search(query)
    }
}