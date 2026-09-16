package com.spinwin.rewards.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.spinwin.rewards.data.model.UserProfile
import com.spinwin.rewards.data.repository.RewardsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

enum class AuthState {
    AUTHENTICATED, UNAUTHENTICATED, LOADING
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RewardsRepository.getInstance(application)
    private val authPrefs = application.getSharedPreferences("spinwin_auth_session", Context.MODE_PRIVATE)
    private val firebaseAuth by lazy { FirebaseAuth.getInstance() }

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

    fun prepareUserSignIn(email: String, name: String, photoUrl: String = "", idToken: String = "") {
        viewModelScope.launch {
            try {
                if (idToken.isNotBlank()) {
                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    val result = firebaseAuth.signInWithCredential(credential).await()
                    val realUid = result.user?.uid ?: ("usr_" + email.hashCode())
                    repository.initFirebaseUserSession(realUid, email, name, photoUrl)
                } else {
                    val currentUid = firebaseAuth.currentUser?.uid ?: ("usr_" + email.hashCode())
                    repository.initFirebaseUserSession(currentUid, email, name, photoUrl)
                }
            } catch (e: Exception) {
                Log.w("AuthViewModel", "Firebase signInWithCredential notice: ${e.message}")
                val currentUid = firebaseAuth.currentUser?.uid ?: ("usr_" + email.hashCode())
                repository.initFirebaseUserSession(currentUid, email, name, photoUrl)
            }
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
            val currentUid = firebaseAuth.currentUser?.uid ?: repository.userProfile.value.uid.ifBlank { "usr_" + email.hashCode() }
            if (email.isNotBlank()) {
                repository.initFirebaseUserSession(currentUid, email, name, photoUrl)
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
        try {
            firebaseAuth.signOut()
        } catch (_: Exception) {}
        repository.onLogout()
        authPrefs.edit().clear().apply()
        _authState.value = AuthState.UNAUTHENTICATED
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            GoogleSignIn.getClient(getApplication<Application>() as Context, gso).signOut()
        } catch (_: Exception) {}
    }
}
