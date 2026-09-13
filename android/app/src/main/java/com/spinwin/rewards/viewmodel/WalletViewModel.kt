package com.spinwin.rewards.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.spinwin.rewards.data.model.TransactionRecord
import com.spinwin.rewards.data.model.UserProfile
import com.spinwin.rewards.data.repository.RewardsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class WalletTab {
    ALL, EARNED, WITHDRAWN, PENDING
}

class WalletViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RewardsRepository.getInstance(application)

    val userProfile: StateFlow<UserProfile> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    val allTransactions: StateFlow<List<TransactionRecord>> = repository.transactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTab = MutableStateFlow(WalletTab.ALL)
    val selectedTab: StateFlow<WalletTab> = _selectedTab.asStateFlow()

    private val _showWithdrawSheet = MutableStateFlow(false)
    val showWithdrawSheet: StateFlow<Boolean> = _showWithdrawSheet.asStateFlow()

    private val _withdrawStatusMessage = MutableStateFlow<String?>(null)
    val withdrawStatusMessage: StateFlow<String?> = _withdrawStatusMessage.asStateFlow()

    fun selectTab(tab: WalletTab) {
        _selectedTab.value = tab
    }

    fun openWithdrawSheet() {
        _withdrawStatusMessage.value = null
        _showWithdrawSheet.value = true
    }

    fun closeWithdrawSheet() {
        _showWithdrawSheet.value = false
    }

    val activeCountry: StateFlow<com.spinwin.rewards.data.model.CountryInfo> = repository.activeCountry

    fun requestWithdrawal(
        amountRupees: Double,
        payoutId: String,
        method: String = "PayPal",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.submitWithdrawal(amountRupees, payoutId, method)
            val sym = activeCountry.value.currencySymbol
            result.fold(
                onSuccess = {
                    _withdrawStatusMessage.value = "Withdrawal request of $sym${String.format(java.util.Locale.US, "%.2f", amountRupees)} via $method submitted successfully! Processing within 2 hours."
                    onSuccess()
                },
                onFailure = { err ->
                    _withdrawStatusMessage.value = err.message
                    onError(err.message ?: "Failed to process withdrawal")
                }
            )
        }
    }
}
