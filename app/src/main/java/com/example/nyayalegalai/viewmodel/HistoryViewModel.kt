package com.example.nyayalegalai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nyayalegalai.database.ChatHistoryMessage
import com.example.nyayalegalai.database.ChatSession
import com.example.nyayalegalai.repository.ChatHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

class HistoryViewModel(private val repository: ChatHistoryRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val allSessions: Flow<List<ChatSession>> = _searchQuery.flatMapLatest { query ->
        if (query.isEmpty()) repository.allSessions
        else repository.searchHistory(query)
    }

    fun getSessionsByType(type: String): Flow<List<ChatSession>> = repository.getSessionsByType(type)

    fun getMessagesForSession(sessionId: Long): Flow<List<ChatHistoryMessage>> =
        repository.getMessagesForSession(sessionId)

    fun getLastMessageForSession(sessionId: Long): Flow<ChatHistoryMessage?> =
        repository.getLastMessageForSession(sessionId)

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
        }
    }

    fun togglePin(sessionId: Long, pinned: Boolean) {
        viewModelScope.launch {
            repository.togglePin(sessionId, pinned)
        }
    }

    fun renameSession(sessionId: Long, newTitle: String) {
        viewModelScope.launch {
            repository.renameSession(sessionId, newTitle)
        }
    }

    fun deleteAllHistory() {
        viewModelScope.launch {
            repository.deleteAllHistory()
        }
    }
}
