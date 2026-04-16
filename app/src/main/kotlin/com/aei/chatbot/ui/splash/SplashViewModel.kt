package com.aei.chatbot.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aei.chatbot.domain.usecase.LoadSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    loadSettingsUseCase: LoadSettingsUseCase
) : ViewModel() {
    val isFirstLaunch = loadSettingsUseCase.settings
        .map { it.isFirstLaunch }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
}
