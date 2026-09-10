package com.iseeu.app.ui.onboarding

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iseeu.app.R
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
    // Holds a string resource id (not text) so the ViewModel doesn't need a Context — the
    // Composable resolves it with stringResource().
    data class Error(@StringRes val messageRes: Int) : OnboardingUiState
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
                CreateFamilyResult.Offline -> uiState = OnboardingUiState.Error(R.string.error_offline)
                CreateFamilyResult.Failed -> uiState = OnboardingUiState.Error(R.string.error_generic)
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
                JoinFamilyResult.NotFound -> uiState = OnboardingUiState.Error(R.string.error_family_not_found)
                JoinFamilyResult.Offline -> uiState = OnboardingUiState.Error(R.string.error_offline)
                JoinFamilyResult.Failed -> uiState = OnboardingUiState.Error(R.string.error_generic)
            }
        }
    }
}
