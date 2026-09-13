package com.spinwin.rewards.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.spinwin.rewards.data.model.LeaderboardUser
import com.spinwin.rewards.data.model.TransactionRecord
import com.spinwin.rewards.data.model.UserProfile
import com.spinwin.rewards.data.repository.RewardsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RewardsRepository.getInstance(application)

    val userProfile: StateFlow<UserProfile> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    val recentTransactions: StateFlow<List<TransactionRecord>> = repository.transactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isOnline: StateFlow<Boolean> = repository.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val activeCountry: StateFlow<com.spinwin.rewards.data.model.CountryInfo> = repository.activeCountry
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.spinwin.rewards.data.model.CountryRegistry.DEFAULT)

    fun claimDailyCheckIn(onClaimed: (Int) -> Unit) {
        viewModelScope.launch {
            val currentStreak = userProfile.value.streakDays
            val points = currentStreak * 10
            repository.claimDailyCheckIn()
            onClaimed(points)
        }
    }

    fun watchAdReward(onRewarded: (Int) -> Unit) {
        viewModelScope.launch {
            repository.watchAdReward()
            onRewarded(20)
        }
    }

    fun getLeaderboardPreview(): List<LeaderboardUser> {
        return repository.getLeaderboard().take(3)
    }

    fun updateUserName(name: String) {
        viewModelScope.launch {
            repository.updateUserName(name)
        }
    }

    fun updateUserDetails(name: String, phone: String, age: String, countryCode: String? = null) {
        viewModelScope.launch {
            repository.updateUserDetails(name, phone, age, countryCode)
        }
    }

    fun updateCountry(country: com.spinwin.rewards.data.model.CountryInfo) {
        viewModelScope.launch {
            repository.updateCountry(country)
        }
    }
}
