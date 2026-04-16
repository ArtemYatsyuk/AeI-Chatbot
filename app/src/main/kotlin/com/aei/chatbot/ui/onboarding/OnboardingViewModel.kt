package com.aei.chatbot.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aei.chatbot.domain.usecase.SaveSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val saveSettingsUseCase: SaveSettingsUseCase
) : ViewModel() {
    fun markFirstLaunchDone() {
        viewModelScope.launch {
            saveSettingsUseCase { it.copy(isFirstLaunch = false) }
        }
    }
}
