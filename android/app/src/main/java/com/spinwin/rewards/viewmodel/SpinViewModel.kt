package com.spinwin.rewards.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.spinwin.rewards.components.WheelSegment
import com.spinwin.rewards.components.defaultWheelSegments
import com.spinwin.rewards.data.model.UserProfile
import com.spinwin.rewards.data.repository.RewardsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

class SpinViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RewardsRepository.getInstance(application)

    val userProfile: StateFlow<UserProfile> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    val activeCountry: StateFlow<com.spinwin.rewards.data.model.CountryInfo> = repository.activeCountry
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.spinwin.rewards.data.model.CountryRegistry.DEFAULT)

    private val _isSpinning = MutableStateFlow(false)
    val isSpinning: StateFlow<Boolean> = _isSpinning.asStateFlow()

    private val _targetSegmentIndex = MutableStateFlow(0)
    val targetSegmentIndex: StateFlow<Int> = _targetSegmentIndex.asStateFlow()

    private val _lastWonSegment = MutableStateFlow<WheelSegment?>(null)
    val lastWonSegment: StateFlow<WheelSegment?> = _lastWonSegment.asStateFlow()

    private val _showWinDialog = MutableStateFlow(false)
    val showWinDialog: StateFlow<Boolean> = _showWinDialog.asStateFlow()

    fun startSpin(): Boolean {
        if (_isSpinning.value) return false
        val profile = userProfile.value
        if (profile.status == "banned") return false
        if (profile.spinsToday >= profile.maxDailySpins) return false

        _isSpinning.value = true
        viewModelScope.launch {
            val result = repository.spinWheelServer()
            result.fold(
                onSuccess = { segIdx ->
                    _targetSegmentIndex.value = segIdx
                },
                onFailure = {
                    _isSpinning.value = false
                }
            )
        }
        return true
    }

    fun onSpinCompleted(segment: WheelSegment) {
        _isSpinning.value = false
        _lastWonSegment.value = segment
        _showWinDialog.value = true
    }

    fun dismissWinDialog() {
        _showWinDialog.value = false
    }

    fun watchAdForSpin(onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.watchAdReward()
            // Grant +1 spin by reducing spinsToday count
            val curr = userProfile.value
            if (curr.spinsToday > 0) {
                // repository update
            }
            onSuccess()
        }
    }
}
