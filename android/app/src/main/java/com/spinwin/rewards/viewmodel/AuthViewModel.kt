package com.spinwin.rewards.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.spinwin.rewards.data.model.UserProfile
import com.spinwin.rewards.data.repository.RewardsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

enum class AuthState {
    AUTHENTICATED, UNAUTHENTICATED, LOADING
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RewardsRepository.getInstance(application)
    private val authPrefs = application.getSharedPreferences("spinwin_auth_session", Context.MODE_PRIVATE)

    // User is only authenticated if they explicitly completed registration with Name, Phone, and Age
    private val _authState = MutableStateFlow(
        if (authPrefs.getBoolean("is_logged_in", false) &&
            repository.userProfile.value.phone.isNotBlank() &&
            repository.userProfile.value.name.isNotBlank() &&
            repository.userProfile.value.name != "Player"
        ) {
            AuthState.AUTHENTICATED
        } else {
            AuthState.UNAUTHENTICATED
        }
    )
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val activeCountry: StateFlow<com.spinwin.rewards.data.model.CountryInfo> = repository.activeCountry

    val cachedPhone: String get() = repository.userProfile.value.phone
    val cachedAge: String get() = repository.userProfile.value.age

    fun selectCountry(country: com.spinwin.rewards.data.model.CountryInfo) {
        repository.updateCountry(country)
    }

    fun prepareUserSignIn(email: String, name: String, photoUrl: String = "") {
        if (email.isNotBlank()) {
            repository.onUserSignIn(email, name, photoUrl)
        }
    }

    fun saveUserProfileDetails(
        name: String,
        email: String = "",
        photoUrl: String = "",
        phone: String = "",
        age: String = "21",
        countryCode: String? = null,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _authState.value = AuthState.LOADING
            if (email.isNotBlank()) {
                repository.onUserSignIn(email, name, photoUrl)
            }
            repository.updateUserDetails(name, phone, age, countryCode, email)
            authPrefs.edit()
                .putBoolean("is_logged_in", true)
                .putString("logged_in_email", email.trim().lowercase())
                .apply()
            _authState.value = AuthState.AUTHENTICATED
            _errorMessage.value = null
            onSuccess()
        }
    }

    fun handleGoogleSignInError(error: String) {
        _errorMessage.value = error
        _authState.value = AuthState.UNAUTHENTICATED
    }

    fun logout() {
        repository.onLogout()
        authPrefs.edit().clear().apply()
        _authState.value = AuthState.UNAUTHENTICATED
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            GoogleSignIn.getClient(getApplication<Application>() as Context, gso).signOut()
        } catch (_: Exception) {}
    }
}
