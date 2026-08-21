package com.example.nyayalegalai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nyayalegalai.database.UserAccount
import com.example.nyayalegalai.database.UserAccountDao
import com.example.nyayalegalai.utils.LocalUser
import com.example.nyayalegalai.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val userAccountDao: UserAccountDao,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val user = userAccountDao.login(email, pass)
            if (user != null) {
                val localUser = LocalUser(user.email, user.email, user.name, user.role, user.phone, user.barId)
                sessionManager.saveUser(localUser, pass)
                _authState.value = AuthState.Success(user.role)
            } else {
                _authState.value = AuthState.Error("Invalid email or password")
            }
        }
    }

    fun signup(user: UserAccount) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                userAccountDao.registerUser(user)
                val localUser = LocalUser(user.email, user.email, user.name, user.role, user.phone, user.barId)
                sessionManager.saveUser(localUser, user.password)
                _authState.value = AuthState.Success(user.role)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Signup failed. User might already exist.")
            }
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val role: String) : AuthState()
    data class Error(val message: String) : AuthState()
}
