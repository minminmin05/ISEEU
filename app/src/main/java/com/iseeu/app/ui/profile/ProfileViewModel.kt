package com.iseeu.app.ui.profile

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iseeu.app.data.local.PrefsDataStore
import com.iseeu.app.data.repository.AuthRepository
import com.iseeu.app.data.repository.MemberRepository
import com.iseeu.app.service.LocationForegroundService
import com.iseeu.app.util.AvatarColorPalette
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val memberRepository: MemberRepository,
    private val authRepository: AuthRepository,
    private val prefsDataStore: PrefsDataStore,
) : ViewModel() {

    var displayName by mutableStateOf("")
        private set
    var avatarColor by mutableStateOf(AvatarColorPalette.colors.first())
        private set
    var isSharingEnabled by mutableStateOf(true)
        private set
    var familyCode by mutableStateOf("")
        private set

    init {
        viewModelScope.launch {
            val code = prefsDataStore.familyCode.first().orEmpty()
            familyCode = code
            isSharingEnabled = prefsDataStore.isSharingEnabled.first()

            val uid = authRepository.ensureSignedIn()
            memberRepository.getMemberOnce(code, uid)?.let { member ->
                displayName = member.displayName
                avatarColor = member.avatarColor
            }
        }
    }

    fun onDisplayNameChanged(value: String) {
        displayName = value
    }

    fun onAvatarColorChanged(value: String) {
        avatarColor = value
    }

    fun save() {
        if (displayName.isBlank()) return
        viewModelScope.launch {
            val code = prefsDataStore.familyCode.first() ?: return@launch
            val uid = authRepository.ensureSignedIn()
            memberRepository.updateProfile(code, uid, displayName.trim(), avatarColor)
        }
    }

    fun setSharingEnabled(enabled: Boolean, context: Context) {
        isSharingEnabled = enabled

        if (enabled) {
            LocationForegroundService.start(context)
        } else {
            LocationForegroundService.stop(context)
        }

        viewModelScope.launch {
            prefsDataStore.setSharingEnabled(enabled)
            val code = prefsDataStore.familyCode.first() ?: return@launch
            val uid = authRepository.ensureSignedIn()
            memberRepository.setVisibility(code, uid, enabled)
        }
    }
}
