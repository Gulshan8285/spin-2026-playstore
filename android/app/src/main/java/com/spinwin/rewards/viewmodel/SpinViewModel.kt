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
        if (profile.spinsToday >= profile.maxDailySpins) return false

        // Pick weighted target segment index (simulating server decision)
        // Indices: 0: 10pts, 1: 20pts, 2: 30pts, 3: 50pts, 4: 100pts, 5: 5pts, 6: Try Again, 7: 25pts
        val weights = listOf(25, 20, 15, 10, 5, 15, 5, 5) // Weighted probabilities
        val sumWeights = weights.sum()
        var rand = Random.nextInt(sumWeights)
        var selectedIdx = 0
        for (i in weights.indices) {
            if (rand < weights[i]) {
                selectedIdx = i
                break
            }
            rand -= weights[i]
        }

        _targetSegmentIndex.value = selectedIdx
        _isSpinning.value = true
        return true
    }

    fun onSpinCompleted(segment: WheelSegment) {
        _isSpinning.value = false
        _lastWonSegment.value = segment
        repository.recordSpin(segment.points)
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
