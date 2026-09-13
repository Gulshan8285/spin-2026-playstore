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
    AUTHENTICATED, UNAUTHENTICATED, PHONE_OTP_SENT, LOADING
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RewardsRepository.getInstance(application)
    private val authPrefs = application.getSharedPreferences("spinwin_auth_session", Context.MODE_PRIVATE)

    private val _authState = MutableStateFlow(
        if (authPrefs.getBoolean("is_logged_in", false)) AuthState.AUTHENTICATED else AuthState.UNAUTHENTICATED
    )
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _phoneNumber = MutableStateFlow("")
    val phoneNumber: StateFlow<String> = _phoneNumber.asStateFlow()

    private val _otpCode = MutableStateFlow("")
    val otpCode: StateFlow<String> = _otpCode.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val activeCountry: StateFlow<com.spinwin.rewards.data.model.CountryInfo> = repository.activeCountry

    fun selectCountry(country: com.spinwin.rewards.data.model.CountryInfo) {
        repository.updateCountry(country)
    }

    fun onPhoneNumberChange(phone: String) {
        _phoneNumber.value = phone
    }

    fun onOtpCodeChange(otp: String) {
        _otpCode.value = otp
    }

    fun sendPhoneOtp() {
        if (_phoneNumber.value.length < 6) {
            _errorMessage.value = "Please enter a valid mobile number"
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.LOADING
            _authState.value = AuthState.PHONE_OTP_SENT
            _errorMessage.value = null
        }
    }

    fun verifyOtp(onSuccess: () -> Unit) {
        if (_otpCode.value.length != 6) {
            _errorMessage.value = "Please enter the 6-digit verification code"
            return
        }
        viewModelScope.launch {
            _errorMessage.value = null
            onSuccess()
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
                repository.updateGoogleUser(name, email, photoUrl)
            }
            repository.updateUserDetails(name, phone, age, countryCode)
            authPrefs.edit().putBoolean("is_logged_in", true).apply()
            _authState.value = AuthState.AUTHENTICATED
            _errorMessage.value = null
            onSuccess()
        }
    }

    fun handleGoogleSignInSuccess(
        name: String,
        email: String,
        photoUrl: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _authState.value = AuthState.LOADING
            repository.updateGoogleUser(name, email, photoUrl)
            authPrefs.edit().putBoolean("is_logged_in", true).apply()
            _authState.value = AuthState.AUTHENTICATED
            _errorMessage.value = null
            onSuccess()
        }
    }

    fun handleGoogleSignInError(error: String) {
        _errorMessage.value = error
        _authState.value = AuthState.UNAUTHENTICATED
    }

    fun loginWithGoogle(onSuccess: () -> Unit) {
        viewModelScope.launch {
            authPrefs.edit().putBoolean("is_logged_in", true).apply()
            _authState.value = AuthState.AUTHENTICATED
            onSuccess()
        }
    }

    fun continueAsGuest(onSuccess: () -> Unit) {
        authPrefs.edit().putBoolean("is_logged_in", true).apply()
        _authState.value = AuthState.AUTHENTICATED
        onSuccess()
    }

    fun logout() {
        authPrefs.edit().putBoolean("is_logged_in", false).apply()
        _authState.value = AuthState.UNAUTHENTICATED
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            GoogleSignIn.getClient(getApplication<Application>() as Context, gso).signOut()
        } catch (_: Exception) {}
    }
}
