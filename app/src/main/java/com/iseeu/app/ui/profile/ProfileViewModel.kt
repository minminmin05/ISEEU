package com.iseeu.app.ui.profile

import android.content.Context
import android.net.Uri
import com.google.firebase.FirebaseException
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iseeu.app.R
import com.iseeu.app.data.local.PrefsDataStore
import com.iseeu.app.data.repository.AuthRepository
import com.iseeu.app.data.repository.MemberRepository
import com.iseeu.app.service.LocationForegroundService
import com.iseeu.app.util.AvatarColorPalette
import com.iseeu.app.util.AvatarImageProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    var avatarPhotoBase64 by mutableStateOf<String?>(null)
        private set
    var isUploadingPhoto by mutableStateOf(false)
        private set
    @get:StringRes
    var photoErrorRes by mutableStateOf<Int?>(null)
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

            try {
                val uid = authRepository.ensureSignedIn()
                memberRepository.getMemberOnce(code, uid)?.let { member ->
                    displayName = member.displayName
                    avatarColor = member.avatarColor
                    avatarPhotoBase64 = member.avatarPhotoBase64
                }
            } catch (e: FirebaseException) {
                // leave the local defaults in place; the user can still edit and retry save()
            }
        }
    }

    fun onDisplayNameChanged(value: String) {
        displayName = value
    }

    fun onAvatarColorChanged(value: String) {
        avatarColor = value
    }

    fun onPhotoPicked(imageUri: Uri, context: Context) {
        viewModelScope.launch {
            isUploadingPhoto = true
            photoErrorRes = null
            val code = prefsDataStore.familyCode.first()
            if (code == null) {
                isUploadingPhoto = false
                return@launch
            }
            try {
                val encoded = withContext(Dispatchers.IO) {
                    AvatarImageProcessor.compressToBase64(context, imageUri)
                } ?: throw IllegalStateException("could not decode picked image")
                val uid = authRepository.ensureSignedIn()
                memberRepository.updateAvatarPhoto(code, uid, encoded)
                avatarPhotoBase64 = encoded
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "photo upload failed", e)
                photoErrorRes = R.string.profile_photo_upload_error
            } finally {
                isUploadingPhoto = false
            }
        }
    }

    fun save() {
        if (displayName.isBlank()) return
        viewModelScope.launch {
            val code = prefsDataStore.familyCode.first() ?: return@launch
            try {
                val uid = authRepository.ensureSignedIn()
                memberRepository.updateProfile(code, uid, displayName.trim(), avatarColor)
            } catch (e: FirebaseException) {
                // no retry/error UI in Phase 1 — the edit just silently doesn't persist this attempt
            }
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
            try {
                val uid = authRepository.ensureSignedIn()
                memberRepository.setVisibility(code, uid, enabled)
            } catch (e: FirebaseException) {
                // the service start/stop above already reflects the toggle locally; the Firestore
                // side will catch up next time this succeeds
            }
        }
    }
}
