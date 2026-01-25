package com.example.pleb2.ui

import androidx.lifecycle.ViewModel
import com.example.data.settings.AccountRepository
import com.example.data.viewmodel.OnboardingViewModel as SharedOnboardingViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AndroidOnboardingViewModel @Inject constructor(
    private val accountRepository: AccountRepository
) : ViewModel() {
    val shared = SharedOnboardingViewModel(
        accountRepository = accountRepository
    )
}
