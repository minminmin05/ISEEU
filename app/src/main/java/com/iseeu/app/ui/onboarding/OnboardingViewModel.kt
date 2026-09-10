package com.iseeu.app.ui.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iseeu.app.data.local.PrefsDataStore
import com.iseeu.app.data.repository.AuthRepository
import com.iseeu.app.data.repository.CreateFamilyResult
import com.iseeu.app.data.repository.FamilyRepository
import com.iseeu.app.data.repository.JoinFamilyResult
import com.iseeu.app.util.FamilyCodeGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface OnboardingUiState {
    data object Idle : OnboardingUiState
    data object Loading : OnboardingUiState
    data class FamilyCreated(val code: String) : OnboardingUiState
    data object Joined : OnboardingUiState
    data class Error(val message: String) : OnboardingUiState
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val familyRepository: FamilyRepository,
    private val authRepository: AuthRepository,
    private val prefsDataStore: PrefsDataStore,
) : ViewModel() {

    var displayName by mutableStateOf("")
        private set

    var uiState by mutableStateOf<OnboardingUiState>(OnboardingUiState.Idle)
        private set

    fun onDisplayNameChanged(value: String) {
        displayName = value
    }

    fun createFamily() {
        if (displayName.isBlank()) return
        uiState = OnboardingUiState.Loading
        viewModelScope.launch {
            when (val result = familyRepository.createFamily(displayName.trim())) {
                is CreateFamilyResult.Success -> {
                    prefsDataStore.saveOnboarding(result.code, authRepository.ensureSignedIn())
                    uiState = OnboardingUiState.FamilyCreated(result.code)
                }
                CreateFamilyResult.Offline -> uiState = OnboardingUiState.Error("You're offline — connect and try again")
                CreateFamilyResult.Failed -> uiState = OnboardingUiState.Error("Something went wrong — try again")
            }
        }
    }

    fun joinFamily(rawCode: String) {
        if (displayName.isBlank()) return
        val code = FamilyCodeGenerator.normalize(rawCode)
        if (code.isBlank()) return
        uiState = OnboardingUiState.Loading
        viewModelScope.launch {
            when (val result = familyRepository.joinFamily(code, displayName.trim())) {
                JoinFamilyResult.Success -> {
                    prefsDataStore.saveOnboarding(code, authRepository.ensureSignedIn())
                    uiState = OnboardingUiState.Joined
                }
                JoinFamilyResult.NotFound -> uiState = OnboardingUiState.Error("Family not found — check the code and try again")
                JoinFamilyResult.Offline -> uiState = OnboardingUiState.Error("You're offline — connect and try again")
                JoinFamilyResult.Failed -> uiState = OnboardingUiState.Error("Something went wrong — try again")
            }
        }
    }
}
