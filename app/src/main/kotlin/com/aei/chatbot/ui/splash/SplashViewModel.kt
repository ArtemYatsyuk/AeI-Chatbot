package com.aei.chatbot.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aei.chatbot.domain.usecase.LoadSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    loadSettingsUseCase: LoadSettingsUseCase
) : ViewModel() {
    val isFirstLaunch: Flow<Boolean> = loadSettingsUseCase.settings.map { it.isFirstLaunch }
}
